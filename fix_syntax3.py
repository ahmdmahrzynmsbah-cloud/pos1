import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

old_str = """                    Text(
                      "إجمالي المشتريات التراكمية",
               // Dialogs
      if (showAddExpenseDialog) {"""

new_str = """                    Text(
                      "إجمالي المشتريات التراكمية",
                      fontWeight = FontWeight.Medium,
                      fontSize = 12.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Text(
                    "${formatMoney(0.0)} $currencySymbol",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = CoralError
                  )
                }
              }
            }
          }
        }
      }

      // Dialogs
      if (showAddExpenseDialog) {"""

content = content.replace(old_str, new_str)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)

