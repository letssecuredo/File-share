package {PACKAGE_NAME}

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class FileShareActivity : AppCompatActivity() {

    private val permissionRequestCode = 3001

    override fun onCreate(savedInstanceState: Bundle?) {
        SettingsManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_share)

        // Request essential permissions
        requestEssentialPermissions()

        val btnSend = findViewById<MaterialButton>(R.id.btnSend)
        val btnReceive = findViewById<MaterialButton>(R.id.btnReceive)
        val btnHistory = findViewById<MaterialCardView>(R.id.btnHistory)
        val btnSettings = findViewById<MaterialCardView>(R.id.btnSettings)

        btnSend.setOnClickListener {
            vibrate()
            startActivity(Intent(this, SendActivity::class.java))
        }

        btnReceive.setOnClickListener {
            vibrate()
            startActivity(Intent(this, ReceiveActivity::class.java))
        }

        btnHistory.setOnClickListener {
            vibrate()
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        btnSettings.setOnClickListener {
            vibrate()
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun vibrate() {
        if (SettingsManager.isVibrateEnabled(this)) {
            window.decorView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    private fun requestEssentialPermissions() {
        val perms = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.CAMERA)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (perms.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, perms.toTypedArray(), permissionRequestCode)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh theme in case user changed it
        SettingsManager.applyTheme(this)
    }
}
