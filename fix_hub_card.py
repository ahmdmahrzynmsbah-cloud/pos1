import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

old_func = """@Composable
fun HubActionCard(
    title: String,
    icon: ImageVector,
    iconBgColor: Color,
    onClick: () -> Unit,
    secondaryText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(152.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Icon in circle
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
            }

            // Middle: Title
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Bottom: Secondary action pill
            if (secondaryText != null && onSecondaryClick != null) {
                Surface(
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
                }
            } else {
                Spacer(modifier = Modifier.height(26.dp))
            }
        }
    }
}"""

new_func = """class TopCutoutShape(private val cutoutRadius: Float, private val cornerRadius: Float) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
        val rectPath = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(
                0f, 0f, size.width, size.height,
                androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
            ))
        }
        val cutoutPath = androidx.compose.ui.graphics.Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(
                center = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
                radius = cutoutRadius
            ))
        }
        val finalPath = androidx.compose.ui.graphics.Path()
        finalPath.op(rectPath, cutoutPath, androidx.compose.ui.graphics.PathOperation.Difference)
        return androidx.compose.ui.graphics.Outline.Generic(finalPath)
    }
}

@Composable
fun HubActionCard(
    title: String,
    icon: ImageVector,
    iconBgColor: Color,
    onClick: () -> Unit,
    secondaryText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val cutoutRadius = with(density) { 34.dp.toPx() } // leaves a 6dp gap for 56dp circle (radius 28)
    val cornerRadius = with(density) { 16.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp) // Slightly taller to accommodate floating icon overlapping
    ) {
        // Main Notched Card Body
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp) // Push card body down
                .fillMaxHeight(),
            shape = TopCutoutShape(cutoutRadius, cornerRadius),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 34.dp, bottom = 8.dp, start = 8.dp, end = 8.dp), // Push content down below notch
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Title
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Secondary action pill
                if (secondaryText != null && onSecondaryClick != null) {
                    Surface(
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
                    }
                } else {
                    Spacer(modifier = Modifier.height(26.dp))
                }
            }
        }

        // Floating Icon
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(56.dp) // Larger circle
                .shadow(4.dp, CircleShape) // Drop shadow
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(28.dp) // Larger icon inside
            )
        }
    }
}"""

if old_func in content:
    content = content.replace(old_func, new_func)
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
        f.write(content)
    print("UI replaced successfully.")
else:
    print("Could not find the target codeblock!")
