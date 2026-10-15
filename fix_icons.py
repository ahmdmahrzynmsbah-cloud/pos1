import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# 1. Remove iconBgColor parameter from HubActionCard declaration
content = re.sub(r'iconBgColor:\s*Color,\s*', '', content)

# 2. Update the Box for the Icon
old_icon_box = """            // Top: Icon in circle
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }"""

new_icon_box = """            // Top: Icon in circle
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = LocalContentColor.current,
                    modifier = Modifier.size(24.dp)
                )
            }"""
content = content.replace(old_icon_box, new_icon_box)

# 3. Update the Surface for the secondary pill
old_surface = """                Surface(
                    onClick = onSecondaryClick,
                    shape = RoundedCornerShape(8.dp),
                    color = iconBgColor.copy(alpha = 0.1f),
                    modifier = Modifier.height(26.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = iconBgColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = secondaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = iconBgColor
                        )
                    }
                }"""

new_surface = """                Surface(
                    onClick = onSecondaryClick,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.height(26.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = secondaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }"""
content = content.replace(old_surface, new_surface)

# 4. Remove iconBgColor = ... from callers
content = re.sub(r'iconBgColor\s*=\s*[^,]+,\s*', '', content)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)
print("Replaced!")
