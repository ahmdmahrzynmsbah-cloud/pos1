import sys

def patch():
    with open('app/src/main/java/com/example/ui/InvoiceFormScreen.kt', 'r') as f:
        content = f.read()

    # Find the save button
    old_btn = """                Button(
                    onClick = {
                        val parsed = chkDateInput.toLongOrNull() ?: System.currentTimeMillis()
                        performActualSave(chkNumInput, chkBankInput, parsed)
                        showCheckDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PosTealHeader)
                ) {
                    Text("حفظ وتأكيد", color = Color.White)
                }"""
    new_btn = """                Button(
                    onClick = {
                        val parsed = chkDateInput.toLongOrNull() ?: System.currentTimeMillis()
                        performActualSave(chkNumInput, chkBankInput, parsed)
                        showCheckDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PosTealHeader)
                ) {
                    Text(if (isDraftMode) "حفظ المسودة" else "حفظ وتأكيد", color = Color.White)
                }"""
    content = content.replace(old_btn, new_btn)

    old_btn2 = """                Button(
                    onClick = {
                        performActualSave(null, null, null)
                        showConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PosTealHeader)
                ) {
                    Text("حفظ الفاتورة", color = Color.White)
                }"""
    new_btn2 = """                Button(
                    onClick = {
                        performActualSave(null, null, null)
                        showConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PosTealHeader)
                ) {
                    Text(if (isDraftMode) "حفظ المسودة" else "حفظ الفاتورة", color = Color.White)
                }"""
    content = content.replace(old_btn2, new_btn2)
    
    # Check if there is "دفع وإنشاء الفاتورة" text anywhere else
    old_btn_main = """                Button(
                    onClick = {
                        if (paymentMethod == "شيك") showCheckDialog = true
                        else showConfirmationDialog = true
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PosTealHeader)
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isPurchaseMode) "دفع وتوريد" else "دفع وإنشاء", fontWeight = FontWeight.Bold, color = Color.White)
                }"""
    new_btn_main = """                Button(
                    onClick = {
                        if (paymentMethod == "شيك") showCheckDialog = true
                        else showConfirmationDialog = true
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PosTealHeader)
                ) {
                    Icon(if (isDraftMode) Icons.Default.Save else Icons.Default.Payment, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isDraftMode) "حفظ كمسودة" else if (isPurchaseMode) "دفع وتوريد" else "دفع وإنشاء", fontWeight = FontWeight.Bold, color = Color.White)
                }"""
    content = content.replace(old_btn_main, new_btn_main)

    with open('app/src/main/java/com/example/ui/InvoiceFormScreen.kt', 'w') as f:
        f.write(content)

patch()
