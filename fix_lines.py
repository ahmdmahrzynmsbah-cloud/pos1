import re

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
    lines = f.readlines()

# Delete lines 2213 to 2231 (0-indexed 2212 to 2230)
# But wait, let's just find the `private fun restoreSuppliersFromBackup()` and keep it and `{`
# Line 2212 is:     private fun restoreSuppliersFromBackup()
# Line 2231 is:  {
# So we can just replace lines 2212 to 2231 with: `    private fun restoreSuppliersFromBackup() {`

lines = lines[:2211] + ["    private fun restoreSuppliersFromBackup() {\n"] + lines[2231:]

# Now for the first block: lines 74 to 91.
# Line 73 is:         restoreSuppliersFromBackup()
# Line 74 is empty.
# Lines 75 to 91 is the launch block.

new_block = """
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
        }
"""
lines = lines[:74] + [new_block] + lines[91:]

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
    f.writelines(lines)
