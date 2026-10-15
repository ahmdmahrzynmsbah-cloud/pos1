import re

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
    content = f.read()

old_verify = """        // Verify active status in background if logged in synchronously
        if (savedUid != "admin1" && savedName != null) {
          viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
              val systemUser = repository.getSystemUserById(savedUid)
              if (systemUser != null && !systemUser.isActive) {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                  logoutSystem()
                }
              }
            } catch (e: Exception) {
              Log.e("SessionLoad", "Failed to verify session", e)
            }
          }
        }"""

content = content.replace(old_verify, "")

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
    f.write(content)

print("Verification block removed.")
