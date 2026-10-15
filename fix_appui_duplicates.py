import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# First occurrence at ~2843 should be removed. Let's find it.
# It seems it was added near `confirmButton = { TextButton(onClick = { showSupPicker = false }) ...`
bad_block_pattern = r"      if \(showPrintTypeDialogForCust\).*?\} // Customer Account Statement Dialog"
# wait, the first one might not have the // Customer Account Statement Dialog at the end if I injected it wrongly.
# Let's just remove the first one based on its surrounding context: `showSupPicker = false`

pattern_remove = r"      if \(showPrintTypeDialogForCust\) \{\s*val executePrintAction = \{ isDetailed.*?\}\s*\)\s*\}"

# We'll just remove all occurrences and put it back carefully.
content = re.sub(pattern_remove, "", content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)

