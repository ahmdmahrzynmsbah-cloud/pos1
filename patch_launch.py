import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

target = "createBackupLauncher.launch(fileName)"
replacement = """try {
                          createBackupLauncher.launch(fileName)
                        } catch (e: Exception) {
                          viewModel.showToast("لا يوجد تطبيق مدعوم لإدارة الملفات على هذا الجهاز.")
                        }"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
        f.write(content)
    print("Patched successfully")
else:
    print("Target not found")
