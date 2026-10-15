import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

pattern = r"        confirmButton = \{\s*Row\(\s*modifier = Modifier\.fillMaxWidth\(\),\s*horizontalArrangement = Arrangement\.SpaceBetween,\s*verticalAlignment = Alignment\.CenterVertically\s*\)\s*\{\s*TextButton\(onClick = \{ showStatementForCust = null \}\) \{ Text\(\"إغلاق\"\) \}\s*Row\(horizontalArrangement = Arrangement\.spacedBy\(8\.dp\)\) \{.*?Text\(\"PDF\", fontSize = 12\.sp\)\s*\}\s*\}\s*\}\s*\}"

replacement = """        confirmButton = {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            TextButton(onClick = { showStatementForCust = null }) { Text("إغلاق") }
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
        }"""

new_content = re.sub(pattern, replacement, content, flags=re.DOTALL)
if new_content == content:
    print("Warning: regex didn't match anything")
else:
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
        f.write(new_content)
    print("Success")

