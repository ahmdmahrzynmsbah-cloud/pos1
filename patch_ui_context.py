import sys

def patch():
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
        content = f.read()

    old = """fun AppUI(viewModel: AppViewModel) {
    // Force RTL direction for gorgeous Arabic POS look
    
    val toastMessage by viewModel.toastMessage.collectAsState(null)"""
    new = """fun AppUI(viewModel: AppViewModel) {
    // Force RTL direction for gorgeous Arabic POS look
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val toastMessage by viewModel.toastMessage.collectAsState(null)"""
    content = content.replace(old, new)
    
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
        f.write(content)

patch()
