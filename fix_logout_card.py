with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

target = '''        Card(
      onClick = { onLogout() },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("more_item_logout"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
      elevation = CardDefaults.cardElevation(1.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.ExitToApp,
            contentDescription = "تسجيل الخروج",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(24.dp)
          )
        }
        Text(
          text = "تسجيل الخروج من الحساب ",
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.weight(1f)
        )
        Icon(
          imageVector = Icons.Default.ChevronLeft,
          contentDescription = "انتقال",
          tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
          modifier = Modifier.size(20.dp)
        )
      }
    }'''

replacement = '''        Spacer(modifier = Modifier.height(6.dp))
        Card(
          onClick = { onLogout() },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("more_item_logout"),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(2.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = "تسجيل الخروج",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
              )
            }
            Text(
              text = "تسجيل الخروج من الحساب",
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.weight(1f)
            )
            Icon(
              imageVector = Icons.Default.ChevronLeft,
              contentDescription = "انتقال",
              tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
              modifier = Modifier.size(20.dp)
            )
          }
        }'''

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
        f.write(content)
    print("Logout card style fixed successfully!")
else:
    print("Target not found directly, performing regex replacement...")
    import re
    pattern = r'Card\(\s*onClick = \{ onLogout\(\) \},.*?testTag\("more_item_logout"\).*?\}\s*\}\s*\}'
    # fallback
    match = re.search(pattern, content, re.DOTALL)
    if match:
        content = content[:match.start()] + replacement + content[match.end():]
        with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
            f.write(content)
        print("Regex match replaced successfully.")
    else:
        print("Could not match logout card.")
