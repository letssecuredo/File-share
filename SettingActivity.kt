package {PACKAGE_NAME}

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        SettingsManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // ─── Device Name ───
        val etDeviceName = findViewById<TextInputEditText>(R.id.etDeviceName)
        etDeviceName.setText(SettingsManager.getDeviceName(this))

        // ─── Theme ───
        val rbSystem = findViewById<MaterialRadioButton>(R.id.rbThemeSystem)
        val rbLight = findViewById<MaterialRadioButton>(R.id.rbThemeLight)
        val rbDark = findViewById<MaterialRadioButton>(R.id.rbThemeDark)

        when (SettingsManager.getTheme(this)) {
            SettingsManager.THEME_LIGHT -> rbLight.isChecked = true
            SettingsManager.THEME_DARK -> rbDark.isChecked = true
            else -> rbSystem.isChecked = true
        }

        // ─── Toggles ───
        val swAutoAccept = findViewById<MaterialSwitch>(R.id.swAutoAccept)
        val swVibrate = findViewById<MaterialSwitch>(R.id.swVibrate)
        val swSound = findViewById<MaterialSwitch>(R.id.swSound)
        val swKeepAwake = findViewById<MaterialSwitch>(R.id.swKeepAwake)
        val swQrLogo = findViewById<MaterialSwitch>(R.id.swQrLogo)

        swAutoAccept.isChecked = SettingsManager.isAutoAccept(this)
        swVibrate.isChecked = SettingsManager.isVibrateEnabled(this)
        swSound.isChecked = SettingsManager.isSoundEnabled(this)
        swKeepAwake.isChecked = SettingsManager.isKeepAwake(this)
        swQrLogo.isChecked = SettingsManager.isQrLogoEnabled(this)

        // ─── Port ───
        val etPort = findViewById<TextInputEditText>(R.id.etPort)
        etPort.setText(SettingsManager.getPort(this).toString())

        // ─── Save Button ───
        val btnSave = findViewById<MaterialButton>(R.id.btnSave)
        btnSave.setOnClickListener {
            val name = etDeviceName.text?.toString()?.trim().orEmpty()
            if (name.isBlank()) {
                Toast.makeText(this, "Device name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            SettingsManager.setDeviceName(this, name)

            val port = etPort.text?.toString()?.toIntOrNull() ?: 8888
            if (port !in 1024..65535) {
                Toast.makeText(this, "Port must be between 1024-65535", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            SettingsManager.setPort(this, port)

            val newTheme = when {
                rbLight.isChecked -> SettingsManager.THEME_LIGHT
                rbDark.isChecked -> SettingsManager.THEME_DARK
                else -> SettingsManager.THEME_SYSTEM
            }
            SettingsManager.setTheme(this, newTheme)

            SettingsManager.setAutoAccept(this, swAutoAccept.isChecked)
            SettingsManager.setVibrateEnabled(this, swVibrate.isChecked)
            SettingsManager.setSoundEnabled(this, swSound.isChecked)
            SettingsManager.setKeepAwake(this, swKeepAwake.isChecked)
            SettingsManager.setQrLogoEnabled(this, swQrLogo.isChecked)

            Toast.makeText(this, "✅ Settings saved", Toast.LENGTH_SHORT).show()
            finish()
        }

        // ─── Reset Button ───
        val btnReset = findViewById<MaterialButton>(R.id.btnReset)
        btnReset.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Reset all settings?")
                .setMessage("This will restore all settings to default values.")
                .setPositiveButton("Reset") { _, _ ->
                    SettingsManager.resetAll(this)
                    Toast.makeText(this, "Settings reset", Toast.LENGTH_SHORT).show()
                    recreate()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // ─── Clear History ───
        val btnClearHistory = findViewById<MaterialButton>(R.id.btnClearHistory)
        btnClearHistory.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Clear history?")
                .setMessage("All transfer history will be deleted.")
                .setPositiveButton("Clear") { _, _ ->
                    HistoryManager.clear(this)
                    Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
