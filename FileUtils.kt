package {PACKAGE_NAME}

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object FileUtils {

    fun copyUriToCache(context: Context, uri: Uri): File? {
        return try {
            val name = queryName(context, uri) ?: "file_${UUID.randomUUID()}"
            val out = File(context.cacheDir, name)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(out).use { output -> input.copyTo(output) }
            }
            out
        } catch (e: Exception) { null }
    }

    private fun queryName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        } catch (e: Exception) { null }
    }

    fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }

    fun formatSpeed(bytesPerSec: Double): String {
        return when {
            bytesPerSec < 1024 -> "%.0f B/s".format(bytesPerSec)
            bytesPerSec < 1024 * 1024 -> "%.1f KB/s".format(bytesPerSec / 1024)
            else -> "%.1f MB/s".format(bytesPerSec / (1024 * 1024))
        }
    }

    fun getMimeType(name: String): String = when {
        name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) -> "image/jpeg"
        name.endsWith(".png", true) -> "image/png"
        name.endsWith(".gif", true) -> "image/gif"
        name.endsWith(".mp4", true) -> "video/mp4"
        name.endsWith(".mp3", true) -> "audio/mpeg"
        name.endsWith(".pdf", true) -> "application/pdf"
        name.endsWith(".apk", true) -> "application/vnd.android.package-archive"
        name.endsWith(".zip", true) -> "application/zip"
        name.endsWith(".txt", true) -> "text/plain"
        else -> "application/octet-stream"
    }

    fun getFileIcon(name: String): String = when {
        name.endsWith(".jpg", true) || name.endsWith(".png", true) ||
            name.endsWith(".jpeg", true) || name.endsWith(".gif", true) -> "🖼️"
        name.endsWith(".mp4", true) || name.endsWith(".mkv", true) -> "🎬"
        name.endsWith(".mp3", true) || name.endsWith(".wav", true) -> "🎵"
        name.endsWith(".pdf", true) -> "📕"
        name.endsWith(".apk", true) -> "📦"
        name.endsWith(".zip", true) -> "🗜️"
        name.endsWith(".txt", true) || name.endsWith(".md", true) -> "📄"
        name.endsWith(".doc", true) || name.endsWith(".docx", true) -> "📘"
        name.endsWith(".xls", true) || name.endsWith(".xlsx", true) -> "📊"
        else -> "📁"
    }
}
