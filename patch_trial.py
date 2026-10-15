import re

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
    content = f.read()

# Change all 30 days to 10 days
content = re.sub(r'30L \* 24L \* 60L \* 60L \* 1000L', '10L * 24L * 60L * 60L * 1000L', content)
content = re.sub(r'30 days trial', '10 days trial', content)
content = re.sub(r'30 Days Trial', '10 Days Trial', content)

# Check expiration on startup
startup_check = """
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            currentUser.collect { user ->
                if (user != null && user.role != "ADMIN") {
                    val sysUser = repository.getSystemUserById(user.uid)
                    if (sysUser != null) {
                        val isExpired = sysUser.trialEndDate != null && System.currentTimeMillis() > sysUser.trialEndDate!!
                        if (isExpired && !sysUser.isSubscribed) {
                            // Expired
                            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                logout()
                                _authState.value = AuthState.Error("عذراً، لقد انتهت فترة اشتراكك (10 أيام). يرجى الاشتراك للمتابعة.")
                            }
                        }
                    }
                }
            }
        }
"""
content = content.replace('restoreSuppliersFromBackup()', 'restoreSuppliersFromBackup()\n' + startup_check)

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
    f.write(content)
