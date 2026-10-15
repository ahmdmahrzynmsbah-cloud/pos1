import sys

def patch():
    with open('app/src/main/java/com/example/ui/InvoiceFormScreen.kt', 'r') as f:
        content = f.read()

    # Find PosSalesInvoiceScreen signature
    sig_old = """fun PosSalesInvoiceScreen(
    viewModel: AppViewModel,
    onBackClick: () -> Unit,
    isPurchaseMode: Boolean = false,
    isReturnMode: Boolean = false
) {"""

    sig_new = """fun PosSalesInvoiceScreen(
    viewModel: AppViewModel,
    onBackClick: () -> Unit,
    isPurchaseMode: Boolean = false,
    isReturnMode: Boolean = false,
    isDraftMode: Boolean = false
) {"""
    
    content = content.replace(sig_old, sig_new)
    
    with open('app/src/main/java/com/example/ui/InvoiceFormScreen.kt', 'w') as f:
        f.write(content)

patch()
