import sys

def patch():
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
        content = f.read()

    # Add to Admin Section
    old_admin_menu = """                SidebarItem(
                    id = "settings",
                    label = "إعدادات النظام والتهيئة",
                    icon = Icons.Default.Settings,
                    isSelected = currentTab == "settings",
                    onClick = { onTabSelected("settings"); onCloseDrawer?.invoke() }
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {"""
            
    new_admin_menu = """                SidebarItem(
                    id = "settings",
                    label = "إعدادات النظام والتهيئة",
                    icon = Icons.Default.Settings,
                    isSelected = currentTab == "settings",
                    onClick = { onTabSelected("settings"); onCloseDrawer?.invoke() }
                )
                SidebarItem(
                    id = "employees",
                    label = "إدارة الموظفين",
                    icon = Icons.Default.Person,
                    isSelected = currentTab == "employees",
                    onClick = { onTabSelected("employees"); onCloseDrawer?.invoke() }
                )
                SidebarItem(
                    id = "expenses",
                    label = "سندات الصرف",
                    icon = Icons.Default.AttachMoney,
                    isSelected = currentTab == "expenses",
                    onClick = { onTabSelected("expenses"); onCloseDrawer?.invoke() }
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {"""
            
    content = content.replace(old_admin_menu, new_admin_menu)

    # Add to Non-Admin Section (canAccessAccounting)
    old_acc_menu = """                        SidebarItem(
                            id = "checks",
                            label = "سجل الشيكات",
                            icon = Icons.Default.Article,
                            isSelected = currentTab == "checks",
                            onClick = { onTabSelected("checks"); onCloseDrawer?.invoke() }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))"""
                    
    new_acc_menu = """                        SidebarItem(
                            id = "checks",
                            label = "سجل الشيكات",
                            icon = Icons.Default.Article,
                            isSelected = currentTab == "checks",
                            onClick = { onTabSelected("checks"); onCloseDrawer?.invoke() }
                        )
                        SidebarItem(
                            id = "employees",
                            label = "إدارة الموظفين",
                            icon = Icons.Default.Person,
                            isSelected = currentTab == "employees",
                            onClick = { onTabSelected("employees"); onCloseDrawer?.invoke() }
                        )
                        SidebarItem(
                            id = "expenses",
                            label = "سندات الصرف",
                            icon = Icons.Default.AttachMoney,
                            isSelected = currentTab == "expenses",
                            onClick = { onTabSelected("expenses"); onCloseDrawer?.invoke() }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))"""

    content = content.replace(old_acc_menu, new_acc_menu)
    
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
        f.write(content)

patch()
