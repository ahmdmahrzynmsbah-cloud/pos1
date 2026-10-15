import re

with open('app/src/main/java/com/example/ui/BalancesReportScreen.kt', 'r') as f:
    content = f.read()

# 1. Fix the Title
title_old = """                    Text(
                        text = "كشف الأرصدة والديون ⚖️",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )"""
title_new = """                    Text(
                        text = "كشف الأرصدة والديون",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )"""
content = content.replace(title_old, title_new)

# 2. Fix the Tabs text and spacer
cust_old = """                                Icon(
                                    Icons.Default.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "كشف أرصدة العملاء (${customerReportItems.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )"""
cust_new = """                                Icon(
                                    Icons.Default.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "كشف أرصدة العملاء (${customerReportItems.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )"""
content = content.replace(cust_old, cust_new)

sup_old = """                                Icon(
                                    Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "كشف أرصدة الموردين (${supplierReportItems.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )"""
sup_new = """                                Icon(
                                    Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "كشف أرصدة الموردين (${supplierReportItems.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )"""
content = content.replace(sup_old, sup_new)

# 3. Fix the Filter Row missing scroll
filter_old = """                        // Filter Chips Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {"""
filter_new = """                        // Filter Chips Row
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {"""
content = content.replace(filter_old, filter_new)


with open('app/src/main/java/com/example/ui/BalancesReportScreen.kt', 'w') as f:
    f.write(content)
print("Changes applied!")
