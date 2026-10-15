import sys

def patch():
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
        content = f.read()

    old_block = """                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
    
    new_block = """                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            
                            OutlinedButton(
                                onClick = { viewModel.exportItemMovementToPdf(context, product, allMovements, netMovementQty, isPreview = true) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("معاينة", fontSize = 11.sp)
                            }
                            
                            Button(
                                onClick = { viewModel.exportItemMovementToPdf(context, product, allMovements, netMovementQty) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("PDF", fontSize = 11.sp)
                            }
                            
                            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", modifier = Modifier.size(20.dp))
                            }
                        }"""
    
    content = content.replace(old_block, new_block)
    
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
        f.write(content)

patch()
