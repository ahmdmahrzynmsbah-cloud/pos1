with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if 'desc +=' in line:
        print(f"Line {i}: {repr(line)}")
        print(f"Line {i+1}: {repr(lines[i+1])}")

