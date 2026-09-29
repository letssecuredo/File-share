package {PACKAGE_NAME}

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class TcpServer(
    private val port: Int,
    private val session: Session,
    private val files: List<File>
) {
    private var serverSocket: ServerSocket? = null
    private val stopped = AtomicBoolean(false)

    interface Listener {
        fun onClientConnected(remoteName: String)
        fun onFileProgress(fileName: String, bytesSent: Long, totalBytes: Long, speedBps: Double)
        fun onFileComplete(fileName: String)
        fun onAllComplete()
        fun onError(message: String)
    }

    suspend fun start(listener: Listener) = withContext(Dispatchers.IO) {
        try {
            val ss = ServerSocket(port)
            serverSocket = ss
            while (!stopped.get()) {
                val client = try { ss.accept() } catch (e: Exception) { break } ?: break
                handleClient(client, listener)
                break
            }
        } catch (e: Exception) {
            listener.onError(e.message ?: "Server error")
        } finally { stop() }
    }

    private suspend fun handleClient(client: Socket, listener: Listener) = withContext(Dispatchers.IO) {
        try {
            client.tcpNoDelay = true
            val out = DataOutputStream(client.getOutputStream())
            val inp = DataInputStream(client.getInputStream())

            val hello = TransferProtocol.readJson(inp) ?: run {
                listener.onError("Handshake failed"); return@withContext
            }
            if (hello.get("type")?.asString != "hello") {
                listener.onError("Invalid handshake"); return@withContext
            }
            if (hello.get("token")?.asString != session.token) {
                TransferProtocol.sendJson(out, TransferProtocol.message("reject").apply {
                    addProperty("reason", "invalid_token")
                })
                listener.onError("Token mismatch"); return@withContext
            }

            TransferProtocol.sendJson(out, TransferProtocol.message("accept").apply {
                addProperty("sessionId", session.id)
                addProperty("serverName", session.deviceName)
            })
            listener.onClientConnected(hello.get("clientName")?.asString ?: "Device")

            val listMsg = TransferProtocol.message("file_list").apply {
                val arr = JsonArray()
                files.forEach { f ->
                    arr.add(JsonObject().apply {
                        addProperty("name", f.name)
                        addProperty("size", f.length())
                        addProperty("mime", FileUtils.getMimeType(f.name))
                    })
                }
                add("files", arr)
            }
            TransferProtocol.sendJson(out, listMsg)

            val startCmd = TransferProtocol.readJson(inp) ?: return@withContext
            if (startCmd.get("type")?.asString != "start") {
                listener.onError("Start signal missing"); return@withContext
            }

            val buffer = ByteArray(64 * 1024)
            files.forEach { file ->
                TransferProtocol.sendJson(out, TransferProtocol.message("file_start").apply {
                    addProperty("name", file.name)
                    addProperty("size", file.length())
                })

                val total = file.length()
                var sent = 0L
                var lastSent = 0L
                var lastTime = System.currentTimeMillis()

                file.inputStream().use { input ->
                    while (sent < total && !stopped.get()) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        out.write(buffer, 0, read)
                        sent += read

                        val now = System.currentTimeMillis()
                        val dt = now - lastTime
                        if (dt >= 300) {
                            val speed = (sent - lastSent) * 1000.0 / dt
                            listener.onFileProgress(file.name, sent, total, speed)
                            lastSent = sent
                            lastTime = now
                        }
                    }
                }
                out.flush()

                TransferProtocol.sendJson(out, TransferProtocol.message("file_end").apply {
                    addProperty("name", file.name)
                })
                listener.onFileComplete(file.name)
            }

            TransferProtocol.sendJson(out, TransferProtocol.message("done"))
            listener.onAllComplete()
        } catch (e: Exception) {
            listener.onError(e.message ?: "Transfer error")
        } finally {
            try { client.close() } catch (_: Exception) {}
        }
    }

    fun stop() {
        stopped.set(true)
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
    }
}
