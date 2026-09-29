package {PACKAGE_NAME}

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object QrGenerator {

    fun generate(
        content: String,
        size: Int = 768,
        logo: Bitmap? = null
    ): Bitmap {
        val hints = mapOf(EncodeHintType.MARGIN to 2)
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }

        if (logo != null) {
            return overlayLogo(bmp, logo)
        }
        return bmp
    }

    private fun overlayLogo(qr: Bitmap, logo: Bitmap): Bitmap {
        val result = qr.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val size = qr.width
        val logoSize = (size * 0.22).toInt() // 22% of QR size
        val pad = (logoSize * 0.12).toInt()
        val left = (size - logoSize) / 2
        val top = (size - logoSize) / 2

        // White background circle behind logo
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        val radius = logoSize / 2f + pad
        canvas.drawCircle(size / 2f, size / 2f, radius, paint)

        // Draw logo
        val scaled = Bitmap.createScaledBitmap(logo, logoSize, logoSize, true)
        val src = Rect(0, 0, scaled.width, scaled.height)
        val dst = Rect(left, top, left + logoSize, top + logoSize)
        canvas.drawBitmap(scaled, src, dst, null)

        return result
    }
}
