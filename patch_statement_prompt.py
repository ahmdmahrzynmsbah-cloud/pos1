import re

# Patch AccountStatementsScreen.kt
with open('app/src/main/java/com/example/ui/AccountStatementsScreen.kt', 'r') as f:
    content = f.read()

# Add invoiceItems collectAsState
content = content.replace("val customerTransactions by viewModel.allCustomerTransactions.collectAsState(emptyList())",
"""val customerTransactions by viewModel.allCustomerTransactions.collectAsState(emptyList())
  val invoiceItems by viewModel.allInvoiceItems.collectAsState(emptyList())
  var showStatementTypeDialog by remember { mutableStateOf(false) }""")

# Replace the onClick of the Print icon
old_on_click = """          IconButton(
            onClick = {
              if (selectedCustomer != null) {"""
new_on_click = """          IconButton(
            onClick = {
              if (selectedCustomer != null || selectedSupplier != null) {
                  showStatementTypeDialog = true
              }
            }
          ) {
            Icon(Icons.Default.Print, contentDescription = "طباعة ومشاركة الكشف", tint = MaterialTheme.colorScheme.primary)
          }
        }
      },
      colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface
      )
    )

    if (showStatementTypeDialog) {
      AlertDialog(
        onDismissRequest = { showStatementTypeDialog = false },
        title = { Text("نوع كشف الحساب", fontWeight = FontWeight.Bold) },
        text = { Text("هل تريد إنشاء كشف حساب مجمل أم تفصيلي؟") },
        confirmButton = {
          Button(
            onClick = {
              showStatementTypeDialog = false
              generateStatementPreview(true)
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
          ) { Text("تفصيلي") }
        },
        dismissButton = {
          Button(
            onClick = {
              showStatementTypeDialog = false
              generateStatementPreview(false)
            }
          ) { Text("مجمل") }
        }
      )
    }

    // Function to generate the statement
    val generateStatementPreview = { isDetailed: Boolean ->
        if (selectedCustomer != null) {"""

# Oh wait, we can't easily extract the rest of the on_click block if it's large.
