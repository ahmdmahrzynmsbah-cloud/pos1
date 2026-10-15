import sys

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    lines = f.readlines()

new_kpi = """          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
          }
"""

lines = lines[:8604] + [new_kpi] + lines[8657:]

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.writelines(lines)
print("Lines replaced.")
