import re

with open("app/src/main/java/com/example/ui/AppViewModel.kt", "r") as f:
    content = f.read()

old_func = re.search(r'    fun exportItemMovementToPdf\(.*?\n    fun exportInvoiceToPdf\(', content, re.DOTALL)
if old_func:
    new_func = """    fun exportItemMovementToPdf(context: android.content.Context, product: com.example.data.Product, movements: List<com.example.data.MovementRecord>, netQty: Int, branchName: String = "") {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    showToast("جاري التجهيز... سيتم التنزيل قريباً 📄")
                }

                val pdfDocument = android.graphics.pdf.PdfDocument()
                // Constants for layout
                val pageW = 595 // A4 width at 72 PPI
                val margin = 30f
                val rowHeight = 25f
                val headerHeight = 35f
                
                val contentHeight = 200 + headerHeight + (movements.size * rowHeight) + 50
                val pageH = if (contentHeight > 842) contentHeight.toInt() else 842 // Minimum A4 height

                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW, pageH, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 12f
                    isAntiAlias = true
                }
                val bgPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#F5F5F5")
                    style = android.graphics.Paint.Style.FILL
                }
                val borderPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#E0E0E0")
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 1f
                }

                // 1. Draw Page Border
                canvas.drawRect(margin, margin, pageW - margin, pageH - margin, borderPaint)

                var y = margin + 30f
                val xRight = pageW - margin - 10f
                val xLeft = margin + 10f
                
                // 2. Report Header (Title & Logo Placeholder)
                paint.textAlign = android.graphics.Paint.Align.CENTER
                paint.isFakeBoldText = true
                paint.textSize = 22f
                paint.color = android.graphics.Color.parseColor("#1565C0") // Primary Blue
                canvas.drawText("كشف حركة مادة تفصيلي", pageW / 2f, y, paint)
                
                y += 20f
                paint.textSize = 12f
                paint.color = android.graphics.Color.GRAY
                canvas.drawText("الفرع: ${if(branchName.isEmpty()) "الرئيسي" else branchName}", pageW / 2f, y, paint)
                
                y += 30f
                
                // 3. Info Section (Boxed)
                val infoBoxTop = y
                bgPaint.color = android.graphics.Color.parseColor("#FAFAFA")
                canvas.drawRect(margin, infoBoxTop, pageW - margin, infoBoxTop + 80f, bgPaint)
                canvas.drawRect(margin, infoBoxTop, pageW - margin, infoBoxTop + 80f, borderPaint)
                
                paint.color = android.graphics.Color.BLACK
                paint.textAlign = android.graphics.Paint.Align.RIGHT
                paint.textSize = 14f
                y += 25f
                
                val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale("ar"))
                paint.isFakeBoldText = true
                canvas.drawText("المادة:", xRight, y, paint)
                paint.isFakeBoldText = false
                canvas.drawText(" ${product.nameAr}", xRight - 50f, y, paint)
                
                paint.isFakeBoldText = true
                canvas.drawText("الباركود:", pageW / 2f, y, paint)
                paint.isFakeBoldText = false
                canvas.drawText(" ${product.barcode.ifEmpty { "بدون" }}", (pageW / 2f) - 60f, y, paint)

                y += 25f
                paint.isFakeBoldText = true
                canvas.drawText("القسم:", xRight, y, paint)
                paint.isFakeBoldText = false
                canvas.drawText(" ${product.category}", xRight - 50f, y, paint)
                
                paint.isFakeBoldText = true
                canvas.drawText("تاريخ الطباعة:", pageW / 2f, y, paint)
                paint.isFakeBoldText = false
                canvas.drawText(" ${sdf.format(java.util.Date())}", (pageW / 2f) - 90f, y, paint)

                y += 25f
                paint.isFakeBoldText = true
                paint.color = android.graphics.Color.parseColor("#2E7D32")
                canvas.drawText("الرصيد الحالي:", xRight, y, paint)
                canvas.drawText(" $netQty وحدة", xRight - 100f, y, paint)
                paint.color = android.graphics.Color.BLACK
                
                y += 30f // Space before table
                
                // 4. Table Header
                bgPaint.color = android.graphics.Color.parseColor("#1976D2") // Header Blue
                canvas.drawRect(margin, y, pageW - margin, y + headerHeight, bgPaint)
                canvas.drawRect(margin, y, pageW - margin, y + headerHeight, borderPaint)
                
                paint.color = android.graphics.Color.WHITE
                paint.isFakeBoldText = true
                paint.textSize = 13f
                paint.textAlign = android.graphics.Paint.Align.CENTER
                
                // Columns X centers
                val colDate = xRight - 60f
                val colType = xRight - 170f
                val colParty = xRight - 280f
                val colQty = xRight - 380f
                val colPrice = xRight - 440f
                val colBal = xRight - 500f
                
                val headerY = y + 22f
                canvas.drawText("التاريخ", colDate, headerY, paint)
                canvas.drawText("النوع والمرجع", colType, headerY, paint)
                canvas.drawText("الطرف", colParty, headerY, paint)
                canvas.drawText("الكمية", colQty, headerY, paint)
                canvas.drawText("السعر", colPrice, headerY, paint)
                canvas.drawText("الرصيد", colBal, headerY, paint)
                
                y += headerHeight
                
                // 5. Table Rows
                var runningBalance = 0
                val sortedMovements = movements.sortedBy { it.date }
                paint.isFakeBoldText = false
                
                sortedMovements.forEachIndexed { index, m ->
                    // Alternate row color
                    bgPaint.color = if (index % 2 == 0) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#F9F9F9")
                    canvas.drawRect(margin, y, pageW - margin, y + rowHeight, bgPaint)
                    
                    // Draw vertical dividers
                    canvas.drawLine(xRight - 120f, y, xRight - 120f, y + rowHeight, borderPaint)
                    canvas.drawLine(xRight - 230f, y, xRight - 230f, y + rowHeight, borderPaint)
                    canvas.drawLine(xRight - 340f, y, xRight - 340f, y + rowHeight, borderPaint)
                    canvas.drawLine(xRight - 410f, y, xRight - 410f, y + rowHeight, borderPaint)
                    canvas.drawLine(xRight - 470f, y, xRight - 470f, y + rowHeight, borderPaint)
                    
                    val sign = if (m.isIncoming) 1 else -1
                    runningBalance += (m.quantity * sign)
                    
                    val textY = y + 17f
                    paint.color = android.graphics.Color.BLACK
                    paint.textSize = 11f
                    
                    // Date
                    canvas.drawText(sdf.format(java.util.Date(m.date)), colDate, textY, paint)
                    // Type
                    var cleanType = m.type.replace(Regex("[📦🛒🔄↪️⚙️]"), "").trim()
                    canvas.drawText(cleanType, colType, textY, paint)
                    // Party
                    canvas.drawText(m.partyName.take(15), colParty, textY, paint)
                    
                    // Qty
                    val qtyStr = if (m.isIncoming) "+${m.quantity}" else "-${m.quantity}"
                    paint.color = if (m.isIncoming) android.graphics.Color.parseColor("#2E7D32") else android.graphics.Color.parseColor("#C62828")
                    paint.isFakeBoldText = true
                    canvas.drawText(qtyStr, colQty, textY, paint)
                    
                    // Price
                    paint.color = android.graphics.Color.DKGRAY
                    paint.isFakeBoldText = false
                    canvas.drawText(String.format("%.2f", m.price), colPrice, textY, paint)
                    
                    // Balance
                    paint.color = android.graphics.Color.BLACK
                    paint.isFakeBoldText = true
                    canvas.drawText(runningBalance.toString(), colBal, textY, paint)
                    
                    // Bottom border for row
                    canvas.drawLine(margin, y + rowHeight, pageW - margin, y + rowHeight, borderPaint)
                    
                    y += rowHeight
                    paint.isFakeBoldText = false
                }
                
                // 6. Footer
                y += 30f
                paint.color = android.graphics.Color.GRAY
                paint.textSize = 10f
                paint.textAlign = android.graphics.Paint.Align.CENTER
                canvas.drawText("تم إنشاء هذا التقرير آلياً - نظام المبيعات والمخزون", pageW / 2f, y, paint)
                
                pdfDocument.finishPage(page)
                
                val fileName = "ItemMovement_${product.id}_${System.currentTimeMillis()}.pdf"
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            pdfDocument.writeTo(outputStream)
                        }
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            showToast("تم تنزيل كشف حركة المادة بنجاح ✔️")
                        }
                    }
                } else {
                    val targetFile = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), fileName)
                    targetFile.outputStream().use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        showToast("تم تنزيل كشف حركة المادة بنجاح ✔️")
                    }
                }
                pdfDocument.close()
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    showToast("حدث خطأ أثناء التنزيل ❌: ${e.message}")
                }
            }
        }
    }
\n    fun exportInvoiceToPdf("""
    content = content.replace(old_func.group(0), new_func)
    
    with open("app/src/main/java/com/example/ui/AppViewModel.kt", "w") as f:
        f.write(content)
else:
    print("Could not find function to replace.")
