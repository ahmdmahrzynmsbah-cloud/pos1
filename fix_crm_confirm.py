import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

target = r"          \}\,\n          confirmButton = \{\}\n        \)\n      \}\n\n      // Supplier Statement of Account Dialog"

replacement = """          },
          confirmButton = {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              TextButton(onClick = { selectedCustomerForStatement = null }) { Text("إغلاق") }
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = {
                      pendingPrintActionForCust = "whatsapp"
                      showPrintTypeDialogForCust = true
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                  Text("واتساب", color = MaterialTheme.colorScheme.onPrimary)
                }
                Button(
                  onClick = {
                      pendingPrintActionForCust = "pdf"
                      showPrintTypeDialogForCust = true
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                  Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("PDF", fontSize = 12.sp)
                }
              }
            }
          }
        )
      }

      // Supplier Statement of Account Dialog"""

if target in content:
    print("Found exact match.")
    content = content.replace(target, replacement)
else:
    print("Exact match not found. Using regex.")
    content = re.sub(target, replacement, content, flags=re.MULTILINE)
    
with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)
