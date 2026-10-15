with open("app/src/main/java/com/example/ui/AppViewModel.kt", "r") as f:
    content = f.read()

func = """
    fun exportItemMovementToPdf(context: android.content.Context, product: com.example.data.Product, movements: List<com.example.data.MovementRecord>, netQty: Int) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    showToast("جاري إنشاء كشف حركة المادة PDF... 📄")
                }

                val pdfDocument = android.graphics.pdf.PdfDocument()
                // Auto adjust page height
                val pageHeight = 400 + (movements.size * 30)
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(600, pageHeight, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 14f
                }
                
                var y = 40f
                val xRight = 580f
                
                paint.textAlign = android.graphics.Paint.Align.CENTER
                paint.isFakeBoldText = true
                paint.textSize = 20f
                canvas.drawText("كشف حركة مادة تفصيلي", 300f, y, paint)
                y += 40f
                
                paint.textAlign = android.graphics.Paint.Align.RIGHT
                paint.textSize = 14f
                paint.isFakeBoldText = false
                
                val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
                canvas.drawText("تاريخ الطباعة: ${sdf.format(java.util.Date())}", xRight, y, paint)
                y += 20f
                
                paint.isFakeBoldText = true
                canvas.drawText("المادة: ${product.nameAr}", xRight, y, paint)
                y += 20f
                
                paint.isFakeBoldText = false
                canvas.drawText("الباركود: ${product.barcode.ifEmpty { "بدون" }} | القسم: ${product.category}", xRight, y, paint)
                y += 20f
                canvas.drawText("الرصيد الحالي (صافي الحركة): $netQty", xRight, y, paint)
                y += 30f
                
                // Draw table header
                canvas.drawLine(20f, y - 15f, 580f, y - 15f, paint)
                paint.isFakeBoldText = true
                canvas.drawText("التاريخ", 580f, y, paint)
                canvas.drawText("النوع", 450f, y, paint)
                canvas.drawText("الطرف", 330f, y, paint)
                canvas.drawText("الكمية", 200f, y, paint)
                canvas.drawText("السعر", 140f, y, paint)
                canvas.drawText("الرصيد", 70f, y, paint)
                y += 15f
                canvas.drawLine(20f, y, 580f, y, paint)
                y += 25f
                
                paint.isFakeBoldText = false
                var runningBalance = 0
                // Sort ascending by date to calculate running balance correctly
                val sortedMovements = movements.sortedBy { it.date }
                
                sortedMovements.forEach { m ->
                    val sign = if (m.isIncoming) 1 else -1
                    runningBalance += (m.quantity * sign)
                    
                    canvas.drawText(sdf.format(java.util.Date(m.date)), 580f, y, paint)
                    canvas.drawText(m.type.take(15), 450f, y, paint)
                    canvas.drawText(m.partyName.take(15), 330f, y, paint)
                    
                    val qtyStr = if (m.isIncoming) "+${m.quantity}" else "-${m.quantity}"
                    if (m.isIncoming) paint.color = android.graphics.Color.parseColor("#388E3C") // Green
                    else paint.color = android.graphics.Color.parseColor("#D32F2F") // Red
                    
                    canvas.drawText(qtyStr, 200f, y, paint)
                    
                    paint.color = android.graphics.Color.BLACK
                    canvas.drawText(String.format("%.2f", m.price), 140f, y, paint)
                    
                    paint.isFakeBoldText = true
                    canvas.drawText(runningBalance.toString(), 70f, y, paint)
                    paint.isFakeBoldText = false
                    
                    y += 25f
                }
                
                canvas.drawLine(20f, y, 580f, y, paint)
                
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
                            showToast("تم تنزيل كشف حركة المادة بنجاح في مجلد التنزيلات ✅")
                        }
                    }
                } else {
                    val targetFile = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), fileName)
                    targetFile.outputStream().use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        showToast("تم تنزيل كشف حركة المادة بنجاح في مجلد التنزيلات ✅")
                    }
                }
                pdfDocument.close()
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    showToast("حدث خطأ أثناء تنزيل كشف حركة المادة PDF ❌")
                }
            }
        }
    }
"""

content = content.replace("    fun exportInvoiceToPdf(", func + "\n    fun exportInvoiceToPdf(")

with open("app/src/main/java/com/example/ui/AppViewModel.kt", "w") as f:
    f.write(content)
