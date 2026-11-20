package com.devfahim00.openridmik

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Records short voice clips using Android's native [MediaRecorder] (AAC/M4A) and
 * transcribes them via the Gemini Developer REST API (zero external dependencies).
 *
 * Privacy & safety:
 * - API key stays strictly on-device in private SharedPreferences.
 * - Temporary audio file is always deleted immediately after transcription or error.
 */
object GeminiVoiceEngine {

    const val KEY_API = "gemini_api_key"
    const val KEY_MODEL = "gemini_model"
    const val KEY_LANG = "gemini_voice_lang"
    const val KEY_CLEAN = "gemini_voice_clean"

    const val DEFAULT_MODEL = "gemini-2.5-flash"

    enum class State { IDLE, RECORDING, TRANSCRIBING }

    @Volatile
    var state: State = State.IDLE
        private set

    private var recorder: MediaRecorder? = null
    private var audioFile: File? = null
    private val main = Handler(Looper.getMainLooper())

    fun hasMicPermission(c: Context): Boolean =
        c.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    fun apiKey(c: Context): String = Prefs.getString(c, KEY_API, "").trim()

    fun model(c: Context): String = Prefs.getString(c, KEY_MODEL, DEFAULT_MODEL).trim().ifEmpty { DEFAULT_MODEL }

    fun langMode(c: Context): String = Prefs.getString(c, KEY_LANG, "auto").ifEmpty { "auto" }

    fun cleanPunctuation(c: Context): Boolean = Prefs.getString(c, KEY_CLEAN, "1") != "0"

    fun isConfigured(c: Context): Boolean = hasMicPermission(c) && apiKey(c).isNotEmpty()

    /** Starts recording to a temporary M4A file in cacheDir. Returns null on success or an error message. */
    fun startRecording(c: Context): String? {
        if (state != State.IDLE) return "Voice engine is busy."
        if (!hasMicPermission(c)) return "Microphone permission is required."
        if (apiKey(c).isEmpty()) return "Please add your free Gemini API key in AI Voice Settings."

        return try {
            val out = File(c.cacheDir, "myavro_voice.m4a")
            if (out.exists()) out.delete()

            @Suppress("DEPRECATION")
            val mr = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(c) else MediaRecorder()
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioSamplingRate(16000)
            mr.setAudioEncodingBitRate(32000)
            mr.setAudioChannels(1)
            mr.setMaxDuration(60_000)
            mr.setOutputFile(out.absolutePath)
            mr.prepare()
            mr.start()

            recorder = mr
            audioFile = out
            state = State.RECORDING
            null
        } catch (e: Exception) {
            releaseRecorder()
            state = State.IDLE
            "Could not start microphone: " + (e.message ?: "unknown error")
        }
    }

    /** Stops recording and transcribes the captured audio asynchronously via Gemini API. */
    fun stopAndTranscribe(
        c: Context,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (state != State.RECORDING) {
            onError("Not currently recording.")
            return
        }
        val app = c.applicationContext
        val file = audioFile
        try {
            recorder?.stop()
        } catch (_: Exception) {
            releaseRecorder()
            file?.delete()
            state = State.IDLE
            onError("Recording was too short. Please speak and tap Mic again.")
            return
        }
        releaseRecorder()

        if (file == null || !file.exists() || file.length() < 512L) {
            file?.delete()
            state = State.IDLE
            onError("No speech captured. Please try again.")
            return
        }

        state = State.TRANSCRIBING
        val key = apiKey(app)
        val mdl = model(app)
        val prompt = buildPrompt(langMode(app), cleanPunctuation(app))

        Thread {
            try {
                val bytes = file.readBytes()
                val text = callGeminiTranscribe(key, mdl, prompt, bytes)
                main.post {
                    state = State.IDLE
                    if (text.isEmpty()) {
                        onError("Could not detect speech in the recording.")
                    } else {
                        onSuccess(text)
                    }
                }
            } catch (e: Exception) {
                main.post {
                    state = State.IDLE
                    onError(e.message ?: "Gemini transcription failed.")
                }
            } finally {
                try { file.delete() } catch (_: Exception) {}
                audioFile = null
            }
        }.start()
    }

    /** Immediately cancels any active recording and deletes temporary audio. */
    fun cancel() {
        try {
            if (state == State.RECORDING) {
                recorder?.stop()
            }
        } catch (_: Exception) {
        }
        releaseRecorder()
        try { audioFile?.delete() } catch (_: Exception) {}
        audioFile = null
        if (state == State.RECORDING) state = State.IDLE
    }

    /** Quick text-only API key test used from VoiceSettingsActivity. */
    fun testApiKey(c: Context, onResult: (Boolean, String) -> Unit) {
        val key = apiKey(c)
        val mdl = model(c)
        if (key.isEmpty()) {
            onResult(false, "Please save an API key first.")
            return
        }
        Thread {
            try {
                val encodedKey = URLEncoder.encode(key, "UTF-8")
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$mdl:generateContent?key=$encodedKey")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 10000
                conn.readTimeout = 15000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                val body = JSONObject().put(
                    "contents",
                    JSONArray().put(
                        JSONObject().put(
                            "parts",
                            JSONArray().put(JSONObject().put("text", "Reply with OK")),
                        ),
                    ),
                )
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                conn.disconnect()
                main.post {
                    if (code == 200) {
                        onResult(true, "Connected! Model '$mdl' is ready.")
                    } else if (code == 400 || code == 403) {
                        onResult(false, "Invalid API key (HTTP $code). Check your key in Google AI Studio.")
                    } else if (code == 404) {
                        onResult(false, "Model '$mdl' not found (HTTP 404). Try another Flash model ID.")
                    } else if (code == 429) {
                        onResult(false, "Rate limit reached (HTTP 429). Wait a moment and try again.")
                    } else {
                        onResult(false, "API check failed (HTTP $code).")
                    }
                }
            } catch (e: Exception) {
                main.post { onResult(false, "Network error: " + (e.message ?: "check internet")) }
            }
        }.start()
    }

    private fun releaseRecorder() {
        try { recorder?.reset() } catch (_: Exception) {}
        try { recorder?.release() } catch (_: Exception) {}
        recorder = null
    }

    private fun buildPrompt(lang: String, clean: Boolean): String {
        val langRule = when (lang) {
            "bn" -> "Transcribe the spoken audio strictly in natural Bengali (Bangla script)."
            "en" -> "Transcribe the spoken audio strictly in natural English."
            else -> "Transcribe the spoken audio accurately. If the speaker speaks Bengali (Bangla), write in Bangla script; if they mix English technical words, keep those English words naturally; if they speak English, write in English."
        }
        val cleanRule = if (clean) {
            "Add natural punctuation (। , ? !) and remove filler sounds like um/uh."
        } else {
            "Keep the transcription verbatim with minimal edits."
        }
        return "$langRule $cleanRule Output ONLY the final transcribed text with no quotes, no prefixes, and no explanations."
    }

    private fun callGeminiTranscribe(apiKey: String, model: String, prompt: String, audioBytes: ByteArray): String {
        val encodedKey = URLEncoder.encode(apiKey, "UTF-8")
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$encodedKey")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")

        val b64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
        val parts = JSONArray()
            .put(JSONObject().put("text", prompt))
            .put(
                JSONObject().put(
                    "inline_data",
                    JSONObject()
                        .put("mime_type", "audio/mp4")
                        .put("data", b64),
                ),
            )
        val payload = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", parts)))
            .put("generationConfig", JSONObject().put("temperature", 0.1))

        try {
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val resp = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val msg = when (code) {
                    400, 403 -> "Invalid Gemini API key (HTTP $code)."
                    404 -> "Gemini model '$model' not found (HTTP 404)."
                    429 -> "Gemini free quota limit reached (HTTP 429). Try again shortly."
                    else -> "Gemini API error (HTTP $code)."
                }
                throw IllegalStateException(msg)
            }
            val json = JSONObject(resp)
            val candidates = json.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""
            val content = candidates.getJSONObject(0).optJSONObject("content") ?: return ""
            val outParts = content.optJSONArray("parts") ?: return ""
            val sb = StringBuilder()
            for (i in 0 until outParts.length()) {
                val t = outParts.getJSONObject(i).optString("text", "")
                if (t.isNotEmpty()) sb.append(t)
            }
            return sb.toString().trim()
        } finally {
            conn.disconnect()
        }
    }
}

/**
 * Crisp 24x24 Canvas-drawn vector microphone icon matching OpenRidmik's IconView style.
 */
class MicIconView(ctx: Context) : android.view.View(ctx) {
    var color: Int = android.graphics.Color.BLACK
        set(v) {
            field = v
            invalidate()
        }
    var recording: Boolean = false
        set(v) {
            field = v
            invalidate()
        }

    private val stroke = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
    }
    private val fill = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.FILL
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        val size = minOf(width, height) * 0.52f
        val unit = size / 24f
        val ox = (width - size) / 2f
        val oy = (height - size) / 2f
        fun x(v: Float) = ox + v * unit
        fun y(v: Float) = oy + v * unit

        stroke.color = color
        stroke.strokeWidth = 2f * unit
        fill.color = color

        // Mic capsule
        val capsule = android.graphics.RectF(x(9f), y(2f), x(15f), y(13f))
        if (recording) {
            canvas.drawRoundRect(capsule, 3f * unit, 3f * unit, fill)
        } else {
            canvas.drawRoundRect(capsule, 3f * unit, 3f * unit, stroke)
        }
        // U-shaped cradle
        val arc = android.graphics.RectF(x(5.5f), y(6f), x(18.5f), y(17.5f))
        canvas.drawArc(arc, 0f, 180f, false, stroke)
        // Stem & base
        canvas.drawLine(x(12f), y(17.5f), x(12f), y(21.5f), stroke)
        canvas.drawLine(x(8.5f), y(21.5f), x(15.5f), y(21.5f), stroke)

        if (recording) {
            fill.color = 0xFFEF4444.toInt()
            canvas.drawCircle(x(20f), y(4f), 2.6f * unit, fill)
        }
    }
}
