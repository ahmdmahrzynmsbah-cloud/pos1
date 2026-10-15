import re

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
    content = f.read()

# Fix the startup logout logic
old_startup = """    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
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

new_startup = """    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      currentUserAccount.collect { user ->
        if (user != null && user.role != "ADMIN") {
          var sysUser = repository.getSystemUserById(user.uid)
          if (sysUser != null) {
            val isExpiredLocally = sysUser.trialEndDate != null && System.currentTimeMillis() > sysUser.trialEndDate!! && !sysUser.isSubscribed
            if (isExpiredLocally || !sysUser.isActive) {
              // Try to fetch latest from cloud before logging out!
              val proj = firebaseProjectId.value
              val apiKey = firebaseWebApiKey.value
              if (proj.isNotEmpty() && apiKey.isNotEmpty()) {
                  try {
                      val remoteUsers = FirebaseSyncManager.getCollection(proj, apiKey, "system_users")
                      val matched = remoteUsers.find { it.first == user.uid }
                      if (matched != null) {
                         val remoteUser = systemUserFromMap(matched.first, matched.second)
                         repository.updateSystemUser(remoteUser)
                         sysUser = remoteUser
                      }
                  } catch (e: Exception) {}
              }
              
              val isExpiredRemote = sysUser!!.trialEndDate != null && System.currentTimeMillis() > sysUser.trialEndDate!! && !sysUser!!.isSubscribed
              if (isExpiredRemote || !sysUser!!.isActive) {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                  logoutSystem()
                  if (isExpiredRemote) {
                      _authState.value = AuthState.Error("عذراً، لقد انتهت فترة اشتراكك. يرجى تجديد الاشتراك.")
                  } else {
                      _authState.value = AuthState.Error("عذراً، تم إيقاف الحساب.")
                  }
                }
              }
            }
          }
        }
      }
    }"""

content = content.replace(old_startup, new_startup)

# Also fix the synchronous verification which also logs out
old_sync_verify = """        // Verify active status in background if logged in synchronously
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
              Log.e("Session", "Failed to verify session", e)
            }
          }
        }"""

new_sync_verify = """        // Verify active status in background if logged in synchronously
        if (savedUid != "admin1" && savedName != null) {
          // This check is now handled more robustly by the currentUserAccount.collect block above,
          // which fetches from cloud if local says inactive. We can just skip this redundant local check.
        }"""

content = content.replace(old_sync_verify, new_sync_verify)

# Fix the login function to fetch from cloud even if user exists locally
old_login = """      try {
        val authResult = FirebaseSyncManager.signIn(apiKey, email, pass)
        var user = repository.getSystemUserById(authResult.uid)
        if (user == null) {
          // Try fetching from cloud
          val remoteUsers = FirebaseSyncManager.getCollection(proj, apiKey, "system_users")
          val matched = remoteUsers.find { it.first == authResult.uid }
          if (matched != null) {
            user = systemUserFromMap(matched.first, matched.second)
            repository.insertSystemUser(user)
          }
        }"""

new_login = """      try {
        val authResult = FirebaseSyncManager.signIn(apiKey, email, pass)
        var user: com.example.data.SystemUser? = null
        try {
          // ALWAYS fetch from cloud first to get latest subscription status
          val remoteUsers = FirebaseSyncManager.getCollection(proj, apiKey, "system_users")
          val matched = remoteUsers.find { it.first == authResult.uid }
          if (matched != null) {
            user = systemUserFromMap(matched.first, matched.second)
            val existing = repository.getSystemUserById(authResult.uid)
            if (existing != null) repository.updateSystemUser(user) else repository.insertSystemUser(user)
          }
        } catch (e: Exception) {
          Log.e("Login", "Failed to fetch remote user", e)
        }
        
        if (user == null) {
          user = repository.getSystemUserById(authResult.uid)
        }"""

content = content.replace(old_login, new_login)

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
    f.write(content)

print("AppViewModel updated.")
