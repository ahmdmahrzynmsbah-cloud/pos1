# Revert that specific line
with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    # I replaced all "color = MaterialTheme.colorScheme.surface" with "color = MaterialTheme.colorScheme.onPrimary"
    # But some of them are supposed to be surface.
    # Lines that were surface: 499, 624 (surfaceVariant), 2123, 2233, 2279, 2714, 2984, 3094, 3140, 3418 (surfaceVariant), 3617, 3859, 3889, 6897, 10744
    if i+1 in [499, 2123, 2233, 2279, 2714, 2984, 3094, 3140, 6897]:
        lines[i] = line.replace("onPrimary", "surface")

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.writelines(lines)
