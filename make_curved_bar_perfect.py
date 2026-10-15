code = '''package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CurvedNavItem(
    val id: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun CurvedBottomNavigationBar(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    canAccessInventory: Boolean = true,
    canAccessAccounting: Boolean = true,
    canAccessReports: Boolean = true,
    isAdmin: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val barBackgroundColor = MaterialTheme.colorScheme.surface
    
    // Luxury Navy Scoop accent color adapting to Light/Dark Mode
    val scoopColor = if (isDark) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color(0xFF1E3A8A) // Rich Deep Royal Navy
    }
    
    val centerButtonBg = if (isDark) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFF1E3A8A)
    }
    
    val centerButtonContentColor = if (isDark) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        Color.White
    }

    // Top border color
    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // 2 items on the right, 2 items on the left (RTL friendly)
    // Left items (Right side in RTL, Left in LTR): Dashboard & Inventory
    val leftItems = remember(isAdmin, canAccessReports, canAccessInventory) {
        if (isAdmin) {
            listOf(
                CurvedNavItem("admin_users", "المستخدمين", Icons.Default.SupervisedUserCircle, Icons.Outlined.SupervisedUserCircle),
                CurvedNavItem("admin_settings", "التخصيص", Icons.Default.Brush, Icons.Outlined.Brush)
            )
        } else {
            val list = mutableListOf<CurvedNavItem>()
            if (canAccessReports) {
                list.add(CurvedNavItem("dashboard", "الداشبورد", Icons.Default.Dashboard, Icons.Outlined.Dashboard))
            } else {
                list.add(CurvedNavItem("crm", "العملاء", Icons.Default.People, Icons.Outlined.People))
            }
            if (canAccessInventory) {
                list.add(CurvedNavItem("inventory", "المستودع", Icons.Default.Inventory, Icons.Outlined.Inventory))
            } else {
                list.add(CurvedNavItem("statements", "الكشوفات", Icons.Default.ReceiptLong, Icons.Outlined.ReceiptLong))
            }
            list
        }
    }

    // Right items: Accounting & More
    val rightItems = remember(isAdmin, canAccessAccounting) {
        if (isAdmin) {
            listOf(
                CurvedNavItem("admin_logs", "السجلات", Icons.Default.History, Icons.Outlined.History),
                CurvedNavItem("settings", "الإعدادات", Icons.Default.Settings, Icons.Outlined.Settings)
            )
        } else {
            val list = mutableListOf<CurvedNavItem>()
            if (canAccessAccounting) {
                list.add(CurvedNavItem("accounting", "المحاسبة", Icons.Default.AccountBalance, Icons.Outlined.AccountBalance))
            } else {
                list.add(CurvedNavItem("checks", "الشيكات", Icons.Default.Article, Icons.Outlined.Article))
            }
            list.add(CurvedNavItem("more", "المزيد", Icons.Default.Menu, Icons.Outlined.Menu))
            list
        }
    }

    // The primary default landing tab in the center is POS ("pos" / نقطة البيع)
    val isCenterSelected = currentTab == "pos"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // Height of the bar base without insets
        val barHeight = 76.dp

        // Canvas drawing the background bar with the smooth curved notch and scoop
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight + navBottomPadding)
                .align(Alignment.BottomCenter)
        ) {
            val w = size.width
            val totalH = size.height
            val centerX = w / 2f
            val topY = 0f

            val scoopHalfWidth = 74.dp.toPx()
            val scoopDepth = 36.dp.toPx()

            // 1. Draw Surface Background with Notch
            val barPath = Path().apply {
                moveTo(0f, topY)
                lineTo(centerX - scoopHalfWidth, topY)
                
                // Smooth Bézier curve downward into the scoop
                cubicTo(
                    centerX - scoopHalfWidth * 0.52f, topY,
                    centerX - scoopHalfWidth * 0.48f, topY + scoopDepth,
                    centerX, topY + scoopDepth
                )
                cubicTo(
                    centerX + scoopHalfWidth * 0.48f, topY + scoopDepth,
                    centerX + scoopHalfWidth * 0.52f, topY,
                    centerX + scoopHalfWidth, topY
                )
                
                lineTo(w, topY)
                lineTo(w, totalH)
                lineTo(0f, totalH)
                close()
            }

            // Draw Bar Background
            drawPath(
                path = barPath,
                color = barBackgroundColor
            )

            // Draw Top Border Lines
            drawLine(
                color = borderColor,
                start = Offset(0f, topY),
                end = Offset(centerX - scoopHalfWidth, topY),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = borderColor,
                start = Offset(centerX + scoopHalfWidth, topY),
                end = Offset(w, topY),
                strokeWidth = 1.5.dp.toPx()
            )

            // 2. Draw Navy Scoop / Cradle Accent (matching reference image)
            val cradleW = 70.dp.toPx()
            val cradleDepth = 42.dp.toPx()
            val cradleThickness = 12.dp.toPx()

            val scoopPath = Path().apply {
                moveTo(centerX - cradleW, topY)
                cubicTo(
                    centerX - cradleW * 0.52f, topY,
                    centerX - cradleW * 0.45f, topY + cradleDepth,
                    centerX, topY + cradleDepth
                )
                cubicTo(
                    centerX + cradleW * 0.45f, topY + cradleDepth,
                    centerX + cradleW * 0.52f, topY,
                    centerX + cradleW, topY
                )
                // Bottom curve back
                cubicTo(
                    centerX + cradleW * 0.62f, topY + cradleThickness,
                    centerX + cradleW * 0.45f, topY + cradleDepth + cradleThickness,
                    centerX, topY + cradleDepth + cradleThickness
                )
                cubicTo(
                    centerX - cradleW * 0.45f, topY + cradleDepth + cradleThickness,
                    centerX - cradleW * 0.62f, topY + cradleThickness,
                    centerX - cradleW, topY
                )
                close()
            }

            drawPath(
                path = scoopPath,
                color = scoopColor
            )
        }

        // Navigation Items Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .align(Alignment.BottomCenter)
                .padding(bottom = (navBottomPadding * 0.4f) + 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Group
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                leftItems.forEach { item ->
                    val isSelected = currentTab == item.id
                    CurvedNavButton(
                        item = item,
                        isSelected = isSelected,
                        onClick = { onTabSelected(item.id) },
                        isDark = isDark
                    )
                }
            }

            // Center Spacer for the Main Center Button and Label
            Column(
                modifier = Modifier
                    .width(88.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = if (isAdmin) "الرئيسية" else "نقطة البيع",
                    fontSize = 11.sp,
                    fontWeight = if (isCenterSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isCenterSelected) {
                        if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF1E3A8A)
                    } else {
                        if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    },
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            // Right Group
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                rightItems.forEach { item ->
                    val isSelected = if (item.id == "more") {
                        currentTab in listOf("more", "crm", "statements", "balances_report", "checks", "settings", "admin_users", "admin_logs", "admin_firebase", "admin_branches", "invoice_form")
                    } else currentTab == item.id
                    
                    CurvedNavButton(
                        item = item,
                        isSelected = isSelected,
                        onClick = { onTabSelected(item.id) },
                        isDark = isDark
                    )
                }
            }
        }

        // The Center Main Action Button (نقطة البيع - POS Main Tab)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-6).dp)
                .size(62.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    spotColor = if (isDark) Color.Black else Color(0xFF1E3A8A).copy(alpha = 0.6f)
                )
                .clip(CircleShape)
                .background(if (isCenterSelected) centerButtonBg else if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = scoopColor)
                ) {
                    onTabSelected("pos")
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCenterSelected) Icons.Default.PointOfSale else Icons.Outlined.PointOfSale,
                contentDescription = "نقطة البيع",
                tint = if (isCenterSelected) centerButtonContentColor else if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF1E3A8A),
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun CurvedNavButton(
    item: CurvedNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean
) {
    val unselectedColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val activeColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF1E3A8A)

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.label,
            tint = if (isSelected) activeColor else unselectedColor,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) activeColor else unselectedColor,
            maxLines = 1
        )
    }
}
'''

with open('app/src/main/java/com/example/ui/CurvedBottomBar.kt', 'w') as f:
    f.write(code)

print("Updated CurvedBottomBar.kt with original icons and crystal clear bottom text.")
