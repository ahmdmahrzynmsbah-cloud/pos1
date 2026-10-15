import re

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
    content = f.read()

old_sync = """        // 1. Sync System Users strictly (Cloud Database is reserved EXCLUSIVELY for accounts)
        val localUsers = repository.allSystemUsers.first()
        localUsers.forEach { user ->
          try {
            FirebaseSyncManager.saveDocument(proj, apiKey, "system_users", user.uid, user.toMap())
          } catch (e: Exception) {
            Log.e("Sync", "Failed to upload user: ${user.uid}", e)
          }
        }
        try {
          val remoteUsers = FirebaseSyncManager.getCollection(proj, apiKey, "system_users")
          remoteUsers.forEach { (uid, fields) ->
            val remoteUser = systemUserFromMap(uid, fields)
            val localUser = repository.getSystemUserById(uid)
            if (localUser == null) {
              repository.insertSystemUser(remoteUser)
            } else {
              repository.updateSystemUser(remoteUser)
            }
          }
        } catch (e: Exception) {"""

new_sync = """        // 1. Sync System Users strictly (Cloud Database is reserved EXCLUSIVELY for accounts)
        // ONLY Admin is allowed to upload system_users to avoid overwriting admin changes (like subscription renewals)
        if (currentUserAccount.value?.role == "ADMIN") {
            val localUsers = repository.allSystemUsers.first()
            localUsers.forEach { user ->
              try {
                FirebaseSyncManager.saveDocument(proj, apiKey, "system_users", user.uid, user.toMap())
              } catch (e: Exception) {
                Log.e("Sync", "Failed to upload user: ${user.uid}", e)
              }
            }
        }
        try {
          val remoteUsers = FirebaseSyncManager.getCollection(proj, apiKey, "system_users")
          remoteUsers.forEach { (uid, fields) ->
            val remoteUser = systemUserFromMap(uid, fields)
            val localUser = repository.getSystemUserById(uid)
            if (localUser == null) {
              repository.insertSystemUser(remoteUser)
            } else {
              // Only overwrite local if it's different to avoid unnecessary writes, or always overwrite to get admin changes
              repository.updateSystemUser(remoteUser)
            }
          }
        } catch (e: Exception) {"""

content = content.replace(old_sync, new_sync)

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
    f.write(content)

print("AppViewModel sync fixed.")
