import sys

def patch():
    with open('app/src/main/java/com/example/ui/EmployeesScreen.kt', 'r') as f:
        content = f.read()

    # 1. Add context and change preview click logic
    old_start = """fun EmployeesScreen(viewModel: AppViewModel, onPrintStatement: (Employee, List<com.example.data.Expense>) -> Unit) {
    val employees by viewModel.allEmployees.collectAsState()"""
    new_start = """fun EmployeesScreen(viewModel: AppViewModel, onPrintStatement: (Employee, List<com.example.data.Expense>) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val employees by viewModel.allEmployees.collectAsState()"""
    content = content.replace(old_start, new_start)

    old_items = """                    EmployeeCard(
                        employee = employee,
                        onPreviewClick = { selectedEmployeeForStatement = it },
                        onDownloadClick = { emp -> 
                            val empExpenses = expenses.filter { it.employeeId == emp.id }
                            onPrintStatement(emp, empExpenses) 
                        }
                    )"""
    new_items = """                    EmployeeCard(
                        employee = employee,
                        onPreviewClick = { emp -> 
                            val empExpenses = expenses.filter { it.employeeId == emp.id }
                            viewModel.exportEmployeeStatementToPdf(context, emp, empExpenses, true)
                        },
                        onDownloadClick = { emp -> 
                            val empExpenses = expenses.filter { it.employeeId == emp.id }
                            onPrintStatement(emp, empExpenses) 
                        }
                    )"""
    content = content.replace(old_items, new_items)
    
    # 2. Fix the layout of the EmployeeCard
    old_card_def = """fun EmployeeCard(employee: Employee, onPreviewClick: (Employee) -> Unit, onDownloadClick: (Employee) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(employee.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(employee.role, color = Color.Gray, fontSize = 14.sp)
                    Text("الراتب: ${employee.salary}", color = Color.Gray, fontSize = 14.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onPreviewClick(employee) }) {
                    Icon(androidx.compose.material.icons.Icons.Default.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("معاينة")
                }
                Button(onClick = { onDownloadClick(employee) }) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تنزيل PDF")
                }
            }
        }
    }
}"""
    new_card_def = """fun EmployeeCard(employee: Employee, onPreviewClick: (Employee) -> Unit, onDownloadClick: (Employee) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(employee.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(employee.role, color = Color.Gray, fontSize = 14.sp)
                    Text("الراتب: ${employee.salary}", color = Color.Gray, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { onPreviewClick(employee) }) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("معاينة")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(onClick = { onDownloadClick(employee) }) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تنزيل PDF")
                }
            }
        }
    }
}"""
    content = content.replace(old_card_def, new_card_def)

    with open('app/src/main/java/com/example/ui/EmployeesScreen.kt', 'w') as f:
        f.write(content)

patch()
