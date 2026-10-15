package com.example.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
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
}
