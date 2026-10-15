with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

idx = content.find('// 2. DASHBOARD SCREEN')
print("Preceding 200 chars:")
print(content[idx-200:idx])

# Replace from testTag("more_item_logout") up to // 2. DASHBOARD SCREEN
start_tag = 'testTag("more_item_logout")'
start_idx = content.find(start_tag)
if start_idx != -1:
    new_card_code = '''testTag("more_item_logout"),
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
    }
  }
}

'''
    content = content[:start_idx] + new_card_code + content[idx:]
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
        f.write(content)
    print("Replaced successfully and closed all braces properly!")
