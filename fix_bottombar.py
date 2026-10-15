import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# First, we need to create the custom bottom app bar layout.
# We will replace the standard NavigationBar with a Box that has a custom shape
# using BottomAppBar and a FloatingActionButton.

old_bottom_bar = """      bottomBar = {
        if (isCompact) {
          Column {
          HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
          NavigationBar(
          containerColor = MaterialTheme.colorScheme.surface,
          tonalElevation = 0.dp,
          windowInsets = WindowInsets.navigationBars
            ) {"""

new_bottom_bar = """      bottomBar = {
        if (isCompact) {
            val canAccessReports = userAccount?.role == "ADMIN" || currentSystemUser?.canAccessReports != false
            val canAccessInventory = userAccount?.role == "ADMIN" || currentSystemUser?.canAccessInventory != false
            val canAccessAccounting = userAccount?.role == "ADMIN" || currentSystemUser?.canAccessAccounting != false
            
            val tabs = if (userAccount?.role == "ADMIN") {
                mutableListOf(
                  TabItem("admin_users", "لوحة التحكم", Icons.Default.SupervisedUserCircle, Icons.Outlined.SupervisedUserCircle),
                  TabItem("admin_settings", "تخصيص التطبيق", Icons.Default.Brush, Icons.Outlined.Brush),
                  TabItem("settings", "الإعدادات", Icons.Default.Settings, Icons.Outlined.Settings)
                )
            } else {
                val list = mutableListOf(TabItem("pos", "نقطة البيع", Icons.Default.PointOfSale, Icons.Outlined.PointOfSale))
                if (canAccessInventory) list.add(TabItem("inventory", "المستودع", Icons.Default.Inventory, Icons.Outlined.Inventory))
                list.add(TabItem("PLACEHOLDER", "", Icons.Default.Add, Icons.Default.Add)) // Placeholder for FAB
                if (canAccessAccounting) list.add(TabItem("accounting", "المحاسبة", Icons.Default.AccountBalance, Icons.Outlined.AccountBalance))
                list.add(TabItem("more", "المزيد", Icons.Default.Menu, Icons.Outlined.Menu))
                list
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                BottomAppBar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
"""

content = content.replace(old_bottom_bar, new_bottom_bar)

old_nav_logic = """              val canAccessReports = userAccount?.role == "ADMIN" || currentSystemUser?.canAccessReports != false
              val canAccessInventory = userAccount?.role == "ADMIN" || currentSystemUser?.canAccessInventory != false
              val canAccessAccounting = userAccount?.role == "ADMIN" || currentSystemUser?.canAccessAccounting != false
              val canAccessAdvancedTools = userAccount?.role == "ADMIN" || currentSystemUser?.canAccessAdvancedTools != false
              val tabs = if (userAccount?.role == "ADMIN") {
                mutableListOf(
                  TabItem("admin_users", "لوحة التحكم", Icons.Default.SupervisedUserCircle, Icons.Outlined.SupervisedUserCircle),
                  TabItem("admin_settings", "تخصيص التطبيق", Icons.Default.Brush, Icons.Outlined.Brush),
                  TabItem("settings", "الإعدادات", Icons.Default.Settings, Icons.Outlined.Settings)
                )
              } else if (isCompact) {
                val list = mutableListOf(TabItem("pos", "نقطة البيع", Icons.Default.PointOfSale, Icons.Outlined.PointOfSale))
                if (canAccessInventory) list.add(TabItem("inventory", "المستودع", Icons.Default.Inventory, Icons.Outlined.Inventory))
                if (canAccessReports) list.add(TabItem("dashboard", "الداشبورد", Icons.Default.Dashboard, Icons.Outlined.Dashboard))
                if (canAccessAccounting) list.add(TabItem("accounting", "المحاسبة", Icons.Default.AccountBalance, Icons.Outlined.AccountBalance))
                list.add(TabItem("more", "المزيد", Icons.Default.Menu, Icons.Outlined.Menu))
                list
              } else {
                val baseList = mutableListOf(TabItem("pos", "نقطة البيع", Icons.Default.PointOfSale, Icons.Outlined.PointOfSale))
                if (canAccessReports) baseList.add(TabItem("dashboard", "الداشبورد", Icons.Default.Dashboard, Icons.Outlined.Dashboard))
                if (canAccessInventory) baseList.add(TabItem("inventory", "المستودع", Icons.Default.Inventory, Icons.Outlined.Inventory))
                if (canAccessAccounting) baseList.add(TabItem("accounting", "المحاسبة", Icons.Default.AccountBalance, Icons.Outlined.AccountBalance))
                if (canAccessAdvancedTools) {
                  baseList.add(TabItem("crm", "العملاء CRM", Icons.Default.People, Icons.Outlined.People))
                }
                baseList.add(TabItem("settings", "الإعدادات", Icons.Default.Settings, Icons.Outlined.Settings))
                baseList
              }
              
            tabs.forEach { tab ->
                val selected = if (isCompact && tab.id == "more") {
                currentTab in listOf("more", "crm", "statements", "balances_report", "checks", "settings", "admin_users", "admin_logs", "admin_firebase", "admin_branches", "invoice_form")
              } else currentTab == tab.id
              NavigationBarItem(
                selected = selected,
                onClick = { currentTab = if (tab.id == "more") "more" else tab.id },
                icon = { Icon(if (selected) tab.iconSelected else tab.iconUnselected, contentDescription = tab.label) },
                label = { Text(tab.label, fontSize = 9.sp, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = MaterialTheme.colorScheme.primary,
                  selectedTextColor = MaterialTheme.colorScheme.primary,
                  indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
              )
            }
          }
          }
        }"""

new_nav_logic = """                  Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceEvenly,
                      verticalAlignment = Alignment.CenterVertically
                  ) {
                      tabs.forEach { tab ->
                          if (tab.id == "PLACEHOLDER") {
                              Spacer(modifier = Modifier.weight(1f))
                          } else {
                              val selected = if (isCompact && tab.id == "more") {
                                  currentTab in listOf("more", "crm", "statements", "balances_report", "checks", "settings", "admin_users", "admin_logs", "admin_firebase", "admin_branches", "invoice_form")
                              } else currentTab == tab.id
                              
                              NavigationBarItem(
                                  selected = selected,
                                  onClick = { currentTab = if (tab.id == "more") "more" else tab.id },
                                  icon = { Icon(if (selected) tab.iconSelected else tab.iconUnselected, contentDescription = tab.label) },
                                  label = { Text(tab.label, fontSize = 9.sp, maxLines = 1) },
                                  colors = NavigationBarItemDefaults.colors(
                                      selectedIconColor = MaterialTheme.colorScheme.primary,
                                      selectedTextColor = MaterialTheme.colorScheme.primary,
                                      indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                  ),
                                  modifier = Modifier.weight(1f)
                              )
                          }
                      }
                  }
                }
                
                // The floating FAB in the center
                if (userAccount?.role != "ADMIN" && canAccessReports) {
                    val isDashboardSelected = currentTab == "dashboard"
                    FloatingActionButton(
                        onClick = { currentTab = "dashboard" },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        containerColor = if (isDashboardSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isDashboardSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-20).dp)
                            .size(64.dp)
                    ) {
                        Icon(
                            if (isDashboardSelected) Icons.Default.Dashboard else Icons.Outlined.Dashboard, 
                            contentDescription = "الداشبورد",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }"""

content = content.replace(old_nav_logic, new_nav_logic)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)

print("AppUI bottom bar updated.")
