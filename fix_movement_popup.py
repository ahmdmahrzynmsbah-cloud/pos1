import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# 1. KPI Cards Row -> 2x2 Grid
kpi_old = """          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Current Stock Card
            Card(
              modifier = Modifier.weight(1f),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
              shape = RoundedCornerShape(12.dp)
            ) {
              Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("المخزون الحالي", fontSize = 10.sp, color = Color.Gray)
                Text("${product.quantity} ${product.unit}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
              }
            }

            // Total Incoming Card
            Card(
              modifier = Modifier.weight(1f),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
              shape = RoundedCornerShape(12.dp)
            ) {
              Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("إجمالي الوارد ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text("+$totalIncomingQty ${product.unit}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
              }
            }

            // Total Outgoing Card
            Card(
              modifier = Modifier.weight(1f),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
              shape = RoundedCornerShape(12.dp)
            ) {
              Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("إجمالي الصادر ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                Text("-$totalOutgoingQty ${product.unit}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onErrorContainer)
              }
            }

            // Net Movement Card
            Card(
              modifier = Modifier.weight(1f),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
              shape = RoundedCornerShape(12.dp)
            ) {
              Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("رصيد الحركة ", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                Text("${if (netMovementQty >= 0) "+$netMovementQty" else "$netMovementQty"} ${product.unit}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
              }
            }
          }"""

kpi_new = """          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              // Total Incoming Card
              Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(12.dp)
              ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("إجمالي الوارد", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text("+$totalIncomingQty ${product.unit}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
              }

              // Total Outgoing Card
              Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp)
              ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("إجمالي الصادر", fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text("-$totalOutgoingQty ${product.unit}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                }
              }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              // Current Stock Card
              Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
              ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("المخزون الحالي", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text("${product.quantity} ${product.unit}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }

              // Net Movement Card
              Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
              ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("رصيد الحركة", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text("${if (netMovementQty >= 0) "+$netMovementQty" else "$netMovementQty"} ${product.unit}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                }
              }
            }
          }"""
content = content.replace(kpi_old, kpi_new)

if kpi_new in content:
    print("KPI UI replaced successfully.")

# Clean up trailing spaces in filter options
filters_old = """val filterOptions = listOf("الكل", "وارد ", "صادر ", "مبيعات ", "مشتريات ", "مرتجعات ")"""
filters_new = """val filterOptions = listOf("الكل", "وارد", "صادر", "مبيعات", "مشتريات", "مرتجعات")"""
content = content.replace(filters_old, filters_new)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)

