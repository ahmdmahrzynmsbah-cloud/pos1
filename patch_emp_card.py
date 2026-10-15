import sys

def patch():
    with open('app/src/main/java/com/example/ui/EmployeesScreen.kt', 'r') as f:
        content = f.read()

    # Change the EmployeeCard definition
    old_card_def = """fun EmployeeCard(employee: Employee, onPrintStatement: (Employee) -> Unit) {
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
            Button(onClick = { onPrintStatement(employee) }) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تنزيل PDF")
            }
        }
    }
}"""
    
    new_card_def = """import androidx.compose.material.icons.filled.Visibility
fun EmployeeCard(employee: Employee, onPreviewClick: (Employee) -> Unit, onDownloadClick: (Employee) -> Unit) {
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

    content = content.replace(old_card_def, new_card_def)

    # Change the EmployeeCard usage
    old_card_usage = "EmployeeCard(employee, onPrintStatement = { selectedEmployeeForStatement = it })"
    new_card_usage = """EmployeeCard(
                        employee = employee,
                        onPreviewClick = { selectedEmployeeForStatement = it },
                        onDownloadClick = { emp -> 
                            val empExpenses = expenses.filter { it.employeeId == emp.id }
                            onPrintStatement(emp, empExpenses) 
                        }
                    )"""
    content = content.replace(old_card_usage, new_card_usage)

    with open('app/src/main/java/com/example/ui/EmployeesScreen.kt', 'w') as f:
        f.write(content)

patch()
