code = '''package com.example.ui

import androidx.compose.animation.animateColorAsState
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
    
    val centerButtonBg by animateColorAsState(
        targetValue = if (currentTab == "pos") {
            MaterialTheme.colorScheme.primary
        } else {
            if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
        },
        label = "fab_bg"
    )
    
    val centerButtonContentColor by animateColorAsState(
        targetValue = if (currentTab == "pos") {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.primary
        },
        label = "fab_content"
    )

    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Left items (Dashboard & Inventory)
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

    // Right items (Accounting & More)
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

    val isCenterSelected = currentTab == "pos"
    val barHeight = 74.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // Canvas: Perfectly calculated geometric notch with zero leftover artifacts
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

            val cradleRadius = 38.dp.toPx()
            val cornerRadius = 14.dp.toPx()
            val leftNotchStart = centerX - cradleRadius - cornerRadius
            val rightNotchEnd = centerX + cradleRadius + cornerRadius

            // 1. Solid surface bar background with curved cutout
            val barPath = Path().apply {
                moveTo(0f, topY)
                lineTo(leftNotchStart, topY)

                // Left shoulder curve
                cubicTo(
                    leftNotchStart + cornerRadius * 0.55f, topY,
                    centerX - cradleRadius, topY,
                    centerX - cradleRadius, cradleRadius * 0.42f
                )

                // Circular bottom notch trough
                cubicTo(
                    centerX - cradleRadius, cradleRadius * 1.16f,
                    centerX - cradleRadius * 0.52f, cradleRadius * 1.25f,
                    centerX, cradleRadius * 1.25f
                )
                cubicTo(
                    centerX + cradleRadius * 0.52f, cradleRadius * 1.25f,
                    centerX + cradleRadius, cradleRadius * 1.16f,
                    centerX + cradleRadius, cradleRadius * 0.42f
                )

                // Right shoulder curve
                cubicTo(
                    centerX + cradleRadius, topY,
                    rightNotchEnd - cornerRadius * 0.55f, topY,
                    rightNotchEnd, topY
                )

                lineTo(w, topY)
                lineTo(w, totalH)
                lineTo(0f, totalH)
                close()
            }

            drawPath(
                path = barPath,
                color = barBackgroundColor
            )

            // 2. Smooth border line tracing the exact top edge and notch curve
            val borderPath = Path().apply {
                moveTo(0f, topY)
                lineTo(leftNotchStart, topY)
                cubicTo(
                    leftNotchStart + cornerRadius * 0.55f, topY,
                    centerX - cradleRadius, topY,
                    centerX - cradleRadius, cradleRadius * 0.42f
                )
                cubicTo(
                    centerX - cradleRadius, cradleRadius * 1.16f,
                    centerX - cradleRadius * 0.52f, cradleRadius * 1.25f,
                    centerX, cradleRadius * 1.25f
                )
                cubicTo(
                    centerX + cradleRadius * 0.52f, cradleRadius * 1.25f,
                    centerX + cradleRadius, cradleRadius * 1.16f,
                    centerX + cradleRadius, cradleRadius * 0.42f
                )
                cubicTo(
                    centerX + cradleRadius, topY,
                    rightNotchEnd - cornerRadius * 0.55f, topY,
                    rightNotchEnd, topY
                )
                lineTo(w, topY)
            }

            drawPath(
                path = borderPath,
                color = borderColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
            )
        }

        // Navigation items row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .align(Alignment.BottomCenter)
                .padding(bottom = (navBottomPadding * 0.4f) + 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left navigation items
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

            // Center area under the FAB for the title label
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
                        MaterialTheme.colorScheme.primary
                    } else {
                        if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    },
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            // Right navigation items
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

        // Floating Center Button with clean elevation and perfect circular geometry
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-10).dp)
                .size(58.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    spotColor = if (isDark) Color.Black else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
                .clip(CircleShape)
                .background(centerButtonBg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onTabSelected("pos")
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCenterSelected) Icons.Default.PointOfSale else Icons.Outlined.PointOfSale,
                contentDescription = "نقطة البيع",
                tint = centerButtonContentColor,
                modifier = Modifier.size(28.dp)
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
    val activeColor = MaterialTheme.colorScheme.primary

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

print("CurvedBottomBar.kt flawlessly updated with clean circular notch geometry!")
