import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

replacements = {
    r"Color\(0xFF1565C0\)": "MaterialTheme.colorScheme.primary",
    r"Color\(0xFF00796B\)": "MaterialTheme.colorScheme.secondary",
    r"Color\(0xFF2B78C6\)": "MaterialTheme.colorScheme.primary",
    r"Color\(0xFF1D4ED8\)": "MaterialTheme.colorScheme.primary",
    r"Color\(0xFFDBEAFE\)": "MaterialTheme.colorScheme.primaryContainer",
    r"Color\(0xFFEFF6FF\)": "MaterialTheme.colorScheme.primaryContainer",
    r"Color\(0xFFDCFCE7\)": "MaterialTheme.colorScheme.secondaryContainer",
    r"Color\(0xFF16A34A\)": "MaterialTheme.colorScheme.secondary",
    r"Color\(0xFF15803D\)": "MaterialTheme.colorScheme.onSecondaryContainer",
    r"Color\(0xFF475569\)": "MaterialTheme.colorScheme.onSurfaceVariant",
    r"Color\(0xFF94A3B8\)": "MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.6f)",
    r"Color\(0xFFDC2626\)": "MaterialTheme.colorScheme.error",
    r"Color\(0xFFFEE2E2\)": "MaterialTheme.colorScheme.errorContainer",
    r"Color\(0xFFB91C1C\)": "MaterialTheme.colorScheme.onErrorContainer",
    r"Color\(0xFFFFEDD5\)": "MaterialTheme.colorScheme.tertiaryContainer",
    r"Color\(0xFFC2410C\)": "MaterialTheme.colorScheme.onTertiaryContainer",
    r"Color\(0xFFEA580C\)": "MaterialTheme.colorScheme.tertiary",
    r"Color\(0xFFF3E8FF\)": "MaterialTheme.colorScheme.primaryContainer",
    r"Color\(0xFF7E22CE\)": "MaterialTheme.colorScheme.onPrimaryContainer",
    r"Color\(0xFF9333EA\)": "MaterialTheme.colorScheme.primary",
    r"Color\(0xFFF0FDF4\)": "MaterialTheme.colorScheme.secondaryContainer",
    r"Color\(0xFFFEF2F2\)": "MaterialTheme.colorScheme.errorContainer",
    r"Color\(0xFF86EFAC\)": "MaterialTheme.colorScheme.secondary",
    r"Color\(0xFFFCA5A5\)": "MaterialTheme.colorScheme.error",
}

for pattern, replacement in replacements.items():
    content = re.sub(pattern, replacement, content)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)

print("Remaining colors replaced.")
