import sys

def patch():
    with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
        content = f.read()

    # Add pdfPreviewFile state
    old_state = "    val subscriptionStatus = MutableStateFlow(\"ACTIVE\")"
    new_state = "    val subscriptionStatus = MutableStateFlow(\"ACTIVE\")\n    val pdfPreviewFile = MutableStateFlow<java.io.File?>(null)"
    content = content.replace(old_state, new_state)

    # Change exportEmployeeStatementToPdf to use the state for preview
    old_save_q = """                    val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            pdfDocument.writeTo(outputStream)
                        }
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            if (isPreview) {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    showToast("لا يوجد تطبيق لفتح ملفات PDF")
                                }
                            } else {
                                showToast("تم تنزيل الملف بنجاح ✅")
                            }
                        }
                    }"""
    
    new_save_q = """                    val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            pdfDocument.writeTo(outputStream)
                        }
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            showToast("تم تنزيل الملف بنجاح ✅")
                        }
                    }"""
    
    old_save_pre_q = """                    targetFile.outputStream().use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (isPreview) {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(android.net.Uri.fromFile(targetFile), "application/pdf")
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                showToast("لا يوجد تطبيق لفتح ملفات PDF")
                            }
                        } else {
                            showToast("تم تنزيل الملف بنجاح ✅")
                        }
                    }"""
    
    new_save_pre_q = """                    targetFile.outputStream().use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        showToast("تم تنزيل الملف بنجاح ✅")
                    }"""
    
    # We will replace the whole block starting from `val fileName =`
    old_block = """                val fileName = "Employee_Statement_${employee.name.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
                val resolver = context.contentResolver
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
""" + old_save_q + """
                } else {
                    val targetFile = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), fileName)
""" + old_save_pre_q + """
                }
                
                pdfDocument.close()"""
                
    new_block = """                if (isPreview) {
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
""" + new_save_q + """
                    } else {
                        val targetFile = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), fileName)
""" + new_save_pre_q + """
                    }
                    pdfDocument.close()
                }"""
    
    content = content.replace(old_block, new_block)
    
    with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
        f.write(content)

patch()
