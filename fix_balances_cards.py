import re

with open('app/src/main/java/com/example/ui/BalancesReportScreen.kt', 'r') as f:
    content = f.read()

pattern = re.compile(r'(// Section 2: Summary KPI Stat Cards.*?)(?=\s*// Section 3: Search Input & Filter Chips)', re.DOTALL)

replacement = """// Section 2: Summary KPI Stat Cards
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 1: Total Count
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (activeSubTab == "customers") "عدد العملاء" else "عدد الموردين",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$totalCount",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        // Card 4: Net Balance
                        Card(
                            modifier = Modifier.weight(1.5f),
                            colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "صافي المبالغ المستحقة",
                                    fontSize = 11.sp,
                                    color = PrimaryBlue
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${formatMoneyLocal(netBalance)} $currencySymbol",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 2: Total Debits (مدين)
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = CoralError.copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, CoralError.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (activeSubTab == "customers") "ديون العملاء (مدين)" else "مستحق لنا (مدين)",
                                    fontSize = 11.sp,
                                    color = CoralError,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${formatMoneyLocal(totalDebits)} $currencySymbol",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CoralError
                                )
                            }
                        }
                        
                        // Card 3: Total Credits (دائن)
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (activeSubTab == "customers") "أرصدة فائضة (دائن)" else "ديون الموردين (دائن)",
                                    fontSize = 11.sp,
                                    color = EmeraldGreen,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${formatMoneyLocal(totalCredits)} $currencySymbol",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                            }
                        }
                    }
                }
            }"""

new_content = pattern.sub(replacement, content)

if new_content != content:
    with open('app/src/main/java/com/example/ui/BalancesReportScreen.kt', 'w') as f:
        f.write(new_content)
    print("Successfully replaced.")
else:
    print("Target not found.")

