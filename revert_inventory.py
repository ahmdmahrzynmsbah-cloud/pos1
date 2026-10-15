import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

lazy_row_code = """
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = inventorySelectedCategory == null,
                            onClick = { inventorySelectedCategory = null },
                            label = { Text("الكل", fontSize = 11.sp) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = inventorySelectedCategory == cat.nameAr,
                            onClick = { inventorySelectedCategory = cat.nameAr },
                            label = { Text(cat.nameAr, fontSize = 11.sp) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
"""

# Replace all occurrences back to the original
content = content.replace(
    lazy_row_code + "\n                Card(\n                    modifier = Modifier\n                        .fillMaxWidth()\n                        .weight(1f),",
    "                Card(\n                    modifier = Modifier\n                        .fillMaxWidth()\n                        .weight(1f),"
)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)
print("Reverted mistakes")
