package {PACKAGE_NAME}

import com.google.gson.Gson
import java.util.UUID
import kotlin.random.Random

data class FileMeta(val name: String, val size: Long, val mime: String)

data class Session(
    val id: String,
    val token: String,
    val deviceName: String,
    val ip: String,
    val port: Int,
    val files: List<FileMeta>,
    val createdAt: Long
)

data class QrPayload(
    val v: Int = 1,
    val sid: String,
    val ip: String,
    val port: Int,
    val token: String,
    val name: String,
    val files: List<FileMeta>
)

object SessionManager {
    private val gson = Gson()

    fun createSession(deviceName: String, ip: String, port: Int, files: List<FileMeta>): Session {
        return Session(
            id = UUID.randomUUID().toString(),
            token = randomToken(32),
            deviceName = deviceName,
            ip = ip,
            port = port,
            files = files,
            createdAt = System.currentTimeMillis()
        )
    }

    fun buildQrString(session: Session): String {
        val payload = QrPayload(
            sid = session.id,
            ip = session.ip,
            port = session.port,
            token = session.token,
            name = session.deviceName,
            files = session.files
        )
        return gson.toJson(payload)
    }

    fun parseQrString(raw: String): QrPayload? {
        return try {
            val payload = gson.fromJson(raw, QrPayload::class.java)
            if (payload.v != 1) return null
            if (payload.token.isBlank() || payload.ip.isBlank()) return null
            payload
        } catch (e: Exception) { null }
    }

    private fun randomToken(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
}
