import sys

def patch():
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
        content = f.read()

    # Add employees and expenses routes
    old_routes = """                                    "more" -> MoreScreen(viewModel, onNavigate = { currentTab = it }, onLogout = { showLogoutDialog = true })
                                    "backup_restore" -> BackupRestoreScreen(viewModel, onBackClick = { currentTab = "more" })"""
                                    
    new_routes = """                                    "more" -> MoreScreen(viewModel, onNavigate = { currentTab = it }, onLogout = { showLogoutDialog = true })
                                    "backup_restore" -> BackupRestoreScreen(viewModel, onBackClick = { currentTab = "more" })
                                    "employees" -> EmployeesScreen(viewModel, onPrintStatement = { emp -> viewModel.showToast("جاري طباعة كشف حساب: ${emp.name}") })
                                    "expenses" -> ExpenseVoucherScreen(viewModel)"""

    content = content.replace(old_routes, new_routes)

    with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
        f.write(content)

patch()
