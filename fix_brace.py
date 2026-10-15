with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

target = '''            Icon(
              imageVector = Icons.Default.ChevronLeft,
              contentDescription = "انتقال",
              tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
              modifier = Modifier.size(20.dp)
            )
          }
        }}// =========================================='''

replacement = '''            Icon(
              imageVector = Icons.Default.ChevronLeft,
              contentDescription = "انتقال",
              tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
              modifier = Modifier.size(20.dp)
            )
          }
        }
    }
}

// =========================================='''

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
        f.write(content)
    print("Fixed closing brace successfully!")
else:
    print("Target not found, checking...")
