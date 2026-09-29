package {PACKAGE_NAME}

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket

object TransferProtocol {
    private val gson = Gson()
    private const val MAX_JSON = 1_048_576

    fun sendJson(out: DataOutputStream, obj: JsonObject) {
        val bytes = gson.toJson(obj).toByteArray(Charsets.UTF_8)
        out.writeInt(bytes.size)
        out.write(bytes)
        out.flush()
    }

    fun readJson(inp: DataInputStream): JsonObject? {
        val len = try { inp.readInt() } catch (e: Exception) { return null }
        if (len <= 0 || len > MAX_JSON) return null
        val buf = ByteArray(len)
        inp.readFully(buf)
        return gson.fromJson(String(buf, Charsets.UTF_8), JsonObject::class.java)
    }

    fun message(type: String): JsonObject = JsonObject().apply { addProperty("type", type) }

    fun connect(host: String, port: Int, timeoutMs: Int = 8000): Socket {
        val socket = Socket()
        socket.connect(InetSocketAddress(host, port), timeoutMs)
        socket.soTimeout = 30000
        socket.tcpNoDelay = true
        return socket
    }
}
