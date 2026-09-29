package {PACKAGE_NAME}

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object SettingsManager {

    private const val PREFS_NAME = "wifi_share_prefs"

    // Keys
    private const val KEY_DEVICE_NAME = "device_name"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_AUTO_ACCEPT = "auto_accept"
    private const val KEY_VIBRATE = "vibrate"
    private const val KEY_SOUND = "sound"
    private const val KEY_KEEP_AWAKE = "keep_awake"
    private const val KEY_PORT = "port"
    private const val KEY_QR_LOGO = "qr_logo"

    // Theme values
    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ─── Device Name ───
    fun getDeviceName(context: Context): String {
        val saved = prefs(context).getString(KEY_DEVICE_NAME, null)
        if (!saved.isNullOrBlank()) return saved
        return NetworkUtils.getDefaultDeviceName()
    }

    fun setDeviceName(context: Context, name: String) {
        prefs(context).edit().putString(KEY_DEVICE_NAME, name.trim()).apply()
    }

    // ─── Theme ───
    fun getTheme(context: Context): String =
        prefs(context).getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM

    fun setTheme(context: Context, theme: String) {
        prefs(context).edit().putString(KEY_THEME, theme).apply()
        applyTheme(context, theme)
    }

    fun applyTheme(context: Context, theme: String = getTheme(context)) {
        when (theme) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    // ─── Auto Accept ───
    fun isAutoAccept(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_ACCEPT, false)

    fun setAutoAccept(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_ACCEPT, value).apply()
    }

    // ─── Vibrate ───
    fun isVibrateEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_VIBRATE, true)

    fun setVibrateEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_VIBRATE, value).apply()
    }

    // ─── Sound ───
    fun isSoundEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SOUND, true)

    fun setSoundEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_SOUND, value).apply()
    }

    // ─── Keep Awake ───
    fun isKeepAwake(context: Context): Boolean =
        prefs(context).getBoolean(KEY_KEEP_AWAKE, true)

    fun setKeepAwake(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_KEEP_AWAKE, value).apply()
    }

    // ─── Port ───
    fun getPort(context: Context): Int =
        prefs(context).getInt(KEY_PORT, 8888)

    fun setPort(context: Context, port: Int) {
        prefs(context).edit().putInt(KEY_PORT, port.coerceIn(1024, 65535)).apply()
    }

    // ─── QR Logo ───
    fun isQrLogoEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_QR_LOGO, true)

    fun setQrLogoEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_QR_LOGO, value).apply()
    }

    // ─── Reset ───
    fun resetAll(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
