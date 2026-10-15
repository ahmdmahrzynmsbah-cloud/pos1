import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# Add inventorySelectedCategory state
if "var inventorySelectedCategory" not in content:
    content = content.replace(
        "var inventorySearchQuery by remember { mutableStateOf(\"\") }",
        "var inventorySearchQuery by remember { mutableStateOf(\"\") }\n        var inventorySelectedCategory by remember { mutableStateOf<String?>(null) }"
    )

# Update filteredProductsList
filtered_logic = """val filteredProductsList = remember(products, inventorySearchQuery, showOnlyLowStock, inventorySelectedCategory) {
            products.filter { prod ->
                val matchesQuery = inventorySearchQuery.isBlank() ||
                        prod.nameAr.contains(inventorySearchQuery, ignoreCase = true) ||
                        prod.barcode.contains(inventorySearchQuery) ||
                        prod.category.contains(inventorySearchQuery, ignoreCase = true)
                val matchesLowStock = if (showOnlyLowStock) prod.quantity <= prod.minQuantityAlert else true
                val matchesCategory = inventorySelectedCategory == null || prod.category == inventorySelectedCategory
                matchesQuery && matchesLowStock && matchesCategory
            }
        }"""
content = re.sub(
    r'val filteredProductsList = remember\(products, inventorySearchQuery, showOnlyLowStock\).*?}',
    filtered_logic,
    content,
    flags=re.DOTALL
)

# Insert LazyRow for categories below search bar
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

content = content.replace(
    "                Card(\n                    modifier = Modifier\n                        .fillMaxWidth()\n                        .weight(1f),",
    lazy_row_code + "\n                Card(\n                    modifier = Modifier\n                        .fillMaxWidth()\n                        .weight(1f),"
)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)
print("Updated InventoryScreen")
