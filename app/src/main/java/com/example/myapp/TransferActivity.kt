package {PACKAGE_NAME}

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch

class TransferActivity : AppCompatActivity() {

    private var client: TcpClient? = null
    private var startTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        SettingsManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer)

        startTime = System.currentTimeMillis()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.title = "Receiving"
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        val host = intent.getStringExtra("host") ?: return finish()
        val port = intent.getIntExtra("port", 8888)
        val token = intent.getStringExtra("token") ?: return finish()
        val serverName = intent.getStringExtra("serverName") ?: "Device"
        val fileCount = intent.getIntExtra("fileCount", 0)

        val tvServerName = findViewById<TextView>(R.id.tvServerName)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvProgress = findViewById<TextView>(R.id.tvProgress)
        val progressBar = findViewById<LinearProgressIndicator>(R.id.progressBar)
        val tvCurrentFile = findViewById<TextView>(R.id.tvCurrentFile)

        tvServerName.text = "From: $serverName"
        tvStatus.text = if (fileCount > 0) "Incoming $fileCount file${if (fileCount > 1) "s" else ""}…" else "Connecting…"

        client = TcpClient(host, port, token, SettingsManager.getDeviceName(this))
        lifecycleScope.launch {
            client?.run(object : TcpClient.Listener {

                override fun onConnected(serverName: String) {
                    runOnUiThread {
                        tvStatus.text = "✓ Connected to $serverName"
                    }
                }

                override fun onFileList(files: List<TcpClient.RemoteFile>) {
                    runOnUiThread {
                        val total = files.sumOf { it.size }
                        tvStatus.text = "Receiving ${files.size} file${if (files.size > 1) "s" else ""} · ${FileUtils.formatBytes(total)}"
                    }
                }

                override fun onFileProgress(name: String, received: Long, total: Long, speedBps: Double) {
                    val pct = if (total > 0) (received * 100 / total) else 0
                    runOnUiThread {
                        tvCurrentFile.text = "${FileUtils.getFileIcon(name)} $name"
                        progressBar.setProgressCompat(pct, true)
                        tvProgress.text = "${FileUtils.formatBytes(received)} / ${FileUtils.formatBytes(total)} · ${FileUtils.formatSpeed(speedBps)}"
                    }
                }

                override fun onFileComplete(name: String, savedPath: String) {
                    runOnUiThread {
                        tvCurrentFile.text = "✓ $name"
                        vibrate(20)
                    }
                }

                override fun onAllComplete(saveDir: String) {
                    runOnUiThread {
                        progressBar.setProgressCompat(100, true)
                        vibrate(50)
                        val elapsed = System.currentTimeMillis() - startTime
                        tvStatus.text = "✅ Received successfully in ${elapsed / 1000}s"
                        tvStatus.setTextColor(getColor(R.color.success))
                        Toast.makeText(this@TransferActivity, "Saved to $saveDir", Toast.LENGTH_LONG).show()
                        // Auto-finish after 1.5s
                        tvStatus.postDelayed({ finish() }, 1500)
                    }
                }

                override fun onError(message: String) {
                    runOnUiThread {
                        tvStatus.text = "❌ $message"
                        tvStatus.setTextColor(getColor(R.color.error))
                    }
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client?.stop()
    }

    private fun vibrate(ms: Long) {
        if (!SettingsManager.isVibrateEnabled(this)) return
        try {
            val v = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                v.vibrate(android.os.VibrationEffect.createOneShot(ms, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") v.vibrate(ms)
            }
        } catch (_: Exception) {}
    }
}
