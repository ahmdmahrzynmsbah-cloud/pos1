import sys

def patch():
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
        content = f.read()

    old = """                            toastMessage?.let { msg ->
                            Box(
                            modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 24.dp),
                            contentAlignment = Alignment.BottomCenter
                                ) {"""
    new = """                            toastMessage?.let { msg ->
                            Box(
                            modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 80.dp),
                            contentAlignment = Alignment.TopCenter
                                ) {"""
    content = content.replace(old, new)
    
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
        f.write(content)

patch()
