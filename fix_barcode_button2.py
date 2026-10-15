import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

old_barcode = """                trailingIcon = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                      onClick = { showCameraForBarcode = true },
                      modifier = Modifier.testTag("scan_product_barcode_camera_btn")
                    ) {
                      Icon(Icons.Default.PhotoCamera, contentDescription = "مسح بالكاميرا", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    TextButton(
                      onClick = {
                        val randDigits = (1..9).map { (0..9).random() }.joinToString("")
                        barcode = "628$randDigits"
                      }
                    ) {
                      Text("توليد ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }"""

new_barcode = """                trailingIcon = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                      onClick = { showCameraForBarcode = true },
                      modifier = Modifier.testTag("scan_product_barcode_camera_btn")
                    ) {
                      Icon(Icons.Default.PhotoCamera, contentDescription = "مسح بالكاميرا", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                      onClick = {
                        val randDigits = (1..9).map { (0..9).random() }.joinToString("")
                        barcode = "628$randDigits"
                      }
                    ) {
                      Icon(Icons.Default.Autorenew, contentDescription = "توليد", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                  }
                }"""

content = content.replace(old_barcode, new_barcode)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)

print("Barcode 2 updated successfully.")
