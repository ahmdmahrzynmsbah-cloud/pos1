import sys

def patch():
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
        content = f.read()

    old_block = """                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            IconButton(onClick = {
                                viewModel.exportItemMovementToPdf(context, product, allMovements, netMovementQty)
                            }) {
                                Icon(Icons.Default.Print, contentDescription = "طباعة PDF", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق")
                            }
                        }"""
    
    new_block = """                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            
                            IconButton(onClick = {
                                viewModel.exportItemMovementToPdf(context, product, allMovements, netMovementQty, isPreview = true)
                            }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Visibility, contentDescription = "معاينة", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            
                            IconButton(onClick = {
                                viewModel.exportItemMovementToPdf(context, product, allMovements, netMovementQty)
                            }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            
                            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", modifier = Modifier.size(20.dp))
                            }
                        }"""
    
    content = content.replace(old_block, new_block)
    
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
        f.write(content)

patch()
