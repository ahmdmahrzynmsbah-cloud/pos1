import sys

def patch():
    with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
        content = f.read()

    start_idx = content.find('val fileName = "Employee_Statement_${employee.name.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"')
    if start_idx == -1:
        print("Not found start")
        return
        
    end_idx = content.find('pdfDocument.close()', start_idx)
    if end_idx == -1:
        print("Not found end")
        return
        
    old_block = content[start_idx:end_idx + len('pdfDocument.close()')]
    
    new_block = """if (isPreview) {
                    val tempFile = java.io.File(context.cacheDir, "Employee_Statement_Preview.pdf")
                    tempFile.outputStream().use { pdfDocument.writeTo(it) }
                    pdfDocument.close()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        pdfPreviewFile.value = tempFile
                    }
                } else {
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
                                showToast("تم تنزيل الملف بنجاح ✅")
                            }
                        }
                    } else {
                        val targetFile = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), fileName)
                        targetFile.outputStream().use { outputStream ->
                            pdfDocument.writeTo(outputStream)
                        }
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            showToast("تم تنزيل الملف بنجاح ✅")
                        }
                    }
                    pdfDocument.close()
                }"""
                
    content = content.replace(old_block, new_block)
    
    with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
        f.write(content)

patch()
