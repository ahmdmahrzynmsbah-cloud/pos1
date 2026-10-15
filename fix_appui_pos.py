import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# Replace the activeShift block up to `} else {`
block = re.search(r'if \(activeShift == null && currentTab == "pos"\) \{[\s\S]*?\} else \{', content)
if block:
    content = content.replace(block.group(0), "")
    
    # We need to remove the matching closing brace of that `else {`.
    # It's probably after the `AnimatedContent` block which ends around line 629.
    # Let's do it carefully by finding `} // MainTabs` or just counting braces.
    # Since it's tricky, let's just find the closing brace that follows `-> MoreScreen(...)`
    content = re.sub(r'("more" -> MoreScreen[^\n]+)\n\s*\}\n\s*\}', r'\1\n                                }\n', content)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)
