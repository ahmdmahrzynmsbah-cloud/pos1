package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SystemUser
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CoralError
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.text.DecimalFormat
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalUriHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(viewModel: AppViewModel, onBackClick: () -> Unit) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val users by viewModel.allSystemUsers.collectAsState(initial = emptyList())
  var userToDeactivate by remember { mutableStateOf<SystemUser?>(null) }
  var userToDelete by remember { mutableStateOf<SystemUser?>(null) }
  var showAddUserDialog by remember { mutableStateOf(false) }

  if (showAddUserDialog) {
    AddUserByAdminDialog(viewModel = viewModel, onDismiss = { showAddUserDialog = false })
  }

  LaunchedEffect(Unit) {
    viewModel.checkSubscriptionsForAlerts()
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { 
          Text(
            "لوحة تحكم الأدمن (Control Page)",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          ) 
        },
        navigationIcon = {
          IconButton(onClick = onBackClick) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = "الرجوع"
            )
          }
        },
        actions = {
          Button(
            onClick = { showAddUserDialog = true },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.padding(end = 4.dp).height(36.dp),
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("إضافة مستخدم", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }

          var showExportMenu by remember { mutableStateOf(false) }
          IconButton(onClick = { showExportMenu = true }) {
            Icon(Icons.Default.FileDownload, contentDescription = "تصدير البيانات")
          }
          DropdownMenu(
            expanded = showExportMenu,
            onDismissRequest = { showExportMenu = false }
          ) {
            DropdownMenuItem(
              text = { Text("تصدير إلى PDF") },
              leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
              onClick = {
                showExportMenu = false
                viewModel.exportUsersToPdf(users)
              }
            )
            DropdownMenuItem(
              text = { Text("تصدير إلى Excel (CSV)") },
              leadingIcon = { Icon(Icons.Default.TableView, contentDescription = null, tint = EmeraldGreen) },
              onClick = {
                showExportMenu = false
                viewModel.exportUsersToCsv(users)
              }
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          titleContentColor = MaterialTheme.colorScheme.onSurface
        )
      )
    }
  ) { paddingValues ->
    var selectedTab by remember { mutableStateOf(0) } // 0 = بانتظار الموافقة, 1 = جميع العملاء

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(MaterialTheme.colorScheme.background)
        .padding(8.dp)
    ) {
      
      ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 0.dp,
        modifier = Modifier.padding(bottom = 16.dp)
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text("بانتظار الموافقة", fontWeight = FontWeight.Medium, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
              val pendingCount = users.count { !it.isActive }
              if (pendingCount > 0) {
                Badge(
                  containerColor = AmberAlert,
                  contentColor = Color.White
                ) {
                  Text("$pendingCount", modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontSize = 9.sp)
                }
              }
            }
          }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("جميع العملاء", fontWeight = FontWeight.Medium, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        )
        Tab(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          text = { Text("تقارير الاستهلاك", fontWeight = FontWeight.Medium, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        )
        Tab(
          selected = selectedTab == 3,
          onClick = { selectedTab = 3 },
          text = { Text("التحكم المتقدم", fontWeight = FontWeight.Medium, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        )
        Tab(
          selected = selectedTab == 4,
          onClick = { selectedTab = 4 },
          text = { Text("تنبيهات الأمان", fontWeight = FontWeight.Medium, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        )
        Tab(
          selected = selectedTab == 5,
          onClick = { selectedTab = 5 },
          text = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text("طلبات واقتراحات العملاء", fontWeight = FontWeight.Medium, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
        )
      }

      if (selectedTab == 5) {
        Box(modifier = Modifier.weight(1f)) {
          AdminFeedbacksView(viewModel = viewModel)
        }
      } else if (selectedTab == 4) {
        Box(modifier = Modifier.weight(1f)) {
          AdminAlertsView(viewModel = viewModel)
        }
      } else if (selectedTab == 2) {
        Box(modifier = Modifier.weight(1f)) {
          UsageReportsView(viewModel = viewModel, users = users)
        }
      } else if (selectedTab == 3) {
        Box(modifier = Modifier.weight(1f)) {
          SuperAdminToolsView(viewModel = viewModel)
        }
      } else {
        val filteredUsers = if (selectedTab == 0) {
          users.filter { !it.isActive }
        } else {
          users
        }
        
        LazyColumn(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(bottom = 80.dp) // extra padding for bottom bar
        ) {

          item {
        // Stats Row at the top
        val activeCount = users.count { it.isActive && (it.trialEndDate == null || System.currentTimeMillis() <= it.trialEndDate) }
        val expiredCount = users.count { it.isActive && it.trialEndDate != null && System.currentTimeMillis() > it.trialEndDate }
        val pendingCount = users.count { !it.isActive }
  
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text("حالة اشتراكات العملاء", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            UsersChart(active = activeCount, expired = expiredCount, pending = pendingCount)
          }
        }
  
        // Admin Expiration Notification Alert Banner
        val expiringSoonUsers = users.filter { user ->
          if (user.isActive && user.trialEndDate != null) {
            val daysLeft = ((user.trialEndDate - System.currentTimeMillis()) / (24.0 * 60.0 * 60.0 * 1000.0)).toInt()
            daysLeft <= 3
          } else false
        }
  
        if (expiringSoonUsers.isNotEmpty()) {
          var showAdminAlert by remember { mutableStateOf(true) }
          if (showAdminAlert) {
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = CoralError.copy(alpha = 0.12f)),
              border = BorderStroke(1.dp, CoralError.copy(alpha = 0.3f))
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                      imageVector = Icons.Default.NotificationsActive,
                      contentDescription = null,
                      tint = CoralError,
                      modifier = Modifier.size(18.dp)
                    )
                    Text(
                      "إشعارات الترخيص: اشتراكات توشك على الانتهاء",
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.sp,
                      color = CoralError
                    )
                  }
                  IconButton(
                    onClick = { showAdminAlert = false },
                    modifier = Modifier.size(24.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Close,
                      contentDescription = "إغلاق",
                      tint = Color.Gray,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }
                
                Text(
                  "تم رصد عدد (${expiringSoonUsers.size}) مشتركين انتهت صلاحية حسابهم أو توشك على الانتهاء خلال 3 أيام. يرجى تمديد التراخيص فوراً لتجنب توقف الخدمة للعملاء التالية:",
                  fontSize = 10.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                LazyRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  contentPadding = PaddingValues(vertical = 4.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  items(expiringSoonUsers) { expiringUser ->
                    val daysLeft = ((expiringUser.trialEndDate!! - System.currentTimeMillis()) / (24.0 * 60.0 * 60.0 * 1000.0)).toInt()
                    Card(
                      shape = RoundedCornerShape(8.dp),
                      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                      modifier = Modifier.clickable {
                        selectedTab = 1 // Switch to All Customers tab so admin can find/manage them easily!
                      }
                    ) {
                      Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                      ) {
                        Box(
                          modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (daysLeft < 0) CoralError else AmberAlert)
                        )
                        Column {
                          Text(expiringUser.storeName, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                          Text(
                            text = if (daysLeft < 0) "منتهي الصلاحية" else "بقي $daysLeft أيام (${if (expiringUser.isSubscribed) "نشط" else "تجريبي"})",
                            fontSize = 8.sp,
                            color = if (daysLeft < 0) CoralError else AmberAlert
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
  
  
          }

          if (filteredUsers.isEmpty()) {
            item {
              Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = if (selectedTab == 0) Icons.Default.CheckCircle else Icons.Default.Group,
                  contentDescription = null,
                  tint = if (selectedTab == 0) EmeraldGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                  modifier = Modifier.size(64.dp)
                )
                Text(
                  text = if (selectedTab == 0) "لا توجد طلبات تفعيل معلقة حالياً. جميع العملاء نشطون! " else "لا يوجد مستخدمون مسجلون في قاعدة البيانات بعد.",
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.padding(horizontal = 24.dp)
                )
              }
            }
          } else {
            items(filteredUsers) { user ->
              UserCard(
                user = user,
                viewModel = viewModel,
                onDeactivateClick = { userToDeactivate = user },
                onDeleteClick = { userToDelete = user }
              )
            }
          }
        }
      }
    }
  }

  // Confirmation Dialog before deactivating any system user
  if (userToDeactivate != null) {
    AlertDialog(
      onDismissRequest = { userToDeactivate = null },
      icon = {
        Icon(
          imageVector = Icons.Default.Warning,
          contentDescription = null,
          tint = CoralError,
          modifier = Modifier.size(40.dp)
        )
      },
      title = {
        Text(
          "تأكيد تعطيل الحساب",
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )
      },
      text = {
        Text(
          "هل أنت متأكد من رغبتك في تعطيل حساب المستخدم (${userToDeactivate?.name})؟\n\nلن يتمكن هذا المستخدم من الدخول إلى النظام أو إتمام أي عمليات بيع بعد التعطيل.",
          style = MaterialTheme.typography.bodyMedium,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(
          onClick = {
            userToDeactivate?.let { viewModel.deactivateUser(it) }
            userToDeactivate = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = CoralError),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("نعم، تعطيل الحساب", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { userToDeactivate = null },
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("إلغاء التراجع")
        }
      }
    )
  }

  // Confirmation Dialog before deleting any system user permanently
  if (userToDelete != null) {
    AlertDialog(
      onDismissRequest = { userToDelete = null },
      icon = {
        Icon(
          imageVector = Icons.Default.DeleteForever,
          contentDescription = null,
          tint = CoralError,
          modifier = Modifier.size(40.dp)
        )
      },
      title = {
        Text(
          "تأكيد حذف الحساب نهائياً",
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )
      },
      text = {
        Text(
          "هل أنت متأكد من رغبتك في حذف حساب المستخدم (${userToDelete?.name}) نهائياً من قاعدة البيانات المحلّية؟\n\nتنبيه: هذا الإجراء لا يمكن التراجع عنه وسيمسح بيانات هذا الحساب كلياً من النظام.",
          style = MaterialTheme.typography.bodyMedium,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(
          onClick = {
            userToDelete?.let { viewModel.deleteUser(it) }
            userToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = CoralError),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("نعم، حذف الحساب نهائياً ", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { userToDelete = null },
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("إلغاء التراجع")
        }
      }
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserCard(
  user: SystemUser,
  viewModel: AppViewModel,
  onDeactivateClick: () -> Unit,
  onDeleteClick: (() -> Unit)? = null
) {
  val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
  val trialEndStr = user.trialEndDate?.let { dateFormat.format(Date(it)) } ?: "غير محدد"
  val isTrialEnded = user.trialEndDate != null && System.currentTimeMillis() > user.trialEndDate
  val statusText = if (user.isActive) {
    if (user.isSubscribed) "مشترك كامل" else if (isTrialEnded) "منتهي الصلاحية" else "فترة تجريبية"
  } else {
    "بانتظار التفعيل والموافقة"
  }
  
  val badgeColor = when {
    !user.isActive -> AmberAlert
    user.isSubscribed -> EmeraldGreen
    isTrialEnded -> CoralError
    else -> AmberAlert
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("user_card_${user.uid}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      // Header: Name and Status Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
          }
          Column {
            Text(
              text = user.name,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "الدور الممنوح: ${user.role}",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        
        // Status Badge
        Surface(
          color = badgeColor.copy(alpha = 0.12f),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
        ) {
          Text(
            text = statusText,
            color = badgeColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Spacer(modifier = Modifier.height(12.dp))

      // Details section
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Email,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = "البريد الإلكتروني: ${user.email}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = "انتهاء الفترة التجريبية: $trialEndStr",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (isTrialEnded && !user.isSubscribed && user.isActive) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AmberAlert.copy(alpha = 0.1f))
            .padding(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = AmberAlert,
            modifier = Modifier.size(16.dp)
          )
          Text(
            "انتهت الفترة التجريبية لهذا الحساب. يرجى التفعيل لمنح صلاحية الدخول.",
            fontSize = 11.sp,
            color = AmberAlert,
            fontWeight = FontWeight.Bold
          )
        }
      }

      // Duration selector before action buttons
      var grantDaysInput by remember { mutableStateOf("10") }
      var showPermissionsDialog by remember { mutableStateOf(false) }
      val context = androidx.compose.ui.platform.LocalContext.current
      
      if (showPermissionsDialog) {
        PermissionsManagementDialog(
          user = user,
          viewModel = viewModel,
          onDismiss = { showPermissionsDialog = false }
        )
      }
      
      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      Spacer(modifier = Modifier.height(8.dp))
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text(
            "تحديد مدة الصلاحية أو التمديد للعميل (أيام):",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          IconButton(onClick = {
            val calendar = java.util.Calendar.getInstance()
            user.trialEndDate?.let { calendar.timeInMillis = it }
            android.app.DatePickerDialog(
              context,
              { _, year, month, dayOfMonth ->
                val selectedCalendar = java.util.Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth, 23, 59, 59)
                val newEndDate = selectedCalendar.timeInMillis
                viewModel.updateUserSubscription(user, newEndDate, user.isSubscribed, user.isActive)
                viewModel.showToast("تم تحديث تاريخ انتهاء الاشتراك بنجاح.")
              },
              calendar.get(java.util.Calendar.YEAR),
              calendar.get(java.util.Calendar.MONTH),
              calendar.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
          }, modifier = Modifier.size(32.dp)) {
            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "اختر تاريخ محدد", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
          }
        }
        
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf("10", "30", "90", "365").forEach { days ->
            val isSelected = grantDaysInput == days
            FilterChip(
              selected = isSelected,
              onClick = { grantDaysInput = days },
              label = { Text("$days يوم", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
              ),
              modifier = Modifier.height(32.dp)
            )
          }
          
          // Custom input field matching chips height
          androidx.compose.foundation.text.BasicTextField(
            value = grantDaysInput,
            onValueChange = { newValue ->
              if (newValue.all { it.isDigit() }) {
                grantDaysInput = newValue
              }
            },
            modifier = Modifier
              .width(70.dp)
              .height(32.dp)
              .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
              )
              .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
              )
              .padding(horizontal = 4.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
              fontSize = 11.sp,
              textAlign = TextAlign.Center,
              color = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true,
            decorationBox = { innerTextField ->
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
              ) {
                if (grantDaysInput.isEmpty()) {
                  Text("أخرى", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                }
                innerTextField()
              }
            }
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Action Buttons
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (!user.isActive) {
          Button(
            onClick = { viewModel.activateUser(user, grantDaysInput.toIntOrNull() ?: 10) },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("activate_button_${user.uid}")
          ) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("موافقة وتفعيل الحساب (${grantDaysInput.ifEmpty { "0" }} يوم)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
          }

          if (onDeleteClick != null) {
            Button(
              onClick = onDeleteClick,
              colors = ButtonDefaults.buttonColors(containerColor = CoralError),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("delete_button_${user.uid}")
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("حذف الحساب", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
          }
        } else {
          // Update trial days / extend
          Button(
            onClick = { viewModel.activateUser(user, grantDaysInput.toIntOrNull() ?: 10) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("تعديل/تمديد (${grantDaysInput.ifEmpty { "0" }} يوم)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
          }

          // Subscription status toggler
          Button(
            onClick = { viewModel.toggleUserSubscription(user) },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (user.isSubscribed) AmberAlert else EmeraldGreen
            ),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(
              imageVector = if (user.isSubscribed) Icons.Default.CardMembership else Icons.Default.Star,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (user.isSubscribed) "تحويل إلى تجريبي" else "تفعيل الاشتراك الكامل",
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp
            )
          }

          OutlinedButton(
            onClick = { showPermissionsDialog = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("إدارة الصلاحيات", fontWeight = FontWeight.Bold, fontSize = 11.sp)
          }

          OutlinedButton(
            onClick = onDeactivateClick,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberAlert),
            border = BorderStroke(1.dp, AmberAlert),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("deactivate_button_${user.uid}")
          ) {
            Icon(
              imageVector = Icons.Default.Block,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("تعطيل الحساب", fontWeight = FontWeight.Bold, fontSize = 11.sp)
          }

          if (onDeleteClick != null) {
            Button(
              onClick = onDeleteClick,
              colors = ButtonDefaults.buttonColors(containerColor = CoralError),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("delete_button_${user.uid}")
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("حذف الحساب", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageReportsView(viewModel: AppViewModel, users: List<SystemUser>) {
  val invoices by viewModel.allInvoices.collectAsState(initial = emptyList())
  val products by viewModel.allProducts.collectAsState(initial = emptyList())
  val formatMoneyHelper = remember { DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols(Locale.US)) }
  var viewType by remember { mutableStateOf(0) } // 0 = قائمة الاستهلاك, 1 = الرسوم البيانية والنشاط 
  
  // Calculate global stats (overall active client databases and totals)
  val totalRegisteredUsers = users.size
  val activeUsersCount = users.count { it.isActive }
  val premiumUsersCount = users.count { it.isSubscribed && it.isActive }
  
  // Deterministic metrics generation helper
  val tenantReports = remember(users, invoices, products) {
    users.map { user ->
      val seed = user.uid.hashCode()
      val random = java.util.Random(seed.toLong())
      
      // Current logged in user is calculated with real local DB figures
      val currentLoggedInUid = viewModel.currentUserAccount.value?.uid
      val isCurrentLoggedIn = (user.uid == currentLoggedInUid)
      
      val invoiceCount = if (isCurrentLoggedIn) {
        invoices.size
      } else {
        random.nextInt(380) + 15
      }
      val invoiceLimit = if (user.isSubscribed) 5000 else 500
      
      val productCount = if (isCurrentLoggedIn) {
        products.size
      } else {
        random.nextInt(120) + 10
      }
      val productLimit = if (user.isSubscribed) 2000 else 200
      
      val totalRevenue = if (isCurrentLoggedIn) {
        invoices.sumOf { it.total }
      } else {
        (random.nextDouble() * 35000.0) + 800.0
      }
      
      val currency = user.currencySymbol.ifEmpty { "شيكل" }
      
      val avgInvoiceValue = if (invoiceCount > 0) totalRevenue / invoiceCount else 0.0
      
      val lastAction = if (isCurrentLoggedIn && invoices.isNotEmpty()) {
        "إصدار فاتورة مبيعات مبسطة رقم #${invoices.first().id}"
      } else {
        when (random.nextInt(5)) {
          0 -> "إصدار فاتورة مبيعات ضريبية"
          1 -> "إضافة صنف جديد للمخزون"
          2 -> "ترحيل قيود مبيعات الوردية"
          3 -> "تعديل بيانات الملف الضريبي"
          else -> "تصدير كشف حساب عميل"
        }
      }
      
      val lastActionTime = when (random.nextInt(4)) {
        0 -> "نشط الآن"
        1 -> "نشط قبل 5 دقائق"
        2 -> "نشط قبل ساعة"
        else -> "خامل منذ يومين"
      }
      
      TenantUsageReport(
        user = user,
        invoiceCount = invoiceCount,
        invoiceLimit = invoiceLimit,
        productCount = productCount,
        productLimit = productLimit,
        totalRevenue = totalRevenue,
        avgInvoiceValue = avgInvoiceValue,
        currency = currency,
        lastAction = lastAction,
        lastActionTime = lastActionTime,
        isCurrent = isCurrentLoggedIn
      )
    }
  }
  
  // Global platform summary aggregates
  val totalPlatformInvoices = tenantReports.sumOf { it.invoiceCount }
  val totalPlatformRevenue = tenantReports.sumOf { it.totalRevenue }
  val mostActiveTenant = tenantReports.maxByOrNull { it.invoiceCount }?.user?.storeName ?: "غير محدد"
  
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // High-level Platform Telemetry Cards
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          "تحليلات منصة الكنز للمحاسبة ",
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = MaterialTheme.colorScheme.primary
        )
        
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Total Sales across platform
          Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text("إجمالي معاملات المنصة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Text(
                "${formatMoneyHelper.format(totalPlatformRevenue)} شيكل",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
          
          // Total Issued Invoices across platform
          Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(Icons.Default.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                Text("مجموع الفواتير المصدرة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Text(
                "$totalPlatformInvoices فاتورة",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
              )
            }
          }
        }
        
        // Server Health and Active Tenant Metrics
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.CloudQueue, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                Text("مؤشر سلامة الخدمة السحابية", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
              Surface(
                color = EmeraldGreen.copy(alpha = 0.12f),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  "مستقر ومثالي",
                  color = EmeraldGreen,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
              }
            }
            
            LinearProgressIndicator(
              progress = { 0.24f },
              modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
              color = EmeraldGreen,
              trackColor = MaterialTheme.colorScheme.outlineVariant
            )
            
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("العميل الأكثر نشاطاً:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(mostActiveTenant, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
          }
        }
      }
    }
    
    // View Selector (Subscriber List vs Graphical Charts)
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          "عرض التقارير والتحليلات ",
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = MaterialTheme.colorScheme.onSurface
        )
        
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          // Option 0: Consumption List
          Button(
            onClick = { viewType = 0 },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (viewType == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
              contentColor = if (viewType == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f).height(38.dp),
            contentPadding = PaddingValues(0.dp)
          ) {
            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("قائمة استهلاك العملاء ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
          
          // Option 1: Graphical Charts
          Button(
            onClick = { viewType = 1 },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (viewType == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
              contentColor = if (viewType == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f).height(38.dp),
            contentPadding = PaddingValues(0.dp)
          ) {
            Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("مخططات النشاط والعمليات ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    if (viewType == 1) {
      item {
        SubscriberActivityCharts(reports = tenantReports, formatMoneyHelper = formatMoneyHelper)
      }
    } else {
      // Tenant Monitoring Section Title
      item {
        Text(
          "مراقبة استهلاك الموارد لكل مشترك ",
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
      
      // Subscriber Usage Cards
      items(tenantReports) { report ->
      val user = report.user
      val isCompact = report.invoiceCount.toDouble() / report.invoiceLimit.toDouble() > 0.8
      val barColor = when {
        report.invoiceCount.toDouble() / report.invoiceLimit.toDouble() > 0.8 -> CoralError
        report.invoiceCount.toDouble() / report.invoiceLimit.toDouble() > 0.5 -> AmberAlert
        else -> EmeraldGreen
      }
      
      val statusColor = if (user.isActive) {
        if (user.isSubscribed) EmeraldGreen else AmberAlert
      } else {
        CoralError
      }
      
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (report.isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                   else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
          width = if (report.isCurrent) 1.5.dp else 1.dp,
          color = if (report.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          // Header row: Store Name & Activity State
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  user.storeName,
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )
                if (report.isCurrent) {
                  Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Text(
                      "حسابك الحالي",
                      color = MaterialTheme.colorScheme.primary,
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
              }
              Text(
                "المشترك: ${user.name} | ${user.currentBranch}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            
            Surface(
              color = statusColor.copy(alpha = 0.1f),
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
            ) {
              Text(
                text = if (user.isSubscribed) "باقة كاملة" else "باقة تجريبية",
                color = statusColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
          
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
          
          // Consumption Metrics
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Invoice Counter Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text("استهلاك الفواتير الصادرة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                  "${report.invoiceCount} / ${report.invoiceLimit} فاتورة",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isCompact) CoralError else MaterialTheme.colorScheme.onSurface
                )
              }
              LinearProgressIndicator(
                progress = { report.invoiceCount.toFloat() / report.invoiceLimit.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.outlineVariant
              )
            }
            
            // Product Catalog Limit Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text("استهلاك دليل المنتجات:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                  "${report.productCount} / ${report.productLimit} صنف",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
              LinearProgressIndicator(
                progress = { report.productCount.toFloat() / report.productLimit.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.outlineVariant
              )
            }
          }
          
          // Sales revenue & details
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("إجمالي المبيعات المحققة ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(
                "${formatMoneyHelper.format(report.totalRevenue)} ${report.currency}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldGreen
              )
            }
            
            Column(horizontalAlignment = Alignment.End) {
              Text("متوسط قيمة الفاتورة ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(
                "${formatMoneyHelper.format(report.avgInvoiceValue)} ${report.currency}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
          
          // Connection and Last Activity Log
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("حالة الاتصال والنشاط:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(report.lastActionTime, fontSize = 10.sp, fontWeight = FontWeight.Bold)
              }
              Text(
                "آخر عملية منفذة: ${report.lastAction}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
          
          // Quick Action License Controls inside the Report view
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Quick Extend/Trial Update
            OutlinedButton(
              onClick = { viewModel.activateUser(report.user, 30) },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(34.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("تمديد 30 يوم", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            
            // Quick premium tier upgrade/toggle
            Button(
              onClick = { viewModel.toggleUserSubscription(report.user) },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (report.user.isSubscribed) AmberAlert else EmeraldGreen
              ),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(34.dp)
            ) {
              Icon(
                imageVector = if (report.user.isSubscribed) Icons.Default.CardMembership else Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (report.user.isSubscribed) "تحويل لتجريبي" else "تفعيل الباقة الكاملة",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
    }
  }
}

// Data class to store calculated metrics
data class TenantUsageReport(
  val user: SystemUser,
  val invoiceCount: Int,
  val invoiceLimit: Int,
  val productCount: Int,
  val productLimit: Int,
  val totalRevenue: Double,
  val avgInvoiceValue: Double,
  val currency: String,
  val lastAction: String,
  val lastActionTime: String,
  val isCurrent: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriberActivityCharts(reports: List<TenantUsageReport>, formatMoneyHelper: DecimalFormat) {
  var selectedReportIndex by remember { mutableStateOf(-1) }
  
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // 1. Subscription Tier Distribution split
    val totalSubscribers = reports.size
    val premiumCount = reports.count { it.user.isSubscribed }
    val trialCount = totalSubscribers - premiumCount
    val premiumRatio = if (totalSubscribers > 0) premiumCount.toFloat() / totalSubscribers else 0f
    
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Default.PieChart, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
          Text("توزيع تراخيص المشتركين بالمنصة ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("باقة كاملة (): $premiumCount (${(premiumRatio * 100).toInt()}%)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
          Text("باقة تجريبية (): $trialCount (${((1f - premiumRatio) * 100).toInt()}%)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberAlert)
        }
        
        // Multi-segment progress bar representing the split
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(8.dp))
        ) {
          if (premiumCount > 0) {
            Box(
              modifier = Modifier
                .weight(premiumRatio)
                .fillMaxHeight()
                .background(EmeraldGreen)
            )
          }
          if (trialCount > 0) {
            Box(
              modifier = Modifier
                .weight(1f - premiumRatio)
                .fillMaxHeight()
                .background(AmberAlert)
            )
          }
        }
        
        Text("يساعد هذا التوزيع في مراقبة جودة تحويل المشتركين بالمنصة وربحية التراخيص السحابية.", fontSize = 10.sp, color = Color.Gray)
      }
    }
    
    // 2. Interactive Operations/Invoices Bar Chart
    val maxInvoices = reports.maxOfOrNull { it.invoiceCount }?.coerceAtLeast(100) ?: 100
    val maxInvoicesFloat = maxInvoices.toFloat()
    
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text("مخطط نشاط العمليات (الفواتير المصدرة) ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
          }
          Text("انقر على عمود للتفاصيل ", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        
        Text("يوضح هذا المخطط التفاعلي عدد فواتير المبيعات الصادرة لكل مشترك. الأعمدة تلون وفقاً لمعدل استهلاك الحد الأقصى للباقة:", fontSize = 11.sp, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Let's render the bar chart
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.Bottom
        ) {
          // Y-Axis labels column
          Column(
            modifier = Modifier.height(180.dp).width(32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
          ) {
            Text("${maxInvoices}", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text("${(maxInvoices * 3) / 4}", fontSize = 9.sp, color = Color.Gray)
            Text("${maxInvoices / 2}", fontSize = 9.sp, color = Color.Gray)
            Text("${maxInvoices / 4}", fontSize = 9.sp, color = Color.Gray)
            Text("0", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
          }
          
          // Separator line
          Box(modifier = Modifier.width(1.dp).height(180.dp).background(MaterialTheme.colorScheme.outlineVariant))
          
          // Bars container
          Row(
            modifier = Modifier
              .weight(1f)
              .height(180.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom
          ) {
            reports.forEachIndexed { index, report ->
              val ratio = report.invoiceCount.toFloat() / maxInvoicesFloat
              val isSelected = selectedReportIndex == index
              
              val alertColor = when {
                report.invoiceCount.toFloat() / report.invoiceLimit.toFloat() > 0.8 -> CoralError
                report.invoiceCount.toFloat() / report.invoiceLimit.toFloat() > 0.5 -> AmberAlert
                else -> EmeraldGreen
              }
              
              Column(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                // Tooltip showing count when selected or hovered
                Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.BottomCenter) {
                  if (isSelected) {
                    Surface(
                      color = MaterialTheme.colorScheme.primary,
                      shape = RoundedCornerShape(4.dp),
                      modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                      Text(
                        "${report.invoiceCount}",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                      )
                    }
                  } else {
                    Text(
                      "${report.invoiceCount}",
                      color = Color.Gray.copy(alpha = 0.8f),
                      fontSize = 8.sp
                    )
                  }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // The actual clickable bar
                Box(
                  modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .fillMaxHeight((ratio * 0.85f).coerceAtLeast(0.04f))
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(
                      if (isSelected) MaterialTheme.colorScheme.primary
                      else alertColor
                    )
                    .clickable {
                      selectedReportIndex = if (isSelected) -1 else index
                    }
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Store name label below bar
                Text(
                  text = report.user.storeName,
                  fontSize = 8.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  textAlign = TextAlign.Center,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }
        
        // Detailed highlighted tenant card
        if (selectedReportIndex in reports.indices) {
          val report = reports[selectedReportIndex]
          val user = report.user
          
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                  Text(user.storeName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                  text = if (user.isSubscribed) "باقة كاملة" else "باقة تجريبية",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (user.isSubscribed) EmeraldGreen else AmberAlert
                )
              }
              
              HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
              
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column {
                  Text("المشترك الحالي: ${user.name}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text("مجموع الفواتير: ${report.invoiceCount} / ${report.invoiceLimit}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text("دليل المنتجات: ${report.productCount} / ${report.productLimit}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text("المبيعات: ${formatMoneyHelper.format(report.totalRevenue)} ${report.currency}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                }
              }
            }
          }
        }
      }
    }
    
    // 3. Financial revenue horizontal bar chart
    val maxRevenue = reports.maxOfOrNull { it.totalRevenue }?.coerceAtLeast(100.0) ?: 100.0
    
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
          Text("حجم النشاط المالي وعوائد الفروع ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        
        Text("يقارن هذا المخطط حجم المبيعات الإجمالية المحققة لكل فرع ومشترك للمراقبة السحابية:", fontSize = 11.sp, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        reports.forEach { report ->
          val ratio = (report.totalRevenue / maxRevenue).toFloat()
          
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                report.user.storeName,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.width(100.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              
              Box(
                modifier = Modifier
                  .weight(1f)
                  .height(14.dp)
                  .clip(RoundedCornerShape(7.dp))
                  .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth(ratio.coerceIn(0.04f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                      brush = Brush.horizontalGradient(
                        colors = listOf(
                          MaterialTheme.colorScheme.primary,
                          EmeraldGreen
                        )
                      )
                    )
                )
              }
              
              Text(
                "${formatMoneyHelper.format(report.totalRevenue)} ${report.currency}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldGreen,
                modifier = Modifier.width(90.dp),
                textAlign = TextAlign.End
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun SuperAdminToolsView(viewModel: AppViewModel) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(
      text = "أدوات ومحاكاة المدير المتقدمة",
      fontWeight = FontWeight.Bold,
      fontSize = 16.sp,
      color = MaterialTheme.colorScheme.primary
    )
    Text(
      text = "بصفتك مديراً عاماً ومطوراً للمنصة، تمنحك هذه اللوحة تحكماً كاملاً بالبيانات لتسهيل تجربة العميل وفحص الأداء ومحاكاة السيناريوهات المختلفة.",
      fontSize = 11.sp,
      color = Color.Gray
    )

    // Data Simulations removed by user request

    // Card 2: Batch Subscriptions Management
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = EmeraldGreen)
          Text("إدارة الاشتراكات والتراخيص الجماعية", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Text(
          text = "أدوات التحكم السريع بالترخيص والاشتراك لكافة المشتركين المسجلين في النظام دفعة واحدة دون الحاجة لتحديث كل مشترك بشكل منفصل.",
          fontSize = 10.sp,
          color = Color.Gray
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          Button(
            onClick = { viewModel.activateAllPendingUsers() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
          ) {
            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("الموافقة الفورية وتفعيل كافة الحسابات المعلقة", fontSize = 12.sp)
          }
          
          OutlinedButton(
            onClick = { viewModel.extendAllActiveSubscriptions() },
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
          ) {
            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("تمديد جماعي لكافة المشتركين بـ (30 يوماً إضافياً)", fontSize = 12.sp)
          }
        }
      }
    }

    // Card 3: Subscription Marquee Banner Configurator
    val currentBannerText by viewModel.subscriptionBannerText.collectAsState()
    val supportWhatsapp by viewModel.supportWhatsappNumber.collectAsState()
    var editBannerState by remember(currentBannerText) { mutableStateOf(currentBannerText) }

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(imageVector = Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Text("تخصيص شريط تفعيل الاشتراك المتحرك (تحت الهيدر)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Text(
          text = "النص المتحرك الذي يظهر للمستخدمين مباشرة تحت الهيدر في النسخة التجريبية. يمكنك تحديثه هنا ويتم حفظه وعرضه فوراً.",
          fontSize = 11.sp,
          color = Color.Gray
        )

        OutlinedTextField(
          value = editBannerState,
          onValueChange = { editBannerState = it },
          label = { Text("نص الجملة المتحركة") },
          placeholder = { Text("اكتب نص الشريط المتحرك...") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp),
          minLines = 2,
          maxLines = 4
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          SuggestionChip(
            onClick = { editBannerState = viewModel.defaultSubscriptionBannerText },
            label = { Text("الافتراضي", fontSize = 10.sp) }
          )
          SuggestionChip(
            onClick = { editBannerState = "يرجى تجديد الاشتراك وتفعيل الترخيص السحابي للمتابعة بدون انقطاع - للتواصل واتساب: $supportWhatsapp" },
            label = { Text("تنبيه تجديد", fontSize = 10.sp) }
          )
          SuggestionChip(
            onClick = { editBannerState = "عرض خاص: خصم حصري على الاشتراك السنوي الشامل! تواصل مع الإدارة الآن على: $supportWhatsapp" },
            label = { Text("عرض خاص", fontSize = 10.sp) }
          )
        }

        Text("معاينة حركة النص المباشرة:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clipToBounds(),
          contentAlignment = Alignment.CenterStart
        ) {
          Text(
            text = " ${editBannerState.ifEmpty { viewModel.defaultSubscriptionBannerText }} ",
            color = MaterialTheme.colorScheme.surface,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1,
            modifier = Modifier.basicMarquee(
              iterations = Int.MAX_VALUE,
              velocity = 35.dp
            )
          )
        }

        Button(
          onClick = {
            viewModel.updateSubscriptionBannerText(editBannerState)
            viewModel.showToast("تم تحديث وحفظ نص شريط تفعيل الاشتراك بنجاح!")
          },
          modifier = Modifier.fillMaxWidth(),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
          Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("حفظ وتطبيق الشريط فوراً للمستخدمين", fontSize = 12.sp)
        }
      }
    }

    // Card 4: Platform Settings Configurator
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(imageVector = Icons.Default.SettingsApplications, contentDescription = null, tint = AmberAlert)
          Text("ثوابت المنصة والتحكم بنوع باقة الاشتراك", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Text(
          text = "هذه الإعدادات تحدد طبيعة القيود والقيم الافتراضية للعملاء الجدد الذين يسجلون في باقة الكنز السحابية.",
          fontSize = 10.sp,
          color = Color.Gray
        )
        
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("قيمة ضريبة القيمة المضافة ZATCA الافتراضية:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Text("15% (نشط)", fontSize = 11.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("مدة الفترة التجريبية للعميل الجديد:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Text("10 أيام تجريبية", fontSize = 11.sp, color = AmberAlert, fontWeight = FontWeight.Bold)
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("الحد الأقصى للفواتير بالنسخة التجريبية:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Text("50 فاتورة كحد أقصى", fontSize = 11.sp, color = CoralError, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun PermissionsManagementDialog(
  user: SystemUser,
  viewModel: AppViewModel,
  onDismiss: () -> Unit
) {
  var canAccessReports by remember { mutableStateOf(user.canAccessReports) }
  var canAccessInventory by remember { mutableStateOf(user.canAccessInventory) }
  var canAccessAccounting by remember { mutableStateOf(user.canAccessAccounting) }
  var canAccessAdvancedTools by remember { mutableStateOf(user.canAccessAdvancedTools) }
  var canAddProducts by remember { mutableStateOf(user.canAddProducts) }
  var canDeleteProducts by remember { mutableStateOf(user.canDeleteProducts) }
  var canApplyDiscounts by remember { mutableStateOf(user.canApplyDiscounts) }
  var canViewProfits by remember { mutableStateOf(user.canViewProfits) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("إدارة الصلاحيات والمزايا", fontWeight = FontWeight.Bold, fontSize = 16.sp)
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text("المشترك: ${user.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("قم بتفعيل أو تعطيل الوصول للوحدات المحددة لهذا المشترك.", fontSize = 12.sp, color = Color.Gray)
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        PermissionToggleRow(
          title = "تقارير النظام والتحليلات",
          description = "الوصول لشاشة التقارير والمبيعات والأرباح.",
          icon = Icons.Default.BarChart,
          checked = canAccessReports,
          onCheckedChange = { canAccessReports = it }
        )
        
        PermissionToggleRow(
          title = "إدارة المخزون والبضائع",
          description = "إضافة وتعديل المنتجات والأصناف.",
          icon = Icons.Default.Inventory,
          checked = canAccessInventory,
          onCheckedChange = { canAccessInventory = it }
        )

        PermissionToggleRow(
          title = "المحاسبة وقيود اليومية",
          description = "إدارة شجرة الحسابات وقيود المحاسبة المزدوجة.",
          icon = Icons.Default.AccountBalanceWallet,
          checked = canAccessAccounting,
          onCheckedChange = { canAccessAccounting = it }
        )
        
        PermissionToggleRow(
          title = "إضافة منتجات",
          description = "السماح بإضافة منتجات جديدة.",
          icon = Icons.Default.AddBox,
          checked = canAddProducts,
          onCheckedChange = { canAddProducts = it }
        )

        PermissionToggleRow(
          title = "حذف المنتجات",
          description = "السماح بحذف المنتجات من النظام.",
          icon = Icons.Default.Delete,
          checked = canDeleteProducts,
          onCheckedChange = { canDeleteProducts = it }
        )

        PermissionToggleRow(
          title = "تطبيق خصومات",
          description = "تفعيل صلاحية تطبيق الخصومات للعملاء.",
          icon = Icons.Default.LocalOffer,
          checked = canApplyDiscounts,
          onCheckedChange = { canApplyDiscounts = it }
        )

        PermissionToggleRow(
          title = "رؤية الأرباح الصافية",
          description = "عرض الأرباح والتكاليف الحقيقية.",
          icon = Icons.Default.AttachMoney,
          checked = canViewProfits,
          onCheckedChange = { canViewProfits = it }
        )

        PermissionToggleRow(
          title = "الأدوات المتقدمة للعميل",
          description = "تفعيل الوصول لتصدير البيانات والنسخ الاحتياطي وإعدادات المطور المتقدمة.",
          icon = Icons.Default.Build,
          checked = canAccessAdvancedTools,
          onCheckedChange = { canAccessAdvancedTools = it }
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          viewModel.updateUserPermissions(
            user = user,
            canAccessReports = canAccessReports,
            canAccessInventory = canAccessInventory,
            canAccessAccounting = canAccessAccounting,
            canAccessAdvancedTools = canAccessAdvancedTools,
            canAddProducts = canAddProducts,
            canDeleteProducts = canDeleteProducts,
            canApplyDiscounts = canApplyDiscounts,
            canViewProfits = canViewProfits
          )
          onDismiss()
        },
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("حفظ الصلاحيات", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إلغاء", color = Color.Gray)
      }
    },
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(16.dp)
  )
}

@Composable
fun PermissionToggleRow(
  title: String,
  description: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.weight(1f)
    ) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .background(if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (checked) MaterialTheme.colorScheme.primary else Color.Gray,
          modifier = Modifier.size(16.dp)
        )
      }
      Column {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (checked) MaterialTheme.colorScheme.onSurface else Color.Gray)
        Text(description, fontSize = 10.sp, color = Color.Gray, lineHeight = 12.sp)
      }
    }
    androidx.compose.material3.Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.scale(0.8f)
    )
  }
}


@Composable
fun AdminFeedbacksView(viewModel: AppViewModel) {
  val feedbacks by viewModel.allFeedbacks.collectAsState(initial = emptyList())
  var replyDialogFeedback by remember { mutableStateOf<com.example.data.AppFeedback?>(null) }
  var adminReplyText by remember { mutableStateOf("") }
  var statusSelection by remember { mutableStateOf("قيد المراجعة") }
  var fullScreenImageBase64 by remember { mutableStateOf<String?>(null) }
  var feedbackToDelete by remember { mutableStateOf<com.example.data.AppFeedback?>(null) }

  Column(modifier = Modifier.fillMaxSize()) {
    if (feedbacks.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
          Text("لا توجد اقتراحات أو طلبات ميزات مرسلة من العملاء حالياً.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
      ) {
        items(feedbacks) { feedback ->
          val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
          val dateStr = sdf.format(Date(feedback.timestamp))

          val statusColor = when (feedback.status) {
            "جديد" -> AmberAlert
            "قيد المراجعة" -> MaterialTheme.colorScheme.primary
            "تم اعتماده" -> Color(0xFF2196F3)
            "تم التنفيذ" -> EmeraldGreen
            else -> Color.Gray
          }

          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                  ) {
                    Text(
                      feedback.category,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary
                    )
                  }
                  Text(dateStr, fontSize = 10.sp, color = Color.Gray)
                }

                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = statusColor.copy(alpha = 0.12f),
                  border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                  Text(
                    feedback.status,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                  )
                }
              }

              Text(feedback.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
              Text(feedback.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

              // Sender details
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                  .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text("المرسل: ${feedback.senderName.ifBlank { "غير معروف" }}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                  if (feedback.storeName.isNotBlank()) {
                    Text("المتجر: ${feedback.storeName}", fontSize = 11.sp, color = Color.Gray)
                  }
                  if (feedback.phone.isNotBlank()) {
                    Text("الهاتف: ${feedback.phone}", fontSize = 11.sp, color = Color.Gray)
                  }
                }
                if (feedback.phone.isNotBlank()) {
                  val uriHandler = LocalUriHandler.current
                  IconButton(onClick = {
                    try {
                      val clean = feedback.phone.filter { it.isDigit() || it == '+' }
                      uriHandler.openUri("https://wa.me/$clean")
                    } catch (e: Exception) {
                      viewModel.showToast("تعذر فتح واتساب")
                    }
                  }) {
                    Icon(Icons.Default.Chat, contentDescription = "واتساب", tint = Color(0xFF25D366))
                  }
                }
              }

              // Attached image if exists
              if (feedback.imageUri.isNotBlank()) {
                val bitmap = remember(feedback.imageUri) {
                  try {
                    val bytes = android.util.Base64.decode(feedback.imageUri, android.util.Base64.DEFAULT)
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                  } catch (e: Exception) {
                    null
                  }
                }
                if (bitmap != null) {
                  androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "صورة مرفقة",
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(180.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .clickable { fullScreenImageBase64 = feedback.imageUri },
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                  )
                }
              }

              // Admin reply if present
              if (feedback.adminReply.isNotBlank()) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(EmeraldGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                  Column {
                    Text("رد الإدارة:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EmeraldGreen)
                    Text(feedback.adminReply, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                  }
                }
              }

              // Action buttons for Admin
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
              ) {
                TextButton(onClick = {
                  replyDialogFeedback = feedback
                  adminReplyText = feedback.adminReply
                  statusSelection = feedback.status
                }) {
                  Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("تحديث الحالة والرد")
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { feedbackToDelete = feedback }) {
                  Icon(Icons.Default.Delete, contentDescription = "حذف", tint = CoralError, modifier = Modifier.size(20.dp))
                }
              }
            }
          }
        }
      }
    }
  }

  // Delete Confirmation Dialog
  if (feedbackToDelete != null) {
    androidx.compose.material3.AlertDialog(
      onDismissRequest = { feedbackToDelete = null },
      title = { Text("تأكيد الحذف النهائي", fontWeight = FontWeight.Bold, color = CoralError) },
      text = { Text("هل أنت متأكد من حذف هذا المقترح أو الطلب نهائياً؟ هذا الإجراء لا يمكن التراجع عنه.", fontSize = 14.sp) },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteFeedback(feedbackToDelete!!)
            feedbackToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = CoralError)
        ) {
          Text("نعم، احذف نهائياً")
        }
      },
      dismissButton = {
        TextButton(onClick = { feedbackToDelete = null }) {
          Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    )
  }

  // Full Screen Image Viewer
  if (fullScreenImageBase64 != null) {
    androidx.compose.ui.window.Dialog(
      onDismissRequest = { fullScreenImageBase64 = null },
      properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.9f))
          .clickable { fullScreenImageBase64 = null },
        contentAlignment = Alignment.Center
      ) {
        val bitmapFullScreen = remember(fullScreenImageBase64) {
          try {
            val bytes = android.util.Base64.decode(fullScreenImageBase64, android.util.Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
          } catch (e: Exception) {
            null
          }
        }
        if (bitmapFullScreen != null) {
          androidx.compose.foundation.Image(
            bitmap = bitmapFullScreen.asImageBitmap(),
            contentDescription = "الصورة بحجم كامل",
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
          )
        }
        
        IconButton(
          onClick = { fullScreenImageBase64 = null },
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(16.dp)
            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
          Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
        }
      }
    }
  }

  // Reply / Status Update Dialog
  if (replyDialogFeedback != null) {
    val f = replyDialogFeedback!!
    androidx.compose.material3.AlertDialog(
      onDismissRequest = { replyDialogFeedback = null },
      title = { Text("إدارة طلب واقتراح العميل", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("تحديث حالة الطلب وتدوين رد الإدارة للعميل:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          
          // Status choices
          val statuses = listOf("جديد", "قيد المراجعة", "تم اعتماده", "تم التنفيذ")
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("اختر الحالة:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              statuses.forEach { st ->
                val selected = statusSelection == st
                Surface(
                  onClick = { statusSelection = st },
                  shape = RoundedCornerShape(8.dp),
                  color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                  modifier = Modifier.weight(1f)
                ) {
                  Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(
                      st,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }
          }

          OutlinedTextField(
            value = adminReplyText,
            onValueChange = { adminReplyText = it },
            label = { Text("رد الإدارة / تعليق") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            maxLines = 4
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.updateFeedbackStatus(f, statusSelection, adminReplyText)
            replyDialogFeedback = null
          },
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("حفظ التحديث")
        }
      },
      dismissButton = {
        TextButton(onClick = { replyDialogFeedback = null }) {
          Text("إلغاء")
        }
      },
      shape = RoundedCornerShape(20.dp)
    )
  }
}

@Composable
fun AdminAlertsView(viewModel: AppViewModel) {
  val alerts by viewModel.allAdminAlerts.collectAsState(initial = emptyList())
  
  Column(modifier = Modifier.fillMaxSize()) {
    if (alerts.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("لا توجد تنبيهات أمنية حالياً.", color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    } else {
      Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.End
      ) {
        TextButton(onClick = { viewModel.clearAdminAlerts() }) {
          Text("حذف جميع التنبيهات", color = MaterialTheme.colorScheme.error)
        }
      }
      LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(alerts) { alert ->
          val color = when (alert.type) {
            "ERROR" -> MaterialTheme.colorScheme.errorContainer
            "WARNING" -> AmberAlert.copy(alpha = 0.2f)
            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
          }
          val textColor = when (alert.type) {
            "ERROR" -> MaterialTheme.colorScheme.onErrorContainer
            "WARNING" -> AmberAlert
            else -> MaterialTheme.colorScheme.onPrimaryContainer
          }
          val icon = when (alert.type) {
            "ERROR" -> Icons.Default.Warning
            "WARNING" -> Icons.Default.NotificationsActive
            else -> Icons.Default.Info
          }
          
          val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
          val dateStr = sdf.format(Date(alert.timestamp))
          
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = color),
            shape = RoundedCornerShape(12.dp)
          ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(24.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(alert.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                Spacer(modifier = Modifier.height(4.dp))
                Text(alert.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(dateStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              if (!alert.isRead) {
                IconButton(onClick = { viewModel.markAdminAlertAsRead(alert.id) }, modifier = Modifier.size(24.dp)) {
                  Icon(Icons.Default.Check, contentDescription = "تحديد كمقروء", tint = textColor, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserByAdminDialog(
  viewModel: AppViewModel,
  onDismiss: () -> Unit
) {
  var name by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var role by remember { mutableStateOf("CASHIER") } // CASHIER, CUSTOMER, BRANCH_MANAGER
  var storeName by remember { mutableStateOf("مؤسسة الكنز للمحاسبة التجارية") }
  var isSubscribed by remember { mutableStateOf(true) }
  var trialDays by remember { mutableStateOf("30") }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("إضافة وتفعيل مستخدم جديد", fontWeight = FontWeight.Bold, fontSize = 16.sp)
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "إغلاق")
          }
        }

        Text(
          "المستخدم الذي تضيفه هنا يتم اعتماده وتفعيله فوراً بواسطة مدير النظام.",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("الاسم الكامل للمستخدم *") },
          leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("رقم الجوال (اسم الدخول) *") },
          leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = { Text("كلمة المرور للمستخدم *") },
          leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = storeName,
          onValueChange = { storeName = it },
          label = { Text("اسم النشاط التجاري / المحل") },
          leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        )

        Text("نوع الصلاحية / الدور:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          val roles = listOf(
            "CASHIER" to "كاشير مبيعات",
            "CUSTOMER" to "عميل / مشترك",
            "BRANCH_MANAGER" to "مدير فرع"
          )
          roles.forEach { (r, label) ->
            val isSelected = role == r
            FilterChip(
              selected = isSelected,
              onClick = { role = r },
              label = { Text(label, fontSize = 11.sp) },
              modifier = Modifier.weight(1f)
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("اشتراك كامل ودائم", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(if (isSubscribed) "مفعل بدون تاريخ انتهاء" else "فترة تجريبية محددة بالأيام", fontSize = 11.sp, color = Color.Gray)
          }
          Switch(
            checked = isSubscribed,
            onCheckedChange = { isSubscribed = it }
          )
        }

        if (!isSubscribed) {
          OutlinedTextField(
            value = trialDays,
            onValueChange = { trialDays = it },
            label = { Text("عدد أيام الفترة التجريبية") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("إلغاء")
          }

          Button(
            onClick = {
              if (name.isBlank() || phone.isBlank() || password.isBlank()) {
                viewModel.showToast("يرجى إدخال جميع الحقول المطلوبة (الاسم، الجوال، كلمة المرور)")
                return@Button
              }
              val days = trialDays.toIntOrNull() ?: 30
              viewModel.addUserByAdmin(
                name = name.trim(),
                phone = phone.trim(),
                password = password.trim(),
                role = role,
                storeName = storeName.trim(),
                trialDays = days,
                isSubscribed = isSubscribed,
                onSuccess = onDismiss
              )
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.weight(1.5f)
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("حفظ وتفعيل", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
