package {PACKAGE_NAME}

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {

    fun isWifiConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val n = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(n) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (e: Exception) { false }
    }

    fun getLocalIpAddress(): String? {
        return try {
            val ifaces = NetworkInterface.getNetworkInterfaces()
            while (ifaces.hasMoreElements()) {
                val iface = ifaces.nextElement()
                if (!iface.isUp || iface.isLoopback) continue
                if (iface.name.startsWith("wlan") || iface.name.startsWith("eth") || iface.name.startsWith("ap")) {
                    val addrs = iface.inetAddresses
                    while (addrs.hasMoreElements()) {
                        val addr = addrs.nextElement()
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            return addr.hostAddress
                        }
                    }
                }
            }
            null
        } catch (e: Exception) { null }
    }

    fun getDefaultDeviceName(): String {
        return try {
            val model = Build.MODEL ?: "Android"
            val manufacturer = Build.MANUFACTURER ?: ""
            if (model.lowercase().startsWith(manufacturer.lowercase())) {
                model.replaceFirstChar { it.uppercase() }
            } else {
                "$manufacturer $model".trim().replaceFirstChar { it.uppercase() }
            }
        } catch (e: Exception) { "Android Device" }
    }

    fun getWifiName(context: Context): String? {
        return try {
            @Suppress("DEPRECATION")
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val info = wm.connectionInfo
            info?.ssid?.replace("\"", "")?.takeIf { it != "<unknown ssid>" }
        } catch (e: Exception) { null }
    }
}
