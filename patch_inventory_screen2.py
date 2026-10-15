import sys

def patch():
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
        content = f.read()

    # 1. Remove the wrongly placed block
    wrong_block = """        if (previewFile != null) {
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
        }"""
    
    content = content.replace(wrong_block, "")
    
    # 2. Insert it before `fun ProductMovementDialog`
    target_idx = content.find('fun ProductMovementDialog')
    
    # find the previous '}' before target_idx
    insert_idx = content.rfind('}', 0, target_idx)
    
    content = content[:insert_idx] + wrong_block + "\n    " + content[insert_idx:]
    
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
        f.write(content)

patch()
