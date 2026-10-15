import sys

def patch():
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
        content = f.read()

    # Find PosCartSection
    sig_old = """@Composable
fun PosCartSection(
modifier: Modifier = Modifier,
viewModel: AppViewModel,
cart: List<CartItem>,
customers: List<Customer>,
selectedCustomerId: Int?,
subtotal: Double,
discount: Double,
taxPercent: Double,
taxAmount: Double,
total: Double,
onCheckoutClick: () -> Unit
    ) {"""

    sig_new = """@Composable
fun PosCartSection(
modifier: Modifier = Modifier,
viewModel: AppViewModel,
cart: List<CartItem>,
customers: List<Customer>,
selectedCustomerId: Int?,
subtotal: Double,
discount: Double,
taxPercent: Double,
taxAmount: Double,
total: Double,
isDraftMode: Boolean = false,
onCheckoutClick: () -> Unit
    ) {"""
    
    content = content.replace(sig_old, sig_new)
    
    # Replace the button text inside PosCartSection
    
    button_old = """            Icon(Icons.Default.Payment, contentDescription = "دفع")
            Spacer(modifier = Modifier.width(8.dp))
            Text("دفع وإنشاء الفاتورة", fontSize = 14.sp, fontWeight = FontWeight.Bold)"""
            
    button_new = """            Icon(if (isDraftMode) Icons.Default.Save else Icons.Default.Payment, contentDescription = "دفع")
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isDraftMode) "حفظ مسودة الفاتورة 📝" else "دفع وإنشاء الفاتورة", fontSize = 14.sp, fontWeight = FontWeight.Bold)"""
            
    content = content.replace(button_old, button_new)
    
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
        f.write(content)

patch()
