import sys

def patch():
    with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
        content = f.read()

    # Change signature
    old_sig = "fun exportEmployeeStatementToPdf(context: android.content.Context, employee: com.example.data.Employee, expenses: List<com.example.data.Expense>) {"
    new_sig = "fun exportEmployeeStatementToPdf(context: android.content.Context, employee: com.example.data.Employee, expenses: List<com.example.data.Expense>, isPreview: Boolean = false) {"
    content = content.replace(old_sig, new_sig)

    # Change the saving logic to support preview
    old_save_q = """                    val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            pdfDocument.writeTo(outputStream)
                        }
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            showToast("تم تنزيل الملف بنجاح ✅")
                        }
                    }"""
    
    new_save_q = """                    val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
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
    content = content.replace(old_save_q, new_save_q)

    # Also for non-Q
    old_save_pre_q = """                    targetFile.outputStream().use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        showToast("تم تنزيل الملف بنجاح ✅")
                    }"""
    new_save_pre_q = """                    targetFile.outputStream().use { outputStream ->
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
    content = content.replace(old_save_pre_q, new_save_pre_q)

    with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
        f.write(content)

patch()
