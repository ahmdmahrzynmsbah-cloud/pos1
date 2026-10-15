import sys

def patch():
    with open('app/src/main/java/com/example/ui/EmployeesScreen.kt', 'r') as f:
        content = f.read()

    # Add PdfViewer Composable
    pdf_viewer_code = """
@Composable
fun PdfViewer(file: java.io.File) {
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(file) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val fileDescriptor = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                val pdfRenderer = android.graphics.pdf.PdfRenderer(fileDescriptor)
                if (pdfRenderer.pageCount > 0) {
                    val page = pdfRenderer.openPage(0)
                    val newBitmap = android.graphics.Bitmap.createBitmap(page.width * 2, page.height * 2, android.graphics.Bitmap.Config.ARGB_8888)
                    newBitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(newBitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmap = newBitmap
                }
                pdfRenderer.close()
                fileDescriptor.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    bitmap?.let { b ->
        import androidx.compose.ui.graphics.asImageBitmap
        import androidx.compose.foundation.Image
        import androidx.compose.foundation.verticalScroll
        import androidx.compose.foundation.rememberScrollState
        Image(
            bitmap = b.asImageBitmap(), 
            contentDescription = "PDF Preview", 
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        )
    } ?: run {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
"""
    # Fix imports
    content = content.replace("import java.util.*", "import java.util.*\nimport androidx.compose.ui.graphics.asImageBitmap\nimport androidx.compose.foundation.Image\nimport androidx.compose.foundation.verticalScroll\nimport androidx.compose.foundation.rememberScrollState\n")

    # Add preview dialog logic
    old_start = """    var showAddDialog by remember { mutableStateOf(false) }
    var selectedEmployeeForStatement by remember { mutableStateOf<Employee?>(null) }"""
    new_start = """    var showAddDialog by remember { mutableStateOf(false) }
    var selectedEmployeeForStatement by remember { mutableStateOf<Employee?>(null) }
    val previewFile by viewModel.pdfPreviewFile.collectAsState()
    
    if (previewFile != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { viewModel.pdfPreviewFile.value = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("معاينة الكشف", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.pdfPreviewFile.value = null }) {
                            Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Divider()
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
                        PdfViewer(previewFile!!)
                    }
                }
            }
        }
    }"""
    content = content.replace(old_start, new_start)

    content += "\n" + pdf_viewer_code.replace("import androidx.compose.ui.graphics.asImageBitmap\n        import androidx.compose.foundation.Image\n        import androidx.compose.foundation.verticalScroll\n        import androidx.compose.foundation.rememberScrollState", "")

    # Add close icon import
    content = content.replace("import androidx.compose.material.icons.filled.Print", "import androidx.compose.material.icons.filled.Print\nimport androidx.compose.material.icons.filled.Close")

    with open('app/src/main/java/com/example/ui/EmployeesScreen.kt', 'w') as f:
        f.write(content)

patch()
