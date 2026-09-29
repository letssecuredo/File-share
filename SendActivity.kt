package {PACKAGE_NAME}

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.io.File

class SendActivity : AppCompatActivity() {

    private var server: TcpServer? = null
    private var copiedFiles: List<File> = emptyList()
    private var port: Int = 8888

    private lateinit var tvIp: TextView
    private lateinit var tvFileCount: TextView
    private lateinit var tvStatus: TextView
    private lateinit var ivQr: ImageView
    private lateinit var btnPickFiles: MaterialButton
    private lateinit var btnCancel: MaterialButton

    private val pickFiles = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@registerForActivityResult
        val files = uris.mapNotNull { FileUtils.copyUriToCache(this, it) }
        if (files.isEmpty()) {
            Toast.makeText(this, "Could not read selected files", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        copiedFiles = files
        startServer()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        SettingsManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)

        port = SettingsManager.getPort(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        tvIp = findViewById(R.id.tvIp)
        tvFileCount = findViewById(R.id.tvFileCount)
        tvStatus = findViewById(R.id.tvStatus)
        ivQr = findViewById(R.id.ivQr)
        btnPickFiles = findViewById(R.id.btnPickFiles)
        btnCancel = findViewById(R.id.btnCancel)

        btnPickFiles.setOnClickListener { pickFiles.launch(arrayOf("*/*")) }
        btnCancel.setOnClickListener { cancelAndFinish() }

        if (!NetworkUtils.isWifiConnected(this)) {
            Toast.makeText(this, "⚠️ Please connect to WiFi first", Toast.LENGTH_LONG).show()
            finish()
            return
        }
    }

    private fun startServer() {
        val ip = NetworkUtils.getLocalIpAddress()
        if (ip == null) {
            Toast.makeText(this, "Could not detect local IP address", Toast.LENGTH_LONG).show()
            return
        }

        val deviceName = SettingsManager.getDeviceName(this)
        val fileMetas = copiedFiles.map { FileMeta(it.name, it.length(), FileUtils.getMimeType(it.name)) }
        val session = SessionManager.createSession(deviceName, ip, port, fileMetas)

        // Generate QR
        val qrData = SessionManager.buildQrString(session)
        val logoBitmap = if (SettingsManager.isQrLogoEnabled(this)) {
            try {
                android.graphics.BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            } catch (e: Exception) { null }
        } else null

        val qrBitmap = QrGenerator.generate(qrData, 768, logoBitmap)
        ivQr.setImageBitmap(qrBitmap)

        // Animate QR appearance
        ivQr.alpha = 0f
        ivQr.animate().alpha(1f).setDuration(400).start()

        tvIp.text = "🌐 $ip:$port"
        val totalSize = copiedFiles.sumOf { it.length() }
        tvFileCount.text = "${copiedFiles.size} file${if (copiedFiles.size > 1) "s" else ""} · ${FileUtils.formatBytes(totalSize)}"
        tvStatus.text = "Waiting for receiver to scan…"

        btnPickFiles.text = "Change files"

        // Start server
        server = TcpServer(port, session, copiedFiles)
        lifecycleScope.launch {
            server?.start(object : TcpServer.Listener {
                override fun onClientConnected(remoteName: String) {
                    runOnUiThread {
                        vibrate(30)
                        tvStatus.text = "🔗 Connected to $remoteName"
                        tvStatus.setTextColor(getColor(R.color.success))
                    }
                }

                override fun onFileProgress(fileName: String, bytesSent: Long, totalBytes: Long, speedBps: Double) {
                    val pct = if (totalBytes > 0) (bytesSent * 100 / totalBytes) else 0
                    runOnUiThread {
                        tvStatus.text = "📤 $fileName · $pct% · ${FileUtils.formatSpeed(speedBps)}"
                    }
                }

                override fun onFileComplete(fileName: String) {
                    runOnUiThread {
                        tvStatus.text = "✓ Sent: $fileName"
                    }
                }

                override fun onAllComplete() {
                    runOnUiThread {
                        vibrate(50)
                        Toast.makeText(this@SendActivity, "✅ All files sent successfully", Toast.LENGTH_LONG).show()
                        finish()
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

    private fun cancelAndFinish() {
        server?.stop()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
    }

    private fun vibrate(ms: Long = 15) {
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
