import sys

def patch():
    with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
        content = f.read()

    new_method = """
    fun exportEmployeeStatementToPdf(context: android.content.Context, employee: com.example.data.Employee, expenses: List<com.example.data.Expense>) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    showToast("جاري تجهيز وتنزيل كشف حساب الموظف... 📄")
                }
                
                val pdfDocument = android.graphics.pdf.PdfDocument()
                val pageW = 595
                val margin = 30f
                val rowHeight = 25f
                val headerHeight = 35f
                
                val contentHeight = 300 + headerHeight + (expenses.size * rowHeight) + 50
                val pageH = if (contentHeight > 842) contentHeight.toInt() else 842

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
                    color = android.graphics.Color.parseColor("#1565C0")
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 2f
                }
                val headerBgPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#E3F2FD")
                    style = android.graphics.Paint.Style.FILL
                }

                canvas.drawRect(margin, margin, pageW - margin, pageH - margin, borderPaint)

                var y = margin + 40f
                val xRight = pageW - margin - 20f
                val xLeft = margin + 20f
                val xCenter = pageW / 2f
                
                paint.textAlign = android.graphics.Paint.Align.CENTER
                paint.isFakeBoldText = true
                paint.textSize = 24f
                paint.color = android.graphics.Color.parseColor("#1565C0")
                canvas.drawText(storeName.value.ifEmpty { "مؤسسة الكنز للمحاسبة التجارية" }, xCenter, y, paint)
                
                y += 30f
                paint.textSize = 18f
                paint.color = android.graphics.Color.BLACK
                canvas.drawText("كشف حساب موظف تفصيلي", xCenter, y, paint)
                
                y += 20f
                paint.textSize = 12f
                paint.isFakeBoldText = false
                paint.color = android.graphics.Color.DKGRAY
                val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
                canvas.drawText("تاريخ استخراج الكشف: ${sdf.format(java.util.Date())}", xCenter, y, paint)
                
                y += 30f
                canvas.drawLine(margin + 20, y, pageW - margin - 20, y, borderPaint)
                y += 30f
                
                paint.textAlign = android.graphics.Paint.Align.RIGHT
                paint.textSize = 14f
                paint.color = android.graphics.Color.BLACK
                paint.isFakeBoldText = true
                canvas.drawText("معلومات الموظف:", xRight, y, paint)
                y += 25f
                
                paint.textSize = 12f
                paint.isFakeBoldText = false
                canvas.drawText("اسم الموظف: ${employee.name}", xRight, y, paint)
                canvas.drawText("الوظيفة: ${employee.role}", xCenter, y, paint)
                y += 20f
                canvas.drawText("الراتب الأساسي: ${employee.salary} ر.س", xRight, y, paint)
                canvas.drawText("الهاتف: ${employee.phone.ifEmpty { "غير محدد" }}", xCenter, y, paint)
                y += 40f
                
                canvas.drawRect(xLeft, y - 20f, xRight, y + 10f, headerBgPaint)
                canvas.drawRect(xLeft, y - 20f, xRight, y + 10f, borderPaint)
                
                paint.isFakeBoldText = true
                paint.color = android.graphics.Color.BLACK
                val colDateX = xRight - 10f
                val colTypeX = xRight - 100f
                val colNotesX = xRight - 220f
                val colAmountX = xLeft + 60f
                
                canvas.drawText("التاريخ", colDateX, y, paint)
                canvas.drawText("النوع", colTypeX, y, paint)
                canvas.drawText("البيان", colNotesX, y, paint)
                canvas.drawText("المبلغ", colAmountX, y, paint)
                
                y += 20f
                paint.isFakeBoldText = false
                
                val dateSdf = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
                var totalExpenses = 0.0
                
                if (expenses.isEmpty()) {
                    y += 15f
                    paint.textAlign = android.graphics.Paint.Align.CENTER
                    canvas.drawText("لا يوجد مسحوبات أو سندات صرف مسجلة", xCenter, y, paint)
                    y += 20f
                } else {
                    for ((index, exp) in expenses.withIndex()) {
                        if (index % 2 == 0) {
                            canvas.drawRect(xLeft, y - 15f, xRight, y + 10f, bgPaint)
                        }
                        paint.textAlign = android.graphics.Paint.Align.RIGHT
                        
                        canvas.drawText(dateSdf.format(java.util.Date(exp.date)), colDateX, y, paint)
                        canvas.drawText(exp.type, colTypeX, y, paint)
                        
                        var note = exp.notes
                        if (note.length > 25) note = note.take(22) + "..."
                        canvas.drawText(note, colNotesX, y, paint)
                        
                        paint.color = android.graphics.Color.parseColor("#D32F2F")
                        paint.isFakeBoldText = true
                        canvas.drawText("${exp.amount}", colAmountX, y, paint)
                        
                        paint.color = android.graphics.Color.BLACK
                        paint.isFakeBoldText = false
                        
                        totalExpenses += exp.amount
                        y += rowHeight
                    }
                }
                
                canvas.drawLine(xLeft, y - 15f, xRight, y - 15f, borderPaint)
                y += 20f
                
                y += 20f
                paint.textAlign = android.graphics.Paint.Align.RIGHT
                paint.textSize = 14f
                paint.isFakeBoldText = true
                
                val netSalary = employee.salary - totalExpenses
                
                canvas.drawText("الخلاصة المالية:", xRight, y, paint)
                y += 25f
                
                paint.textSize = 12f
                paint.isFakeBoldText = false
                canvas.drawText("إجمالي الراتب المستحق: ${employee.salary} ر.س", xRight, y, paint)
                y += 20f
                
                paint.color = android.graphics.Color.parseColor("#D32F2F")
                canvas.drawText("إجمالي المسحوبات: $totalExpenses ر.س", xRight, y, paint)
                y += 20f
                
                paint.color = android.graphics.Color.parseColor("#388E3C")
                paint.isFakeBoldText = true
                paint.textSize = 14f
                canvas.drawText("صافي الراتب المتبقي: $netSalary ر.س", xRight, y, paint)
                
                pdfDocument.finishPage(page)

                val fileName = "Employee_Statement_${employee.name.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
                val resolver = context.contentResolver
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
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
                            showToast("تم تنزيل كشف حساب الموظف بنجاح ✅")
                        }
                    }
                } else {
                    val targetFile = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), fileName)
                    targetFile.outputStream().use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        showToast("تم تنزيل كشف حساب الموظف بنجاح ✅")
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
}
"""
    
    content = content.rstrip()
    if content.endswith("}"):
        content = content[:-1].rstrip()
        
    content += "\n" + new_method
    
    with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
        f.write(content)

patch()
