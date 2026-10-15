import sys

def patch():
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
        content = f.read()

    # Find InventoryScreen signature
    start_idx = content.find('fun InventoryScreen(')
    if start_idx == -1:
        print("Not found InventoryScreen")
        return
        
    old_vars = """        var inventorySearchQuery by remember { mutableStateOf("") }
        var selectedProductForMovement by remember { mutableStateOf<Product?>(null) }"""
        
    new_vars = """        var inventorySearchQuery by remember { mutableStateOf("") }
        var selectedProductForMovement by remember { mutableStateOf<Product?>(null) }
        val previewFile by viewModel.pdfPreviewFile.collectAsState()"""
        
    content = content.replace(old_vars, new_vars)
    
    # Add Dialog at the end of InventoryScreen
    end_idx = content.find('fun SetupScreen(', start_idx)
    # The last composable block of InventoryScreen ends before SetupScreen
    # So we will insert before the last closing brace of InventoryScreen
    
    last_brace_idx = content.rfind('}', start_idx, end_idx)
    
    dialog_code = """
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
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                        HorizontalDivider()
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
                            PdfViewer(previewFile!!)
                        }
                    }
                }
            }
        }
    """
    
    content = content[:last_brace_idx] + dialog_code + content[last_brace_idx:]
    
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
        f.write(content)

patch()
