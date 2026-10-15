import sys
import re

def patch():
    with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
        content = f.read()

    # Find the end of exportInvoiceToPdf
    # It ends with:
    #                 }
    #                 
    #                 pdfDocument.close()
    #             } catch (e: Exception) {
    #                 kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
    #                     showToast("خطأ أثناء استخراج PDF: ${e.localizedMessage}")
    
    old_code = """                    }
                }
                
                pdfDocument.close()
            } catch (e: Exception) {"""
            
    new_code = """                    }
                }
                
                if (isPreview) {
                    val tempFile = java.io.File(context.cacheDir, "Invoice_Preview_${System.currentTimeMillis()}.pdf")
                    tempFile.outputStream().use { pdfDocument.writeTo(it) }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        pdfPreviewFile.value = tempFile
                    }
                }
                
                pdfDocument.close()
            } catch (e: Exception) {"""

    if old_code in content:
        content = content.replace(old_code, new_code)
    else:
        print("Could not find exact block to replace")
        
    with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
        f.write(content)

patch()
