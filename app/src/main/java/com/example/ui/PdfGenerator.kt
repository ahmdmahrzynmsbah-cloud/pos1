package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {
  fun generateStatementPdfFile(
    context: Context,
    storeName: String,
    storePhone: String,
    title: String,
    bodyRows: List<StatementRow>,
    summaryText: String,
    entityDetails: String
  ): File? {
    try {
      val pdfDocument = PdfDocument()
      val pageW = 595
      val rowHeight = 35f
      val contentHeight = 450f + (bodyRows.size * rowHeight)
      val pageH = if (contentHeight > 842) contentHeight.toInt() else 842

      val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, 1).create()
      val page = pdfDocument.startPage(pageInfo)
      val canvas = page.canvas

      val primaryColor = Color.parseColor("#0F766E") // Emerald/Teal dark
      val secondaryColor = Color.parseColor("#E2E8F0") // Slate 200
      val accentColor = Color.parseColor("#0ea5e9") // Sky blue
      val textDark = Color.parseColor("#1E293B")
      val textLight = Color.WHITE
      val greenColor = Color.parseColor("#16A34A")
      val redColor = Color.parseColor("#DC2626")

      // App Standard Fonts
      val fontBold = try {
        androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.ibm_plex_sans_arabic_bold)
          ?: androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.ibm_plex_sans_arabic_regular)
          ?: Typeface.DEFAULT_BOLD
      } catch (e: Exception) { Typeface.DEFAULT_BOLD }
      val fontSemiBold = try {
        androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.ibm_plex_sans_arabic_semibold)
          ?: fontBold
      } catch (e: Exception) { fontBold }
      val fontMedium = try {
        androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.ibm_plex_sans_arabic_medium)
          ?: Typeface.DEFAULT
      } catch (e: Exception) { Typeface.DEFAULT }
      val fontRegular = try {
        androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.ibm_plex_sans_arabic_regular)
          ?: Typeface.DEFAULT
      } catch (e: Exception) { Typeface.DEFAULT }

      val paint = Paint().apply {
        isAntiAlias = true
        textSize = 12f
        typeface = fontRegular
        color = textDark
      }
      
      // Draw Header Background
      val headerBg = Paint().apply { color = primaryColor }
      canvas.drawRect(0f, 0f, pageW.toFloat(), 120f, headerBg)

      var y = 50f
      val xRight = pageW - 40f
      val xLeft = 40f
      val xCenter = pageW / 2f

      paint.textAlign = Paint.Align.CENTER
      paint.textSize = 28f
      paint.color = textLight
      paint.typeface = fontBold
      canvas.drawText(storeName, xCenter, y, paint)
      
      y += 30f
      paint.textSize = 14f
      paint.typeface = fontMedium
      paint.color = Color.parseColor("#CCFBF1")
      canvas.drawText("هاتف: $storePhone", xCenter, y, paint)

      y = 160f
      paint.textAlign = Paint.Align.RIGHT
      paint.textSize = 22f
      paint.typeface = fontBold
      paint.color = primaryColor
      canvas.drawText(title, xRight, y, paint)

      y += 30f
      paint.textSize = 14f
      paint.typeface = fontRegular
      paint.color = textDark
      
      // Draw entity details with a small accent line
      val entityBg = Paint().apply { color = Color.parseColor("#F1F5F9") }
      val rectF = RectF(xRight - 250f, y - 20f, xRight, y + 10f)
      canvas.drawRoundRect(rectF, 8f, 8f, entityBg)
      
      val accentPaint = Paint().apply { color = primaryColor; strokeWidth = 4f }
      canvas.drawLine(xRight, y - 20f, xRight, y + 10f, accentPaint)
      
      canvas.drawText(entityDetails, xRight - 10f, y, paint)

      y += 50f
      
      // Table Header
      val tableHeaderBg = Paint().apply { color = primaryColor }
      val borderPaint = Paint().apply { 
        color = secondaryColor
        style = Paint.Style.STROKE
        strokeWidth = 1f 
      }
      
      canvas.drawRoundRect(RectF(xLeft, y - 25f, xRight, y + 15f), 6f, 6f, tableHeaderBg)

      paint.typeface = fontBold
      paint.textAlign = Paint.Align.CENTER
      paint.color = textLight
      paint.textSize = 13f

      val col1X = xRight - 70f // Date
      val col2X = xCenter + 30f // Description
      val col3X = xLeft + 140f // Debit
      val col4X = xLeft + 50f // Credit

      canvas.drawText("التاريخ", col1X, y, paint)
      canvas.drawText("البيان", col2X, y, paint)
      canvas.drawText("مدين (+)", col3X, y, paint)
      canvas.drawText("دائن (-)", col4X, y, paint)

      y += 25f
      paint.typeface = fontRegular

      val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.US)
      val altRowBg = Paint().apply { color = Color.parseColor("#F8FAFC") }
      
      for ((index, row) in bodyRows.withIndex()) {
        if (index % 2 == 0) {
          canvas.drawRect(xLeft, y - 20f, xRight, y + 15f, altRowBg)
        }
        
        paint.color = textDark
        paint.typeface = fontRegular
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(sdf.format(Date(row.date)), col1X, y, paint)
        
        paint.textAlign = Paint.Align.RIGHT
        var desc = row.description
        if (desc.length > 32) desc = desc.take(30) + ".."
        canvas.drawText(desc, col2X + 80f, y, paint)

        paint.textAlign = Paint.Align.CENTER
        paint.typeface = fontSemiBold
        paint.color = if (row.debit > 0) greenColor else textDark
        canvas.drawText(if (row.debit > 0) String.format(Locale.US, "%.2f", row.debit) else "-", col3X, y, paint)
        
        paint.color = if (row.credit > 0) redColor else textDark
        canvas.drawText(if (row.credit > 0) String.format(Locale.US, "%.2f", row.credit) else "-", col4X, y, paint)
        
        paint.typeface = fontRegular
        y += rowHeight
      }

      canvas.drawLine(xLeft, y - 20f, xRight, y - 20f, borderPaint)

      y += 30f
      
      // Summary Box
      val summaryBg = Paint().apply { color = Color.parseColor("#ECFDF5") }
      val summaryBorder = Paint().apply { color = Color.parseColor("#10B981"); style = Paint.Style.STROKE; strokeWidth = 2f }
      val summaryRect = RectF(xLeft, y - 20f, xRight, y + 90f)
      
      canvas.drawRoundRect(summaryRect, 12f, 12f, summaryBg)
      canvas.drawRoundRect(summaryRect, 12f, 12f, summaryBorder)
      
      paint.textAlign = Paint.Align.RIGHT
      paint.typeface = fontBold
      paint.textSize = 15f
      paint.color = textDark
      val lines = summaryText.split("|")
      
      var sumY = y + 5f
      for (line in lines) {
        val iconStr = if (line.contains("المبيعات") || line.contains("مدين")) " " 
               else if (line.contains("السداد") || line.contains("دائن")) " " 
               else " "
        canvas.drawText(iconStr + line.trim(), xRight - 20f, sumY, paint)
        sumY += 30f
      }
      
      // Footer
      y = pageH - 40f
      paint.textAlign = Paint.Align.CENTER
      paint.textSize = 11f
      paint.typeface = fontRegular
      paint.color = Color.parseColor("#94A3B8")
      canvas.drawText("تم استخراج هذا التقرير آلياً بواسطة تطبيق الإدارة والمبيعات الذكي", xCenter, y, paint)

      pdfDocument.finishPage(page)

      val dir = File(context.cacheDir, "pdfs")
      if (!dir.exists()) dir.mkdirs()
      val file = File(dir, "Statement_${System.currentTimeMillis()}.pdf")
      file.outputStream().use { pdfDocument.writeTo(it) }
      pdfDocument.close()
      return file
    } catch (e: Exception) {
      e.printStackTrace()
      return null
    }
  }

  data class InvoiceItemPdfData(
    val name: String,
    val quantity: Double,
    val unitPrice: Double,
    val total: Double
  )

  fun generateInvoicePdfFile(
    context: Context,
    storeName: String,
    storePhone: String,
    storeVatNumber: String = "",
    branchName: String,
    docTitle: String,
    invoiceNumber: String,
    invoiceDate: Long,
    cashierName: String,
    partyName: String,
    partyPhone: String = "",
    paymentMethod: String,
    items: List<InvoiceItemPdfData>,
    subtotal: Double,
    discount: Double = 0.0,
    taxAmount: Double = 0.0,
    total: Double,
    currencySymbol: String
  ): File? {
    try {
      val pdfDoc = PdfDocument()
      val pageW = 460
      val baseH = 460
      val itemsH = (items.size * 26)
      val pageH = baseH + itemsH

      val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, 1).create()
      val page = pdfDoc.startPage(pageInfo)
      val canvas: Canvas = page.canvas

      val marginLeft = 20f
      val marginRight = pageW - 20f

      val fontBold = try {
        ResourcesCompat.getFont(context, R.font.ibm_plex_sans_arabic_bold)
          ?: ResourcesCompat.getFont(context, R.font.ibm_plex_sans_arabic_regular)
          ?: Typeface.DEFAULT_BOLD
      } catch (e: Exception) { Typeface.DEFAULT_BOLD }
      val fontSemiBold = try {
        ResourcesCompat.getFont(context, R.font.ibm_plex_sans_arabic_semibold)
          ?: fontBold
      } catch (e: Exception) { fontBold }
      val fontMedium = try {
        ResourcesCompat.getFont(context, R.font.ibm_plex_sans_arabic_medium)
          ?: Typeface.DEFAULT
      } catch (e: Exception) { Typeface.DEFAULT }
      val fontRegular = try {
        ResourcesCompat.getFont(context, R.font.ibm_plex_sans_arabic_regular)
          ?: Typeface.DEFAULT
      } catch (e: Exception) { Typeface.DEFAULT }

      val bgPaint = Paint().apply { isAntiAlias = true }
      val linePaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 0.8f
        color = Color.rgb(226, 232, 240)
      }

      // Top Header Banner (Modern Emerald/Teal brand)
      val isReturn = total < 0 || subtotal < 0
      val bannerColor = if (isReturn) Color.rgb(185, 28, 28) else Color.rgb(15, 118, 110)
      bgPaint.color = bannerColor
      val headerRect = RectF(marginLeft, 16f, marginRight, 76f)
      canvas.drawRoundRect(headerRect, 8f, 8f, bgPaint)

      val titlePaint = Paint().apply {
        isAntiAlias = true
        textSize = 17f
        typeface = fontBold
        textAlign = Paint.Align.CENTER
        color = Color.WHITE
      }
      val subHeaderPaint = Paint().apply {
        isAntiAlias = true
        textSize = 10.5f
        typeface = fontMedium
        textAlign = Paint.Align.CENTER
        color = Color.rgb(204, 251, 241)
      }

      canvas.drawText(storeName, pageW / 2f, 42f, titlePaint)
      canvas.drawText("$docTitle  •  $branchName", pageW / 2f, 62f, subHeaderPaint)

      var y = 90f

      // Store & Invoice Metadata Card
      bgPaint.color = Color.rgb(248, 250, 252)
      val metaRect = RectF(marginLeft, y, marginRight, y + 68f)
      canvas.drawRoundRect(metaRect, 6f, 6f, bgPaint)
      canvas.drawRect(marginLeft, y, marginRight, y + 68f, Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.8f
        color = Color.rgb(203, 213, 225)
      })

      val metaLabelPaint = Paint().apply {
        isAntiAlias = true
        textSize = 9f
        typeface = fontRegular
        textAlign = Paint.Align.RIGHT
        color = Color.rgb(100, 116, 139)
      }
      val metaValPaint = Paint().apply {
        isAntiAlias = true
        textSize = 9.5f
        typeface = fontSemiBold
        textAlign = Paint.Align.RIGHT
        color = Color.rgb(15, 23, 42)
      }

      val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
      val dateFormatted = sdf.format(Date(invoiceDate))

      // Right Column
      canvas.drawText("رقم الفاتورة: #$invoiceNumber", marginRight - 12f, y + 20f, metaValPaint)
      canvas.drawText("الطرف: $partyName", marginRight - 12f, y + 38f, metaValPaint)
      canvas.drawText("الكاشير: ${cashierName.ifBlank { "الآدمن" }}", marginRight - 12f, y + 56f, metaLabelPaint)

      // Left Column
      val col2X = marginRight - 220f
      canvas.drawText("التاريخ: $dateFormatted", col2X, y + 20f, metaLabelPaint)
      canvas.drawText("طريقة الدفع: $paymentMethod", col2X, y + 38f, metaLabelPaint)
      if (partyPhone.isNotBlank()) {
        canvas.drawText("هاتف العميل: $partyPhone", col2X, y + 56f, metaLabelPaint)
      } else if (storePhone.isNotBlank()) {
        canvas.drawText("هاتف المتجر: $storePhone", col2X, y + 56f, metaLabelPaint)
      }

      y += 80f

      // Items Table Header
      bgPaint.color = bannerColor
      val tblHeaderRect = RectF(marginLeft, y, marginRight, y + 24f)
      canvas.drawRoundRect(tblHeaderRect, 4f, 4f, bgPaint)

      val thPaint = Paint().apply {
        isAntiAlias = true
        textSize = 9.5f
        typeface = fontBold
        textAlign = Paint.Align.RIGHT
        color = Color.WHITE
      }

      canvas.drawText("الصنف", marginRight - 12f, y + 16f, thPaint)
      canvas.drawText("الكمية", marginRight - 190f, y + 16f, thPaint)
      canvas.drawText("سعر الوحدة", marginRight - 270f, y + 16f, thPaint)
      canvas.drawText("الإجمالي ($currencySymbol)", marginRight - 340f, y + 16f, thPaint)

      y += 24f

      // Item Rows
      val rowHeight = 24f
      val tdTextPaint = Paint().apply {
        isAntiAlias = true
        textSize = 9f
        typeface = fontRegular
        textAlign = Paint.Align.RIGHT
        color = Color.rgb(30, 41, 59)
      }
      val tdBoldPaint = Paint().apply {
        isAntiAlias = true
        textSize = 9f
        typeface = fontSemiBold
        textAlign = Paint.Align.RIGHT
        color = Color.rgb(15, 23, 42)
      }

      items.forEachIndexed { index, item ->
        val isEven = (index % 2 == 0)
        bgPaint.color = if (isEven) Color.WHITE else Color.rgb(248, 250, 252)
        canvas.drawRect(marginLeft, y, marginRight, y + rowHeight, bgPaint)
        canvas.drawLine(marginLeft, y + rowHeight, marginRight, y + rowHeight, linePaint)

        canvas.drawText(item.name.take(24), marginRight - 12f, y + 16f, tdBoldPaint)
        canvas.drawText("x${item.quantity}", marginRight - 190f, y + 16f, tdTextPaint)
        canvas.drawText(String.format(Locale.US, "%,.2f", item.unitPrice), marginRight - 270f, y + 16f, tdTextPaint)
        canvas.drawText(String.format(Locale.US, "%,.2f", item.total), marginRight - 340f, y + 16f, tdBoldPaint)

        y += rowHeight
      }

      y += 10f

      // Totals Breakdown Card
      bgPaint.color = Color.rgb(248, 250, 252)
      val totalBoxRect = RectF(marginLeft, y, marginRight, y + 78f)
      canvas.drawRoundRect(totalBoxRect, 6f, 6f, bgPaint)
      canvas.drawRect(marginLeft, y, marginRight, y + 78f, Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.8f
        color = Color.rgb(203, 213, 225)
      })

      val totLabelPaint = Paint().apply {
        isAntiAlias = true
        textSize = 9.5f
        typeface = fontMedium
        textAlign = Paint.Align.RIGHT
        color = Color.rgb(71, 85, 105)
      }
      val totValPaint = Paint().apply {
        isAntiAlias = true
        textSize = 10f
        typeface = fontBold
        textAlign = Paint.Align.RIGHT
        color = Color.rgb(15, 23, 42)
      }
      val grandTotalPaint = Paint().apply {
        isAntiAlias = true
        textSize = 13f
        typeface = fontBold
        textAlign = Paint.Align.RIGHT
        color = bannerColor
      }

      canvas.drawText("المجموع الفرعي:", marginRight - 15f, y + 20f, totLabelPaint)
      canvas.drawText("${String.format(Locale.US, "%,.2f", kotlin.math.abs(subtotal))} $currencySymbol", marginRight - 140f, y + 20f, totValPaint)

      if (discount > 0) {
        canvas.drawText("الخصم:", marginRight - 15f, y + 36f, totLabelPaint)
        canvas.drawText("- ${String.format(Locale.US, "%,.2f", discount)} $currencySymbol", marginRight - 140f, y + 36f, totValPaint)
      }

      if (taxAmount > 0) {
        canvas.drawText("ضريبة القيمة المضافة:", marginRight - 15f, y + 52f, totLabelPaint)
        canvas.drawText("${String.format(Locale.US, "%,.2f", taxAmount)} $currencySymbol", marginRight - 140f, y + 52f, totValPaint)
      }

      canvas.drawLine(marginLeft + 10f, y + 56f, marginRight - 10f, y + 56f, linePaint)

      canvas.drawText("المجموع النهائي:", marginRight - 15f, y + 70f, grandTotalPaint)
      canvas.drawText("${String.format(Locale.US, "%,.2f", kotlin.math.abs(total))} $currencySymbol", marginRight - 140f, y + 70f, grandTotalPaint)

      y += 90f

      // Footer note
      val footerPaint = Paint().apply {
        isAntiAlias = true
        textSize = 8.5f
        typeface = fontRegular
        textAlign = Paint.Align.CENTER
        color = Color.rgb(100, 116, 139)
      }
      canvas.drawText("شكراً لتعاملكم معنا  •  طريقة الدفع: $paymentMethod", pageW / 2f, y, footerPaint)

      pdfDoc.finishPage(page)

      val outputDir = File(context.cacheDir, "invoices")
      if (!outputDir.exists()) outputDir.mkdirs()
      val file = File(outputDir, "Invoice_${invoiceNumber}_${System.currentTimeMillis()}.pdf")
      file.outputStream().use { pdfDoc.writeTo(it) }
      pdfDoc.close()
      return file
    } catch (e: Exception) {
      e.printStackTrace()
      return null
    }
  }

  fun shareInvoicePdfToWhatsApp(
    context: Context,
    pdfFile: File,
    phoneNumber: String,
    messageText: String,
    onSuccess: ((String) -> Unit)? = null,
    onError: ((String) -> Unit)? = null
  ) {
    try {
      val cleanPhone = phoneNumber.replace("+", "").replace(" ", "").filter { it.isDigit() }
      val formattedPhone = when {
        cleanPhone.startsWith("00") -> cleanPhone.removePrefix("00")
        cleanPhone.startsWith("0") && cleanPhone.length == 11 -> "2" + cleanPhone
        cleanPhone.startsWith("0") && cleanPhone.length == 10 -> "966" + cleanPhone.removePrefix("0")
        else -> cleanPhone
      }

      val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        pdfFile
      )

      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, messageText)
        if (formattedPhone.isNotEmpty()) {
          putExtra("jid", "$formattedPhone@s.whatsapp.net")
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

      shareIntent.setPackage("com.whatsapp")
      try {
        context.startActivity(shareIntent)
        onSuccess?.invoke("تم فتح تطبيق واتساب وتجهيز الفاتورة PDF للعميل ")
      } catch (e1: Exception) {
        try {
          shareIntent.setPackage("com.whatsapp.w4b")
          context.startActivity(shareIntent)
          onSuccess?.invoke("تم فتح واتساب للأعمال وتجهيز الفاتورة PDF للعميل ")
        } catch (e2: Exception) {
          shareIntent.setPackage(null)
          val chooser = Intent.createChooser(shareIntent, "مشاركة الفاتورة PDF عبر واتساب")
          chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          context.startActivity(chooser)
          onSuccess?.invoke("يرجى اختيار تطبيق واتساب لإرسال الفاتورة")
        }
      }
    } catch (e: Exception) {
      onError?.invoke("تعذر فتح واتساب: ${e.localizedMessage}")
    }
  }
}

