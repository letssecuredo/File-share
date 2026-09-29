package {PACKAGE_NAME}

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class TcpClient(
    private val host: String,
    private val port: Int,
    private val token: String,
    private val clientName: String
) {
    private var socket: Socket? = null
    private val stopped = AtomicBoolean(false)

    data class RemoteFile(val name: String, val size: Long, val mime: String)

    interface Listener {
        fun onConnected(serverName: String)
        fun onFileList(files: List<RemoteFile>)
        fun onFileProgress(name: String, received: Long, total: Long, speedBps: Double)
        fun onFileComplete(name: String, savedPath: String)
        fun onAllComplete(saveDir: String)
        fun onError(message: String)
    }

    suspend fun run(listener: Listener) = withContext(Dispatchers.IO) {
        try {
            val s = TransferProtocol.connect(host, port)
            socket = s
            val out = DataOutputStream(s.getOutputStream())
            val inp = DataInputStream(s.getInputStream())

            TransferProtocol.sendJson(out, TransferProtocol.message("hello").apply {
                addProperty("token", token)
                addProperty("clientName", clientName)
            })

            val resp = TransferProtocol.readJson(inp) ?: run {
                listener.onError("No response"); return@withContext
            }
            when (resp.get("type")?.asString) {
                "reject" -> {
                    listener.onError("Rejected: ${resp.get("reason")?.asString ?: "unknown"}")
                    return@withContext
                }
                "accept" -> listener.onConnected(resp.get("serverName")?.asString ?: "Device")
                else -> {
                    listener.onError("Unexpected: ${resp.get("type")?.asString}")
                    return@withContext
                }
            }

            val listMsg = TransferProtocol.readJson(inp) ?: run {
                listener.onError("No file list"); return@withContext
            }
            if (listMsg.get("type")?.asString != "file_list") {
                listener.onError("Expected file_list"); return@withContext
            }

            val files = mutableListOf<RemoteFile>()
            listMsg.getAsJsonArray("files").forEach { el ->
                val o = el.asJsonObject
                files.add(RemoteFile(
                    name = o.get("name").asString,
                    size = o.get("size").asLong,
                    mime = o.get("mime")?.asString ?: "application/octet-stream"
                ))
            }
            listener.onFileList(files)

            TransferProtocol.sendJson(out, TransferProtocol.message("start"))

            val saveDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "WiFiShare"
            )
            if (!saveDir.exists()) saveDir.mkdirs()

            val buffer = ByteArray(64 * 1024)
            while (!stopped.get()) {
                val msg = TransferProtocol.readJson(inp) ?: break
                when (msg.get("type")?.asString) {
                    "file_start" -> {
                        val name = msg.get("name").asString
                        val size = msg.get("size").asLong
                        val outFile = uniqueFile(saveDir, name)

                        FileOutputStream(outFile).use { fos ->
                            var received = 0L
                            var lastRecv = 0L
                            var lastTime = System.currentTimeMillis()

                            while (received < size && !stopped.get()) {
                                val toRead = minOf(buffer.size.toLong(), size - received).toInt()
                                val read = inp.read(buffer, 0, toRead)
                                if (read == -1) break
                                fos.write(buffer, 0, read)
                                received += read

                                val now = System.currentTimeMillis()
                                val dt = now - lastTime
                                if (dt >= 300) {
                                    val speed = (received - lastRecv) * 1000.0 / dt
                                    listener.onFileProgress(name, received, size, speed)
                                    lastRecv = received
                                    lastTime = now
                                }
                            }
                            fos.flush()
                        }

                        val endMsg = TransferProtocol.readJson(inp)
                        if (endMsg?.get("type")?.asString == "file_end") {
                            listener.onFileComplete(name, outFile.absolutePath)
                        }
                    }
                    "done" -> {
                        listener.onAllComplete(saveDir.absolutePath)
                        break
                    }
                    else -> break
                }
            }
        } catch (e: Exception) {
            listener.onError(e.message ?: "Client error")
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    fun stop() {
        stopped.set(true)
        try { socket?.close() } catch (_: Exception) {}
    }

    private fun uniqueFile(dir: File, name: String): File {
        var f = File(dir, name)
        if (!f.exists()) return f
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        var i = 1
        while (f.exists()) {
            val newName = if (ext.isNotEmpty()) "${base}_$i.$ext" else "${base}_$i"
            f = File(dir, newName)
            i++
        }
        return f
    }
}
