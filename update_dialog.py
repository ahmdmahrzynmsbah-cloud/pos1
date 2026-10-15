import re

with open("app/src/main/java/com/example/ui/AppUI.kt", "r") as f:
    content = f.read()

old_header = """                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }"""

new_header = """                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            IconButton(onClick = {
                                viewModel.exportItemMovementToPdf(context, product, allMovements, netMovementQty)
                            }) {
                                Icon(Icons.Default.Print, contentDescription = "طباعة PDF", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق")
                            }
                        }
                    }"""

content = content.replace(old_header, new_header)

with open("app/src/main/java/com/example/ui/AppUI.kt", "w") as f:
    f.write(content)
