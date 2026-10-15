import sys

def patch():
    with open('app/src/main/java/com/example/ui/InvoiceDetailsDialog.kt', 'r') as f:
        content = f.read()

    old_buttons = """                Button(
                    onClick = {
                        viewModel.exportInvoiceItemsToPdf(context, inv, invoiceItems, custName)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "تنزيل PDF", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تنزيل كـ PDF")
                }"""
                
    new_buttons = """                Button(
                    onClick = {
                        viewModel.exportInvoiceItemsToPdf(context, inv, invoiceItems, custName, isPreview = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = "معاينة", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("معاينة", fontSize = 12.sp)
                }
                
                Button(
                    onClick = {
                        viewModel.exportInvoiceItemsToPdf(context, inv, invoiceItems, custName)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "تنزيل PDF", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF", fontSize = 12.sp)
                }"""
                
    content = content.replace(old_buttons, new_buttons)
    
    with open('app/src/main/java/com/example/ui/InvoiceDetailsDialog.kt', 'w') as f:
        f.write(content)

patch()
