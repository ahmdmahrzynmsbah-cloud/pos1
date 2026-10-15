with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    lines = f.readlines()

new_launcher = """        val speechRecognizerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!matches.isNullOrEmpty()) {
                    inventorySearchQuery = matches[0]
                }
            }
        }
"""

lines.insert(6976, new_launcher)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.writelines(lines)
