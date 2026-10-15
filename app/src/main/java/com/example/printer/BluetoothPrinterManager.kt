package com.example.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.util.Log
import com.example.data.CartItem
import com.example.data.Invoice
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BluetoothPrinterDevice(
    val name: String,
    val address: String,
    val isBonded: Boolean = true
)

class BluetoothPrinterManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    companion object {
        private const val TAG = "BluetoothPrinter"
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-0000-0000-0000805F9B34")
        private const val PREFS_NAME = "BluetoothPrinterPrefs"
        private const val KEY_PRINTER_ADDRESS = "saved_printer_address"
        private const val KEY_PRINTER_NAME = "saved_printer_name"
    }

    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothPrinterDevice> {
        if (!isBluetoothAvailable() || !isBluetoothEnabled()) return emptyList()
        return try {
            bluetoothAdapter?.bondedDevices?.map { device ->
                BluetoothPrinterDevice(
                    name = device.name ?: "طابعة بدون اسم",
                    address = device.address,
                    isBonded = true
                )
            } ?: emptyList()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting bonded devices", e)
            emptyList()
        }
    }

    fun saveSelectedPrinter(device: BluetoothPrinterDevice) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PRINTER_ADDRESS, device.address)
            .putString(KEY_PRINTER_NAME, device.name)
            .apply()
    }

    fun getSavedPrinter(): BluetoothPrinterDevice? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val address = prefs.getString(KEY_PRINTER_ADDRESS, null) ?: return null
        val name = prefs.getString(KEY_PRINTER_NAME, "طابعة حرارية") ?: "طابعة حرارية"
        return BluetoothPrinterDevice(name = name, address = address, isBonded = true)
    }

    fun clearSavedPrinter() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    @SuppressLint("MissingPermission")
    suspend fun connectToDevice(address: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            disconnect()
            val device: BluetoothDevice = bluetoothAdapter?.getRemoteDevice(address)
                ?: return@withContext Result.failure(Exception("لم يتم العثور على جهاز البلوتوث"))

            val tempSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            bluetoothAdapter?.cancelDiscovery()
            tempSocket.connect()

            socket = tempSocket
            outputStream = tempSocket.outputStream
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to Bluetooth printer", e)
            disconnect()
            Result.failure(Exception("فشل الاتصال بالطابعة: ${e.localizedMessage}"))
        }
    }

    fun disconnect() {
        try {
            outputStream?.close()
            outputStream = null
            socket?.close()
            socket = null
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting printer socket", e)
        }
    }

    suspend fun printTestReceipt(
        printerName: String,
        address: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val connResult = connectToDevice(address)
            if (connResult.isFailure) return@withContext connResult

            val bitmap = createTestReceiptBitmap(printerName)
            val printResult = sendBitmapToPrinter(bitmap)
            disconnect()
            printResult
        } catch (e: Exception) {
            disconnect()
            Result.failure(e)
        }
    }

    suspend fun printInvoiceReceipt(
        address: String,
        storeName: String,
        storePhone: String,
        storeVatNumber: String,
        currentBranch: String,
        invoice: Invoice,
        items: List<CartItem>,
        customerName: String?,
        currencySymbol: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val connResult = connectToDevice(address)
            if (connResult.isFailure) return@withContext connResult

            val bitmap = createInvoiceReceiptBitmap(
                storeName = storeName,
                storePhone = storePhone,
                storeVatNumber = storeVatNumber,
                currentBranch = currentBranch,
                invoice = invoice,
                items = items,
                customerName = customerName,
                currencySymbol = currencySymbol
            )

            val printResult = sendBitmapToPrinter(bitmap)
            disconnect()
            printResult
        } catch (e: Exception) {
            disconnect()
            Result.failure(e)
        }
    }

    private fun sendBitmapToPrinter(bitmap: Bitmap): Result<Boolean> {
        val stream = outputStream ?: return Result.failure(Exception("الطابعة غير متصلة"))
        return try {
            // ESC @ (Initialize printer)
            stream.write(byteArrayOf(0x1B, 0x40))

            // Center alignment
            stream.write(byteArrayOf(0x1B, 0x61, 0x01))

            // Convert bitmap to ESC/POS Raster graphics command (GS v 0)
            val rasterBytes = decodeBitmapToEscPosRaster(bitmap)
            stream.write(rasterBytes)

            // Feed lines & cut
            stream.write(byteArrayOf(0x1B, 0x64, 0x04)) // Feed 4 lines
            stream.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00)) // Paper Cut
            stream.flush()

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing bytes to printer stream", e)
            Result.failure(Exception("خطأ أثناء إرسال البيانات للطباعة: ${e.localizedMessage}"))
        }
    }

    /**
     * Converts an Android Bitmap into ESC/POS Raster Bit Image format (GS v 0 m xL xH yL yH d1...dk)
     * Standard 58mm printer width: 384 dots (48 bytes per line)
     */
    private fun decodeBitmapToEscPosRaster(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8

        val result = mutableListOf<Byte>()

        // GS v 0 0 xL xH yL yH
        result.add(0x1D.toByte())
        result.add(0x76.toByte())
        result.add(0x30.toByte())
        result.add(0x00.toByte())

        // Width in bytes
        result.add((widthBytes % 256).toByte())
        result.add((widthBytes / 256).toByte())

        // Height in dots
        result.add((height % 256).toByte())
        result.add((height / 256).toByte())

        for (y in 0 until height) {
            for (xByte in 0 until widthBytes) {
                var byteVal = 0
                for (b in 0 until 8) {
                    val x = xByte * 8 + b
                    if (x < width) {
                        val pixel = bitmap.getPixel(x, y)
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val bPixel = pixel and 0xFF
                        val luminance = (0.299 * r + 0.587 * g + 0.114 * bPixel).toInt()
                        // Black pixel if dark
                        if (luminance < 160) {
                            byteVal = byteVal or (1 shl (7 - b))
                        }
                    }
                }
                result.add(byteVal.toByte())
            }
        }

        return result.toByteArray()
    }

    /**
     * Draw Test Receipt as monochrome image (384px wide for 58mm printer)
     */
    private fun createTestReceiptBitmap(printerName: String): Bitmap {
        val width = 384
        val height = 450
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var y = 40f
        canvas.drawText("================================", width / 2f, y, paint)
        y += 35f
        paint.textSize = 26f
        canvas.drawText("اختبار الطباعة الحرارية ", width / 2f, y, paint)
        y += 35f
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("الطابعة: $printerName", width / 2f, y, paint)
        y += 30f

        val sdf = SimpleDateFormat("yyyy/MM/dd - HH:mm:ss", Locale.getDefault())
        canvas.drawText("التاريخ: ${sdf.format(Date())}", width / 2f, y, paint)
        y += 35f
        canvas.drawText("--------------------------------", width / 2f, y, paint)
        y += 35f

        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("الاتصال بالطابعة تم بنجاح! ", width / 2f, y, paint)
        y += 35f
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("جاهز لطباعة الفواتير والإيصالات", width / 2f, y, paint)
        y += 40f
        canvas.drawText("================================", width / 2f, y, paint)

        return bitmap
    }

    /**
     * Draw Invoice Receipt as monochrome bitmap canvas image with clean Arabic layout
     */
    private fun createInvoiceReceiptBitmap(
        storeName: String,
        storePhone: String,
        storeVatNumber: String,
        currentBranch: String,
        invoice: Invoice,
        items: List<CartItem>,
        customerName: String?,
        currencySymbol: String
    ): Bitmap {
        val width = 384
        val baseHeight = 520 + (items.size * 35) + 200
        val bitmap = Bitmap.createBitmap(width, baseHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val centerPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val rightPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val leftPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        var y = 35f

        // Header
        centerPaint.textSize = 26f
        centerPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(storeName, width / 2f, y, centerPaint)
        y += 30f

        centerPaint.textSize = 18f
        centerPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("الهاتف: $storePhone", width / 2f, y, centerPaint)
        y += 26f
        canvas.drawText("الرقم الضريبي: $storeVatNumber", width / 2f, y, centerPaint)
        y += 26f
        canvas.drawText("الفرع: $currentBranch", width / 2f, y, centerPaint)
        y += 28f

        centerPaint.textSize = 20f
        canvas.drawText("-----------------------------------------", width / 2f, y, centerPaint)
        y += 28f

        // Invoice Meta
        rightPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("فاتورة رقم: #${invoice.id}", width - 10f, y, rightPaint)
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        leftPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(sdf.format(Date(invoice.invoiceDate)), 10f, y, leftPaint)
        y += 26f

        rightPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("الكاشير: ${invoice.cashierName}", width - 10f, y, rightPaint)
        y += 26f

        if (!customerName.isNull_or_blank()) {
            canvas.drawText("العميل: $customerName", width - 10f, y, rightPaint)
            y += 26f
        }

        canvas.drawText("طريقة الدفع: ${invoice.paymentMethod}", width - 10f, y, rightPaint)
        y += 28f

        centerPaint.textSize = 20f
        canvas.drawText("-----------------------------------------", width / 2f, y, centerPaint)
        y += 28f

        // Table Header
        rightPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        rightPaint.textSize = 18f
        canvas.drawText("الصنف", width - 10f, y, rightPaint)
        centerPaint.textSize = 18f
        canvas.drawText("الكمية", width / 2f, y, centerPaint)
        leftPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        leftPaint.textSize = 18f
        canvas.drawText("الإجمالي", 10f, y, leftPaint)
        y += 28f

        rightPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        leftPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        // Items List
        items.forEach { item ->
            var name = item.product.nameAr
            if (name.length > 16) {
                name = name.take(15) + ".."
            }
            canvas.drawText(name, width - 10f, y, rightPaint)
            canvas.drawText("x${item.quantity}", width / 2f, y, centerPaint)
            val totalItemPrice = String.format(Locale.US, "%.2f", item.product.priceSale * item.quantity)
            canvas.drawText("$totalItemPrice $currencySymbol", 10f, y, leftPaint)
            y += 28f
        }

        canvas.drawText("-----------------------------------------", width / 2f, y, centerPaint)
        y += 28f

        // Totals
        val subtotalStr = String.format(Locale.US, "%.2f", invoice.subtotal)
        canvas.drawText("المجموع الفرعي:", width - 10f, y, rightPaint)
        canvas.drawText("$subtotalStr $currencySymbol", 10f, y, leftPaint)
        y += 26f

        if (invoice.discount > 0) {
            val discStr = String.format(Locale.US, "%.2f", invoice.discount)
            canvas.drawText("الخصم:", width - 10f, y, rightPaint)
            canvas.drawText("-$discStr $currencySymbol", 10f, y, leftPaint)
            y += 26f
        }

        val taxStr = String.format(Locale.US, "%.2f", invoice.taxAmount)
        canvas.drawText("الضريبة (15%):", width - 10f, y, rightPaint)
        canvas.drawText("$taxStr $currencySymbol", 10f, y, leftPaint)
        y += 28f

        rightPaint.textSize = 20f
        rightPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        leftPaint.textSize = 20f
        leftPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val totalStr = String.format(Locale.US, "%.2f", invoice.total)
        canvas.drawText("الإجمالي النهائي:", width - 10f, y, rightPaint)
        canvas.drawText("$totalStr $currencySymbol", 10f, y, leftPaint)
        y += 32f

        centerPaint.textSize = 20f
        canvas.drawText("-----------------------------------------", width / 2f, y, centerPaint)
        y += 28f

        // Footer
        centerPaint.textSize = 18f
        centerPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("شاكرين تسوقكم معنا ", width / 2f, y, centerPaint)
        y += 26f

        centerPaint.textSize = 14f
        centerPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("فاتورة إلكترونية معتمدة من هيئة الزكاة", width / 2f, y, centerPaint)
        y += 30f

        return bitmap
    }

    private fun String?.isNull_or_blank(): Boolean {
        return this == null || this.trim().isEmpty()
    }
}
