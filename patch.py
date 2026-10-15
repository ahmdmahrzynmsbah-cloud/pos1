import sys

def patch():
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
        content = f.read()
    
    # 1. Purchase Dialog Variables
    old_purch_vars = """        var manualNameLocal by remember { mutableStateOf(viewModel.manualSupplierName.value) }
        var paymentTypeLocal by remember { mutableStateOf("نقدي") }
        var notesLocal by remember { mutableStateOf("") }"""
    
    new_purch_vars = """        var manualNameLocal by remember { mutableStateOf(viewModel.manualSupplierName.value) }
        var paymentTypeLocal by remember { mutableStateOf("نقدي") }
        var checkNumberLocal by remember { mutableStateOf("") }
        var checkBankLocal by remember { mutableStateOf("") }
        val defaultCal1 = remember { java.util.Calendar.getInstance() }
        var checkDateLocal by remember { mutableStateOf(defaultCal1.timeInMillis) }
        var notesLocal by remember { mutableStateOf("") }"""
    content = content.replace(old_purch_vars, new_purch_vars)

    # 2. Sales Dialog Variables
    old_sales_vars = """        var manualNameLocal by remember { mutableStateOf(viewModel.manualCustomerName.value) }
        var paymentTypeLocal by remember { mutableStateOf("نقدي") }
        var notesLocal by remember { mutableStateOf("") }"""
    
    new_sales_vars = """        var manualNameLocal by remember { mutableStateOf(viewModel.manualCustomerName.value) }
        var paymentTypeLocal by remember { mutableStateOf("نقدي") }
        var checkNumberLocal by remember { mutableStateOf("") }
        var checkBankLocal by remember { mutableStateOf("") }
        val defaultCal2 = remember { java.util.Calendar.getInstance() }
        var checkDateLocal by remember { mutableStateOf(defaultCal2.timeInMillis) }
        var notesLocal by remember { mutableStateOf("") }"""
    content = content.replace(old_sales_vars, new_sales_vars)

    # 3. Check Details UI
    check_ui = """
                    if (paymentTypeLocal == "شيك") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("بيانات الشيك:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
                        OutlinedTextField(
                            value = checkNumberLocal,
                            onValueChange = { checkNumberLocal = it },
                            label = { Text("رقم الشيك") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = checkBankLocal,
                            onValueChange = { checkBankLocal = it },
                            label = { Text("اسم البنك") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = checkDateLocal }
                        val datePickerDialog = android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val picked = java.util.Calendar.getInstance()
                                picked.set(year, month, dayOfMonth)
                                checkDateLocal = picked.timeInMillis
                            },
                            cal.get(java.util.Calendar.YEAR),
                            cal.get(java.util.Calendar.MONTH),
                            cal.get(java.util.Calendar.DAY_OF_MONTH)
                        )
                        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }) {
                            OutlinedTextField(
                                value = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault()).format(java.util.Date(checkDateLocal)),
                                onValueChange = { },
                                label = { Text("تاريخ الاستلام") },
                                readOnly = true,
                                enabled = false,
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color.Black,
                                    disabledBorderColor = Color.Gray,
                                    disabledLabelColor = Color.Gray
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
"""

    old_target = """                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Notes"""
    new_target = """                                    }
                                }
                            }
                        }
                    }
""" + check_ui + """
                    Spacer(modifier = Modifier.height(12.dp))

                    // Notes"""
    content = content.replace(old_target, new_target)
    
    old_target_2 = """                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Start Invoice Button"""
    new_target_2 = """                                    }
                                }
                            }
                        }
                    }
""" + check_ui + """
                    Spacer(modifier = Modifier.height(24.dp))

                    // Start Invoice Button"""
    content = content.replace(old_target_2, new_target_2)

    # 4. Save logic in Purchase Dialog
    old_purch_save = """                            viewModel.clearCart()
                            showPurchaseDialog = false"""
    new_purch_save = """                            viewModel.clearCart()
                            viewModel.setupPaymentMethod.value = paymentTypeLocal
                            viewModel.setupCheckNumber.value = checkNumberLocal
                            viewModel.setupCheckBankName.value = checkBankLocal
                            viewModel.setupCheckDate.value = checkDateLocal
                            showPurchaseDialog = false"""
    content = content.replace(old_purch_save, new_purch_save)

    # 5. Save logic in Sales Dialog
    old_sales_save = """                            viewModel.taxPercent.value = 0.0
                            showSalesSetupDialog = false"""
    new_sales_save = """                            viewModel.taxPercent.value = 0.0
                            viewModel.setupPaymentMethod.value = paymentTypeLocal
                            viewModel.setupCheckNumber.value = checkNumberLocal
                            viewModel.setupCheckBankName.value = checkBankLocal
                            viewModel.setupCheckDate.value = checkDateLocal
                            showSalesSetupDialog = false"""
    content = content.replace(old_sales_save, new_sales_save)

    with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
        f.write(content)

patch()
