import sys

def patch():
    with open('app/src/main/java/com/example/ui/EmployeesScreen.kt', 'r') as f:
        content = f.read()

    old_card_def = """fun EmployeeCard(employee: Employee, onPreviewClick: (Employee) -> Unit, onDownloadClick: (Employee) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(employee.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(employee.role, color = Color.Gray, fontSize = 12.sp)
                    Text("الراتب: ${employee.salary}", color = Color.Gray, fontSize = 12.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { onPreviewClick(employee) }) {
                    Icon(Icons.Default.Visibility, contentDescription = "معاينة", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { onDownloadClick(employee) }) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "تنزيل PDF", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}"""
    
    new_card_def = """fun EmployeeCard(employee: Employee, onPreviewClick: (Employee) -> Unit, onDownloadClick: (Employee) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(employee.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(employee.role, color = Color.Gray, fontSize = 14.sp)
                    Text("الراتب: ${employee.salary}", color = Color.Gray, fontSize = 14.sp)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                OutlinedButton(onClick = { onPreviewClick(employee) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("معاينة", fontSize = 12.sp)
                }
                Button(onClick = { onDownloadClick(employee) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تنزيل PDF", fontSize = 12.sp)
                }
            }
        }
    }
}"""
    content = content.replace(old_card_def, new_card_def)

    with open('app/src/main/java/com/example/ui/EmployeesScreen.kt', 'w') as f:
        f.write(content)

patch()
