with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "color = MaterialTheme.colorScheme.onPrimary," in line:
        # Check if the previous non-empty line contains "shape =" or "modifier ="
        # or if we are inside a Surface
        # Just manually check the line numbers from grep output
        # 499, 2123, 2279(was surface), 2714, 2984, 6897
        if i+1 in [2123, 2714, 2984, 6897, 499]: 
            lines[i] = line.replace("onPrimary", "surface")

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.writelines(lines)

