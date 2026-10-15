import sys

def patch():
    with open('app/src/main/java/com/example/data/AppRepository.kt', 'r') as f:
        content = f.read()
    
    # Remove all CartItem at the end to be safe
    content = content.split("data class CartItem")[0]
    
    # Make sure we only have one closing brace at the end
    content = content.rstrip()
    while content.endswith("}"):
        content = content[:-1].rstrip()
    
    content += "\n}\n\ndata class CartItem(\n    val product: Product,\n    val quantity: Int\n)\n"
    
    with open('app/src/main/java/com/example/data/AppRepository.kt', 'w') as f:
        f.write(content)

patch()
