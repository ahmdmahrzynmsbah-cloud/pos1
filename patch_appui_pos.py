import sys

def patch():
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
        content = f.read()

    # Find where PosSalesInvoiceScreen is called
    old_block = """    if (activePosView == "hub") {
        MainActionHubScreen(
            viewModel = viewModel,
            onOpenSalesInvoice = {
                isSalesReturn = false
                showSalesSetupDialog = true
            },
            onOpenSalesDraft = { showSalesDraftDialog = true },
            onOpenPurchaseInvoice = {
                isPurchaseReturn = false
                showPurchaseDialog = true
            },
            onOpenPurchaseDraft = { showPurchaseDraftDialog = true },
            onOpenReturns = { showReturnsDialog = true },
            onOpenInventory = { onNavigateTab("inventory") },
            onOpenInvoiceHistory = { showInvoiceHistoryDialog = true }
        )
    } else if (activePosView == "purchase") {
        PosSalesInvoiceScreen(
            viewModel = viewModel,
            onBackClick = { activePosView = "hub" },
            isPurchaseMode = true,
            isReturnMode = isPurchaseReturn
        )
    } else {
        PosSalesInvoiceScreen(
            viewModel = viewModel,
            onBackClick = { activePosView = "hub" },
            isPurchaseMode = false,
            isReturnMode = isSalesReturn
        )
    }"""
    
    new_block = """    var isDraftMode by remember { mutableStateOf(false) }
    
    if (activePosView == "hub") {
        MainActionHubScreen(
            viewModel = viewModel,
            onOpenSalesInvoice = {
                isSalesReturn = false
                isDraftMode = false
                showSalesSetupDialog = true
            },
            onOpenSalesDraft = {
                isSalesReturn = false
                isDraftMode = true
                showSalesSetupDialog = true 
            },
            onOpenPurchaseInvoice = {
                isPurchaseReturn = false
                isDraftMode = false
                showPurchaseDialog = true
            },
            onOpenPurchaseDraft = { 
                isPurchaseReturn = false
                isDraftMode = true
                showPurchaseDialog = true 
            },
            onOpenReturns = { showReturnsDialog = true },
            onOpenInventory = { onNavigateTab("inventory") },
            onOpenInvoiceHistory = { showInvoiceHistoryDialog = true }
        )
    } else if (activePosView == "purchase") {
        PosSalesInvoiceScreen(
            viewModel = viewModel,
            onBackClick = { activePosView = "hub" },
            isPurchaseMode = true,
            isReturnMode = isPurchaseReturn,
            isDraftMode = isDraftMode
        )
    } else {
        PosSalesInvoiceScreen(
            viewModel = viewModel,
            onBackClick = { activePosView = "hub" },
            isPurchaseMode = false,
            isReturnMode = isSalesReturn,
            isDraftMode = isDraftMode
        )
    }"""
    
    content = content.replace(old_block, new_block)
    
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
        f.write(content)

patch()
