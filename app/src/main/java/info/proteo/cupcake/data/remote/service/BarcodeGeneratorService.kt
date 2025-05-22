package info.proteo.cupcake.data.remote.service
import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BarcodeGenerator @Inject constructor() {

    private val formatMap = mapOf(
        "CODE128" to BarcodeFormat.CODE_128,
        "CODE128A" to BarcodeFormat.CODE_128,
        "CODE128B" to BarcodeFormat.CODE_128,
        "CODE128C" to BarcodeFormat.CODE_128,
        "EAN13" to BarcodeFormat.EAN_13,
        "EAN8" to BarcodeFormat.EAN_8,
        "UPC" to BarcodeFormat.UPC_A,
        "UPCE" to BarcodeFormat.UPC_E,
        "CODE39" to BarcodeFormat.CODE_39,
        "ITF14" to BarcodeFormat.ITF,
        "codabar" to BarcodeFormat.CODABAR,
        "QR_CODE" to BarcodeFormat.QR_CODE
    )

    fun generateBarcode(content: String, format: String, width: Int = 500, height: Int = 200): Bitmap? {
        val barcodeFormat = formatMap[format] ?: return null

        val contentToEncode = when (format) {
            "CODE128A" -> "\u00f1" + content  // FNC1 + content for CODE128A
            "CODE128B" -> "\u00f2" + content  // FNC2 + content for CODE128B
            "CODE128C" -> "\u00f3" + content  // FNC3 + content for CODE128C
            else -> content
        }

        try {
            val multiFormatWriter = MultiFormatWriter()
            val finalWidth: Int
            val finalHeight: Int

            // Adjust dimensions for 2D barcodes
            if (barcodeFormat == BarcodeFormat.QR_CODE ||
                barcodeFormat == BarcodeFormat.DATA_MATRIX) {
                finalWidth = width
                finalHeight = width  // Make it square
            } else {
                finalWidth = width
                finalHeight = height
            }

            val bitMatrix = multiFormatWriter.encode(
                contentToEncode,
                barcodeFormat,
                finalWidth,
                finalHeight
            )

            return createBitmapFromBitMatrix(bitMatrix)
        } catch (e: WriterException) {
            return null
        }
    }

    private fun createBitmapFromBitMatrix(bitMatrix: BitMatrix): Bitmap {
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }

        return bitmap
    }
}