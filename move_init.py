import sys

def move_init(filename):
    with open(filename, 'r') as f:
        lines = f.readlines()
    
    init_start = -1
    for i, line in enumerate(lines):
        if line.strip() == "init {":
            init_start = i
            break
            
    if init_start == -1:
        print("init block not found")
        return
        
    init_end = -1
    brace_count = 0
    for i in range(init_start, len(lines)):
        line = lines[i]
        if "{" in line:
            brace_count += line.count("{")
        if "}" in line:
            brace_count -= line.count("}")
            if brace_count == 0:
                init_end = i
                break
                
    if init_end == -1:
        print("init block end not found")
        return
        
    init_block = lines[init_start:init_end+1]
    
    # Remove init block from original position
    new_lines = lines[:init_start] + lines[init_end+1:]
    
    # Find the end of the class (the last '}')
    class_end = -1
    for i in range(len(new_lines)-1, -1, -1):
        if new_lines[i].strip() == "}":
            class_end = i
            break
            
    if class_end == -1:
        print("class end not found")
        return
        
    # Insert init block before the end of the class
    final_lines = new_lines[:class_end] + init_block + new_lines[class_end:]
    
    with open(filename, 'w') as f:
        f.writelines(final_lines)
        
    print("Successfully moved init block!")

move_init("app/src/main/java/com/example/ui/AppViewModel.kt")
