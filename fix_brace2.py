with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

part = 'modifier = Modifier.size(20.dp)\n            )\n          }\n        }}// =========================================='
print("In content?", part in content)

content = content.replace(
    'modifier = Modifier.size(20.dp)            )          }        }}// ==========================================',
    'modifier = Modifier.size(20.dp)\n            )\n          }\n        }\n    }\n}\n\n// =========================================='
)

# Also test substring without indentation
if '// 2. DASHBOARD SCREEN' in content:
    idx = content.find('// 2. DASHBOARD SCREEN')
    print("Preceding 50 chars:", repr(content[idx-50:idx]))

