import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# Fix headers
hdr_old = """          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
              .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("نوع الحركة والمرجع", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
            Text("الطرف الآخر / التاريخ", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
            Text("الكمية والسعر", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
          }"""
hdr_new = """          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
              .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("المرجع", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("التاريخ والطرف", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
            Text("الكمية والسعر", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
          }"""
content = content.replace(hdr_old, hdr_new)

# Fix items
item_old = """                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    // Column 1: Type & Reference
                    Column(modifier = Modifier.weight(1.5f)) {
                      Text(
                        move.type,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = textColor
                      )
                      Text(
                        move.reference,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }

                    // Column 2: Party & Date
                    Column(modifier = Modifier.weight(1.5f)) {
                      Text(
                        move.partyName ?: "---",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                      )
                      Text(
                        move.date,
                        fontSize = 10.sp,
                        color = Color.Gray
                      )
                    }

                    // Column 3: Qty & Amount
                    Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                      Text(
                        "${if(move.isIncoming) "+" else ""}${move.quantity} قطعة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = textColor
                      )
                      if (move.unitPrice != null && move.totalAmount != null) {
                        Text(
                          "= ${formatPosMoney(move.totalAmount)} @ ${formatPosMoney(move.unitPrice)}",
                          fontSize = 9.sp,
                          color = Color.Gray,
                          textAlign = TextAlign.End
                        )
                      }
                    }
                  }"""
item_new = """                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    // Column 1: Type & Reference
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        move.type,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = textColor,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                      )
                      Text(
                        move.reference,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                      )
                    }

                    // Column 2: Party & Date
                    Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                      Text(
                        move.partyName ?: "---",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                      )
                      Text(
                        move.date,
                        fontSize = 9.sp,
                        color = Color.Gray,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                      )
                    }

                    // Column 3: Qty & Amount
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                      Text(
                        "${if(move.isIncoming) "+" else ""}${move.quantity}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = textColor,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                      )
                      if (move.unitPrice != null && move.totalAmount != null) {
                        Text(
                          "@ ${formatPosMoney(move.unitPrice)}",
                          fontSize = 9.sp,
                          color = Color.Gray,
                          maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                      }
                    }
                  }"""
content = content.replace(item_old, item_new)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)
print("Table items updated.")
