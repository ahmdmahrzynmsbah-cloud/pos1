import sys

def patch():
    with open('app/src/main/java/com/example/ui/EmployeesScreen.kt', 'r') as f:
        content = f.read()

    # Update function signature
    content = content.replace(
        "fun EmployeesScreen(viewModel: AppViewModel, onPrintStatement: (Employee) -> Unit)",
        "fun EmployeesScreen(viewModel: AppViewModel, onPrintStatement: (Employee, List<com.example.data.Expense>) -> Unit)"
    )
    
    # Update call inside AlertDialog
    old_btn = """            confirmButton = {
                Button(onClick = { onPrintStatement(emp) }) {"""
    new_btn = """            confirmButton = {
                Button(onClick = { onPrintStatement(emp, empExpenses) }) {"""
    content = content.replace(old_btn, new_btn)
    
    with open('app/src/main/java/com/example/ui/EmployeesScreen.kt', 'w') as f:
        f.write(content)

patch()
