import re

with open('app/src/main/java/com/example/ui/InvoiceFormScreen.kt', 'r') as f:
    content = f.read()

# Replace the buttons list and rendering loop
old_buttons_block = """        val buttons = listOf(
          listOf("C", "÷", "x", "-"),
          listOf("7", "8", "9", "+"),
          listOf("4", "5", "6", "="),
          listOf("1", "2", "3", "0")
        )
        buttons.forEach { row ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            row.forEach { key ->
              Button(
                onClick = {
                  when (key) {
                    "C" -> calcDisplay = "0"
                    "=" -> computeResult()
                    else -> appendChar(key)
                  }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (key in listOf("=", "+", "-", "x", "÷")) PosTealHeader else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                  contentColor = if (key in listOf("=", "+", "-", "x", "÷")) Color.White else Color.Black
                )
              ) {
                Text(key, fontWeight = FontWeight.Bold, fontSize = 14.sp)
              }
            }
          }
        }"""

new_buttons_block = """        val buttons = listOf(
          listOf("C", "⌫", "÷", "x"),
          listOf("7", "8", "9", "-"),
          listOf("4", "5", "6", "+"),
          listOf("1", "2", "3", "="),
          listOf("0", "00", ".", "")
        )
        buttons.forEach { row ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            row.forEach { key ->
              if (key == "") {
                Spacer(modifier = Modifier.weight(1f))
              } else {
                Button(
                  onClick = {
                    when (key) {
                      "C" -> calcDisplay = "0"
                      "⌫" -> {
                        if (calcDisplay.isNotEmpty() && calcDisplay != "0") {
                           calcDisplay = calcDisplay.dropLast(1)
                           if (calcDisplay.isEmpty() || calcDisplay == "-") calcDisplay = "0"
                        }
                      }
                      "=" -> computeResult()
                      else -> appendChar(key)
                    }
                  },
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = if (key in listOf("=", "+", "-", "x", "÷")) PosTealHeader else if (key == "C" || key == "⌫") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (key in listOf("=", "+", "-", "x", "÷")) Color.White else if (key == "C" || key == "⌫") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                  )
                ) {
                  if (key == "⌫") {
                    Icon(Icons.Default.Backspace, contentDescription = "حذف", modifier = Modifier.size(18.dp))
                  } else {
                    Text(key, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                  }
                }
              }
            }
          }
        }"""

content = content.replace(old_buttons_block, new_buttons_block)

# Fix the calcDisplay initialization to use `val` if needed, wait, it's already `var calcDisplay by remember { mutableStateOf("0") }`

with open('app/src/main/java/com/example/ui/InvoiceFormScreen.kt', 'w') as f:
    f.write(content)

print("Calculator updated.")
