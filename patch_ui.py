import re

with open("app/src/main/java/com/example/ui/InvoiceFormScreen.kt", "r") as f:
    content = f.read()

old_sc = """                viewModel.performPurchaseCheckout(
                    paymentMethod = paymentMethod,
                    paidAmount = total,
                    clearCartAfter = false,
                    onSuccess = { createdInvoice ->"""
new_sc = """                viewModel.performPurchaseCheckout(
                    paymentMethod = paymentMethod,
                    paidAmount = total,
                    clearCartAfter = false,
                    isReturnMode = isReturnMode,
                    onSuccess = { createdInvoice ->"""
content = content.replace(old_sc, new_sc)

old_sc2 = """                viewModel.performCheckout(
                    paymentMethod = paymentMethod,
                    paidAmount = total,
                    changeAmount = 0.0,
                    clearCartAfter = false, // Do NOT clear cart automatically!
                    onSuccess = {"""
new_sc2 = """                viewModel.performCheckout(
                    paymentMethod = paymentMethod,
                    paidAmount = total,
                    changeAmount = 0.0,
                    clearCartAfter = false, // Do NOT clear cart automatically!
                    isReturnMode = isReturnMode,
                    onSuccess = {"""
content = content.replace(old_sc2, new_sc2)

with open("app/src/main/java/com/example/ui/InvoiceFormScreen.kt", "w") as f:
    f.write(content)
