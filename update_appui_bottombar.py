import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# Pattern to replace bottomBar in AppUI.kt
pattern = r'bottomBar\s*=\s*\{\s*if\s*\(isCompact\)\s*\{.*?\}\s*\}\s*\}\s*\)\s*\{\s*innerPadding\s*->'

replacement = """bottomBar = {
        if (isCompact) {
            val canAccessReports = userAccount?.role == "ADMIN" || currentSystemUser?.canAccessReports != false
            val canAccessInventory = userAccount?.role == "ADMIN" || currentSystemUser?.canAccessInventory != false
            val canAccessAccounting = userAccount?.role == "ADMIN" || currentSystemUser?.canAccessAccounting != false
            
            CurvedBottomNavigationBar(
                currentTab = currentTab,
                onTabSelected = { tab ->
                    if (tab == "more") {
                        currentTab = "more"
                    } else {
                        currentTab = tab
                    }
                },
                canAccessInventory = canAccessInventory,
                canAccessAccounting = canAccessAccounting,
                canAccessReports = canAccessReports,
                isAdmin = (userAccount?.role == "ADMIN")
            )
        }
      }
    ) { innerPadding ->"""

new_content = re.sub(pattern, replacement, content, flags=re.DOTALL)

if new_content != content:
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
        f.write(new_content)
    print("AppUI.kt successfully updated with CurvedBottomNavigationBar!")
else:
    print("Regex did not match, trying line slice replacement...")
    # fallback
    start_idx = content.find("bottomBar = {")
    end_idx = content.find(") { innerPadding ->", start_idx)
    if start_idx != -1 and end_idx != -1:
        content = content[:start_idx] + replacement + content[end_idx + len(") { innerPadding ->"):]
        with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
            f.write(content)
        print("Line slice replacement done.")
    else:
        print("Could not find indices:", start_idx, end_idx)
