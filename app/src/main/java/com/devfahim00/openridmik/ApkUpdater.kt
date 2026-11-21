package com.devfahim00.openridmik

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Downloads the latest Sum Keyboard APK in-app with live progress and
 * launches Android's native package installer directly without leaving the app.
 */
object ApkUpdater {

    private val downloading = AtomicBoolean(false)
    private val main = Handler(Looper.getMainLooper())

    fun downloadAndInstall(
        activity: Activity,
        apkUrl: String,
        onProgress: (String) -> Unit,
    ) {
        if (!downloading.compareAndSet(false, true)) {
            Toast.makeText(activity, "Download already in progress…", Toast.LENGTH_SHORT).show()
            return
        }

        val app = activity.applicationContext
        main.post { onProgress("Starting download…") }

        Thread {
            try {
                val target = ApkFileProvider.apkFile(app.cacheDir)
                val temp = File(app.cacheDir, "${ApkFileProvider.UPDATE_APK_NAME}.part")
                downloadWithRedirects(apkUrl, temp) { pct ->
                    main.post {
                        onProgress(if (pct >= 0) "Downloading… $pct%" else "Downloading…")
                    }
                }
                if (target.exists()) target.delete()
                if (!temp.renameTo(target)) {
                    temp.copyTo(target, overwrite = true)
                    temp.delete()
                }
                main.post {
                    onProgress("Ready · Opening installer…")
                    launchInstaller(activity)
                }
            } catch (e: Exception) {
                main.post {
                    onProgress("Download failed · Tap to retry")
                    Toast.makeText(activity, "Update download failed: ${e.message ?: "network error"}", Toast.LENGTH_LONG).show()
                }
            } finally {
                downloading.set(false)
            }
        }.start()
    }

    private fun downloadWithRedirects(startUrl: String, outFile: File, onPct: (Int) -> Unit) {
        var currentUrl = startUrl
        var redirects = 0
        while (redirects < 6) {
            val conn = URL(currentUrl).openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.setRequestProperty("User-Agent", "MyAvroKeyboard-updater")
            try {
                val code = conn.responseCode
                if (code in 301..303 || code == 307 || code == 308) {
                    val loc = conn.getHeaderField("Location")
                        ?: throw IllegalStateException("Redirect missing Location")
                    currentUrl = URL(URL(currentUrl), loc).toString()
                    redirects++
                    continue
                }
                if (code != HttpURLConnection.HTTP_OK) {
                    throw IllegalStateException("HTTP $code")
                }
                val total = conn.contentLengthLong
                var readTotal = 0L
                var lastPct = -1
                conn.inputStream.use { input ->
                    FileOutputStream(outFile).use { output ->
                        val buf = ByteArray(16384)
                        while (true) {
                            val n = input.read(buf)
                            if (n <= 0) break
                            output.write(buf, 0, n)
                            readTotal += n
                            if (total > 0) {
                                val pct = ((readTotal * 100L) / total).toInt().coerceIn(0, 100)
                                if (pct != lastPct) {
                                    lastPct = pct
                                    onPct(pct)
                                }
                            } else if (lastPct == -1) {
                                lastPct = 0
                                onPct(-1)
                            }
                        }
                    }
                }
                return
            } finally {
                conn.disconnect()
            }
        }
        throw IllegalStateException("Too many redirects")
    }

    private fun launchInstaller(activity: Activity) {
        if (!activity.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(
                activity,
                "Allow 'Install unknown apps' for Sum Keyboard, then tap Update again",
                Toast.LENGTH_LONG,
            ).show()
            try {
                val permIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${activity.packageName}"),
                )
                activity.startActivity(permIntent)
            } catch (_: Exception) {
                activity.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
            }
            return
        }

        val uri = ApkFileProvider.apkUri(activity.packageName)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(installIntent)
    }
}
