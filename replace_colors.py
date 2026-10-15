import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# Replacements
replacements = {
    r"Color\(0xFF1E293B\)": "MaterialTheme.colorScheme.onSurface",
    r"Color\(0xFF2563EB\)": "MaterialTheme.colorScheme.primary",
    r"Color\(0xFFF8FAFC\)": "MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)",
    r"Color\(0xFFF1F5F9\)": "MaterialTheme.colorScheme.surfaceVariant",
    r"Color\(0xFFCBD5E1\)": "MaterialTheme.colorScheme.outlineVariant",
    r"Color\(0xFF64748B\)": "MaterialTheme.colorScheme.onSurfaceVariant",
    r"Color\(0xFF334155\)": "MaterialTheme.colorScheme.onSurface",
    r"Color\(0xFFE2E8F0\)": "MaterialTheme.colorScheme.outlineVariant",
    r"Color\(0xFF0F172A\)": "MaterialTheme.colorScheme.onBackground",
    r"Color\(0xFF10B981\)": "EmeraldGreen", # Leave this or change to primary? Let's use EmeraldGreen if it exists, or Color(0xFF10B981)
    r"Color\(0xFF3B82F6\)": "MaterialTheme.colorScheme.primary",
    r"Color.White": "MaterialTheme.colorScheme.surface",
    r"Color.Black": "MaterialTheme.colorScheme.onSurface",
    r"Color.DarkGray": "MaterialTheme.colorScheme.onSurfaceVariant",
    r"Color.Gray": "Color.Gray" # Some grays are okay, maybe leave it or change to outline/onSurfaceVariant
}

for pattern, replacement in replacements.items():
    if replacement != "Color.Gray" and replacement != "EmeraldGreen":
        content = re.sub(pattern, replacement, content)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)

print("Colors replaced.")
