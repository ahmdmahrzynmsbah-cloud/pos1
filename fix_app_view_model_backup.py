import re

with open("app/src/main/java/com/example/ui/AppViewModel.kt", "r") as f:
    content = f.read()

# Add functions inside AppViewModel
funcs = """
    fun checkpointDatabase() {
        try {
            database.query(androidx.sqlite.db.SimpleSQLiteQuery("pragma wal_checkpoint(full)"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun closeDatabase() {
        try {
            database.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
"""

# Insert right after class AppViewModel(application: Application) : AndroidViewModel(application) {
# or somewhere inside. Let's find "    fun logoutSystem()" and put it before.
content = content.replace("    fun logoutSystem() {", funcs + "\n    fun logoutSystem() {")

with open("app/src/main/java/com/example/ui/AppViewModel.kt", "w") as f:
    f.write(content)
