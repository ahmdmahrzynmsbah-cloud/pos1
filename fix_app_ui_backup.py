import re

with open("app/src/main/java/com/example/ui/AppUI.kt", "r") as f:
    content = f.read()

# Add to MainTabs switch
content = content.replace(
    '"more" -> MoreScreen(viewModel, onNavigate = { currentTab = it }, onLogout = { showLogoutDialog = true })',
    '"more" -> MoreScreen(viewModel, onNavigate = { currentTab = it }, onLogout = { showLogoutDialog = true })\n                                    "backup_restore" -> BackupRestoreScreen(viewModel, onBackClick = { currentTab = "more" })'
)

# Add to MoreScreen items
more_old = 'Triple("settings", "إعدادات النظام والخيارات المتقدمة ⚙️", Icons.Default.Settings)'
more_new = 'Triple("settings", "إعدادات النظام والخيارات المتقدمة ⚙️", Icons.Default.Settings),\n                Triple("backup_restore", "النسخ الاحتياطي والاستعادة ☁️", Icons.Default.CloudSync)'
content = content.replace(more_old, more_new)

with open("app/src/main/java/com/example/ui/AppUI.kt", "w") as f:
    f.write(content)
