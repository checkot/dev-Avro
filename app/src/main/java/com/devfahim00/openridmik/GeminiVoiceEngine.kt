package com.devfahim00.openridmik

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.sqrt

/**
 * Continuous Auto-Pause Live Speech-to-Text engine powered by Gemini API.
 *
 * Uses Android's native [AudioRecord] (16 kHz, 16-bit mono PCM -> WAV) so the microphone
 * stays continuously open with zero dropped words between sentences:
 * - Detects natural ~1.1s pauses between sentences and streams each chunk in exact order.
 * - Uses minimal thinking budget on Gemini Flash-Lite for fast (~1s) transcription.
 * - Keeps all audio in RAM only (never writes voice recordings to disk).
 */
object GeminiVoiceEngine {

    const val KEY_API = "gemini_api_key"
    const val KEY_MODEL = "gemini_model"
    const val KEY_LANG = "gemini_voice_lang"
    const val KEY_CLEAN = "gemini_voice_clean"

    const val DEFAULT_MODEL = "gemini-3.5-flash-lite"

    private const val SAMPLE_RATE = 16000
    private const val SPEECH_RMS_THRESHOLD = 750.0
    private const val SILENCE_TRIGGER_MS = 1100L
    private const val MIN_SPEECH_MS = 350L
    private const val MAX_CHUNK_MS = 14000L

    enum class State { IDLE, RECORDING, TRANSCRIBING }

    @Volatile
    var state: State = State.IDLE
        private set

    private val running = AtomicBoolean(false)
    private val pendingChunks = AtomicInteger(0)
    private var captureThread: Thread? = null
    private var uploadExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun hasMicPermission(c: Context): Boolean =
        c.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    fun apiKey(c: Context): String = Prefs.getString(c, KEY_API, "").trim()

    fun model(c: Context): String = Prefs.getString(c, KEY_MODEL, DEFAULT_MODEL).trim().ifEmpty { DEFAULT_MODEL }

    fun langMode(c: Context): String = Prefs.getString(c, KEY_LANG, "auto").ifEmpty { "auto" }

    fun cleanPunctuation(c: Context): Boolean = Prefs.getString(c, KEY_CLEAN, "1") != "0"

    fun isConfigured(c: Context): Boolean = hasMicPermission(c) && apiKey(c).isNotEmpty()

    /**
     * Starts continuous live listening. Each spoken phrase is automatically flushed on
     * a short pause (or when stopped) and delivered to [onChunkText] in exact spoken order.
     */
    fun startLiveSession(
        c: Context,
        onChunkText: (String) -> Unit,
        onStateChanged: () -> Unit,
        onError: (String) -> Unit,
    ): String? {
        if (running.get()) return null
        if (!hasMicPermission(c)) return "Microphone permission is required."
        val key = apiKey(c)
        if (key.isEmpty()) return "Please add your free Gemini API key in AI Voice Settings."

        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuf <= 0) return "Audio recording is not supported on this device."

        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuf * 2, SAMPLE_RATE),
            )
        } catch (e: Exception) {
            return "Could not initialize microphone: " + (e.message ?: "unknown error")
        }

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return "Could not initialize microphone."
        }

        val app = c.applicationContext
        val mdl = model(app)
        val prompt = buildPrompt(langMode(app), cleanPunctuation(app))

        if (uploadExecutor.isShutdown) {
            uploadExecutor = Executors.newSingleThreadExecutor()
        }

        running.set(true)
        state = State.RECORDING
        main.post { onStateChanged() }

        captureThread = Thread {
            val pcmBuffer = ByteArrayOutputStream(SAMPLE_RATE * 4)
            val frame = ShortArray(800) // 50ms @ 16kHz
            var hasSpeech = false
            var speechMs = 0L
            var silenceMs = 0L
            var chunkMs = 0L

            try {
                recorder.startRecording()
                while (running.get()) {
                    val read = recorder.read(frame, 0, frame.size)
                    if (read <= 0) continue

                    val frameMs = (read * 1000L) / SAMPLE_RATE
                    val rms = computeRms(frame, read)

                    if (rms >= SPEECH_RMS_THRESHOLD) {
                        hasSpeech = true
                        speechMs += frameMs
                        silenceMs = 0L
                    } else if (hasSpeech) {
                        silenceMs += frameMs
                    }

                    if (hasSpeech) {
                        appendLittleEndian(pcmBuffer, frame, read)
                        chunkMs += frameMs
                    }

                    val pauseReached = hasSpeech && speechMs >= MIN_SPEECH_MS && silenceMs >= SILENCE_TRIGGER_MS
                    val maxReached = hasSpeech && chunkMs >= MAX_CHUNK_MS

                    if (pauseReached || maxReached) {
                        val pcmBytes = pcmBuffer.toByteArray()
                        pcmBuffer.reset()
                        hasSpeech = false
                        speechMs = 0L
                        silenceMs = 0L
                        chunkMs = 0L
                        enqueueChunk(pcmBytes, key, mdl, prompt, onChunkText, onStateChanged, onError)
                    }
                }

                // Flush remaining spoken audio when user taps Mic to stop
                if (hasSpeech && speechMs >= MIN_SPEECH_MS && pcmBuffer.size() > 0) {
                    val pcmBytes = pcmBuffer.toByteArray()
                    pcmBuffer.reset()
                    enqueueChunk(pcmBytes, key, mdl, prompt, onChunkText, onStateChanged, onError)
                }
            } catch (e: Exception) {
                val msg = e.message ?: "recording error"
                main.post { onError("Mic error: $msg") }
            } finally {
                try { recorder.stop() } catch (_: Exception) {}
                try { recorder.release() } catch (_: Exception) {}
                running.set(false)
                updateState(onStateChanged)
            }
        }
        captureThread?.start()
        return null
    }

    /** Stops live recording cleanly and lets any final spoken phrase finish transcribing. */
    fun stopLiveSession(onStateChanged: () -> Unit) {
        running.set(false)
        updateState(onStateChanged)
    }

    /** Immediately stops recording without waiting (used when keyboard closes). */
    fun cancel() {
        running.set(false)
        if (pendingChunks.get() == 0) {
            state = State.IDLE
        }
    }

    private fun updateState(onStateChanged: () -> Unit) {
        state = when {
            running.get() -> State.RECORDING
            pendingChunks.get() > 0 -> State.TRANSCRIBING
            else -> State.IDLE
        }
        main.post { onStateChanged() }
    }

    private fun enqueueChunk(
        pcmBytes: ByteArray,
        key: String,
        mdl: String,
        prompt: String,
        onChunkText: (String) -> Unit,
        onStateChanged: () -> Unit,
        onError: (String) -> Unit,
    ) {
        pendingChunks.incrementAndGet()
        updateState(onStateChanged)
        val wavBytes = wrapWavHeader(pcmBytes, SAMPLE_RATE)

        uploadExecutor.execute {
            try {
                val text = callGeminiTranscribe(key, mdl, prompt, wavBytes)
                if (text.isNotEmpty()) {
                    main.post { onChunkText(text) }
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Gemini transcription failed."
                main.post { onError(msg) }
            } finally {
                pendingChunks.decrementAndGet()
                updateState(onStateChanged)
            }
        }
    }

    private fun computeRms(buf: ShortArray, count: Int): Double {
        var sum = 0.0
        for (i in 0 until count) {
            val v = buf[i].toDouble()
            sum += v * v
        }
        return sqrt(sum / count.coerceAtLeast(1))
    }

    private fun appendLittleEndian(out: ByteArrayOutputStream, buf: ShortArray, count: Int) {
        for (i in 0 until count) {
            val s = buf[i].toInt()
            out.write(s and 0xFF)
            out.write((s shr 8) and 0xFF)
        }
    }

    private fun wrapWavHeader(pcm: ByteArray, sampleRate: Int): ByteArray {
        val totalDataLen = pcm.size + 36
        val byteRate = sampleRate * 2 // 16-bit mono
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        writeIntLE(header, 4, totalDataLen)
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        writeIntLE(header, 16, 16)
        writeShortLE(header, 20, 1) // PCM
        writeShortLE(header, 22, 1) // Mono
        writeIntLE(header, 24, sampleRate)
        writeIntLE(header, 28, byteRate)
        writeShortLE(header, 32, 2) // Block align
        writeShortLE(header, 34, 16) // Bits per sample
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        writeIntLE(header, 40, pcm.size)

        val wav = ByteArray(44 + pcm.size)
        System.arraycopy(header, 0, wav, 0, 44)
        System.arraycopy(pcm, 0, wav, 44, pcm.size)
        return wav
    }

    private fun writeIntLE(b: ByteArray, offset: Int, v: Int) {
        b[offset] = (v and 0xFF).toByte()
        b[offset + 1] = ((v shr 8) and 0xFF).toByte()
        b[offset + 2] = ((v shr 16) and 0xFF).toByte()
        b[offset + 3] = ((v shr 24) and 0xFF).toByte()
    }

    private fun writeShortLE(b: ByteArray, offset: Int, v: Int) {
        b[offset] = (v and 0xFF).toByte()
        b[offset + 1] = ((v shr 8) and 0xFF).toByte()
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
                        onResult(false, "Model '$mdl' not found (HTTP 404). Try gemini-3.5-flash-lite.")
                    } else if (code == 429) {
                        onResult(false, "Rate limit reached (HTTP 429). Wait a moment and try again.")
                    } else {
                        onResult(false, "API check failed (HTTP $code).")
                    }
                }
            } catch (e: Exception) {
                val msg = e.message ?: "check internet"
                main.post { onResult(false, "Network error: $msg") }
            }
        }.start()
    }

    private fun buildPrompt(lang: String, clean: Boolean): String {
        val langRule = when (lang) {
            "bn" -> "Transcribe the spoken audio strictly in natural Bengali (Bangla script)."
            "en" -> "Transcribe the spoken audio strictly in natural English."
            else -> "Transcribe the spoken audio accurately. Write Bengali in Bangla script; keep English technical words naturally; write English in English."
        }
        val cleanRule = if (clean) {
            "Add natural punctuation and remove filler sounds."
        } else {
            "Keep verbatim with minimal edits."
        }
        return "$langRule $cleanRule Return ONLY the transcribed text. If silence or noise only, return empty string."
    }

    private fun callGeminiTranscribe(apiKey: String, model: String, prompt: String, wavBytes: ByteArray): String {
        val encodedKey = URLEncoder.encode(apiKey, "UTF-8")
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$encodedKey")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 12000
        conn.readTimeout = 25000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")

        val b64 = Base64.encodeToString(wavBytes, Base64.NO_WRAP)
        val parts = JSONArray()
            .put(JSONObject().put("text", prompt))
            .put(
                JSONObject().put(
                    "inline_data",
                    JSONObject()
                        .put("mime_type", "audio/wav")
                        .put("data", b64),
                ),
            )
        val genConfig = JSONObject()
        if (model.startsWith("gemini-3")) {
            genConfig.put("thinkingConfig", JSONObject().put("thinkingLevel", "MINIMAL"))
        }
        val payload = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", parts)))
            .put("generationConfig", genConfig)

        try {
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val resp = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val msg = when (code) {
                    400, 403 -> "Invalid Gemini API key (HTTP $code)."
                    404 -> "Model '$model' not found (HTTP 404)."
                    429 -> "Free quota limit reached (HTTP 429). Try again shortly."
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

        val capsule = android.graphics.RectF(x(9f), y(2f), x(15f), y(13f))
        if (recording) {
            canvas.drawRoundRect(capsule, 3f * unit, 3f * unit, fill)
        } else {
            canvas.drawRoundRect(capsule, 3f * unit, 3f * unit, stroke)
        }
        val arc = android.graphics.RectF(x(5.5f), y(6f), x(18.5f), y(17.5f))
        canvas.drawArc(arc, 0f, 180f, false, stroke)
        canvas.drawLine(x(12f), y(17.5f), x(12f), y(21.5f), stroke)
        canvas.drawLine(x(8.5f), y(21.5f), x(15.5f), y(21.5f), stroke)

        if (recording) {
            fill.color = 0xFFEF4444.toInt()
            canvas.drawCircle(x(20f), y(4f), 2.6f * unit, fill)
        }
    }
}
