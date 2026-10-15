import re

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
    content = f.read()

# Fix the first occurrence
bad_block_1 = """        restoreSuppliersFromBackup()
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
        }"""
        
fixed_block_1 = """        restoreSuppliersFromBackup()
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            currentUserAccount.collect { user ->
                if (user != null && user.role != "ADMIN") {
                    val sysUser = repository.getSystemUserById(user.uid)
                    if (sysUser != null) {
                        val isExpired = sysUser.trialEndDate != null && System.currentTimeMillis() > sysUser.trialEndDate!!
                        if (isExpired && !sysUser.isSubscribed) {
                            // Expired
                            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                logoutSystem()
                                _authState.value = AuthState.Error("عذراً، لقد انتهت فترة اشتراكك (10 أيام). يرجى الاشتراك للمتابعة.")
                            }
                        }
                    }
                }
            }
        }"""
        
content = content.replace(bad_block_1, fixed_block_1)

# Fix the second occurrence
bad_block_2 = """    private fun restoreSuppliersFromBackup()
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
        } {"""
        
content = content.replace(bad_block_2, "    private fun restoreSuppliersFromBackup() {")

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
    f.write(content)
