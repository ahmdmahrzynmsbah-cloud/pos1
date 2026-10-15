import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

target = """                        trailingIcon = {
                            if (inventorySearchQuery.isNotEmpty()) {
                                IconButton(onClick = { inventorySearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح", modifier = Modifier.size(18.dp))
                                }
                            }
                        },"""

replacement = """                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (inventorySearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { inventorySearchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "مسح", modifier = Modifier.size(18.dp))
                                    }
                                }
                                IconButton(onClick = {
                                    try {
                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                                            putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث للبحث عن منتج...")
                                        }
                                        speechRecognizerLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        viewModel.showToast("عذراً، ميزة البحث الصوتي غير مدعومة في جهازك")
                                    }
                                }) {
                                    Icon(Icons.Default.Mic, contentDescription = "بحث صوتي", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)
