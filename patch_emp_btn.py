import sys

def patch():
    with open('app/src/main/java/com/example/ui/EmployeesScreen.kt', 'r') as f:
        content = f.read()

    # The dialog button
    old_dialog = """                Button(onClick = { onPrintStatement(emp, empExpenses) }) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("طباعة")
                }"""
    new_dialog = """                Button(onClick = { onPrintStatement(emp, empExpenses) }) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تنزيل PDF")
                }"""
    content = content.replace(old_dialog, new_dialog)

    # The card button
    old_card = """            Button(onClick = { onPrintStatement(employee) }) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("كشف حساب")
            }"""
    new_card = """            Button(onClick = { onPrintStatement(employee) }) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تنزيل PDF")
            }"""
    content = content.replace(old_card, new_card)

    with open('app/src/main/java/com/example/ui/EmployeesScreen.kt', 'w') as f:
        f.write(content)

patch()
