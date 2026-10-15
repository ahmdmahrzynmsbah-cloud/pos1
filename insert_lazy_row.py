with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

anchor = """                    FilterChip(
                        selected = showOnlyLowStock,
                        onClick = { showOnlyLowStock = !showOnlyLowStock },
                        label = { Text("حد الطلب (${lowStockProducts.size}) ⚠️", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CoralError,
                            selectedLabelColor = Color.White
                        )
                    )
                }"""

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
                }"""

if anchor in content:
    content = content.replace(anchor, anchor + "\n" + lazy_row_code, 1)
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
        f.write(content)
    print("Inserted successfully.")
else:
    print("Anchor not found!")
