import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# 1. Update HubActionCard signature
content = content.replace(
    'fun HubActionCard(\n    title: String,\n    icon: ImageVector,\n    onClick: () -> Unit,',
    'fun HubActionCard(\n    title: String,\n    icon: ImageVector,\n    iconBgColor: Color,\n    onClick: () -> Unit,'
)

# 2. Update the Box inside HubActionCard
old_icon_box = """            // Top: Icon in circle
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

new_icon_box = """            // Top: Icon in circle
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
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }"""
content = content.replace(old_icon_box, new_icon_box)

# 3. Update the Surface for secondary action
old_surface = """                Surface(
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

new_surface = """                Surface(
                    onClick = onSecondaryClick,
                    shape = RoundedCornerShape(8.dp),
                    color = iconBgColor.copy(alpha = 0.15f),
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
content = content.replace(old_surface, new_surface)

# 4. Add colors to callers
content = content.replace(
    'title = "إنشاء فاتورة مبيع",\n                icon = Icons.Default.ShoppingCart,\n                onClick',
    'title = "إنشاء فاتورة مبيع",\n                icon = Icons.Default.ShoppingCart,\n                iconBgColor = Color(0xFF0284C7),\n                onClick'
)
content = content.replace(
    'title = "إنشاء فاتورة شراء",\n                icon = Icons.Default.LocalShipping,\n                onClick',
    'title = "إنشاء فاتورة شراء",\n                icon = Icons.Default.LocalShipping,\n                iconBgColor = Color(0xFF16A34A),\n                onClick'
)
content = content.replace(
    'title = "جرد المستودع",\n                icon = Icons.Default.Inventory2,\n                onClick',
    'title = "جرد المستودع",\n                icon = Icons.Default.Inventory2,\n                iconBgColor = Color(0xFFD97706),\n                onClick'
)
content = content.replace(
    'title = "فواتير المرتجع والتوالف",\n                icon = Icons.Default.PublishedWithChanges,\n                onClick',
    'title = "فواتير المرتجع والتوالف",\n                icon = Icons.Default.PublishedWithChanges,\n                iconBgColor = Color(0xFFE11D48),\n                onClick'
)
content = content.replace(
    'title = "سجل الفواتير السابقة",\n                icon = Icons.Default.ReceiptLong,\n                onClick',
    'title = "سجل الفواتير السابقة",\n                icon = Icons.Default.ReceiptLong,\n                iconBgColor = Color(0xFF7C3AED),\n                onClick'
)
content = content.replace(
    'title = "مسودات الفواتير",\n                icon = Icons.Default.EditNote,\n                onClick',
    'title = "مسودات الفواتير",\n                icon = Icons.Default.EditNote,\n                iconBgColor = Color(0xFF059669),\n                onClick'
)


with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)

print("Restored!")
