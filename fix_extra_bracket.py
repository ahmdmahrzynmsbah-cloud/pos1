with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "matchesQuery && matchesLowStock && matchesCategory" in line:
        # The next line is '            }\n'
        # The next line is '        }\n'
        # The next line is '        }\n' <-- EXTRA
        if lines[i+1].strip() == "}" and lines[i+2].strip() == "}" and lines[i+3].strip() == "}":
            print("Found extra bracket, deleting it")
            del lines[i+3]
            break

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.writelines(lines)
