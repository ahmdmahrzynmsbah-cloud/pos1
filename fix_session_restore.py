import re

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
    content = f.read()

# Update loginAs
old_loginAs = """  fun loginAs(account: com.example.data.UserAccount) { 
    _currentUserAccount.value = account
    currentUser.value = account.name
    currentUserRole.value = if (account.role == "ADMIN") "مدير النظام" else "كاشير"
    prefs.edit().putString("logged_in_uid", account.uid).putString("logged_in_role", account.role).apply()
  }"""
new_loginAs = """  fun loginAs(account: com.example.data.UserAccount) { 
    _currentUserAccount.value = account
    currentUser.value = account.name
    currentUserRole.value = if (account.role == "ADMIN") "مدير النظام" else "كاشير"
    prefs.edit()
      .putString("logged_in_uid", account.uid)
      .putString("logged_in_name", account.name)
      .putString("logged_in_phone", account.phone)
      .putString("logged_in_role", account.role)
      .apply()
  }"""
content = content.replace(old_loginAs, new_loginAs)

# Update logoutSystem
old_logout = """  fun logoutSystem() { 
    _currentUserAccount.value = null 
    prefs.edit().remove("logged_in_uid").remove("logged_in_role").apply()
  }"""
new_logout = """  fun logoutSystem() { 
    _currentUserAccount.value = null 
    prefs.edit()
      .remove("logged_in_uid")
      .remove("logged_in_name")
      .remove("logged_in_phone")
      .remove("logged_in_role")
      .apply()
  }"""
content = content.replace(old_logout, new_logout)

# Update init block session loading
old_session = """    // Load saved session with high-resiliency try-catch and cleanup on failure
    viewModelScope.launch {
      try {
        val savedUid = prefs.getString("logged_in_uid", null)
        val savedRole = prefs.getString("logged_in_role", null)
        if (savedUid != null && savedRole != null) {
          if (savedRole == "ADMIN" && savedUid == "admin1") {
            loginAs(UserAccount(uid = "admin1", name = "مدير النظام", phone = "01000000000", role = "ADMIN"))
          } else {
            val systemUser = repository.getSystemUserById(savedUid)
            if (systemUser != null && systemUser.isActive) {
              loginAs(UserAccount(uid = systemUser.uid, name = systemUser.name, phone = systemUser.phone, role = systemUser.role))
            } else {
              // Clear stale session details to allow clean authentication
              logoutSystem()
            }
          }
        }
      } catch (e: Exception) {
        Log.e("SessionLoad", "Failed to restore saved session safely", e)
        try {
          logoutSystem() // Reset preference states on corruption/error
        } catch (ex: Exception) {
          Log.e("SessionLoad", "Failed to logout safely", ex)
        }
      }
    }"""
new_session = """    // Load saved session synchronously from SharedPreferences for instant UI rendering
    try {
      val savedUid = prefs.getString("logged_in_uid", null)
      val savedName = prefs.getString("logged_in_name", null)
      val savedPhone = prefs.getString("logged_in_phone", "") ?: ""
      val savedRole = prefs.getString("logged_in_role", null)
      
      // Fallback for older sessions that didn't save name
      if (savedUid != null && savedRole != null) {
        if (savedUid == "admin1") {
          loginAs(UserAccount(uid = "admin1", name = "مدير النظام", phone = "01000000000", role = "ADMIN"))
        } else if (savedName != null) {
          loginAs(UserAccount(uid = savedUid, name = savedName, phone = savedPhone, role = savedRole))
        } else {
          // If we have uid but no name (old session style), we must load from DB
          viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val sysUser = repository.getSystemUserById(savedUid)
            if (sysUser != null && sysUser.isActive) {
              viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                loginAs(UserAccount(uid = sysUser.uid, name = sysUser.name, phone = sysUser.phone, role = sysUser.role))
              }
            } else {
              viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                logoutSystem()
              }
            }
          }
        }
        
        // Verify active status in background if logged in synchronously
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
        }
      }
    } catch (e: Exception) {
      Log.e("SessionLoad", "Failed to restore saved session safely", e)
      logoutSystem()
    }"""
content = content.replace(old_session, new_session)

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
    f.write(content)

print("AppViewModel updated successfully.")
