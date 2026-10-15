import os

code = '''package com.example.ui

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
import androidx.compose.ui.platform.LocalDensity
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
    // Distinctive deep navy scoop color matching the reference image exactly
    val scoopColor = if (isDark) Color(0xFF1E293B) else Color(0xFF26407B)
    val centralFabBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
    val centralFabIconColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF26407B)

    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val density = LocalDensity.current

    val leftItems = remember(isAdmin, canAccessInventory) {
        if (isAdmin) {
            listOf(
                CurvedNavItem("admin_users", "المستخدمين", Icons.Default.SupervisedUserCircle, Icons.Outlined.SupervisedUserCircle),
                CurvedNavItem("admin_settings", "التخصيص", Icons.Default.Brush, Icons.Outlined.Brush)
            )
        } else {
            val list = mutableListOf(
                CurvedNavItem("pos", "نقطة البيع", Icons.Default.Home, Icons.Outlined.Home)
            )
            if (canAccessInventory) {
                list.add(CurvedNavItem("inventory", "المستودع", Icons.Default.FormatListBulleted, Icons.Outlined.FormatListBulleted))
            } else {
                list.add(CurvedNavItem("crm", "العملاء", Icons.Default.People, Icons.Outlined.People))
            }
            list
        }
    }

    val rightItems = remember(isAdmin, canAccessAccounting) {
        if (isAdmin) {
            listOf(
                CurvedNavItem("admin_logs", "السجلات", Icons.Default.History, Icons.Outlined.History),
                CurvedNavItem("settings", "الإعدادات", Icons.Default.Settings, Icons.Outlined.Settings)
            )
        } else {
            val list = mutableListOf<CurvedNavItem>()
            if (canAccessAccounting) {
                list.add(CurvedNavItem("accounting", "المحاسبة", Icons.Default.ChatBubbleOutline, Icons.Outlined.ChatBubbleOutline))
            } else {
                list.add(CurvedNavItem("statements", "الكشوفات", Icons.Default.ReceiptLong, Icons.Outlined.ReceiptLong))
            }
            list.add(CurvedNavItem("more", "المزيد", Icons.Default.PersonOutline, Icons.Outlined.PersonOutline))
            list
        }
    }

    val isDashboardSelected = currentTab == "dashboard"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // Background with the curved notch and navy scoop
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp + navBottomPadding)
                .align(Alignment.BottomCenter)
        ) {
            val w = size.width
            val barH = size.height
            val centerX = w / 2f
            
            // Top flat line Y coordinate
            val topY = 0f

            // 1. Draw main white/surface bar background
            val barPath = Path().apply {
                moveTo(0f, topY)
                val scoopHalfWidth = 72.dp.toPx()
                val scoopDepth = 38.dp.toPx()
                
                lineTo(centerX - scoopHalfWidth, topY)
                
                // Curve down into the scoop
                cubicTo(
                    centerX - scoopHalfWidth * 0.55f, topY,
                    centerX - scoopHalfWidth * 0.5f, topY + scoopDepth,
                    centerX, topY + scoopDepth
                )
                cubicTo(
                    centerX + scoopHalfWidth * 0.5f, topY + scoopDepth,
                    centerX + scoopHalfWidth * 0.55f, topY,
                    centerX + scoopHalfWidth, topY
                )
                
                lineTo(w, topY)
                lineTo(w, barH)
                lineTo(0f, barH)
                close()
            }

            drawPath(
                path = barPath,
                color = barBackgroundColor
            )

            // Draw top border line for non-scoop parts
            drawLine(
                color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                start = Offset(0f, topY),
                end = Offset(centerX - 72.dp.toPx(), topY),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                start = Offset(centerX + 72.dp.toPx(), topY),
                end = Offset(w, topY),
                strokeWidth = 1.dp.toPx()
            )

            // 2. Draw the distinctive Navy/Accent Scoop geometry
            val scoopPath = Path().apply {
                val scoopW = 72.dp.toPx()
                val scoopD = 44.dp.toPx()
                
                moveTo(centerX - scoopW, topY)
                
                cubicTo(
                    centerX - scoopW * 0.55f, topY,
                    centerX - scoopW * 0.45f, topY + scoopD,
                    centerX, topY + scoopD
                )
                cubicTo(
                    centerX + scoopW * 0.45f, topY + scoopD,
                    centerX + scoopW * 0.55f, topY,
                    centerX + scoopW, topY
                )
                
                // Bottom curve for the thick navy loop/scoop
                val cradleThickness = 14.dp.toPx()
                cubicTo(
                    centerX + scoopW * 0.65f, topY + cradleThickness,
                    centerX + scoopW * 0.45f, topY + scoopD + cradleThickness,
                    centerX, topY + scoopD + cradleThickness
                )
                cubicTo(
                    centerX - scoopW * 0.45f, topY + scoopD + cradleThickness,
                    centerX - scoopW * 0.65f, topY + cradleThickness,
                    centerX - scoopW, topY
                )
                close()
            }

            drawPath(
                path = scoopPath,
                color = scoopColor
            )
        }

        // Navigation Items Row (Left items, Gap in Center for Scoop/FAB, Right items)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = navBottomPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Navigation Items
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

            // Center Spacer for the Scoop and FAB
            Spacer(modifier = Modifier.width(84.dp))

            // Right Navigation Items
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

        // The Floating Center Action Button (Dashboard / Central Action)
        if (canAccessReports || isAdmin) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 4.dp)
                    .size(58.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        spotColor = Color(0xFF26407B).copy(alpha = 0.5f)
                    )
                    .clip(CircleShape)
                    .background(if (isDashboardSelected) scoopColor else centralFabBg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onTabSelected("dashboard")
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDashboardSelected) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                    contentDescription = "الداشبورد",
                    tint = if (isDashboardSelected) Color.White else centralFabIconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
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
    val activeColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF26407B)

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.label,
            tint = if (isSelected) activeColor else unselectedColor,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = item.label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) activeColor else unselectedColor,
            maxLines = 1
        )
    }
}
'''

with open('app/src/main/java/com/example/ui/CurvedBottomBar.kt', 'w') as f:
    f.write(code)

print("CurvedBottomBar.kt updated.")
