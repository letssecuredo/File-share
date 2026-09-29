package {PACKAGE_NAME}

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HistoryItem(
    val id: String,
    val type: String,          // "sent" or "received"
    val deviceName: String,
    val fileNames: List<String>,
    val totalBytes: Long,
    val fileCount: Int,
    val timestamp: Long,
    val success: Boolean
)

object HistoryManager {

    private const val PREFS_NAME = "wifi_share_history"
    private const val KEY_HISTORY = "history_items"
    private const val MAX_ITEMS = 100

    private val gson = Gson()

    fun getAll(context: Context): List<HistoryItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<HistoryItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(context: Context, item: HistoryItem) {
        val list = getAll(context).toMutableList()
        list.add(0, item)
        while (list.size > MAX_ITEMS) list.removeAt(list.size - 1)
        save(context, list)
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun save(context: Context, list: List<HistoryItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HISTORY, gson.toJson(list)).apply()
    }

    fun formatTimestamp(ts: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
        return sdf.format(Date(ts))
    }
}
