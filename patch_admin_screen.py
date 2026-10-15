import sys

with open('app/src/main/java/com/example/ui/AdminUsersScreen.kt', 'r') as f:
    content = f.read()

# 1. Add userToDelete state
old_state = "var userToDeactivate by remember { mutableStateOf<SystemUser?>(null) }"
new_state = """var userToDeactivate by remember { mutableStateOf<SystemUser?>(null) }
    var userToDelete by remember { mutableStateOf<SystemUser?>(null) }"""

content = content.replace(old_state, new_state, 1)

# 2. Pass onDeleteClick in LazyColumn
old_card_call = """UserCard(
                                user = user,
                                viewModel = viewModel,
                                onDeactivateClick = { userToDeactivate = user }
                            )"""

new_card_call = """UserCard(
                                user = user,
                                viewModel = viewModel,
                                onDeactivateClick = { userToDeactivate = user },
                                onDeleteClick = { userToDelete = user }
                            )"""

content = content.replace(old_card_call, new_card_call)

# 3. Add userToDelete AlertDialog
deactivate_dialog_end = """        )
    }
}

@OptIn(ExperimentalLayoutApi::class)"""

delete_dialog_and_end = """        )
    }

    // Confirmation Dialog before deleting any system user permanently
    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = CoralError,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    "تأكيد حذف الحساب نهائياً",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    "هل أنت متأكد من رغبتك في حذف حساب المستخدم (${userToDelete?.name}) نهائياً من قاعدة البيانات المحلّية؟\\n\\nتنبيه: هذا الإجراء لا يمكن التراجع عنه وسيمسح بيانات هذا الحساب كلياً من النظام.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        userToDelete?.let { viewModel.deleteUser(it) }
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralError),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("نعم، حذف الحساب نهائياً 🗑️", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { userToDelete = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إلغاء التراجع")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)"""

content = content.replace(deactivate_dialog_end, delete_dialog_and_end, 1)

# 4. Update UserCard signature
old_sig = """fun UserCard(
    user: SystemUser,
    viewModel: AppViewModel,
    onDeactivateClick: () -> Unit
) {"""

new_sig = """fun UserCard(
    user: SystemUser,
    viewModel: AppViewModel,
    onDeactivateClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {"""

content = content.replace(old_sig, new_sig, 1)

# 5. Add Delete button in UserCard
old_deactivate_btn = """                    OutlinedButton(
                        onClick = onDeactivateClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralError),
                        border = BorderStroke(1.dp, CoralError),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("deactivate_button_${user.uid}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تعطيل الحساب", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }"""

new_deactivate_and_delete_btn = """                    OutlinedButton(
                        onClick = onDeactivateClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberAlert),
                        border = BorderStroke(1.dp, AmberAlert),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("deactivate_button_${user.uid}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تعطيل الحساب", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    if (onDeleteClick != null) {
                        Button(
                            onClick = onDeleteClick,
                            colors = ButtonDefaults.buttonColors(containerColor = CoralError),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("delete_button_${user.uid}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حذف الحساب", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }"""

content = content.replace(old_deactivate_btn, new_deactivate_and_delete_btn, 1)

# Also handle the inactive user case where onDeleteClick can be called
old_inactive_activate_btn = """                    Button(
                        onClick = { viewModel.activateUser(user, grantDaysInput.toIntOrNull() ?: 10) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("activate_button_${user.uid}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("موافقة وتفعيل الحساب (${grantDaysInput.ifEmpty { "0" }} يوم)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }"""

new_inactive_activate_btn = """                    Button(
                        onClick = { viewModel.activateUser(user, grantDaysInput.toIntOrNull() ?: 10) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("activate_button_${user.uid}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("موافقة وتفعيل الحساب (${grantDaysInput.ifEmpty { "0" }} يوم)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    if (onDeleteClick != null) {
                        Button(
                            onClick = onDeleteClick,
                            colors = ButtonDefaults.buttonColors(containerColor = CoralError),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("delete_button_${user.uid}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حذف الحساب", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }"""

content = content.replace(old_inactive_activate_btn, new_inactive_activate_btn, 1)

with open('app/src/main/java/com/example/ui/AdminUsersScreen.kt', 'w') as f:
    f.write(content)

print("AdminUsersScreen patched successfully!")
