package com.example.ui

import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import android.speech.RecognizerIntent
import android.app.Activity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
  fun AppUI(viewModel: AppViewModel) {
  // Force RTL direction for gorgeous Arabic POS look
  val context = androidx.compose.ui.platform.LocalContext.current
  
  val toastMessage by viewModel.toastMessage.collectAsState(null)
  val currencySymbol by viewModel.currencySymbol.collectAsState()
  var currentTab by remember { mutableStateOf("pos") }
  var showNotificationsDialog by remember { mutableStateOf(false) }
  var showUpgradeDialog by remember { mutableStateOf(false) }
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  
  // Fetching Database flows
  val products by viewModel.filteredProducts.collectAsState(emptyList())
  val categories by viewModel.allCategories.collectAsState(emptyList())
  val invoices by viewModel.allInvoices.collectAsState(emptyList())
  val customers by viewModel.allCustomers.collectAsState(emptyList())
  val ledgerEntries by viewModel.allLedgerEntries.collectAsState(emptyList())
  val activeShift by viewModel.activeShift.collectAsState(null)
  val auditLogs by viewModel.allAuditLogs.collectAsState(emptyList())
  val suppliers by viewModel.allSuppliers.collectAsState(emptyList())
  val banks by viewModel.allBanks.collectAsState(emptyList())
  
  val systemUsers by viewModel.allSystemUsers.collectAsState(emptyList())
  
  // Local Settings & simulation
  val currentUser by viewModel.currentUser.collectAsState("كاشير المبيعات")
  val currentUserRole by viewModel.currentUserRole.collectAsState("كاشير")
  val currentBranch by viewModel.currentBranch.collectAsState("الفرع الرئيسي")
  val storeName by viewModel.storeName.collectAsState("مؤسسة الكنز للمحاسبة التجارية")
  val storeLogoUrl by viewModel.storeLogoUrl.collectAsState("")
  val userAccount by viewModel.currentUserAccount.collectAsState()
  
  var showLogoutDialog by remember { mutableStateOf(false) }

  val sharedPrefs = remember { context.getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE) }
  val todayDateStr = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) }
  var showDailyBackupDialog by remember { mutableStateOf(false) }

  LaunchedEffect(todayDateStr) {
    val lastBackupDate = sharedPrefs.getString("last_backup_reminder_date", "")
    if (lastBackupDate.isNullOrEmpty()) {
      sharedPrefs.edit().putString("last_backup_reminder_date", todayDateStr).apply()
    } else if (lastBackupDate != todayDateStr) {
      showDailyBackupDialog = true
    }
  }

  val createBackupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
    contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
  ) { uri: android.net.Uri? ->
    if (uri != null) {
      scope.launch {
        viewModel.showToast("جاري إنشاء وتصدير النسخة الاحتياطية...")
        try {
          val isSuccess = viewModel.exportFullBackup(context, uri)
          if (isSuccess) {
            viewModel.showToast("تم إنشاء النسخة الاحتياطية بنجاح!")
            sharedPrefs.edit().putString("last_backup_reminder_date", todayDateStr).apply()
            showDailyBackupDialog = false
          } else {
            viewModel.showToast("حدث خطأ أثناء إنشاء النسخة.")
          }
        } catch (e: Exception) {
          viewModel.showToast("حدث خطأ أثناء النسخ.")
        }
      }
    }
  }

  val currentSystemUser = remember(userAccount, systemUsers) {
    systemUsers.find { it.uid == userAccount?.uid }
  }
  
  LaunchedEffect(currentSystemUser) {
    if (currentSystemUser != null && currentSystemUser.role != "ADMIN") {
      val isExpired = currentSystemUser.trialEndDate != null && System.currentTimeMillis() > currentSystemUser.trialEndDate
      if (isExpired || !currentSystemUser.isActive) {
        viewModel.logoutSystem()
        viewModel.showToast("تم تسجيل خروجك بسبب انتهاء صلاحية الاشتراك أو تعطيل الحساب.")
      }
    }
  }

  LaunchedEffect(currentTab, userAccount, currentUserRole) {
    if (userAccount?.role != "ADMIN" && userAccount != null) {
      val forbidden = mutableListOf("admin_users", "admin_settings", "admin_firebase", "admin_branches")
      if (currentUserRole != "مدير") {
        forbidden.add("admin_logs")
      }
      if (currentTab in forbidden) {
        currentTab = "pos"
      }
    }
  }
  
  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isCompact = maxWidth < 720.dp
    
    ModalNavigationDrawer(
      drawerState = drawerState,
      gesturesEnabled = isCompact,
      drawerContent = {
        if (isCompact) {
          ModalDrawerSheet(
            drawerContainerColor = MaterialTheme.colorScheme.surface,
            drawerTonalElevation = 0.dp
          ) {
            SideNavigationBar(
              currentTab = currentTab,
              onTabSelected = { currentTab = it },
              currentUser = currentUser,
              currentUserRole = currentUserRole,
              modifier = Modifier
                .width(300.dp)
                .fillMaxHeight(),
              onCloseDrawer = { scope.launch { drawerState.close() } },
              isAdmin = (userAccount?.role == "ADMIN"),
              currentSystemUser = currentSystemUser,
              storeName = storeName,
              storeLogoUrl = storeLogoUrl,
              onLogout = { showLogoutDialog = true }
            )
          }
        }
      }
    ) {
      Scaffold(
        topBar = {
          Column {
          TopAppBar(
            navigationIcon = {
              if (isCompact) {
                IconButton(
                  onClick = { scope.launch { drawerState.open() } },
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "القائمة الجانبية",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
            },
            title = {
          Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            AppLogo(size = 38.dp, logoUrl = storeLogoUrl)
              Column {
              Text(
              text = storeName,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurface
              )
              Text(
              text = "$currentBranch • $currentUser ($currentUserRole)",
              fontSize = 9.sp,
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
          actions = {
            val lowStockProducts = products.filter { it.quantity <= it.minQuantityAlert }
            val feedbacks by viewModel.allFeedbacks.collectAsState(initial = emptyList())
            val pendingFeedbacks = feedbacks.filter { it.status == "جديد" || it.status == "قيد المراجعة" }
            val userRepliedFeedbacks = feedbacks.filter { it.adminReply.isNotBlank() && it.status != "جديد" }
            val isExpiringSoon = if (userAccount?.role != "ADMIN" && currentSystemUser != null && currentSystemUser.trialEndDate != null) {
              val daysLeft = ((currentSystemUser.trialEndDate - System.currentTimeMillis()) / (24.0 * 60.0 * 60.0 * 1000.0)).toInt()
              daysLeft <= 3
            } else false
            val hasUnreadNotifications = lowStockProducts.isNotEmpty() || isExpiringSoon || (userAccount?.role == "ADMIN" && pendingFeedbacks.isNotEmpty()) || (userAccount?.role != "ADMIN" && userRepliedFeedbacks.isNotEmpty())

            IconButton(
              onClick = { showNotificationsDialog = true },
              modifier = Modifier.size(48.dp)
            ) {
              Box(modifier = Modifier.padding(4.dp)) {
                Icon(
                  imageVector = Icons.Default.Notifications,
                  contentDescription = "الإشعارات",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(32.dp)
                )
                if (hasUnreadNotifications) {
                  Box(
                    modifier = Modifier
                      .size(10.dp)
                      .clip(CircleShape)
                      .background(CoralError)
                      .align(Alignment.TopEnd)
                  )
                }
              }
            }
            IconButton(
              onClick = { showLogoutDialog = true },
              modifier = Modifier.size(44.dp)
            ) {
              Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = "تسجيل الخروج / تبديل الحساب",
                tint = CoralError,
                modifier = Modifier.size(24.dp)
              )
            }
          }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
        
        // Subscription/Trial Expiry Warning Banner for Customer
        if (userAccount?.role != "ADMIN" && currentSystemUser != null && currentSystemUser.trialEndDate != null) {
          val daysLeft = ((currentSystemUser.trialEndDate - System.currentTimeMillis()) / (24.0 * 60.0 * 60.0 * 1000.0)).toInt()
          if (daysLeft in 0..3) {
            var showBanner by remember(currentSystemUser.uid) { mutableStateOf(true) }
            if (showBanner) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(if (currentSystemUser.isSubscribed) AmberAlert.copy(alpha = 0.15f) else CoralError.copy(alpha = 0.15f))
                  .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(
                    imageVector = if (currentSystemUser.isSubscribed) Icons.Default.Warning else Icons.Default.HourglassEmpty,
                    contentDescription = null,
                    tint = if (currentSystemUser.isSubscribed) AmberAlert else CoralError,
                    modifier = Modifier.size(20.dp)
                  )
                  Column {
                    Text(
                      text = if (currentSystemUser.isSubscribed) {
                        "تنبيه تجديد الاشتراك السنوي "
                      } else {
                        "تنبيه انتهاء الفترة التجريبية "
                      },
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.sp,
                      color = if (currentSystemUser.isSubscribed) AmberAlert else CoralError
                    )
                    Text(
                      text = if (currentSystemUser.isSubscribed) {
                        "ينتهي اشتراكك السحابي لـ [${currentSystemUser.storeName}] خلال $daysLeft أيام. يرجى تجديد الترخيص للحفاظ على بياناتك."
                      } else {
                        "توشك فترتك التجريبية المجانية على الانتهاء! بقي $daysLeft أيام على إيقاف الفوترة السحابية."
                      },
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                  }
                }
                
                Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Button(
                    onClick = { showUpgradeDialog = true },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = if (currentSystemUser.isSubscribed) AmberAlert else CoralError,
                      contentColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                  ) {
                    Text(if (currentSystemUser.isSubscribed) "تجديد الآن " else "ترقية الاشتراك ", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                  }
                  
                  IconButton(
                    onClick = { showBanner = false },
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
              }
            }
          } else if (daysLeft < 0) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(CoralError.copy(alpha = 0.2f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = null,
                  tint = CoralError,
                  modifier = Modifier.size(22.dp)
                )
                Column {
                  Text(
                    "انتهت صلاحية الاشتراك السحابي ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = CoralError
                  )
                  Text(
                    "عذراً، لقد انتهت صلاحية حسابك السحابي. يرجى ترقية الاشتراك أو التواصل مع الإدارة للتفعيل فوراً.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }
              Button(
                onClick = { showUpgradeDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = CoralError, contentColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(34.dp)
              ) {
                Text("تفعيل وتجديد ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    },
      bottomBar = {
        if (isCompact) {
            val canAccessReports = userAccount?.role == "ADMIN" || currentSystemUser?.canAccessReports != false
            val canAccessInventory = userAccount?.role == "ADMIN" || currentSystemUser?.canAccessInventory != false
            val canAccessAccounting = userAccount?.role == "ADMIN" || currentSystemUser?.canAccessAccounting != false
            
            CurvedBottomNavigationBar(
                currentTab = currentTab,
                onTabSelected = { tab ->
                    if (tab == "more") {
                        currentTab = "more"
                    } else {
                        currentTab = tab
                    }
                },
                canAccessInventory = canAccessInventory,
                canAccessAccounting = canAccessAccounting,
                canAccessReports = canAccessReports,
                isAdmin = (userAccount?.role == "ADMIN")
            )
        }
      }
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .background(MaterialTheme.colorScheme.background)
      ) {
        if (!isCompact) {
          SideNavigationBar(
            currentTab = currentTab,
            onTabSelected = { currentTab = it },
            currentUser = currentUser,
            currentUserRole = currentUserRole,
            modifier = Modifier
              .width(280.dp)
              .fillMaxHeight(),
            isAdmin = (userAccount?.role == "ADMIN"),
            currentSystemUser = currentSystemUser,
            storeName = storeName,
            storeLogoUrl = storeLogoUrl,
            onLogout = { showLogoutDialog = true }
          )
          VerticalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
        }
        
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        ) {
          if (viewModel.subscriptionStatus.collectAsState().value == "TRIAL") {
            val whatsappNum by viewModel.supportWhatsappNumber.collectAsState()
            val bannerText by viewModel.subscriptionBannerText.collectAsState()
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            val infiniteTransition = rememberInfiniteTransition(label = "banner_pulse")
            val animatedAlpha by infiniteTransition.animateFloat(
              initialValue = 0.88f,
              targetValue = 1f,
              animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
              ),
              label = "alpha"
            )
            Surface(
              modifier = Modifier.fillMaxWidth(),
              color = MaterialTheme.colorScheme.primary.copy(alpha = animatedAlpha)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                // Animated Trial Text FIRST (Right side in RTL)
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp, end = 12.dp)
                    .clipToBounds(),
                  contentAlignment = Alignment.CenterStart
                ) {
                  Text(
                    text = " $bannerText ",
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

                // Contact Button SECOND (Other side / Left side in RTL)
                Button(
                  onClick = {
                    val cleanNum = whatsappNum.filter { it.isDigit() }
                    try {
                      uriHandler.openUri("https://wa.me/$cleanNum")
                    } catch (e: Exception) {
                      viewModel.showToast("عفواً، لا يمكن فتح الرابط.")
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  modifier = Modifier.height(32.dp),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Chat,
                      contentDescription = null,
                      tint = Color(0xFF25D366),
                      modifier = Modifier.size(16.dp)
                    )
                    Text(
                      "تواصل معنا",
                      color = MaterialTheme.colorScheme.onSurface,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }
          }
          
          if (viewModel.subscriptionStatus.collectAsState().value == "EXPIRED") {
            val whatsappNum by viewModel.supportWhatsappNumber.collectAsState()
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
              Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CoralError, modifier = Modifier.size(64.dp))
                Text("عفواً، الاشتراك منتهي ", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = CoralError)
                Text("يرجى التواصل مع الإدارة العليا لتجديد التراخيص والاشتراكات.", fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                  onClick = {
                    val cleanNum = whatsappNum.filter { it.isDigit() }
                    try {
                      uriHandler.openUri("https://wa.me/$cleanNum")
                    } catch (e: Exception) {
                      viewModel.showToast("عفواً، لا يمكن فتح الرابط.")
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.height(48.dp)
                ) {
                  Icon(Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("تواصل مع الإدارة عبر واتساب ", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
              }
            }
          } else {
            Box(modifier = Modifier.fillMaxSize()) {
            // Shift Check warning Banner
              
              AnimatedContent(
              targetState = currentTab,
                transitionSpec = {
                fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
              },
              label = "MainTabs"
                ) { tab ->
                  when (tab) {
                  "pos" -> PosScreen(viewModel, products, categories, customers, activeShift, banks, onNavigateTab = { currentTab = it })
                  "dashboard" -> DashboardScreen(viewModel, invoices, products, categories, ledgerEntries)
                  "inventory" -> InventoryScreen(viewModel, products, categories, suppliers)
                  "accounting" -> AccountingScreen(viewModel, ledgerEntries)
                  "statements" -> AccountStatementsScreen(viewModel)
                  "balances_report" -> BalancesReportScreen(viewModel, onBackClick = { currentTab = "more" })
                  "expense_balances_report" -> ExpenseBalancesReportScreen(viewModel, onBackClick = { currentTab = "more" })
                  "checks" -> ChecksScreen(viewModel, invoices, customers)
                  "crm" -> CrmScreen(viewModel, customers, invoices)
                  "settings" -> SettingsScreen(viewModel)
                  "admin_users" -> AdminUsersScreen(viewModel, onBackClick = { currentTab = "more" })
                  "admin_settings" -> AdminSettingsScreen(viewModel, onBackClick = { currentTab = "more" }, onNavigate = { currentTab = it })
                  "admin_logs" -> AdminActivityLogScreen(viewModel, onBackClick = { currentTab = "more" })
                  "admin_firebase" -> AdminFirebaseScreen(viewModel, onBackClick = { currentTab = "more" })
                  "admin_branches" -> AdminBranchesScreen(viewModel, onBackClick = { currentTab = "more" })
                  "invoice_form" -> InvoiceFormScreen(viewModel, onBackClick = { currentTab = "pos" })
                  "more" -> MoreScreen(viewModel, onNavigate = { currentTab = it }, onLogout = { showLogoutDialog = true })
                  "feature_requests" -> FeatureRequestsScreen(viewModel, onBackClick = { currentTab = "more" })
                  "backup_restore" -> BackupRestoreScreen(viewModel, onBackClick = { currentTab = "more" })
                  "transactions_history" -> TransactionsHistoryScreen(viewModel, onBackClick = { currentTab = "pos" })
                  "employees" -> EmployeesScreen(viewModel, onPrintStatement = { emp, expenses -> viewModel.exportEmployeeStatementToPdf(context, emp, expenses) })
                  "receipt_vouchers" -> ReceiptVoucherScreen(viewModel, onBackClick = { currentTab = "pos" })
                  "expenses" -> ExpenseVoucherScreen(viewModel, onOpenBalancesReport = { currentTab = "expense_balances_report" })
                  "journal_vouchers" -> JournalVoucherScreen(viewModel, onNavigate = { currentTab = it })
                }

            }
            
            // Toast Notification Overlay
              toastMessage?.let { msg ->
              Box(
              modifier = Modifier
              .fillMaxSize()
              .padding(top = 80.dp),
              contentAlignment = Alignment.TopCenter
                ) {
                Surface(
                modifier = Modifier
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 8.dp
                  ) {
                  Row(
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically
                    ) {
                    Icon(
                    Icons.Default.Info,
                    contentDescription = "تنبيه",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                    text = msg,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
                LaunchedEffect(msg) {
                kotlinx.coroutines.delay(2800)
                viewModel.clearToast()
              }
            }
          }
        }
        
          if (showNotificationsDialog) {
            val lowStockProducts = products.filter { it.quantity <= it.minQuantityAlert }
            val feedbacks by viewModel.allFeedbacks.collectAsState(initial = emptyList())
            val pendingFeedbacks = feedbacks.filter { it.status == "جديد" || it.status == "قيد المراجعة" }
            val userRepliedFeedbacks = feedbacks.filter { it.adminReply.isNotBlank() && it.status != "جديد" }
            AlertDialog(
              onDismissRequest = { showNotificationsDialog = false },
              title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = CoralError)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("مركز التنبيهات والإشعارات")
                }
              },
              text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  // Subscription expiry warning inside Notification Dialog
                  if (userAccount?.role != "ADMIN" && currentSystemUser != null && currentSystemUser.trialEndDate != null) {
                    val daysLeft = ((currentSystemUser.trialEndDate - System.currentTimeMillis()) / (24.0 * 60.0 * 60.0 * 1000.0)).toInt()
                    if (daysLeft in 0..3 || daysLeft < 0) {
                      Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                          containerColor = if (daysLeft < 0) CoralError.copy(alpha = 0.12f) else AmberAlert.copy(alpha = 0.12f)
                        ),
                        border = BorderStroke(1.dp, if (daysLeft < 0) CoralError.copy(alpha = 0.3f) else AmberAlert.copy(alpha = 0.3f))
                      ) {
                        Row(
                          modifier = Modifier.padding(10.dp),
                          horizontalArrangement = Arrangement.spacedBy(8.dp),
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Icon(
                            imageVector = if (daysLeft < 0) Icons.Default.Lock else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (daysLeft < 0) CoralError else AmberAlert,
                            modifier = Modifier.size(20.dp)
                          )
                          Column(modifier = Modifier.weight(1f)) {
                            Text(
                              text = if (daysLeft < 0) "انتهت فترة الترخيص!" else "اقتراب موعد انتهاء فترة الترخيص!",
                              fontWeight = FontWeight.Bold,
                              fontSize = 11.sp,
                              color = if (daysLeft < 0) CoralError else AmberAlert
                            )
                            Text(
                              text = if (daysLeft < 0) {
                                "يرجى ترقية الاشتراك لتفعيل كامل الصلاحيات."
                              } else {
                                "بقي $daysLeft أيام فقط على انتهاء ${if (currentSystemUser.isSubscribed) "الاشتراك الكامل" else "الفترة التجريبية"}."
                              },
                              fontSize = 10.sp,
                              color = MaterialTheme.colorScheme.onSurface
                            )
                          }
                          TextButton(
                            onClick = { 
                              showNotificationsDialog = false
                              showUpgradeDialog = true 
                            }
                          ) {
                            Text("التفاصيل", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                          }
                        }
                      }
                    }
                  }
                  
                  if (lowStockProducts.isEmpty()) {
                    val hasLicensingNotification = userAccount?.role != "ADMIN" && currentSystemUser != null && currentSystemUser.trialEndDate != null && 
                      ((currentSystemUser.trialEndDate - System.currentTimeMillis()) / (24.0 * 60.0 * 60.0 * 1000.0)).toInt() <= 3
                    
                    if (!hasLicensingNotification) {
                      if (userAccount?.role == "ADMIN" && pendingFeedbacks.isNotEmpty()) { 
                        Text("لديك ${pendingFeedbacks.size} طلبات/اقتراحات جديدة بانتظار المراجعة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp) 
                      } else if (userAccount?.role != "ADMIN" && userRepliedFeedbacks.isNotEmpty()) { 
                        Text("يوجد ردود من الإدارة على ${userRepliedFeedbacks.size} من طلباتك", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp) 
                      } else { 
                        Text("لا توجد إشعارات أو تنبيهات نشطة حالياً.", color = Color.Gray, fontSize = 12.sp) 
                      }
                    }
                  } else {
                    Text("تنبيه: المنتجات التالية وصلت للحد الأدنى من المخزون:", fontWeight = FontWeight.Bold, color = CoralError, fontSize = 12.sp)
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                      items(lowStockProducts) { product ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                          Text(product.nameAr, fontSize = 12.sp)
                          Text("الكمية المتبقية: ${product.quantity}", fontSize = 12.sp, color = CoralError, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider()
                      }
                    }
                  }
                }
              },
              confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) {
                  Text("حسناً")
                }
              }
            )
          }

          // Interactive Subscription Plan Dialog (Upgrade Dialog)
          if (showUpgradeDialog && currentSystemUser != null) {
            var isProcessingPayment by remember { mutableStateOf(false) }
            var paymentSuccess by remember { mutableStateOf(false) }
            
            AlertDialog(
              onDismissRequest = { 
                if (!isProcessingPayment) {
                  showUpgradeDialog = false 
                  paymentSuccess = false
                }
              },
              title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Icon(
                    imageVector = Icons.Default.CardMembership,
                    contentDescription = null,
                    tint = AmberAlert,
                    modifier = Modifier.size(26.dp)
                  )
                  Text(
                    text = if (currentSystemUser.isSubscribed) "تفاصيل الاشتراك السحابي النشط" else "ترقية الاشتراك وتفعيل باقة الكنز ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                  )
                }
              },
              text = {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  if (isProcessingPayment) {
                    Column(
                      modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      CircularProgressIndicator(color = AmberAlert)
                      Text("جاري معالجة عملية الدفع الآمنة سحابياً...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                      Text("يرجى عدم إغلاق النافذة لحين تشفير البيانات وتفعيل الترخيص.", fontSize = 10.sp, color = Color.Gray)
                    }
                  } else if (paymentSuccess) {
                    Column(
                      modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(48.dp))
                      Text("تهانينا! تم تفعيل اشتراك الكنز بنجاح ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                      Text("تم ترقية متجر [${currentSystemUser.storeName}] للباقة الذهبية الكاملة لـ 365 يوماً القادمة. تم تفعيل مزايا الفوترة اللامحدودة والمزامنة الفورية.", fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                  } else {
                    // Plan Comparison Card
                    Card(
                      modifier = Modifier.fillMaxWidth(),
                      shape = RoundedCornerShape(12.dp),
                      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Text("باقة الكنز السحابية الكاملة ", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                          Text("رائد الأعمال الذهبي ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AmberAlert)
                        }
                        
                        Text("تخلص من جميع قيود الفواتير والمنتجات وفعّل الأتمتة الكاملة لمحاسبتك التجارية:", fontSize = 11.sp, color = Color.Gray)
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        
                        val features = listOf(
                          " إصدار فواتير مبيعات سحابية غير محدودة (Unlimited Invoices)",
                          " إضافة منتجات وتصنيفات بلا حدود في مستودعك",
                          " مزامنة سحابية فورية ونسخ احتياطي تلقائي للبيانات والقيود",
                          " تقارير الأرباح والتحليلات والرسوم البيانية المتقدمة للأدمن",
                          " دعم فني ومحاسبي متميز متصل على مدار الساعة 24/7"
                        )
                        
                        features.forEach { feature ->
                          Text(feature, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                      }
                    }
                    
                    // Status Card
                    Card(
                      modifier = Modifier.fillMaxWidth(),
                      shape = RoundedCornerShape(10.dp),
                      colors = CardDefaults.cardColors(
                        containerColor = if (currentSystemUser.isSubscribed) EmeraldGreen.copy(alpha = 0.1f) else CoralError.copy(alpha = 0.1f)
                      )
                    ) {
                      Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Icon(
                          imageVector = if (currentSystemUser.isSubscribed) Icons.Default.Verified else Icons.Default.Info,
                          contentDescription = null,
                          tint = if (currentSystemUser.isSubscribed) EmeraldGreen else CoralError,
                          modifier = Modifier.size(18.dp)
                        )
                        Column {
                          Text(
                            text = if (currentSystemUser.isSubscribed) "حالة الاشتراك الحالي: نشط بالباقة الكاملة " else "حالة الاشتراك الحالي: نسخة تجريبية مؤقتة ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (currentSystemUser.isSubscribed) EmeraldGreen else CoralError
                          )
                          val daysLeft = currentSystemUser.trialEndDate?.let {
                            ((it - System.currentTimeMillis()) / (24.0 * 60.0 * 60.0 * 1000.0)).toInt()
                          } ?: 0
                          Text(
                            text = if (currentSystemUser.isSubscribed) {
                              "اشتراكك السحابي نشط وينتهي بعد $daysLeft يوماً."
                            } else {
                              "بقي $daysLeft أيام فقط على انتهاء الصلاحية التجريبية للفواتير."
                            },
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface
                          )
                        }
                      }
                    }
                  }
                }
              },
              confirmButton = {
                if (paymentSuccess) {
                  Button(
                    onClick = { 
                      showUpgradeDialog = false
                      paymentSuccess = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                  ) {
                    Text("رائع، حسناً")
                  }
                } else if (!isProcessingPayment) {
                  Button(
                    onClick = {
                      isProcessingPayment = true
                      scope.launch {
                        kotlinx.coroutines.delay(1800)
                        isProcessingPayment = false
                        paymentSuccess = true
                        viewModel.toggleUserSubscription(currentSystemUser)
                        viewModel.showToast("تم ترقية الاشتراك وتفعيله بنجاح! ")
                      }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAlert)
                  ) {
                    Text(if (currentSystemUser.isSubscribed) "تجديد/تمديد الاشتراك (365 يوم) " else "تفعيل باقة الكنز اللامحدودة (365 يوم) ")
                  }
                }
              },
              dismissButton = {
                if (!isProcessingPayment && !paymentSuccess) {
                  TextButton(onClick = { showUpgradeDialog = false }) {
                    Text("إلغاء")
                  }
                }
              }
            )
          }

          if (showDailyBackupDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showDailyBackupDialog = false }, properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)) {
              Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 10.dp
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                  // Luxury Icon Header with Halo Effect
                  Box(
                    modifier = Modifier
                      .size(80.dp)
                      .clip(CircleShape)
                      .background(Color(0xFF3B5998).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Box(
                      modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B5998)),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                      )
                    }
                  }

                  Text(
                    text = "النسخة الاحتياطية\nاليومية",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 30.sp,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  
                  Text(
                    text = "يجب إنشاء نسخة احتياطية لبيانات اليوم\nقبل متابعة العمل.",
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  
                  Spacer(modifier = Modifier.height(8.dp))
                  
                  Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                  ) {
                    Button(
                      onClick = {
                        val dateStr = java.text.SimpleDateFormat("yyyy_MM_dd", java.util.Locale.US).format(java.util.Date())
                        val fileName = "AlKanz_Backup_$dateStr.json"
                        try {
                          createBackupLauncher.launch(fileName)
                        } catch (e: Exception) {
                          viewModel.showToast("لا يوجد تطبيق مدعوم لإدارة الملفات على هذا الجهاز.")
                        }
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B5998)),
                      shape = RoundedCornerShape(12.dp),
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                    ) {
                      Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("إنشاء النسخة والمتابعة", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    }

                    TextButton(
                      onClick = {
                        sharedPrefs.edit().putString("last_backup_reminder_date", todayDateStr).apply()
                        showDailyBackupDialog = false
                      },
                      modifier = Modifier.fillMaxWidth().height(50.dp),
                      shape = RoundedCornerShape(12.dp)
                    ) {
                      Text("تخطي مؤقتًا", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                  }
                }
              }
            }
          }

          if (showLogoutDialog) {
            AlertDialog(
              onDismissRequest = { showLogoutDialog = false },
              title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Icon(Icons.Default.ExitToApp, contentDescription = null, tint = CoralError)
                  Text("تأكيد تسجيل الخروج", fontWeight = FontWeight.Bold)
                }
              },
              text = {
                Text("هل أنت متأكد أنك تريد تسجيل الخروج من النظام؟")
              },
              confirmButton = {
                Button(
                  onClick = {
                    showLogoutDialog = false
                    viewModel.logoutSystem()
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = CoralError)
                ) {
                  Text("نعم، تسجيل الخروج")
                }
              },
              dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                  Text("إلغاء")
                }
              }
            )
          }
      }
      }
      }
    }
  }
}

@Composable
fun SideNavigationBar(
  currentTab: String,
  onTabSelected: (String) -> Unit,
  currentUser: String,
  currentUserRole: String,
  modifier: Modifier = Modifier,
  onCloseDrawer: (() -> Unit)? = null,
  isAdmin: Boolean = false,
  currentSystemUser: SystemUser? = null,
  storeName: String = "الكنز للمحاسبة",
  storeLogoUrl: String = "",
  onLogout: () -> Unit = {}
) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(0.dp),
    border = null,
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(12.dp)
    ) {
      // Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 24.dp, top = 8.dp)
      ) {
        AppLogo(size = 44.dp, logoUrl = storeLogoUrl)
        Column {
          Text(
            text = storeName,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "نظام إدارة المبيعات والمخازن",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 16.dp))

      // User Profile Section
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
          .padding(12.dp)
          .padding(bottom = 4.dp)
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
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
            text = currentUser,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = currentUserRole,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
          )
        }
      }
      Spacer(modifier = Modifier.height(20.dp))

      val canAccessReports = isAdmin || currentSystemUser?.canAccessReports != false
      val canAccessInventory = isAdmin || currentSystemUser?.canAccessInventory != false
      val canAccessAccounting = isAdmin || currentSystemUser?.canAccessAccounting != false

      if (isAdmin) {
        // Admin Only Control Center
        SidebarSectionHeader(title = "لوحة التحكم للأدمن ")
        SidebarItem(
          id = "admin_users",
          label = "لوحة التحكم (Control Page)",
          icon = Icons.Default.Lock,
          isSelected = currentTab == "admin_users",
          onClick = { onTabSelected("admin_users"); onCloseDrawer?.invoke() }
        )
        SidebarItem(
          id = "admin_settings",
          label = "تخصيص هوية التطبيق",
          icon = Icons.Default.Brush,
          isSelected = currentTab == "admin_settings",
          onClick = { onTabSelected("admin_settings"); onCloseDrawer?.invoke() }
        )
        SidebarItem(
          id = "admin_logs",
          label = "سجل الأنشطة (Activity Log)",
          icon = Icons.Default.History,
          isSelected = currentTab == "admin_logs",
          onClick = { onTabSelected("admin_logs"); onCloseDrawer?.invoke() }
        )
        SidebarItem(
          id = "admin_firebase",
          label = "إعدادات مزامنة Firebase",
          icon = Icons.Default.CloudSync,
          isSelected = currentTab == "admin_firebase",
          onClick = { onTabSelected("admin_firebase"); onCloseDrawer?.invoke() }
        )
        SidebarItem(
          id = "admin_branches",
          label = "إدارة وتخصيص الفروع",
          icon = Icons.Default.Store,
          isSelected = currentTab == "admin_branches",
          onClick = { onTabSelected("admin_branches"); onCloseDrawer?.invoke() }
        )
        SidebarItem(
          id = "settings",
          label = "إعدادات النظام والتهيئة",
          icon = Icons.Default.Settings,
          isSelected = currentTab == "settings",
          onClick = { onTabSelected("settings"); onCloseDrawer?.invoke() }
        )
        SidebarItem(
          id = "employees",
          label = "إدارة الموظفين",
          icon = Icons.Default.Person,
          isSelected = currentTab == "employees",
          onClick = { onTabSelected("employees"); onCloseDrawer?.invoke() }
        )
        SidebarItem(
          id = "receipt_vouchers",
          label = "سندات القبض (تحصيل)",
          icon = Icons.Default.CallReceived,
          isSelected = currentTab == "receipt_vouchers",
          onClick = { onTabSelected("receipt_vouchers"); onCloseDrawer?.invoke() }
        )
        SidebarItem(
          id = "expenses",
          label = "سندات الصرف",
          icon = Icons.Default.AttachMoney,
          isSelected = currentTab == "expenses",
          onClick = { onTabSelected("expenses"); onCloseDrawer?.invoke() }
        )
        SidebarItem(
          id = "journal_vouchers",
          label = "سند القيد",
          icon = Icons.Default.ReceiptLong,
          isSelected = currentTab == "journal_vouchers",
          onClick = { onTabSelected("journal_vouchers"); onCloseDrawer?.invoke() }
        )
        Spacer(modifier = Modifier.height(16.dp))
      } else {
        // Section 1: العمليات الأساسية (Operations & Sales)
        SidebarSectionHeader(title = "العمليات والبيع")
        SidebarItem(
          id = "pos",
          label = "نقطة البيع والمبيعات",
          icon = Icons.Default.PointOfSale,
          isSelected = currentTab == "pos",
          onClick = { onTabSelected("pos"); onCloseDrawer?.invoke() }
        )
        if (canAccessInventory) {
          SidebarItem(
            id = "inventory",
            label = "إدارة المخزون والمستودعات",
            icon = Icons.Default.Inventory,
            isSelected = currentTab == "inventory",
            onClick = { onTabSelected("inventory"); onCloseDrawer?.invoke() }
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 2: التقارير المالية (Financial Reports)
        if (canAccessReports || canAccessAccounting) {
          SidebarSectionHeader(title = "التقارير والمحاسبة")
          if (canAccessReports) {
            SidebarItem(
              id = "dashboard",
              label = "لوحة التحكم والتحليلات",
              icon = Icons.Default.Dashboard,
              isSelected = currentTab == "dashboard",
              onClick = { onTabSelected("dashboard"); onCloseDrawer?.invoke() }
            )
          }
          if (canAccessAccounting) {
            SidebarItem(
              id = "accounting",
              label = "المحاسبة والتقارير المالية",
              icon = Icons.Default.AccountBalance,
              isSelected = currentTab == "accounting",
              onClick = { onTabSelected("accounting"); onCloseDrawer?.invoke() }
            )
            SidebarItem(
              id = "receipt_vouchers",
              label = "سندات القبض (تحصيل مالي)",
              icon = Icons.Default.CallReceived,
              isSelected = currentTab == "receipt_vouchers",
              onClick = { onTabSelected("receipt_vouchers"); onCloseDrawer?.invoke() }
            )
            SidebarItem(
              id = "statements",
              label = "كشوفات الحسابات التفصيلية",
              icon = Icons.Default.ReceiptLong,
              isSelected = currentTab == "statements",
              onClick = { onTabSelected("statements"); onCloseDrawer?.invoke() }
            )
            SidebarItem(
              id = "balances_report",
              label = "كشف الأرصدة (عملاء/موردين)",
              icon = Icons.Default.AccountBalanceWallet,
              isSelected = currentTab == "balances_report",
              onClick = { onTabSelected("balances_report"); onCloseDrawer?.invoke() }
            )
            SidebarItem(
              id = "expense_balances_report",
              label = "كشف أرصدة المصاريف",
              icon = Icons.Default.Payments,
              isSelected = currentTab == "expense_balances_report",
              onClick = { onTabSelected("expense_balances_report"); onCloseDrawer?.invoke() }
            )
            SidebarItem(
              id = "checks",
              label = "سجل الشيكات",
              icon = Icons.Default.Article,
              isSelected = currentTab == "checks",
              onClick = { onTabSelected("checks"); onCloseDrawer?.invoke() }
            )
            SidebarItem(
              id = "employees",
              label = "إدارة الموظفين",
              icon = Icons.Default.Person,
              isSelected = currentTab == "employees",
              onClick = { onTabSelected("employees"); onCloseDrawer?.invoke() }
            )
            SidebarItem(
              id = "expenses",
              label = "سندات الصرف",
              icon = Icons.Default.AttachMoney,
              isSelected = currentTab == "expenses",
              onClick = { onTabSelected("expenses"); onCloseDrawer?.invoke() }
            )
            SidebarItem(
              id = "journal_vouchers",
              label = "سند القيد",
              icon = Icons.Default.ReceiptLong,
              isSelected = currentTab == "journal_vouchers",
              onClick = { onTabSelected("journal_vouchers"); onCloseDrawer?.invoke() }
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
        }

        val canAccessAdvancedTools = isAdmin || currentSystemUser?.canAccessAdvancedTools != false

        // Section 3: العملاء والورديات (CRM & Shifts)
        if (canAccessAdvancedTools) {
          SidebarSectionHeader(title = "العملاء والورديات")
          SidebarItem(
            id = "crm",
            label = "إدارة العملاء CRM",
            icon = Icons.Default.People,
            isSelected = currentTab == "crm",
            onClick = { onTabSelected("crm"); onCloseDrawer?.invoke() }
          )
          Spacer(modifier = Modifier.height(16.dp))
        }

        // Section 4: الإدارة والضبط (Admin & Settings)
        SidebarSectionHeader(title = "الإدارة والتحكم")
        SidebarItem(
          id = "settings",
          label = "إعدادات النظام والتهيئة",
          icon = Icons.Default.Settings,
          isSelected = currentTab == "settings",
          onClick = { onTabSelected("settings"); onCloseDrawer?.invoke() }
        )
      }
      
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 12.dp))
      
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .clickable { onLogout() }
          .padding(horizontal = 12.dp, vertical = 10.dp)
          .testTag("sidebar_logout_button")
      ) {
        Icon(
          imageVector = Icons.Default.ExitToApp,
          contentDescription = "تسجيل الخروج",
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(20.dp)
        )
        Text(
          text = "تسجيل الخروج",
          color = MaterialTheme.colorScheme.error,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )
      }
      
      Spacer(modifier = Modifier.height(24.dp))
      Text(
        text = "الكنز للمحاسبة v2.5",
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}

@Composable
fun SidebarSectionHeader(title: String) {
  Text(
    text = title,
    fontWeight = FontWeight.Bold,
    fontSize = 11.sp,
    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
  )
}

@Composable
fun SidebarItem(
  id: String,
  label: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val backgroundColor = if (isSelected) {
    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
  } else {
    Color.Transparent
  }
  val contentColor = if (isSelected) {
    MaterialTheme.colorScheme.primary
  } else {
    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
  }
  
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 2.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(backgroundColor)
      .clickable { onClick() }
      .padding(horizontal = 12.dp, vertical = 10.dp)
      .testTag("sidebar_item_$id")
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = contentColor,
      modifier = Modifier.size(20.dp)
    )
    Text(
      text = label,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
      fontSize = 12.sp,
      color = contentColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

// ==========================================
// 1. POS SCREEN (POINT OF SALE)
// ==========================================

// ==========================================
// MAIN ACTION HUB SCREEN (واجهة التطبيق الرئيسية)
// ==========================================
@Composable
fun MainActionHubScreen(
  viewModel: AppViewModel,
  onOpenSalesInvoice: () -> Unit,
  onOpenSalesDraft: () -> Unit,
  onOpenPurchaseInvoice: () -> Unit,
  onOpenPurchaseDraft: () -> Unit,
  onOpenReturns: () -> Unit,
  onOpenInventory: () -> Unit,
  onOpenInvoiceHistory: () -> Unit,
  onOpenTransactionsLog: () -> Unit = {},
  onOpenAdvancedSalesInvoice: () -> Unit = {},
  onOpenJournalVouchers: () -> Unit = {}
) {
  val allProductsState by viewModel.allProducts.collectAsState(initial = emptyList())
  val lowStockList = remember(allProductsState) { allProductsState.filter { it.quantity <= it.minQuantityAlert } }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .verticalScroll(rememberScrollState())
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // Low Stock Alert Banner
    if (lowStockList.isNotEmpty()) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CoralError.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, CoralError.copy(alpha = 0.4f))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Surface(
              shape = CircleShape,
              color = CoralError,
              modifier = Modifier.size(36.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
              }
            }
            Column {
              Text(
                text = "تنبيه: وصول المنتجات إلى حد الطلب! ",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = CoralError
              )
              Text(
                text = "يوجد ${lowStockList.size} منتج في المستودع وصل لمستوى 'حد الطلب' المنخفض ويجب إعادة توريدها.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = onOpenInventory,
            colors = ButtonDefaults.buttonColors(containerColor = CoralError),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text("عرض المستودع ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
          }
        }
      }
    }

    // Row 1: Sales Invoice & Advanced Sales Invoice (نموذج اصدار فواتير مبيعات متطور)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      HubActionCard(
        title = "إنشاء فاتورة مبيع",
        icon = Icons.Default.ShoppingCart,
        iconBgColor = Color(0xFF0284C7),
        onClick = onOpenSalesInvoice,
        secondaryText = "مسودة مبيع ",
        onSecondaryClick = onOpenSalesDraft,
        modifier = Modifier.weight(1f)
      )

      HubActionCard(
        title = "نموذج فواتير مبيعات متطور",
        icon = Icons.Default.Receipt,
        iconBgColor = Color(0xFF0D9488),
        onClick = onOpenAdvancedSalesInvoice,
        secondaryText = "إصدار متقدم ",
        onSecondaryClick = onOpenAdvancedSalesInvoice,
        modifier = Modifier.weight(1f)
      )
    }

    // Row 2: Purchase Invoice & Returns
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      HubActionCard(
        title = "إنشاء فاتورة شراء",
        icon = Icons.Default.LocalShipping,
        iconBgColor = Color(0xFF16A34A),
        onClick = onOpenPurchaseInvoice,
        secondaryText = "مسودة شراء ",
        onSecondaryClick = onOpenPurchaseDraft,
        modifier = Modifier.weight(1f)
      )

      HubActionCard(
        title = "فواتير المرتجع والتوالف",
        icon = Icons.Default.PublishedWithChanges,
        iconBgColor = Color(0xFFE11D48),
        onClick = onOpenReturns,
        secondaryText = "إدارة المرتجع ",
        onSecondaryClick = onOpenReturns,
        modifier = Modifier.weight(1f)
      )
    }

    // Row 3: Inventory Audit & Invoice History
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      HubActionCard(
        title = "جرد المستودع",
        icon = Icons.Default.Inventory2,
        iconBgColor = Color(0xFFD97706),
        onClick = onOpenInventory,
        secondaryText = "متابعة الكميات ",
        onSecondaryClick = onOpenInventory,
        modifier = Modifier.weight(1f)
      )

      HubActionCard(
        title = "سجل الفواتير السابقة",
        icon = Icons.Default.ReceiptLong,
        iconBgColor = Color(0xFF7C3AED),
        onClick = onOpenInvoiceHistory,
        secondaryText = "عرض الأرشيف ",
        onSecondaryClick = onOpenInvoiceHistory,
        modifier = Modifier.weight(1f)
      )
    }

    // Row 4: Drafts & Journal Vouchers
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      HubActionCard(
        title = "مسودات الفواتير",
        icon = Icons.Default.EditNote,
        iconBgColor = Color(0xFF059669),
        onClick = onOpenSalesDraft,
        secondaryText = "متابعة المسودات ",
        onSecondaryClick = onOpenSalesDraft,
        modifier = Modifier.weight(1f)
      )

      HubActionCard(
        title = "سند القيد المحاسبي",
        icon = Icons.Default.ReceiptLong,
        iconBgColor = Color(0xFF4F46E5),
        onClick = onOpenJournalVouchers,
        secondaryText = "سندات الخزينة ",
        onSecondaryClick = onOpenJournalVouchers,
        modifier = Modifier.weight(1f)
      )
    }

    // Row 4: Master Full-Width Transactions Log Button
    val allInvoicesState by viewModel.allInvoices.collectAsState(emptyList())
    val allCustomerTxsState by viewModel.allCustomerTransactions.collectAsState(emptyList())
    val totalTransactionsCount = remember(allInvoicesState, allCustomerTxsState) {
      allInvoicesState.count { !it.isDraft } + allCustomerTxsState.size
    }

    Card(
      onClick = onOpenTransactionsLog,
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 6.dp, bottom = 14.dp),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
      ),
      border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(
            Brush.horizontalGradient(
              listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                MaterialTheme.colorScheme.surface
              )
            )
          )
          .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
          modifier = Modifier.weight(1f)
        ) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 2.dp,
            modifier = Modifier.size(48.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = "سجل المعاملات",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(26.dp)
              )
            }
          }

          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "سجل المعاملات والحركات المالية",
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "عرض شامل ومنظم لكافة الفواتير، المقبوضات، السندات، والمصروفات بالتاريخ والوقت",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
              maxLines = 2,
              lineHeight = 15.sp
            )
          }
        }

        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
          modifier = Modifier.size(32.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.ChevronLeft,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }
  }
}

class TopCutoutShape(private val cutoutRadius: Float, private val cornerRadius: Float) : androidx.compose.ui.graphics.Shape {
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
}


@Composable
fun PosScreen(
  viewModel: AppViewModel,
  products: List<Product>,
  categories: List<Category>,
  customers: List<Customer>,
  activeShift: Shift?,
  banks: List<Bank>,
  onNavigateTab: (String) -> Unit = {}
) {
  val cart by viewModel.cart.collectAsState(emptyList())
  val discount by viewModel.discount.collectAsState(0.0)
  val searchQuery by viewModel.searchQuery.collectAsState("")
  val selectedCategory by viewModel.selectedCategory.collectAsState(null)
  val selectedCustomerId by viewModel.selectedCustomerId.collectAsState(null)
  val taxPercent by viewModel.taxPercent.collectAsState(15.0)
  val currencySymbol by viewModel.currencySymbol.collectAsState("شيكل")
  val context = androidx.compose.ui.platform.LocalContext.current
  
  var activePosView by remember { mutableStateOf("hub") }
  var showCheckoutDialog by remember { mutableStateOf(false) }
  var showReceiptDialog by remember { mutableStateOf(false) }
  var showPrinterDialog by remember { mutableStateOf(false) }

  var showPurchaseDialog by remember { mutableStateOf(false) }
  var showPurchaseDraftDialog by remember { mutableStateOf(false) }
  var showSalesDraftDialog by remember { mutableStateOf(false) }
  var showReturnsDialog by remember { mutableStateOf(false) }
  var showInvoiceHistoryDialog by remember { mutableStateOf(false) }
  var showSalesSetupDialog by remember { mutableStateOf(false) }
  var isSalesReturn by remember { mutableStateOf(false) }
  var isPurchaseReturn by remember { mutableStateOf(false) }

  val allCustomerTransactions by viewModel.allCustomerTransactions.collectAsState(initial = emptyList())
  
  val subtotal = cart.sumOf { it.product.priceSale * it.quantity }
  val discountedSubtotal = (subtotal - discount).coerceAtLeast(0.0)
  val taxAmount = discountedSubtotal * (taxPercent / 100)
  val total = discountedSubtotal + taxAmount
  
  var isDraftMode by remember { mutableStateOf(false) }
  
  if (activePosView == "hub") {
    MainActionHubScreen(
      viewModel = viewModel,
      onOpenSalesInvoice = {
        isSalesReturn = false
        isDraftMode = false
        showSalesSetupDialog = true
      },
      onOpenSalesDraft = {
        isSalesReturn = false
        isDraftMode = true
        showSalesSetupDialog = true 
      },
      onOpenPurchaseInvoice = {
        isPurchaseReturn = false
        isDraftMode = false
        showPurchaseDialog = true
      },
      onOpenPurchaseDraft = { 
        isPurchaseReturn = false
        isDraftMode = true
        showPurchaseDialog = true 
      },
      onOpenReturns = { showReturnsDialog = true },
      onOpenInventory = { onNavigateTab("inventory") },
      onOpenInvoiceHistory = { showInvoiceHistoryDialog = true },
      onOpenTransactionsLog = { onNavigateTab("transactions_history") },
      onOpenAdvancedSalesInvoice = { onNavigateTab("invoice_form") },
      onOpenJournalVouchers = { onNavigateTab("journal_vouchers") }
    )
  } else if (activePosView == "purchase") {
    PosSalesInvoiceScreen(
      viewModel = viewModel,
      onBackClick = { activePosView = "hub" },
      isPurchaseMode = true,
      isReturnMode = isPurchaseReturn,
      isDraftMode = isDraftMode
    )
  } else {
    PosSalesInvoiceScreen(
      viewModel = viewModel,
      onBackClick = { activePosView = "hub" },
      isPurchaseMode = false,
      isReturnMode = isSalesReturn,
      isDraftMode = isDraftMode
    )
  }
  
  // --- POS checkout dialog ---
    if (showCheckoutDialog) {
    var paymentMethod by remember { mutableStateOf("نقدي") }
    var cashPaidInput by remember { mutableStateOf("") }
    var checkNumber by remember { mutableStateOf("") }
    var checkBankCode by remember { mutableStateOf("") }
    var checkDateStr by remember { mutableStateOf("") }
    var checkAmountStr by remember { mutableStateOf(total.toString()) }
    val cashPaid = cashPaidInput.toDoubleOrNull() ?: 0.0
    val checkAmount = checkAmountStr.toDoubleOrNull() ?: 0.0
    val matchedBank = banks.find { it.code == checkBankCode }
      val changeCalculated = if (paymentMethod == "نقدي") {
      (cashPaid - total).coerceAtLeast(0.0)
    } else 0.0
    
    AlertDialog(
    onDismissRequest = { showCheckoutDialog = false },
    title = { Text("بوابة الدفع الفوري الفنية", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
      text = {
      
      Column(
      modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        Row(
        modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
        .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
          ) {
          Text("المبلغ الإجمالي المطلوب:", fontWeight = FontWeight.Bold)
          Text("${formatMoney(total)} ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        
        Text("طريقة الدفع:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
          val methods = listOf("نقدي" to Icons.Default.Money, "بطاقة" to Icons.Default.CreditCard, "محفظة" to Icons.Default.AccountBalanceWallet, "شيك" to Icons.Default.Article)
            methods.forEach { (m, icon) ->
            val isSelected = paymentMethod == m
            androidx.compose.material3.Surface(
              onClick = { paymentMethod = m },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
              color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
              contentColor = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary
            ) {
              Column(
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
              ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(m, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
              }
            }
          }
        }
        
          if (paymentMethod == "نقدي") {
          OutlinedTextField(
          value = cashPaidInput,
          onValueChange = { cashPaidInput = it },
          label = { Text("المبلغ النقدي المسلم:") },
          suffix = { Text(viewModel.currencySymbol.collectAsState().value) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
          )
          
          Row(
          modifier = Modifier
          .fillMaxWidth()
          .background(EmeraldGreen.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
          .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
            ) {
            Text("الباقي للعميل:", fontWeight = FontWeight.Bold, color = EmeraldGreen)
            Text("${formatMoney(changeCalculated)} ", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = EmeraldGreen)
          }
          } else if (paymentMethod == "شيك") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = checkNumber,
                onValueChange = { checkNumber = it },
                label = { Text("رقم الشيك") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
              )
              OutlinedTextField(
                value = checkBankCode,
                onValueChange = { checkBankCode = it },
                label = { Text("رقم البنك") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
              )
              if (matchedBank != null) {
                Text("البنك: ${matchedBank.name}", color = EmeraldGreen, fontSize = 12.sp)
              } else if (checkBankCode.isNotEmpty()) {
                Text("رقم البنك غير مسجل", color = CoralError, fontSize = 12.sp)
              }
              OutlinedTextField(
                value = checkDateStr,
                onValueChange = { checkDateStr = it },
                label = { Text("تاريخ استلام الشيك (مثال: 2026-07-18)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
              )
              OutlinedTextField(
                value = checkAmountStr,
                onValueChange = { checkAmountStr = it },
                label = { Text("مبلغ الشيك") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
              )
            }
          } else {
          Box(
          modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
          .padding(12.dp),
          contentAlignment = Alignment.Center
            ) {
            Text(" مرر بطاقة العميل على جهاز مدى المربوط للخصم المباشر.", fontSize = 12.sp, fontWeight = FontWeight.Medium)
          }
        }
      }
    },
      confirmButton = {
      Button(
        onClick = {
        val actualPaid = if (paymentMethod == "نقدي") cashPaid else total
        viewModel.performCheckout(
        paymentMethod = paymentMethod,
        paidAmount = actualPaid,
        changeAmount = changeCalculated,
        isReturnMode = isSalesReturn,
        checkNumber = checkNumber.takeIf { paymentMethod == "شيك" },
        checkBankName = matchedBank?.name ?: checkBankCode.takeIf { paymentMethod == "شيك" },
        checkDate = if (paymentMethod == "شيك") {
          try {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(checkDateStr)?.time
          } catch (e: Exception) { null }
        } else null,
        customInvoiceDate = viewModel.setupCustomDate.value,
        onSuccess = {
          showCheckoutDialog = false
          showReceiptDialog = true
        }
        )
      },
      enabled = when (paymentMethod) {
        "نقدي" -> cashPaid >= total
        "شيك" -> checkNumber.isNotBlank() && checkBankCode.isNotBlank() && checkDateStr.isNotBlank() && checkAmount >= total
        else -> true
      },
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
      shape = RoundedCornerShape(12.dp)
        ) {
        Text("تأكيد العملية")
      }
    },
      dismissButton = {
        TextButton(onClick = { showCheckoutDialog = false }) {
        Text("إلغاء", color = CoralError)
      }
    }
    )
  }
  
  // --- POS thermal receipt print dialog ---
    if (showReceiptDialog) {
    val lastInvoice by viewModel.lastCheckedOutInvoice.collectAsState(null)
    val lastItems by viewModel.lastCheckedOutItems.collectAsState(emptyList())
    val storePhone by viewModel.storePhone.collectAsState("")
    val storeVatNumber by viewModel.storeVatNumber.collectAsState("")
    val currentBranch by viewModel.currentBranch.collectAsState("الفرع الرئيسي")
    val cashierPhone by viewModel.cashierPhone.collectAsState("")
    val cashierAddress by viewModel.cashierAddress.collectAsState("")
    
    AlertDialog(
    onDismissRequest = { showReceiptDialog = false },
      title = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("الفاتورة الحرارية المبسطة ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { showPrinterDialog = true }) {
              Icon(Icons.Default.Settings, contentDescription = "إعدادات الطابعة", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { viewModel.showToast("تم إرسال الفاتورة لعميلنا عبر الواتساب بنجاح! ") }) {
              Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = MaterialTheme.colorScheme.primary)
            }
          }
        }
      },
      text = {
      Box(
      modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
      .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
      .padding(12.dp)
        ) {
        
        Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
          ) {
          Text(
          text = viewModel.storeName.collectAsState().value,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp,
          textAlign = TextAlign.Center
          )
          Text("الهاتف: $storePhone", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
          if (cashierPhone.isNotEmpty() && cashierPhone != storePhone) {
            Text("هاتف الكاشير: $cashierPhone", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
          }
          Text("الرقم الضريبي: $storeVatNumber", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
          Text("الفرع: $currentBranch", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
          if (cashierAddress.isNotEmpty()) {
            Text("العنوان: $cashierAddress", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
          }
          
          Text("------------------------------------------", color = Color.LightGray)
          
            lastInvoice?.let { inv ->
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("رقم الفاتورة: #${inv.id}", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
              Text(sdf.format(Date(inv.invoiceDate)), color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
            }
            Text("الكاشير: ${inv.cashierName}", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, modifier = Modifier.align(Alignment.Start))
            
            val linkedCust = customers.find { it.id == inv.customerId }
            if (linkedCust != null) {
              Text("العميل: ${linkedCust.name}", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, modifier = Modifier.align(Alignment.Start))
            }
            
            Text("------------------------------------------", color = Color.LightGray)
            
            
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("الصنف", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                Text("الكمية", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                Text("الإجمالي", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
              }
                lastItems.forEach { cartItem ->
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text(cartItem.product.nameAr, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
                  Text("x${cartItem.quantity}", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                  Text("${formatMoney(cartItem.product.priceSale * cartItem.quantity)} ", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }
              }
            }
            
            Text("------------------------------------------", color = Color.LightGray)
            
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("المجموع الفرعي:", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
              Text("${formatMoney(inv.subtotal)} ", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
            }
              if (inv.discount > 0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("الخصم النقدي:", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                Text("- ${formatMoney(inv.discount)} ", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
              }
            }
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("ضريبة القيمة المضافة (15%):", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
              Text("${formatMoney(inv.taxAmount)} ", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
            }
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("المجموع النهائي:", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              Text("${formatMoney(inv.total)} ", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            
            Text("------------------------------------------", color = Color.LightGray)
            
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("طريقة الدفع: ${inv.paymentMethod}", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
              Text("المدفوع: ${formatMoney(inv.paidAmount)} ", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
            }
            if (inv.paymentMethod == "شيك") {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("رقم الشيك: ${inv.checkNumber ?: ""}", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                Text("البنك: ${inv.checkBankName ?: ""}", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
              }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("الباقي المعاد:", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
              Text("${formatMoney(inv.changeAmount)} ", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Box(
            modifier = Modifier
            .size(80.dp)
            .border(1.dp, MaterialTheme.colorScheme.onSurface)
            .padding(4.dp),
            contentAlignment = Alignment.Center
              ) {
              Icon(Icons.Default.QrCode, contentDescription = "Zakat QR Code", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxSize())
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
            "شاكرين تسوقكم معنا ",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
            )
            Text("فاتورة إلكترونية معتمدة من هيئة الزكاة والجمارك", color = Color.Gray, fontSize = 8.sp, textAlign = TextAlign.Center)
          }
        }
      }
    },
      confirmButton = {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Button(
          onClick = {
            lastInvoice?.let { inv ->
              val linkedCust = customers.find { it.id == inv.customerId }
              viewModel.exportInvoiceToPdf(context, inv, lastItems, linkedCust?.name)
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.PictureAsPdf, contentDescription = "تنزيل PDF")
          Spacer(modifier = Modifier.width(6.dp))
          Text("تنزيل كـ PDF")
        }

        Button(
          onClick = {
            lastInvoice?.let { inv ->
              val linkedCust = customers.find { it.id == inv.customerId }
              viewModel.printThermalInvoice(
                invoice = inv,
                items = lastItems,
                customerName = linkedCust?.name,
                onNeedPrinterSelection = {
                  showPrinterDialog = true
                }
              )
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.Print, contentDescription = "طباعة حرارية")
          Spacer(modifier = Modifier.width(6.dp))
          Text("طباعة حرارية (Bluetooth) ")
        }
      }
    },
      dismissButton = {
        TextButton(onClick = { showReceiptDialog = false }) {
        Text("إغلاق")
      }
    }
    )
  }

  // --- 1. Purchase Invoice Setup Dialog ("إعداد الفاتورة" للمورد) ---
  if (showPurchaseDialog) {
    val suppliers by viewModel.allSuppliers.collectAsState(emptyList())
    val selectedSupplierId by viewModel.selectedSupplierId.collectAsState()
    val allSupplierTransactions by viewModel.allSupplierTransactions.collectAsState(emptyList())

    var selectedSupLocal by remember { mutableStateOf<Supplier?>(suppliers.find { it.id == selectedSupplierId }) }
    var manualNameLocal by remember { mutableStateOf(viewModel.manualSupplierName.value) }
    var paymentTypeLocal by remember { mutableStateOf("نقدي") }
    var checkNumberLocal by remember { mutableStateOf("") }
    var checkBankLocal by remember { mutableStateOf("") }
    var checkDateLocal by remember { mutableStateOf(System.currentTimeMillis()) }
    var notesLocal by remember { mutableStateOf("") }
    var showSupPicker by remember { mutableStateOf(false) }
    var supSearchQuery by remember { mutableStateOf("") }
    var showStatementForSup by remember { mutableStateOf<Supplier?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var customDateTime by remember { mutableStateOf(viewModel.setupCustomDate.value ?: System.currentTimeMillis()) }
    val timeFormatter = remember { java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US) }
    val dateFormatter = remember { java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.US) }
    val timeStr = remember(customDateTime) { timeFormatter.format(java.util.Date(customDateTime)) }
    val dateStr = remember(customDateTime) { dateFormatter.format(java.util.Date(customDateTime)) }

    val purchaseDatePickerDialog = remember(customDateTime) {
      val cal = java.util.Calendar.getInstance().apply { timeInMillis = customDateTime }
      android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
          val c = java.util.Calendar.getInstance().apply { timeInMillis = customDateTime }
          c.set(java.util.Calendar.YEAR, year)
          c.set(java.util.Calendar.MONTH, month)
          c.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
          customDateTime = c.timeInMillis
        },
        cal.get(java.util.Calendar.YEAR),
        cal.get(java.util.Calendar.MONTH),
        cal.get(java.util.Calendar.DAY_OF_MONTH)
      )
    }

    val purchaseTimePickerDialog = remember(customDateTime) {
      val cal = java.util.Calendar.getInstance().apply { timeInMillis = customDateTime }
      android.app.TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
          val c = java.util.Calendar.getInstance().apply { timeInMillis = customDateTime }
          c.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
          c.set(java.util.Calendar.MINUTE, minute)
          c.set(java.util.Calendar.SECOND, 0)
          customDateTime = c.timeInMillis
        },
        cal.get(java.util.Calendar.HOUR_OF_DAY),
        cal.get(java.util.Calendar.MINUTE),
        true
      )
    }

    val allInvoicesState by viewModel.allInvoices.collectAsState(emptyList())
    val nextInvoiceNumber = remember(allInvoicesState) {
      ((allInvoicesState.maxOfOrNull { it.id } ?: 0) + 1).toString()
    }

    androidx.compose.ui.window.Dialog(
      onDismissRequest = { showPurchaseDialog = false },
      properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
      Surface(
        modifier = Modifier
          .fillMaxWidth(0.95f)
          .wrapContentHeight()
          .padding(vertical = 12.dp, horizontal = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 680.dp)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(onClick = { showPurchaseDialog = false }) {
              Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
              text = if (isPurchaseReturn) "إعداد فاتورة مرتجع مشتريات " else "إعداد الفاتورة",
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Top Blue Bar
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.CenterVertically
            ) {
              // التوقيت
              Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text("التوقيت ", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                  shape = RoundedCornerShape(20.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.clickable { purchaseTimePickerDialog.show() }
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Text(
                      text = timeStr,
                      color = MaterialTheme.colorScheme.onSurface,
                      fontWeight = FontWeight.Bold,
                      fontSize = 11.sp,
                      maxLines = 1
                    )
                    Icon(Icons.Default.Edit, contentDescription = "تعديل الوقت", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                  }
                }
              }

              // التاريخ
              Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text("التاريخ ", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                  shape = RoundedCornerShape(20.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.clickable { purchaseDatePickerDialog.show() }
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Text(
                      text = dateStr,
                      color = MaterialTheme.colorScheme.onSurface,
                      fontWeight = FontWeight.Bold,
                      fontSize = 11.sp,
                      maxLines = 1
                    )
                    Icon(Icons.Default.Edit, contentDescription = "تعديل التاريخ", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                  }
                }
              }

              // رقم الفاتورة
              Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text("رقم الفاتورة", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                  shape = RoundedCornerShape(20.dp),
                  color = MaterialTheme.colorScheme.surface
                ) {
                  Text(
                    text = nextInvoiceNumber,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // اختيار المورد
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
              .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(16.dp))
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = "اختيار المورد:",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurface
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // Selected Supplier Display Box
              Surface(
                onClick = { showSupPicker = true },
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                color = MaterialTheme.colorScheme.surface
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = selectedSupLocal?.name ?: if (manualNameLocal.isNotBlank()) manualNameLocal else "مورد عام (غير مسجل)",
                    fontWeight = if (selectedSupLocal != null || manualNameLocal.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                    color = if (selectedSupLocal != null || manualNameLocal.isNotBlank()) MaterialTheme.colorScheme.onSurface else Color.Gray,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                  )

                  if (selectedSupLocal != null || manualNameLocal.isNotEmpty()) {
                    IconButton(
                      onClick = {
                        selectedSupLocal = null
                        manualNameLocal = ""
                      },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                  }
                }
              }

              // Supplier Picker Button
              Surface(
                onClick = { showSupPicker = true },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Default.ReceiptLong, contentDescription = "قائمة الموردين", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
                }
              }
            }

            if (selectedSupLocal == null) {
              OutlinedTextField(
                value = manualNameLocal,
                onValueChange = { manualNameLocal = it },
                placeholder = { Text("أو ادخل اسم المورد يدوياً...", fontSize = 13.sp, color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                  focusedBorderColor = MaterialTheme.colorScheme.primary
                )
              )
            } else {
              // Selected Supplier Balance and Statement Button
              val supTx = allSupplierTransactions.filter { it.supplierId == selectedSupLocal!!.id }
              val totalDebit = supTx.filter { it.transactionType == "شراء" }.sumOf { it.amount }
              val totalCredit = supTx.filter { it.transactionType == "دفعة" }.sumOf { it.amount }
              val supBalance = totalDebit - totalCredit

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Button(
                  onClick = { showStatementForSup = selectedSupLocal },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                  modifier = Modifier.height(36.dp)
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    Text("كشف حساب المورد", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                  }
                }

                Text(
                  text = "رصيد الحساب: ${formatMoney(supBalance)} $currencySymbol",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Payment Type - Responsive Segmented Buttons
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text("طريقة الدفع:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              listOf("نقدي", "دائن", "شيك").forEach { option ->
                val isSelected = (paymentTypeLocal == option)
                Surface(
                  onClick = { paymentTypeLocal = option },
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(10.dp),
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                  border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                ) {
                  Row(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    RadioButton(
                      selected = isSelected,
                      onClick = { paymentTypeLocal = option },
                      colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.surface,
                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                      ),
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                      text = option,
                      fontSize = 11.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                      color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      softWrap = false
                    )
                  }
                }
              }
            }
          }

          if (paymentTypeLocal == "شيك") {
            Spacer(modifier = Modifier.height(12.dp))
            Text("بيانات الشيك:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            OutlinedTextField(
              value = checkNumberLocal,
              onValueChange = { checkNumberLocal = it },
              label = { Text("رقم الشيك") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
              keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
            OutlinedTextField(
              value = checkBankLocal,
              onValueChange = { checkBankLocal = it },
              label = { Text("اسم البنك") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = checkDateLocal }
            val datePickerDialog = android.app.DatePickerDialog(
              context,
              { _, year, month, dayOfMonth ->
                val picked = java.util.Calendar.getInstance()
                picked.set(year, month, dayOfMonth)
                checkDateLocal = picked.timeInMillis
              },
              cal.get(java.util.Calendar.YEAR),
              cal.get(java.util.Calendar.MONTH),
              cal.get(java.util.Calendar.DAY_OF_MONTH)
            )
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }) {
              OutlinedTextField(
                value = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.US).format(java.util.Date(checkDateLocal)),
                onValueChange = { },
                label = { Text("تاريخ الاستلام") },
                readOnly = true,
                enabled = false,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                  disabledTextColor = MaterialTheme.colorScheme.onSurface,
                  disabledBorderColor = Color.Gray,
                  disabledLabelColor = Color.Gray
                ),
                modifier = Modifier.fillMaxWidth()
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Notes
          OutlinedTextField(
            value = notesLocal,
            onValueChange = { notesLocal = it },
            placeholder = { Text("ملاحظات", fontSize = 13.sp, color = Color.Gray) },
            modifier = Modifier
              .fillMaxWidth()
              .height(80.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
              focusedBorderColor = MaterialTheme.colorScheme.primary
            )
          )

          Spacer(modifier = Modifier.height(20.dp))

          // Start Invoice Button
          Button(
            onClick = {
              if (selectedSupLocal != null) {
                viewModel.setSelectedSupplier(selectedSupLocal!!.id)
              } else {
                viewModel.setSelectedSupplier(null)
                viewModel.setManualSupplierName(manualNameLocal)
              }
              viewModel.clearCart()
              viewModel.setupPaymentMethod.value = paymentTypeLocal
              viewModel.setupCheckNumber.value = checkNumberLocal
              viewModel.setupCheckBankName.value = checkBankLocal
              viewModel.setupCheckDate.value = checkDateLocal
              viewModel.setupCustomDate.value = customDateTime
              showPurchaseDialog = false
              activePosView = "purchase"
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
          ) {
            Text(if (isPurchaseReturn) "البدء بمرتجع المشتريات" else "البدء بالفاتورة", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimary)
          }
        }
      }
    }

    // Supplier Picker Dialog
    if (showSupPicker) {
      AlertDialog(
        onDismissRequest = { showSupPicker = false },
        title = { Text("اختر المورد ", fontWeight = FontWeight.Bold) },
        text = {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedTextField(
              value = supSearchQuery,
              onValueChange = { supSearchQuery = it },
              placeholder = { Text("بحث عن مورد...") },
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              shape = RoundedCornerShape(12.dp)
            )

            val filtered = suppliers.filter {
              it.name.contains(supSearchQuery, ignoreCase = true) ||
              it.phone.contains(supSearchQuery)
            }

            if (filtered.isEmpty()) {
              Text("لا يوجد موردين مطبقين للبحث", color = Color.Gray, modifier = Modifier.padding(12.dp))
            } else {
              LazyColumn(
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                items(filtered) { sup ->
                  Card(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clickable {
                        selectedSupLocal = sup
                        manualNameLocal = ""
                        showSupPicker = false
                      },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column {
                        Text(sup.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (sup.phone.isNotEmpty()) {
                          Text(sup.phone, fontSize = 12.sp, color = Color.Gray)
                        }
                      }
                      Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.Gray)
                    }
                  }
                }
              }
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { showSupPicker = false }) {
            Text("إلغاء")
          }
        }
      )


    }
  }

  // --- 2. Purchase Draft Dialog ---
  if (showPurchaseDraftDialog) {
    var supplierDraftName by remember { mutableStateOf("") }
    var draftAmount by remember { mutableStateOf("") }
    var draftNotes by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showPurchaseDraftDialog = false },
      title = { Text("إنشاء مسودة فاتورة شراء ", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = supplierDraftName,
            onValueChange = { supplierDraftName = it },
            label = { Text("اسم المورد / المتجر") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = draftAmount,
            onValueChange = { draftAmount = it },
            label = { Text("المبلغ التقديري للمسودة") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = draftNotes,
            onValueChange = { draftNotes = it },
            label = { Text("ملاحظات البضاعة المسودة") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showPurchaseDraftDialog = false
            viewModel.showToast("تم حفظ مسودة الشراء بنجاح ")
          }
        ) {
          Text("حفظ المسودة ")
        }
      },
      dismissButton = {
        TextButton(onClick = { showPurchaseDraftDialog = false }) {
          Text("إلغاء")
        }
      }
    )
  }

  // --- 3. Sales Draft Dialog ---
  if (showSalesDraftDialog) {
    var draftCustName by remember { mutableStateOf("") }
    var draftNoteText by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showSalesDraftDialog = false },
      title = { Text("إنشاء مسودة فاتورة مبيع ", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = draftCustName,
            onValueChange = { draftCustName = it },
            label = { Text("اسم العميل / العميل المؤقت") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = draftNoteText,
            onValueChange = { draftNoteText = it },
            label = { Text("ملاحظات الأصناف أو الطلب") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showSalesDraftDialog = false
            activePosView = "terminal"
            viewModel.showToast("تم فتح الكاشير لتجهيز مسودة المبيع ")
          }
        ) {
          Text("تجهيز بالمنتجات ")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSalesDraftDialog = false }) {
          Text("إلغاء")
        }
      }
    )
  }

  // --- 4. Returns & Damaged Invoices Dialog ---
  if (showReturnsDialog) {
    var selectedReturnType by remember { mutableStateOf<String?>(null) }
    var returnAmountStr by remember { mutableStateOf("") }
    var returnReasonStr by remember { mutableStateOf("") }

    if (selectedReturnType == null) {
      androidx.compose.ui.window.Dialog(
        onDismissRequest = { showReturnsDialog = false },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
      ) {
        Surface(
          modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(12.dp),
          shape = RoundedCornerShape(28.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 6.dp
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "فواتير المرتجع",
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp,
              color = MaterialTheme.colorScheme.primary,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.CenterVertically
            ) {
              // 1. مرتجع مبيع
              Column(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(16.dp))
                  .clickable {
                    showReturnsDialog = false
                    isSalesReturn = true
                    showSalesSetupDialog = true
                  }
                  .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Icon(
                  imageVector = Icons.Default.ShoppingCart,
                  contentDescription = "مرتجع مبيع",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(50.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = "مرتجع مبيع",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = MaterialTheme.colorScheme.onSurface,
                  textAlign = TextAlign.Center
                )
              }

              // 2. مرتجع شراء
              Column(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(16.dp))
                  .clickable {
                    showReturnsDialog = false
                    isPurchaseReturn = true
                    showPurchaseDialog = true
                  }
                  .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Icon(
                  imageVector = Icons.Default.PublishedWithChanges,
                  contentDescription = "مرتجع شراء",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(50.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = "مرتجع شراء",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = MaterialTheme.colorScheme.onSurface,
                  textAlign = TextAlign.Center
                )
              }

              // 3. فاتورة تالف
              Column(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(16.dp))
                  .clickable { selectedReturnType = "فاتورة تالف" }
                  .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteOutline,
                  contentDescription = "فاتورة تالف",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(50.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = "فاتورة تالف",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = MaterialTheme.colorScheme.onSurface,
                  textAlign = TextAlign.Center
                )
              }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.Start
            ) {
              TextButton(onClick = { showReturnsDialog = false }) {
                Text(
                  text = "إغلاق",
                  fontSize = 15.sp,
                  color = Color.Gray,
                  fontWeight = FontWeight.Medium
                )
              }
            }
          }
        }
      }
    } else {
      val currentType = selectedReturnType ?: "مرتجع مبيعات"
      AlertDialog(
        onDismissRequest = { selectedReturnType = null },
        title = {
          Text(
            text = "تسجيل $currentType ",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
        },
        text = {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = returnAmountStr,
              onValueChange = { returnAmountStr = it },
              label = { Text("قيمة المبلغ ($currencySymbol) *") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
              value = returnReasonStr,
              onValueChange = { returnReasonStr = it },
              label = {
                Text(
                  if (currentType == "فاتورة تالف") "تفاصيل الأصناف والتالف *"
                  else "سبب المرتجع / التفاصيل *"
                )
              },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              val amt = returnAmountStr.toDoubleOrNull() ?: 0.0
              if (amt <= 0.0) {
                viewModel.showToast("يرجى إدخال مبلغ صحيح أكبر من صفر")
                return@Button
              }
              val reason = returnReasonStr.trim().ifEmpty { currentType }
              val creditAcc = if (currentType == "فاتورة تالف") "توالف ومفقودات" else "الصندوق الرئيسي"

              viewModel.addExpenseInvoice(
                description = "$currentType: $reason",
                amount = amt,
                creditAccount = creditAcc
              )
              showReturnsDialog = false
              selectedReturnType = null
              viewModel.showToast("تم تسجيل $currentType بنجاح ")
            }
          ) {
            Text("حفظ وتسجيل ")
          }
        },
        dismissButton = {
          TextButton(onClick = { selectedReturnType = null }) {
            Text("رجوع")
          }
        }
      )
    }
  }

  // --- 6. Sales Invoice Setup Dialog ("إعداد الفاتورة") ---
  if (showSalesSetupDialog) {
    var selectedCustLocal by remember { mutableStateOf<Customer?>(customers.find { it.id == selectedCustomerId }) }
    var manualNameLocal by remember { mutableStateOf(viewModel.manualCustomerName.value) }
    var paymentTypeLocal by remember { mutableStateOf("نقدي") }
    var checkNumberLocal by remember { mutableStateOf("") }
    var checkBankLocal by remember { mutableStateOf("") }
    val defaultCal2 = remember { java.util.Calendar.getInstance() }
    var checkDateLocal by remember { mutableStateOf(defaultCal2.timeInMillis) }
    var notesLocal by remember { mutableStateOf("") }
    var taxEnabledLocal by remember { mutableStateOf(taxPercent > 0) }
    var showCustPicker by remember { mutableStateOf(false) }
    var showStatementForCust by remember { mutableStateOf<Customer?>(null) }
    var custSearchQuery by remember { mutableStateOf("") }
    val allInvoiceItemsState by viewModel.allInvoiceItems.collectAsState(emptyList())
    var showPrintTypeDialogForCust by remember { mutableStateOf(false) }
    var pendingPrintActionForCust by remember { mutableStateOf("") }

    val context = androidx.compose.ui.platform.LocalContext.current
    var customDateTime by remember { mutableStateOf(viewModel.setupCustomDate.value ?: System.currentTimeMillis()) }
    val timeFormatter = remember { java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US) }
    val dateFormatter = remember { java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.US) }
    val timeStr = remember(customDateTime) { timeFormatter.format(java.util.Date(customDateTime)) }
    val dateStr = remember(customDateTime) { dateFormatter.format(java.util.Date(customDateTime)) }

    val salesDatePickerDialog = remember(customDateTime) {
      val cal = java.util.Calendar.getInstance().apply { timeInMillis = customDateTime }
      android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
          val c = java.util.Calendar.getInstance().apply { timeInMillis = customDateTime }
          c.set(java.util.Calendar.YEAR, year)
          c.set(java.util.Calendar.MONTH, month)
          c.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
          customDateTime = c.timeInMillis
        },
        cal.get(java.util.Calendar.YEAR),
        cal.get(java.util.Calendar.MONTH),
        cal.get(java.util.Calendar.DAY_OF_MONTH)
      )
    }

    val salesTimePickerDialog = remember(customDateTime) {
      val cal = java.util.Calendar.getInstance().apply { timeInMillis = customDateTime }
      android.app.TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
          val c = java.util.Calendar.getInstance().apply { timeInMillis = customDateTime }
          c.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
          c.set(java.util.Calendar.MINUTE, minute)
          c.set(java.util.Calendar.SECOND, 0)
          customDateTime = c.timeInMillis
        },
        cal.get(java.util.Calendar.HOUR_OF_DAY),
        cal.get(java.util.Calendar.MINUTE),
        true
      )
    }

    val allInvoicesState by viewModel.allInvoices.collectAsState(emptyList())
    val nextInvoiceNumber = remember(allInvoicesState) {
      ((allInvoicesState.maxOfOrNull { it.id } ?: 0) + 1).toString()
    }

    androidx.compose.ui.window.Dialog(
      onDismissRequest = { showSalesSetupDialog = false },
      properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
      Surface(
        modifier = Modifier
          .fillMaxWidth(0.95f)
          .wrapContentHeight()
          .padding(vertical = 12.dp, horizontal = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 680.dp)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(onClick = { showSalesSetupDialog = false }) {
              Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
              text = if (isSalesReturn) "إعداد فاتورة مرتجع مبيعات " else "إعداد الفاتورة",
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Top Blue Bar
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.CenterVertically
            ) {
              // التوقيت
              Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text("التوقيت ", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                  shape = RoundedCornerShape(20.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.clickable { salesTimePickerDialog.show() }
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Text(
                      text = timeStr,
                      color = MaterialTheme.colorScheme.onSurface,
                      fontWeight = FontWeight.Bold,
                      fontSize = 11.sp,
                      maxLines = 1
                    )
                    Icon(Icons.Default.Edit, contentDescription = "تعديل الوقت", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                  }
                }
              }

              // التاريخ
              Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text("التاريخ ", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                  shape = RoundedCornerShape(20.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.clickable { salesDatePickerDialog.show() }
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Text(
                      text = dateStr,
                      color = MaterialTheme.colorScheme.onSurface,
                      fontWeight = FontWeight.Bold,
                      fontSize = 11.sp,
                      maxLines = 1
                    )
                    Icon(Icons.Default.Edit, contentDescription = "تعديل التاريخ", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                  }
                }
              }

              // رقم الفاتورة
              Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text("رقم الفاتورة", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                  shape = RoundedCornerShape(20.dp),
                  color = MaterialTheme.colorScheme.surface
                ) {
                  Text(
                    text = nextInvoiceNumber,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // اختيار العميل / الزبون
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
              .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(16.dp))
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = "اختيار العميل / الزبون:",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurface
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // Selected Customer Display Box
              Surface(
                onClick = { showCustPicker = true },
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                color = MaterialTheme.colorScheme.surface
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = selectedCustLocal?.name ?: if (manualNameLocal.isNotBlank()) manualNameLocal else "زبون عام (غير مسجل)",
                    fontWeight = if (selectedCustLocal != null || manualNameLocal.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                    color = if (selectedCustLocal != null || manualNameLocal.isNotBlank()) MaterialTheme.colorScheme.onSurface else Color.Gray,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                  )

                  if (selectedCustLocal != null || manualNameLocal.isNotEmpty()) {
                    IconButton(
                      onClick = {
                        selectedCustLocal = null
                        manualNameLocal = ""
                      },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                  }
                }
              }

              // Customer Picker Button
              Surface(
                onClick = { showCustPicker = true },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Default.ReceiptLong, contentDescription = "قائمة العملاء", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
                }
              }
            }

            if (selectedCustLocal == null) {
              OutlinedTextField(
                value = manualNameLocal,
                onValueChange = { manualNameLocal = it },
                placeholder = { Text("أو ادخل اسم العميل يدوياً...", fontSize = 13.sp, color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                  focusedBorderColor = MaterialTheme.colorScheme.primary
                )
              )
            } else {
              // Selected Customer Balance and Statement Button
              val custInvoices = allInvoicesState.filter { it.customerId == selectedCustLocal!!.id && !it.isPurchase && !it.isDraft }
              val custTx = allCustomerTransactions.filter { it.customerId == selectedCustLocal!!.id }
              val totalDebit = custInvoices.sumOf { it.total } + custTx.filter { it.transactionType == "بيع" }.sumOf { it.amount }
              val totalCredit = custInvoices.sumOf { it.paidAmount } + custTx.filter { it.transactionType == "دفعة" }.sumOf { it.amount }
              val custBalance = totalDebit - totalCredit

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Button(
                  onClick = { showStatementForCust = selectedCustLocal },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                  modifier = Modifier.height(36.dp)
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    Text("كشف حساب العميل", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                  }
                }

                Text(
                  text = "رصيد الحساب: ${formatMoney(custBalance)} $currencySymbol",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Payment Type - Responsive Segmented Buttons
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text("طريقة الدفع:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              listOf("نقدي", "مدين", "شيك").forEach { option ->
                val isSelected = (paymentTypeLocal == option)
                Surface(
                  onClick = { paymentTypeLocal = option },
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(10.dp),
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                  border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                ) {
                  Row(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    RadioButton(
                      selected = isSelected,
                      onClick = { paymentTypeLocal = option },
                      colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.surface,
                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                      ),
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                      text = option,
                      fontSize = 11.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                      color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      softWrap = false
                    )
                  }
                }
              }
            }
          }

          if (paymentTypeLocal == "شيك") {
            Spacer(modifier = Modifier.height(12.dp))
            Text("بيانات الشيك:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            OutlinedTextField(
              value = checkNumberLocal,
              onValueChange = { checkNumberLocal = it },
              label = { Text("رقم الشيك") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
              keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
            OutlinedTextField(
              value = checkBankLocal,
              onValueChange = { checkBankLocal = it },
              label = { Text("اسم البنك") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = checkDateLocal }
            val datePickerDialog = android.app.DatePickerDialog(
              context,
              { _, year, month, dayOfMonth ->
                val picked = java.util.Calendar.getInstance()
                picked.set(year, month, dayOfMonth)
                checkDateLocal = picked.timeInMillis
              },
              cal.get(java.util.Calendar.YEAR),
              cal.get(java.util.Calendar.MONTH),
              cal.get(java.util.Calendar.DAY_OF_MONTH)
            )
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }) {
              OutlinedTextField(
                value = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.US).format(java.util.Date(checkDateLocal)),
                onValueChange = { },
                label = { Text("تاريخ الاستلام") },
                readOnly = true,
                enabled = false,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                  disabledTextColor = MaterialTheme.colorScheme.onSurface,
                  disabledBorderColor = Color.Gray,
                  disabledLabelColor = Color.Gray
                ),
                modifier = Modifier.fillMaxWidth()
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Notes
          OutlinedTextField(
            value = notesLocal,
            onValueChange = { notesLocal = it },
            placeholder = { Text("ملاحظات", fontSize = 13.sp, color = Color.Gray) },
            modifier = Modifier
              .fillMaxWidth()
              .height(80.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
              focusedBorderColor = MaterialTheme.colorScheme.primary
            )
          )

          Spacer(modifier = Modifier.height(20.dp))

          // Start Invoice Button
          Button(
            onClick = {
              if (selectedCustLocal != null) {
                viewModel.selectCustomer(selectedCustLocal!!.id)
              } else {
                viewModel.selectCustomer(null)
                viewModel.setManualCustomerName(manualNameLocal)
              }
              viewModel.taxPercent.value = 0.0
              viewModel.setupPaymentMethod.value = paymentTypeLocal
              viewModel.setupCheckNumber.value = checkNumberLocal
              viewModel.setupCheckBankName.value = checkBankLocal
              viewModel.setupCheckDate.value = checkDateLocal
              viewModel.setupCustomDate.value = customDateTime
              showSalesSetupDialog = false
              activePosView = "terminal"
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
          ) {
            Text(if (isSalesReturn) "البدء بمرتجع المبيعات" else "البدء بالفاتورة", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimary)
          }
        }
      }
    }

    // Customer Picker Dialog
    if (showCustPicker) {
      AlertDialog(
        onDismissRequest = { showCustPicker = false },
        title = { Text("اختيار العميل", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 350.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedTextField(
              value = custSearchQuery,
              onValueChange = { custSearchQuery = it },
              placeholder = { Text("بحث عن عميل...", fontSize = 12.sp) },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              shape = RoundedCornerShape(10.dp)
            )

            val filteredList = customers.filter {
              it.name.contains(custSearchQuery, ignoreCase = true) || it.phone.contains(custSearchQuery)
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              item {
                Surface(
                  onClick = {
                    selectedCustLocal = null
                    manualNameLocal = ""
                    showCustPicker = false
                  },
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                  Text("زبون عام (غير مسجل)", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
              }
              items(filteredList) { cust ->
                Surface(
                  onClick = {
                    selectedCustLocal = cust
                    manualNameLocal = ""
                    showCustPicker = false
                  },
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(8.dp),
                  color = if (selectedCustLocal?.id == cust.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(cust.phone, fontSize = 12.sp, color = Color.Gray)
                  }
                }
              }
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { showCustPicker = false }) { Text("إغلاق") }
        }
      )
    }

    // Customer Account Statement Dialog
    showStatementForCust?.let { cust ->
      val custInvoices = allInvoicesState.filter { it.customerId == cust.id && !it.isPurchase && !it.isDraft }
      val relatedCustTx = allCustomerTransactions.filter { it.customerId == cust.id }

      val combinedTxList = remember(custInvoices, relatedCustTx) {
        val list = mutableListOf<CombinedCustomerTx>()
        custInvoices.forEach { inv ->
          // Debit for the invoice total
          list.add(
            CombinedCustomerTx(
              date = inv.invoiceDate,
              description = "فاتورة مبيعات رقم #${inv.id}",
              debit = inv.total,
              credit = 0.0,
              refId = inv.id
            )
          )
          // Credit for any amount paid at the time of invoice
          if (inv.paidAmount > 0) {
            list.add(
              CombinedCustomerTx(
                date = inv.invoiceDate + 1000,
                description = "دفعة مسددة على فاتورة #${inv.id}",
                debit = 0.0,
                credit = inv.paidAmount,
                refId = inv.id
              )
            )
          }
        }
        relatedCustTx.forEach { tx ->
          list.add(
            CombinedCustomerTx(
              date = tx.date,
              description = tx.note.ifEmpty { if (tx.transactionType == "بيع") "فاتورة آجلة" else "سند قبض مسدد" },
              debit = if (tx.transactionType == "بيع") tx.amount else 0.0,
              credit = if (tx.transactionType == "دفعة") tx.amount else 0.0,
              refId = tx.id
            )
          )
        }
        list.sortBy { it.date }
        list
      }

      val totalDebit = combinedTxList.sumOf { it.debit }
      val totalCredit = combinedTxList.sumOf { it.credit }
      val outstandingBalance = totalDebit - totalCredit

      var showAddPaymentFields by remember { mutableStateOf(false) }
      var paymentAmountStr by remember { mutableStateOf("") }
      var paymentNote by remember { mutableStateOf("سند قبض نقدي سداد رصيد") }

      AlertDialog(
        onDismissRequest = { showStatementForCust = null },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
          .fillMaxWidth(0.92f)
          .fillMaxHeight(0.85f),
        title = {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Text("كشف حساب العميل: ${cust.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            IconButton(onClick = { showStatementForCust = null }) {
              Icon(Icons.Default.Close, contentDescription = "إغلاق")
            }
          }
        },
        text = {
          Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column {
                  Text("رقم الجوال: ${cust.phone}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  Text("البريد: ${cust.email.ifEmpty { "لا يوجد" }}", fontSize = 11.sp, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text("نقاط الولاء: ${cust.points} نقطة", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                  Text("تصنيف العميل: ${cust.type}", fontSize = 11.sp, color = Color.Gray)
                }
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
              ) {
                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("إجمالي المبيعات", fontSize = 10.sp, color = Color.Gray)
                  Text("${formatMoney(totalDebit)} $currencySymbol", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
              }
              Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
              ) {
                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("إجمالي المدفوعات", fontSize = 10.sp, color = Color.Gray)
                  Text("${formatMoney(totalCredit)} $currencySymbol", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                }
              }
              Card(
                modifier = Modifier.weight(1.2f),
                colors = CardDefaults.cardColors(
                  containerColor = if (outstandingBalance > 0) CoralError.copy(alpha = 0.1f) else EmeraldGreen.copy(alpha = 0.1f)
                )
              ) {
                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("الرصيد المستحق", fontSize = 10.sp, color = Color.Gray)
                  Text(
                    "${formatMoney(outstandingBalance)} $currencySymbol",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (outstandingBalance > 0) CoralError else EmeraldGreen
                  )
                }
              }
            }

            LazyColumn(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              item {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(6.dp))
                    .padding(8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text("التفاصيل", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2f))
                  Text("مدين", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                  Text("دائن", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
              }
              items(combinedTxList) { item ->
                Surface(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                  Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(2f)) {
                      Text(item.description, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                      val dFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
                      Text(dFormat.format(java.util.Date(item.date)), fontSize = 10.sp, color = Color.Gray)
                    }
                    Text(if (item.debit > 0) "${formatMoney(item.debit)}" else "-", fontSize = 12.sp, color = CoralError, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text(if (item.credit > 0) "${formatMoney(item.credit)}" else "-", fontSize = 12.sp, color = EmeraldGreen, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                  }
                }
              }
            }

            // Add Payment Form / Button
            if (showAddPaymentFields) {
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Text("تسجيل سند قبض جديد (سداد مالي) ", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    OutlinedTextField(
                      value = paymentAmountStr,
                      onValueChange = { paymentAmountStr = it },
                      label = { Text("المبلغ ($currencySymbol) *", fontSize = 12.sp) },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      singleLine = true,
                      modifier = Modifier.weight(1.3f),
                      shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                      value = paymentNote,
                      onValueChange = { paymentNote = it },
                      label = { Text("البيان / الملاحظة", fontSize = 12.sp) },
                      singleLine = true,
                      modifier = Modifier.weight(1.7f),
                      shape = RoundedCornerShape(8.dp)
                    )
                  }
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    TextButton(onClick = { showAddPaymentFields = false }) {
                      Text("إلغاء", color = CoralError)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                      onClick = {
                        val amt = paymentAmountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                          viewModel.addCustomerTransaction(
                            CustomerTransaction(
                              customerId = cust.id,
                              transactionType = "دفعة",
                              amount = amt,
                              note = paymentNote.trim().ifEmpty { "سند قبض نقدي" }
                            )
                          )
                          paymentAmountStr = ""
                          showAddPaymentFields = false
                        } else {
                          viewModel.showToast("يرجى كتابة مبلغ سداد صحيح!")
                        }
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                      shape = RoundedCornerShape(8.dp)
                    ) {
                      Text("تأكيد وحفظ الدفعة ", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            } else {
              Button(
                onClick = {
                  showAddPaymentFields = true
                  paymentAmountStr = if (outstandingBalance > 0) outstandingBalance.toString() else ""
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(12.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                  Text("تسجيل دفعة نقدية جديدة (سند قبض) ", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimary)
                }
              }
            }
          }
        },
        confirmButton = {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            TextButton(onClick = { showStatementForCust = null }) { Text("إغلاق") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = {
                    pendingPrintActionForCust = "whatsapp"
                    showPrintTypeDialogForCust = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
              ) {
                Text("واتساب", color = MaterialTheme.colorScheme.onPrimary)
              }
              Button(
                onClick = {
                    pendingPrintActionForCust = "pdf"
                    showPrintTypeDialogForCust = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
              ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("PDF", fontSize = 12.sp)
              }
            }
          }
        }
      )


      if (showPrintTypeDialogForCust) {
        val executePrintAction = { isDetailed: Boolean ->
            val mappedRows = combinedTxList.map { tx ->
                var desc = tx.description
                if (isDetailed && tx.refId > 0 && desc.startsWith("فاتورة")) {
                    val items = allInvoiceItemsState.filter { it.invoiceId == tx.refId }
                    if (items.isNotEmpty()) {
                        desc += "\n" + items.joinToString("\n") { "- ${it.productNameAr} (${it.quantity} x ${formatMoney(it.price)})" }
                    }
                }
                com.example.ui.StatementRow(tx.date, desc, tx.debit, tx.credit)
            }
            val summaryText = "إجمالي المبيعات: ${formatMoney(totalDebit)} | إجمالي السداد: ${formatMoney(totalCredit)} | المتبقي المستحق: ${formatMoney(outstandingBalance)}"
            val entityDetails = "رقم الجوال: ${cust.phone} | التصنيف: ${cust.type}"
            val finalTitle = "كشف حساب عميل: ${cust.name}" + if (isDetailed) " (تفصيلي)" else " (مجمل)"
            
            if (pendingPrintActionForCust == "pdf") {
                viewModel.exportStatementToPdf(context, finalTitle, mappedRows, summaryText, entityDetails)
            } else if (pendingPrintActionForCust == "whatsapp") {
                val cleanPhone = cust.phone.replace("+", "").replace(" ", "").filter { it.isDigit() }
                val pdfFile = com.example.ui.PdfGenerator.generateStatementPdfFile(
                    context = context,
                    storeName = viewModel.storeName.value,
                    storePhone = viewModel.storePhone.value,
                    title = finalTitle,
                    bodyRows = mappedRows,
                    summaryText = summaryText,
                    entityDetails = entityDetails
                )
                if (pdfFile != null) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        putExtra(android.content.Intent.EXTRA_TEXT, "مرفق كشف الحساب من ${viewModel.storeName.value}")
                        if (cleanPhone.isNotEmpty()) {
                            putExtra("jid", "$cleanPhone@s.whatsapp.net")
                        }
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        shareIntent.setPackage("com.whatsapp")
                        context.startActivity(shareIntent)
                    } catch (e: Exception) {
                        try {
                            shareIntent.setPackage("com.whatsapp.w4b")
                            context.startActivity(shareIntent)
                        } catch (e2: Exception) {
                            shareIntent.setPackage(null)
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "مشاركة كشف الحساب"))
                        }
                    }
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showPrintTypeDialogForCust = false },
            title = { Text("نوع كشف الحساب", fontWeight = FontWeight.Bold) },
            text = { Text("هل تريد إنشاء كشف حساب مجمل أم تفصيلي؟") },
            confirmButton = {
                Button(
                    onClick = {
                        showPrintTypeDialogForCust = false
                        executePrintAction(true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) { Text("تفصيلي") }
            },
            dismissButton = {
                Button(onClick = {
                    showPrintTypeDialogForCust = false
                    executePrintAction(false)
                }) { Text("مجمل") }
            }
        )
      }
    } // End of showStatementForCust Let

  }

  // --- Bluetooth Thermal Printer Dialog ---
  if (showPrinterDialog) {
    BluetoothPrinterSettingsDialog(
      viewModel = viewModel,
      onDismissRequest = { showPrinterDialog = false }
    )
  }

  // --- 5. Previous Invoices History Dialog ---
  if (showInvoiceHistoryDialog) {
    val allInvoices by viewModel.allInvoices.collectAsState(emptyList())
    val suppliers by viewModel.allSuppliers.collectAsState(emptyList())
    val supplierTxList by viewModel.allSupplierTransactions.collectAsState(emptyList())
    var searchQueryHistory by remember { mutableStateOf("") }
    var selectedFilterType by remember { mutableStateOf("الكل") }
    var selectedInvoiceForDetails by remember { mutableStateOf<com.example.data.Invoice?>(null) }
    var invoiceToDelete by remember { mutableStateOf<com.example.data.Invoice?>(null) }
    val dateFormat = remember { java.text.SimpleDateFormat("HH:mm yyyy-MM-dd", java.util.Locale.US) }

    val filteredInvoices = remember(allInvoices, searchQueryHistory, selectedFilterType, customers, suppliers, supplierTxList) {
      allInvoices.filter { inv ->
        val custName = customers.find { it.id == inv.customerId }?.name ?: "زبون عام"
        val suppName = suppliers.find { it.id == inv.customerId }?.name ?: "مورد عام"
        
        val matchesQuery = searchQueryHistory.isBlank() ||
            inv.id.toString().contains(searchQueryHistory) ||
            custName.contains(searchQueryHistory, ignoreCase = true) ||
            suppName.contains(searchQueryHistory, ignoreCase = true) ||
            inv.paymentMethod.contains(searchQueryHistory, ignoreCase = true)

        val isPurchase = isPurchaseInvoice(inv, supplierTxList, emptyList(), emptyList(), customers, suppliers)
        val matchesType = when (selectedFilterType) {
          "المبيعات" -> !isPurchase && inv.total >= 0 && !inv.isDraft
          "المشتريات" -> isPurchase && inv.total >= 0 && !inv.isDraft
          "مرتجعات" -> inv.total < 0 && !inv.isDraft
          "المسودات" -> inv.isDraft
          else -> !inv.isDraft // "الكل" shows all non-drafts
        }

        matchesQuery && matchesType
      }
    }

    androidx.compose.ui.window.Dialog(
      onDismissRequest = { showInvoiceHistoryDialog = false },
      properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
      Surface(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background),
        color = MaterialTheme.colorScheme.background
      ) {
        Column(modifier = Modifier.fillMaxSize()) {
          // Top Blue Header Bar
          Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                IconButton(onClick = { showInvoiceHistoryDialog = false }) {
                  Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "رجوع",
                    tint = MaterialTheme.colorScheme.onPrimary
                  )
                }
                Text(
                  text = "سجل الفواتير السابقة",
                  fontWeight = FontWeight.Bold,
                  fontSize = 18.sp,
                  color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(48.dp))
              }
            }
          }

          // Content Container
          Column(
            modifier = Modifier
              .fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)
          ) {
            // Search Input Field
            OutlinedTextField(
              value = searchQueryHistory,
              onValueChange = { searchQueryHistory = it },
              placeholder = {
                Text(
                  "بحث برقم الفاتورة، العميل، المورد، أو طريقة الدفع...",
                  fontSize = 12.sp,
                  color = Color.Gray
                )
              },
              trailingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
              shape = RoundedCornerShape(16.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
              ),
              singleLine = true
            )

            // Filter Chips Row
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
            ) {
              val filterOptions = listOf(
                "الكل (${allInvoices.count { !it.isDraft }})" to "الكل",
                "المبيعات " to "المبيعات",
                "المشتريات " to "المشتريات",
                "مرتجعات " to "مرتجعات",
                "مسودات (${allInvoices.count { it.isDraft }})" to "المسودات"
              )
              items(filterOptions) { (label, typeKey) ->
                val isSelected = selectedFilterType == typeKey
                Surface(
                  shape = RoundedCornerShape(20.dp),
                  color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                  border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
                  ),
                  modifier = Modifier.clickable { selectedFilterType = typeKey }
                ) {
                  Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                  )
                }
              }
            }

            // Invoices Cards List
            if (filteredInvoices.isEmpty()) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(1f),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(
                    Icons.Default.ReceiptLong,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = Color.LightGray
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    "لا توجد فواتير مسجلة مطابقة للبحث",
                    color = Color.Gray,
                    fontSize = 14.sp
                  )
                }
              }
            } else {
              LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(1f)
              ) {
                items(filteredInvoices) { inv ->
                  val custName = customers.find { it.id == inv.customerId }?.name ?: "زبون عام"
                  val suppName = suppliers.find { it.id == inv.customerId }?.name ?: "مورد عام"
                  val formattedDate = dateFormat.format(java.util.Date(inv.invoiceDate))

                  val isReturn = inv.total < 0
                  val isPurchase = isPurchaseInvoice(inv, supplierTxList, emptyList(), emptyList(), customers, suppliers)
                  
                  val typeLabel = when {
                    isReturn && isPurchase -> "مرتجع مشتريات ↪"
                    isReturn -> "مرتجع مبيعات "
                    isPurchase -> "مشتريات "
                    else -> "مبيعات "
                  }
                  val badgeBg = if (isReturn) MaterialTheme.colorScheme.tertiaryContainer else if (isPurchase) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                  val badgeTextColor = if (isReturn) MaterialTheme.colorScheme.onTertiaryContainer else if (isPurchase) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                  val iconBg = if (isReturn) MaterialTheme.colorScheme.tertiary else if (isPurchase) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

                  Card(
                    onClick = { selectedInvoiceForDetails = inv },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                  ) {
                    Column(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                    ) {
                      // Top Row: Receipt Icon + Invoice ID + Type Badge | Amount
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          // Square Icon Box
                          Box(
                            modifier = Modifier
                              .size(36.dp)
                              .clip(RoundedCornerShape(8.dp))
                              .background(iconBg),
                            contentAlignment = Alignment.Center
                          ) {
                            Icon(
                              Icons.Default.ReceiptLong,
                              contentDescription = null,
                              tint = MaterialTheme.colorScheme.onPrimary,
                              modifier = Modifier.size(20.dp)
                            )
                          }

                          Spacer(modifier = Modifier.width(10.dp))

                          Text(
                            text = if (inv.isDraft) "مسودة #${inv.id} " else "فاتورة #${inv.id}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                          )

                          Spacer(modifier = Modifier.width(8.dp))

                          // Type Badge Pill
                          Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = badgeBg
                          ) {
                            Text(
                              text = typeLabel,
                              fontSize = 11.sp,
                              fontWeight = FontWeight.Bold,
                              color = badgeTextColor,
                              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                          }
                        }

                        // Total Amount
                        Text(
                          text = "${formatMoney(inv.total)} $currencySymbol",
                          fontWeight = FontWeight.ExtraBold,
                          fontSize = 16.sp,
                          color = if (inv.total < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                        )
                      }

                      Spacer(modifier = Modifier.height(8.dp))

                      // Middle Row: Supplier / Customer Name | Payment Method
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = if (isPurchase) "المورد: $suppName" else "العميل: $custName",
                          fontSize = 12.sp,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                          text = inv.paymentMethod,
                          fontSize = 12.sp,
                          fontWeight = FontWeight.Medium,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }

                      Spacer(modifier = Modifier.height(10.dp))

                      // Bottom Row: Timestamp | Click for Details Link
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = formattedDate,
                          fontSize = 11.sp,
                          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.6f)
                        )

                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          modifier = Modifier.clickable { selectedInvoiceForDetails = inv }
                        ) {
                          Text(
                            text = "تفاصيل ",
                          )
                        }

                        Surface(
                          shape = RoundedCornerShape(8.dp),
                          color = MaterialTheme.colorScheme.errorContainer,
                          modifier = Modifier.clickable { invoiceToDelete = inv }
                        ) {
                          Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                          ) {
                            Icon(
                              Icons.Default.Delete,
                              contentDescription = "حذف الفاتورة",
                              tint = MaterialTheme.colorScheme.error,
                              modifier = Modifier.size(14.dp)
                            )
                            Text(
                              text = "حذف",
                              fontSize = 11.sp,
                              fontWeight = FontWeight.Bold,
                              color = MaterialTheme.colorScheme.error
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
      }
    }

    // --- Delete Invoice Confirmation Dialog ---
    invoiceToDelete?.let { inv ->
      AlertDialog(
        onDismissRequest = { invoiceToDelete = null },
        title = {
          Text(
            text = "حذف الفاتورة رقم #${inv.id} ",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            fontSize = 16.sp
          )
        },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = "هل أنت تأكد من رغبتك في حذف هذه الفاتورة نهائياً من سجل الفواتير؟",
              fontSize = 14.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "• سيتم إزالة الفاتورة نهائياً من قاعدة البيانات.\n• سيتم تعديل كميات المنتجات في المخزن واسترجاعها تلقائياً.",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              viewModel.deleteInvoice(inv) {
                invoiceToDelete = null
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("تأكيد الحذف ", fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          OutlinedButton(
            onClick = { invoiceToDelete = null },
            shape = RoundedCornerShape(8.dp)
          ) {
            Text("إلغاء")
          }
        }
      )
    }

    // --- Invoice Full Details Dialog ---
    selectedInvoiceForDetails?.let { inv ->
      InvoiceDetailsDialog(
        inv = inv,
        customers = customers,
        viewModel = viewModel,
        onDismiss = { selectedInvoiceForDetails = null }
      )
    }
  }
}

// ==========================================
// 1.1 RESPONSIVE POS SUB-COMPONENTS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosProductsSection(
modifier: Modifier = Modifier,
viewModel: AppViewModel,
products: List<Product>,
categories: List<Category>,
searchQuery: String,
selectedCategory: String?
  ) {
    var selectedProductForDetail by remember { mutableStateOf<Product?>(null) }
    var showCameraScanner by remember { mutableStateOf(false) }
    var cameraFeedbackMsg by remember { mutableStateOf<String?>(null) }
    var enlargedImageUrl by remember { mutableStateOf<String?>(null) }
    
    if (enlargedImageUrl != null) {
      androidx.compose.ui.window.Dialog(
        onDismissRequest = { enlargedImageUrl = null },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.onSurface)
        ) {
          coil.compose.AsyncImage(
            model = enlargedImageUrl,
            contentDescription = "صورة مكبرة",
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
          )
          IconButton(
            onClick = { enlargedImageUrl = null },
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(12.dp)
              .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), CircleShape)
              .size(40.dp)
          ) {
            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
          }
        }
      }
    }

    if (showCameraScanner) {
      BarcodeScannerDialog(
        onDismissRequest = {
          showCameraScanner = false
          cameraFeedbackMsg = null
        },
        onBarcodeScanned = { scannedCode ->
          viewModel.setSearchQuery(scannedCode)
          val matchedProduct = products.find { it.barcode.trim().equals(scannedCode.trim(), ignoreCase = true) }
          if (matchedProduct != null) {
            viewModel.addToCart(matchedProduct, 1)
            cameraFeedbackMsg = "تمت إضافة: ${matchedProduct.nameAr} (+1) للسلة "
            viewModel.showToast("تمت إضافة ${matchedProduct.nameAr} للفاتورة ")
          } else {
            cameraFeedbackMsg = "رمز الباركود: $scannedCode (غير مسجل بالمنتجات)"
            viewModel.showToast("رمز الباركود: $scannedCode (لم يتم العثور على منتج بهذا الباركود)")
          }
        },
        titleText = "مسح باركود المنتجات للفاتورة ",
        keepOpenOnScan = true,
        lastScannedMessage = cameraFeedbackMsg
      )
    }

    Column(modifier = modifier) {
    // Search & Barcode Scan field
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { viewModel.setSearchQuery(it) },
        placeholder = { Text("ابحث بالاسم أو الباركود...", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { viewModel.setSearchQuery("") }) {
              Icon(Icons.Default.Clear, contentDescription = "مسح")
            }
          } else {
            IconButton(onClick = { showCameraScanner = true }) {
              Icon(Icons.Default.QrCodeScanner, contentDescription = "باركود", tint = MaterialTheme.colorScheme.primary)
            }
          }
        },
        singleLine = true,
        modifier = Modifier
          .weight(1f)
          .testTag("search_input"),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
      )

      Button(
        onClick = { showCameraScanner = true },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        modifier = Modifier
          .height(56.dp)
          .testTag("camera_barcode_scan_btn")
      ) {
        Icon(Icons.Default.PhotoCamera, contentDescription = "مسح بالكاميرا", modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("الكاميرا ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }
    
    Spacer(modifier = Modifier.height(10.dp))
    
    // Categories Filter Bar
    LazyRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.fillMaxWidth()
      ) {
        item {
        FilterChip(
        selected = selectedCategory == null,
        onClick = { viewModel.setSelectedCategory(null) },
        label = { Text("الكل") },
        shape = RoundedCornerShape(20.dp)
        )
      }
        items(categories) { cat ->
        FilterChip(
        selected = selectedCategory == cat.nameAr,
        onClick = { viewModel.setSelectedCategory(cat.nameAr) },
        label = { Text(cat.nameAr) },
        shape = RoundedCornerShape(20.dp)
        )
      }
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    
    // Grid of Products
      if (products.isEmpty()) {
      Box(
      modifier = Modifier
      .fillMaxWidth()
      .weight(1f),
      contentAlignment = Alignment.Center
        ) {
        
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
          imageVector = Icons.Default.Inbox,
          contentDescription = "لا يوجد منتجات",
          tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
          modifier = Modifier.size(64.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
          "لا توجد منتجات مطابقة للبحث",
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
          fontSize = 15.sp
          )
        }
      }
      } else {
      LazyVerticalGrid(
      columns = GridCells.Adaptive(140.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.weight(1f)
        ) {
          items(products) { product ->
          val isLowStock = product.quantity <= product.minQuantityAlert
          val isOut = product.quantity <= 0
          
          Card(
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(
          width = 1.dp,
            color = when {
            isOut -> CoralError.copy(alpha = 0.5f)
            isLowStock -> AmberAlert
            else -> MaterialTheme.colorScheme.outline
          }
          ),
          colors = CardDefaults.cardColors(
            containerColor = if (isOut) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            } else {
            MaterialTheme.colorScheme.surface
          }
          ),
          elevation = CardDefaults.cardElevation(1.dp),
          modifier = Modifier.testTag("product_card_${product.id}").clickable {
            if (!isOut) {
              viewModel.addToCart(product, 1)
              viewModel.showToast("تمت إضافة ${product.nameAr} للفاتورة ")
            } else {
              viewModel.showToast("عذراً، المنتج منتهي بالكامل من المخزون!")
            }
          }
            ) {
              Box(modifier = Modifier.fillMaxSize()) {
              
              Column(
              modifier = Modifier
              .fillMaxWidth()
              .padding(10.dp)
                ) {
                Box(
                modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f))
                .clickable {
                  if (product.imageResName.isNotEmpty()) {
                    enlargedImageUrl = product.imageResName
                  }
                },
                contentAlignment = Alignment.Center
                  ) {
                  if (product.imageResName.isNotEmpty()) {
                    coil.compose.AsyncImage(
                      model = product.imageResName,
                      contentDescription = product.nameAr,
                      modifier = Modifier.fillMaxSize(),
                      contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                  } else {
                    Icon(
                      imageVector = when (product.category) {
                        "مشروبات" -> Icons.Default.LocalCafe
                        "مأكولات" -> Icons.Default.Restaurant
                        "حلويات" -> Icons.Default.Cake
                        else -> Icons.Default.Category
                      },
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                      modifier = Modifier.size(28.dp)
                    )
                  }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                text = product.nameAr,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                text = product.category,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column {
                    Text(
                    text = "${formatMoney(product.priceSale)} ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                    text = "متوفر: ${product.quantity}",
                    fontSize = 9.sp,
                    color = if (isLowStock) CoralError else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                  }
                  Box(
                  modifier = Modifier
                  .size(28.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(MaterialTheme.colorScheme.primaryContainer)
                  .clickable {
                    if (!isOut) {
                      viewModel.addToCart(product)
                    } else {
                      viewModel.showToast("عذراً، المنتج منتهي بالكامل من المخزون!")
                    }
                  },
                  contentAlignment = Alignment.Center
                    ) {
                    Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "إضافة",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                    )
                  }
                }
              }
              
                if (isOut) {
                Box(
                modifier = Modifier
                .clip(RoundedCornerShape(bottomStart = 8.dp))
                .background(CoralError)
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .align(Alignment.TopEnd)
                  ) {
                  Text("منتهي", color = MaterialTheme.colorScheme.onPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                } else if (isLowStock) {
                Box(
                modifier = Modifier
                .clip(RoundedCornerShape(bottomStart = 8.dp))
                .background(AmberAlert)
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .align(Alignment.TopEnd)
                  ) {
                  Text("منخفض", color = MaterialTheme.colorScheme.onPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }

      selectedProductForDetail?.let { product ->
        val salesHistory by viewModel.getProductSalesHistory(product.id).collectAsState(initial = emptyList())
        val auditHistory by viewModel.getAuditLogsForProduct(product.nameAr).collectAsState(initial = emptyList())

        androidx.compose.ui.window.Dialog(
          onDismissRequest = { selectedProductForDetail = null }
        ) {
          var animateIn by remember { mutableStateOf(false) }
          LaunchedEffect(Unit) {
            animateIn = true
          }

          androidx.compose.animation.AnimatedVisibility(
            visible = animateIn,
            enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) + 
                androidx.compose.animation.scaleIn(initialScale = 0.85f, animationSpec = androidx.compose.animation.core.tween(300)),
            exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(250)) + 
                androidx.compose.animation.scaleOut(targetScale = 0.85f, animationSpec = androidx.compose.animation.core.tween(250))
          ) {
            Card(
              shape = RoundedCornerShape(24.dp),
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(12.dp)
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .verticalScroll(rememberScrollState())
              ) {
                // Header Image with Close Button Overlay
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                      brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                          MaterialTheme.colorScheme.primary,
                          MaterialTheme.colorScheme.tertiary
                        )
                      )
                    )
                ) {
                  if (product.imageResName.isNotEmpty()) {
                    coil.compose.AsyncImage(
                      model = product.imageResName,
                      contentDescription = product.nameAr,
                      modifier = Modifier.fillMaxSize().clickable { enlargedImageUrl = product.imageResName },
                      contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                  } else {
                    // Decorative placeholder
                    Box(
                      modifier = Modifier.fillMaxSize(),
                      contentAlignment = Alignment.Center
                    ) {
                      Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                          imageVector = when (product.category) {
                            "مشروبات" -> Icons.Default.LocalCafe
                            "مأكولات" -> Icons.Default.Restaurant
                            "حلويات" -> Icons.Default.Cake
                            else -> Icons.Default.Category
                          },
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                          modifier = Modifier.size(64.dp)
                        )
                      }
                    }
                  }

                  // Semi-transparent overlay at the bottom for Category
                  Surface(
                    modifier = Modifier
                      .align(Alignment.BottomStart)
                      .padding(12.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                  ) {
                    Text(
                      text = product.category,
                      color = MaterialTheme.colorScheme.onPrimary,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                  }

                  // Close Button
                  IconButton(
                    onClick = { selectedProductForDetail = null },
                    modifier = Modifier
                      .align(Alignment.TopEnd)
                      .padding(12.dp)
                      .size(32.dp)
                      .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), androidx.compose.foundation.shape.CircleShape)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Close,
                      contentDescription = "إغلاق",
                      tint = MaterialTheme.colorScheme.onPrimary,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }

                // Details Section
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  // Product Name & Subname
                  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                      text = product.nameAr,
                      fontSize = 20.sp,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    if (product.nameEn.isNotEmpty()) {
                      Text(
                        text = product.nameEn,
                        fontSize = 14.sp,
                        color = Color.Gray
                      )
                    }
                    if (product.barcode.isNotEmpty()) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                          .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                          .padding(horizontal = 6.dp, vertical = 2.dp)
                      ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                        Text(
                          text = "الباركود: ${product.barcode}",
                          fontSize = 10.sp,
                          color = Color.Gray
                        )
                      }
                    }
                  }

                  HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                  // Financial Highlight
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    // Purchase Price Card
                    Card(
                      modifier = Modifier.weight(1f),
                      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                      shape = RoundedCornerShape(12.dp)
                    ) {
                      Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                      ) {
                        Text("سعر الشراء", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                          text = formatMoney(product.pricePurchase),
                          fontSize = 14.sp,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                    }

                    // Sale Price Card
                    Card(
                      modifier = Modifier.weight(1f),
                      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
                      shape = RoundedCornerShape(12.dp)
                    ) {
                      Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                      ) {
                        Text("سعر البيع", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        Text(
                          text = formatMoney(product.priceSale),
                          fontSize = 14.sp,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.primary
                        )
                      }
                    }

                    // Expected Profit Margin Card
                    val profit = product.priceSale - product.pricePurchase
                    val marginPercent = if (product.pricePurchase > 0.0) (profit / product.pricePurchase) * 100.0 else 0.0
                    Card(
                      modifier = Modifier.weight(1f),
                      colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.06f)),
                      shape = RoundedCornerShape(12.dp)
                    ) {
                      Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                      ) {
                        Text("الربح المتوقع", fontSize = 10.sp, color = EmeraldGreen)
                        Text(
                          text = "${formatMoney(profit)} (${String.format("%.1f", marginPercent)}%)",
                          fontSize = 12.sp,
                          fontWeight = FontWeight.Bold,
                          color = EmeraldGreen
                        )
                      }
                    }
                  }

                  // Stock Status Row
                  val isLow = product.quantity <= product.minQuantityAlert
                  val isOut = product.quantity <= 0
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(12.dp))
                      .background(
                        when {
                          isOut -> CoralError.copy(alpha = 0.08f)
                          isLow -> AmberAlert.copy(alpha = 0.08f)
                          else -> EmeraldGreen.copy(alpha = 0.08f)
                        }
                      )
                      .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Icon(
                        imageVector = when {
                          isOut -> Icons.Default.Cancel
                          isLow -> Icons.Default.Warning
                          else -> Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        tint = when {
                          isOut -> CoralError
                          isLow -> AmberAlert
                          else -> EmeraldGreen
                        },
                        modifier = Modifier.size(20.dp)
                      )
                      Text(
                        text = "حالة المخزون الحالي:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                          isOut -> CoralError
                          isLow -> AmberAlert
                          else -> EmeraldGreen
                        }
                      )
                    }

                    Text(
                      text = when {
                        isOut -> "منتهي تماماً (0 قطع)"
                        isLow -> "كمية منخفضة جداً (${product.quantity} قطع)"
                        else -> "متوفر بكثرة (${product.quantity} قطع)"
                      },
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = when {
                        isOut -> CoralError
                        isLow -> AmberAlert
                        else -> EmeraldGreen
                      }
                    )
                  }

                  HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                  // Product History Section
                  Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Text("سجل حركة الصنف ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    var selectedHistoryTab by remember { mutableStateOf("sales") }
                    
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      androidx.compose.material3.FilterChip(
                        selected = selectedHistoryTab == "sales",
                        onClick = { selectedHistoryTab = "sales" },
                        label = { Text("المبيعات (${salesHistory.size})", fontSize = 12.sp) }
                      )
                      androidx.compose.material3.FilterChip(
                        selected = selectedHistoryTab == "imports",
                        onClick = { selectedHistoryTab = "imports" },
                        label = { Text("تحديثات المخزون (${auditHistory.size})", fontSize = 12.sp) }
                      )
                    }

                    Column(
                      modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                    ) {
                      if (selectedHistoryTab == "sales") {
                        if (salesHistory.isEmpty()) {
                          Text("لم يتم تسجيل أي مبيعات لهذا المنتج.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(8.dp))
                        } else {
                          salesHistory.take(15).forEach { sale ->
                            val dateStr = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.US).format(java.util.Date(sale.invoiceDate))
                            Row(
                              modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 4.dp),
                              horizontalArrangement = Arrangement.SpaceBetween,
                              verticalAlignment = Alignment.CenterVertically
                            ) {
                              Column {
                                Text("فاتورة #${sale.invoiceId}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(dateStr, fontSize = 10.sp, color = Color.Gray)
                              }
                              Column(horizontalAlignment = Alignment.End) {
                                Text("الكمية: ${sale.quantity}", fontSize = 12.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                                Text(formatMoney(sale.total), fontSize = 10.sp, color = Color.Gray)
                              }
                            }
                            if (sale != salesHistory.take(15).last()) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                          }
                          if (salesHistory.size > 15) {
                            Text("... و ${salesHistory.size - 15} عملية أخرى", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally))
                          }
                        }
                      } else {
                        if (auditHistory.isEmpty()) {
                          Text("لا يوجد سجل تحديثات لهذا المنتج.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(8.dp))
                        } else {
                          auditHistory.take(15).forEach { log ->
                            val dateStr = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.US).format(java.util.Date(log.timestamp))
                            
                            val details = log.details
                            if (details.startsWith("IMPORT|")) {
                              val parts = details.split("|")
                              val prodName = parts.getOrNull(1) ?: "منتج"
                              val supplierName = parts.getOrNull(2) ?: "غير محدد"
                              val purchasePrice = parts.getOrNull(3)?.toDoubleOrNull() ?: 0.0
                              val salePrice = parts.getOrNull(4)?.toDoubleOrNull() ?: 0.0
                              val qtyAdded = parts.getOrNull(5) ?: "0"
                              
                              Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                  Icon(Icons.Default.Receipt, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                                  Spacer(modifier = Modifier.width(8.dp))
                                  Text("فاتورة إستيراد بضاعة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                  Spacer(modifier = Modifier.weight(1f))
                                  Text(dateStr, fontSize = 10.sp, color = Color.Gray)
                                }
                                Card(
                                  modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                  shape = RoundedCornerShape(8.dp),
                                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                ) {
                                  Column(modifier = Modifier.padding(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                      Text("المورد:", fontSize = 11.sp, color = Color.Gray)
                                      Text(supplierName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                      Text("الكمية المستوردة:", fontSize = 11.sp, color = Color.Gray)
                                      Text("$qtyAdded وحدة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                      Text("سعر الشراء:", fontSize = 11.sp, color = Color.Gray)
                                      Text(formatMoney(purchasePrice), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                      Text("سعر البيع المحدد:", fontSize = 11.sp, color = Color.Gray)
                                      Text(formatMoney(salePrice), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                  }
                                }
                              }
                            } else {
                              Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                              ) {
                                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                  Text(log.action, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                  Text(log.details, fontSize = 11.sp)
                                  Text(dateStr, fontSize = 10.sp, color = Color.Gray)
                                }
                              }
                            }
                            if (log != auditHistory.take(15).last()) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                          }
                          if (auditHistory.size > 15) {
                            Text("... و ${auditHistory.size - 15} عملية أخرى", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally))
                          }
                        }
                      }
                    }
                  }

                  HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                  // Add to Cart Block
                  if (isOut) {
                    Button(
                      onClick = { selectedProductForDetail = null },
                      modifier = Modifier.fillMaxWidth(),
                      colors = ButtonDefaults.buttonColors(containerColor = CoralError),
                      shape = RoundedCornerShape(12.dp)
                    ) {
                      Text("عذراً، هذا المنتج غير متوفر حالياً", fontWeight = FontWeight.Bold)
                    }
                  } else {
                    var qtyToAdd by remember { mutableStateOf(1) }

                    Column(
                      modifier = Modifier.fillMaxWidth(),
                      verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = "حدد الكمية المطلوبة للسلة:",
                          fontSize = 12.sp,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onSurface
                        )

                        // Plus Minus Selector
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                          // Minus
                          IconButton(
                            onClick = { if (qtyToAdd > 1) qtyToAdd-- },
                            modifier = Modifier
                              .size(36.dp)
                              .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape)
                          ) {
                            Icon(Icons.Default.Remove, contentDescription = "تقليل", modifier = Modifier.size(16.dp))
                          }

                          Text(
                            text = qtyToAdd.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                          )

                          // Plus
                          IconButton(
                            onClick = { if (qtyToAdd < product.quantity) qtyToAdd++ },
                            modifier = Modifier
                              .size(36.dp)
                              .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape)
                          ) {
                            Icon(Icons.Default.Add, contentDescription = "زيادة", modifier = Modifier.size(16.dp))
                          }
                        }
                      }

                      val totalPrice = product.priceSale * qtyToAdd
                      Button(
                        onClick = {
                          viewModel.addToCart(product, qtyToAdd)
                          selectedProductForDetail = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                      ) {
                        Row(
                          horizontalArrangement = Arrangement.spacedBy(8.dp),
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                          Text(
                            text = "إضافة إلى السلة (${formatMoney(totalPrice)})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
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
    }
  }
}

@Composable
fun PosCartSection(
modifier: Modifier = Modifier,
viewModel: AppViewModel,
cart: List<CartItem>,
customers: List<Customer>,
selectedCustomerId: Int?,
subtotal: Double,
discount: Double,
taxPercent: Double,
taxAmount: Double,
total: Double,
isDraftMode: Boolean = false,
onCheckoutClick: () -> Unit
  ) {
    Column(modifier = modifier) {
    // Header
    Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.ShoppingCart, contentDescription = "سلة", tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text("سلة المبيعات", fontWeight = FontWeight.Bold, fontSize = 16.sp)
      }
        if (cart.isNotEmpty()) {
          IconButton(onClick = { viewModel.clearCart() }) {
          Icon(Icons.Default.DeleteSweep, contentDescription = "مسح الكل", tint = CoralError)
        }
      }
    }
    
    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
    
    // CRM Customer Binder
    Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
    shape = RoundedCornerShape(12.dp)
      ) {
      
        Column(modifier = Modifier.padding(12.dp)) {
        Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
          ) {
          Text("ربط العميل (نقاط ولاء):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (selectedCustomerId != null) {
              TextButton(onClick = { viewModel.selectCustomer(null) }) {
              Text("إلغاء", color = CoralError, fontSize = 11.sp)
            }
          }
        }
        var expandedCustomerMenu by remember { mutableStateOf(false) }
          Box(modifier = Modifier.fillMaxWidth()) {
          val activeCustomer = customers.find { it.id == selectedCustomerId }
          OutlinedButton(
          onClick = { expandedCustomerMenu = true },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
            ) {
            Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
              ) {
              Text(
              text = activeCustomer?.let { "${it.name} (${it.type})" } ?: "اختر العميل...",
              fontSize = 13.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
              )
              Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
          }
          DropdownMenu(
          expanded = expandedCustomerMenu,
          onDismissRequest = { expandedCustomerMenu = false },
          modifier = Modifier.fillMaxWidth(0.8f)
            ) {
            DropdownMenuItem(
            text = { Text("عميل نقدي عام (بدون نقاط)") },
              onClick = {
              viewModel.selectCustomer(null)
              expandedCustomerMenu = false
            }
            )
              customers.forEach { cust ->
              DropdownMenuItem(
              text = { Text("${cust.name} - ${cust.phone} [${cust.type}]") },
                onClick = {
                viewModel.selectCustomer(cust.id)
                expandedCustomerMenu = false
              }
              )
            }
          }
        }
        
        if (selectedCustomerId == null) {
          val manualCustomerName by viewModel.manualCustomerName.collectAsState()
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = manualCustomerName,
            onValueChange = { viewModel.setManualCustomerName(it) },
            label = { Text("أو اكتب اسم العميل يدويًا...", fontSize = 11.sp, color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surface,
              unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
          )
        }
      }
    }
    
    // Cart Items List
      Box(modifier = Modifier.weight(1f)) {
        if (cart.isEmpty()) {
        Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
          ) {
          
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
            imageVector = Icons.Default.AddShoppingCart,
            contentDescription = "سلة فارغة",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
            "السلة فارغة. انقر على المنتجات لإضافتها.",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
          }
        }
        } else {
        LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
          ) {
            items(cart) { item ->
            Row(
            modifier = Modifier
            .fillMaxWidth()
            .background(
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
            RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
              ) {
              
                Column(modifier = Modifier.weight(1f)) {
                Text(
                text = item.product.nameAr,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
                )
                Text(
                text = "${formatMoney(item.product.priceSale)} ",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary
                )
              }
              
              Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                IconButton(
                onClick = { viewModel.decreaseQuantity(item.product) },
                modifier = Modifier.size(24.dp)
                  ) {
                  Icon(Icons.Default.Remove, contentDescription = "تقليل", modifier = Modifier.size(16.dp))
                }
                
                Text(
                text = item.quantity.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
                )
                
                IconButton(
                onClick = { viewModel.addToCart(item.product) },
                modifier = Modifier.size(24.dp)
                  ) {
                  Icon(Icons.Default.Add, contentDescription = "زيادة", modifier = Modifier.size(16.dp))
                }
              }
              
              IconButton(
              onClick = { viewModel.removeFromCart(item.product) },
              modifier = Modifier.size(28.dp)
                ) {
                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = CoralError, modifier = Modifier.size(18.dp))
              }
            }
          }
        }
      }
    }
    
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    
    Column(
    verticalArrangement = Arrangement.spacedBy(4.dp),
    modifier = Modifier.fillMaxWidth()
      ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        var isPercentDiscount by remember { mutableStateOf(false) }
        var discountInputText by remember { mutableStateOf("") }

        LaunchedEffect(subtotal, isPercentDiscount, discountInputText) {
          val parsed = discountInputText.toDoubleOrNull() ?: 0.0
          if (isPercentDiscount) {
            val calculatedAmount = subtotal * (parsed / 100.0)
            viewModel.setDiscount(calculatedAmount)
          } else {
            viewModel.setDiscount(parsed)
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("نوع الخصم:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
          
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .padding(2.dp)
          ) {
            Surface(
              onClick = { isPercentDiscount = true },
              shape = RoundedCornerShape(6.dp),
              color = if (isPercentDiscount) MaterialTheme.colorScheme.primary else Color.Transparent
            ) {
              Text(
                text = "% نسبة مئوية",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPercentDiscount) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
            
            Spacer(modifier = Modifier.width(2.dp))

            Surface(
              onClick = { isPercentDiscount = false },
              shape = RoundedCornerShape(6.dp),
              color = if (!isPercentDiscount) MaterialTheme.colorScheme.primary else Color.Transparent
            ) {
              Text(
                text = "مبلغ ثابت",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (!isPercentDiscount) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (isPercentDiscount) "نسبة الخصم (%):" else "مبلغ الخصم المباشر:",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
              value = discountInputText,
              onValueChange = { discountInputText = it },
              textStyle = TextStyle(fontSize = 11.sp, textAlign = TextAlign.Center),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              placeholder = { Text(if (isPercentDiscount) "0 %" else "0.0", fontSize = 11.sp) },
              modifier = Modifier
                .width(90.dp)
                .height(40.dp),
              shape = RoundedCornerShape(12.dp),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
              )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (isPercentDiscount) "%" else viewModel.currencySymbol.collectAsState().value,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        if (isPercentDiscount && discount > 0) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            Text(
              text = "قيمة الخصم: ${formatMoney(discount)} ${viewModel.currencySymbol.collectAsState().value}",
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.secondary,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
      
      Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
        ) {
        Text("المجموع الفرعي:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text("${formatMoney(subtotal)} ", fontSize = 12.sp, fontWeight = FontWeight.Medium)
      }
      
        if (discount > 0) {
        Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
          ) {
          Text("خصم مطبق:", fontSize = 12.sp, color = CoralError)
          Text("- ${formatMoney(discount)} ", fontSize = 12.sp, color = CoralError, fontWeight = FontWeight.Medium)
        }
      }
      
      Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
        ) {
        Text("الضريبة المضافة (${taxPercent}%):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text("${formatMoney(taxAmount)} ", fontSize = 12.sp, fontWeight = FontWeight.Medium)
      }
      
      Row(
      modifier = Modifier
      .background(
      MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
      RoundedCornerShape(12.dp)
      )
      .padding(horizontal = 8.dp, vertical = 6.dp)
      .fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
        ) {
        Text("المجموع النهائي:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(
        "${formatMoney(total)} ",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
        )
      }
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    
    Button(
    onClick = onCheckoutClick,
    modifier = Modifier
    .fillMaxWidth()
    .height(48.dp)
    .testTag("checkout_button"),
    enabled = cart.isNotEmpty(),
    shape = RoundedCornerShape(12.dp),
    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
      ) {
      Icon(if (isDraftMode) Icons.Default.Save else Icons.Default.Payment, contentDescription = "دفع")
      Spacer(modifier = Modifier.width(8.dp))
      Text(if (isDraftMode) "حفظ مسودة الفاتورة " else "دفع وإنشاء الفاتورة", fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
  fun MoreScreen(viewModel: AppViewModel, onNavigate: (String) -> Unit, onLogout: () -> Unit = {}) {
  val userRole by viewModel.currentUserRole.collectAsState()
  val userAccount by viewModel.currentUserAccount.collectAsState()

  Column(
  modifier = Modifier
  .fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)
  .verticalScroll(rememberScrollState()),
  verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
    Text(
      text = "الخدمات والميزات الإضافية ",
      fontWeight = FontWeight.Bold,
      fontSize = 20.sp,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(bottom = 8.dp)
    )
    
    val items = remember(userRole, userAccount) {
      val base = mutableListOf(
        Triple("feature_requests", "اقتراح ميزة / طلبات الدعم", Icons.Default.Lightbulb),
        Triple("transactions_history", "سجل كافة المعاملات والحركات المالية", Icons.Default.ReceiptLong),
        Triple("invoice_form", "نموذج إصدار فواتير مبيعات متطور", Icons.Default.Receipt),
        Triple("crm", "إضافة عميل أو مورد جديد CRM", Icons.Default.People),
        Triple("receipt_vouchers", "سندات القبض وتحصيل العملاء", Icons.Default.CallReceived),
        Triple("expenses", "سندات الصرف والمصروفات", Icons.Default.AttachMoney),
        Triple("statements", "كشوف الحسابات (عملاء وموردين)", Icons.Default.ReceiptLong),
        Triple("balances_report", "كشف الأرصدة (عملاء وموردين)", Icons.Default.AccountBalanceWallet),
        Triple("expense_balances_report", "كشف أرصدة المصاريف", Icons.Default.Payments),
        Triple("checks", "سجل الشيكات البنكية", Icons.Default.Article),
        Triple("settings", "إعدادات النظام والخيارات المتقدمة", Icons.Default.Settings),
        Triple("backup_restore", "النسخ الاحتياطي والاستعادة", Icons.Default.CloudSync)
      )
      if (userRole == "مدير" || userAccount?.role == "ADMIN") {
        base.add(Triple("admin_logs", "سجل الأنشطة (Activity Log)", Icons.Default.History))
      }
      if (userAccount?.role == "ADMIN") {
        base.add(Triple("admin_users", "إدارة مستخدمي التطبيق وفترات التجربة", Icons.Default.Lock))
        base.add(Triple("admin_firebase", "إعدادات مزامنة Firebase", Icons.Default.CloudSync))
        base.add(Triple("admin_branches", "إدارة وتخصيص فروع النظام", Icons.Default.Store))
      }
      base
    }
    
      items.forEach { (route, title, icon) ->
      Card(
      onClick = { onNavigate(route) },
      modifier = Modifier
      .fillMaxWidth()
      .testTag("more_item_$route"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(2.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
          contentAlignment = Alignment.Center
            ) {
            Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
            )
          }
          Text(
          text = title,
          fontWeight = FontWeight.SemiBold,
          fontSize = 15.sp,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f)
          )
          Icon(
          imageVector = Icons.Default.ChevronLeft,
          contentDescription = "انتقال",
          tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
          modifier = Modifier.size(20.dp)
          )
        }
      }
    }
    
            Spacer(modifier = Modifier.height(6.dp))
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
    }
  }
}

// 2. DASHBOARD SCREEN (REPORTS & CHARTS)
// ==========================================
@Composable
fun DateRangeExportDialog(
  initialFromDate: Long?,
  initialToDate: Long?,
  isPdf: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (fromDate: Long?, toDate: Long?, fromStr: String?, toStr: String?) -> Unit
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val sdf = remember { java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.US) }

  var fromMillis by remember { mutableStateOf(initialFromDate) }
  var toMillis by remember { mutableStateOf(initialToDate) }

  val fromStr = remember(fromMillis) { if (fromMillis != null) sdf.format(java.util.Date(fromMillis!!)) else "غير محدد (من البداية)" }
  val toStr = remember(toMillis) { if (toMillis != null) sdf.format(java.util.Date(toMillis!!)) else "غير محدد (حتى الآن)" }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = { Icon(if (isPdf) Icons.Default.ReceiptLong else Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
    title = {
      Text(
        text = if (isPdf) "تحديد فترة طباعة تقرير PDF " else "تحديد فترة تصدير Excel ",
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "اختر تاريخ البداية والنهاية لاستخراج التقرير التفصيلي المحدد:",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // From Date Card
        OutlinedCard(
          onClick = {
            val cal = java.util.Calendar.getInstance()
            if (fromMillis != null) cal.timeInMillis = fromMillis!!
            android.app.DatePickerDialog(
              context,
              { _, year, month, dayOfMonth ->
                cal.set(year, month, dayOfMonth, 0, 0, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                fromMillis = cal.timeInMillis
              },
              cal.get(java.util.Calendar.YEAR),
              cal.get(java.util.Calendar.MONTH),
              cal.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("من تاريخ (بداية الفترة):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(fromStr, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            }
            Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          }
        }

        // To Date Card
        OutlinedCard(
          onClick = {
            val cal = java.util.Calendar.getInstance()
            if (toMillis != null) cal.timeInMillis = toMillis!!
            android.app.DatePickerDialog(
              context,
              { _, year, month, dayOfMonth ->
                cal.set(year, month, dayOfMonth, 23, 59, 59)
                cal.set(java.util.Calendar.MILLISECOND, 999)
                toMillis = cal.timeInMillis
              },
              cal.get(java.util.Calendar.YEAR),
              cal.get(java.util.Calendar.MONTH),
              cal.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("إلى تاريخ (نهاية الفترة):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(toStr, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            }
            Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          TextButton(
            onClick = {
              fromMillis = null
              toMillis = null
            },
            modifier = Modifier.weight(1f)
          ) {
            Text("كافة الأيام", fontSize = 11.sp)
          }
          TextButton(
            onClick = {
              val cal = java.util.Calendar.getInstance()
              cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
              cal.set(java.util.Calendar.MINUTE, 0)
              cal.set(java.util.Calendar.SECOND, 0)
              cal.set(java.util.Calendar.MILLISECOND, 0)
              fromMillis = cal.timeInMillis

              cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
              cal.set(java.util.Calendar.MINUTE, 59)
              cal.set(java.util.Calendar.SECOND, 59)
              cal.set(java.util.Calendar.MILLISECOND, 999)
              toMillis = cal.timeInMillis
            },
            modifier = Modifier.weight(1f)
          ) {
            Text("اليوم فقط", fontSize = 11.sp)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val fStr = if (fromMillis != null) sdf.format(java.util.Date(fromMillis!!)) else null
          val tStr = if (toMillis != null) sdf.format(java.util.Date(toMillis!!)) else null
          onConfirm(fromMillis, toMillis, fStr, tStr)
        }
      ) {
        Text(if (isPdf) "طباعة / تصدير PDF" else "تصدير Excel")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إلغاء")
      }
    }
  )
}

@Composable
fun DashboardScreen(
  viewModel: AppViewModel,
  invoices: List<Invoice>,
  products: List<Product>,
  categories: List<Category>,
  ledgerEntries: List<LedgerEntry>
) {
  var showAddPurchaseDialog by remember { mutableStateOf(false) }

  val context = androidx.compose.ui.platform.LocalContext.current
  val supplierTransactions by viewModel.allSupplierTransactions.collectAsState(initial = emptyList())
  val customerTransactions by viewModel.allCustomerTransactions.collectAsState(initial = emptyList())
  val expenses by viewModel.allExpenses.collectAsState(initial = emptyList())
  val suppliers by viewModel.allSuppliers.collectAsState(initial = emptyList())
  val currencySymbol by viewModel.currencySymbol.collectAsState()

  // --- 1. General Treasury Calculation (الصندوق العام - لجميع الأيام) ---
  val allTimeBalances = viewModel.calculateAccountBalances(ledgerEntries)
  val ledgerTreasury = allTimeBalances["الصندوق"] ?: 0.0

  val allTimeCashSales = invoices.filter { !it.isDraft && !it.isPurchase }.sumOf { it.paidAmount }
  val allTimeCustomerCollections = customerTransactions.filter { it.transactionType in listOf("سداد", "دفعة", "تحصيل", "إيداع") }.sumOf { it.amount }
  val allTimePurchasePayments = invoices.filter { it.isPurchase && !it.isDraft }.sumOf { it.paidAmount } + supplierTransactions.filter { it.transactionType in listOf("سداد", "دفعة") }.sumOf { it.amount }
  val allTimeExpenses = expenses.sumOf { it.amount }

  val directTreasuryAllTime = allTimeCashSales + allTimeCustomerCollections - allTimePurchasePayments - allTimeExpenses
  val generalTreasury = if (ledgerTreasury != 0.0) ledgerTreasury else directTreasuryAllTime

  // --- 2. Date Range Selection State (تحديد الفترة من - إلى) ---
  var periodPreset by remember { mutableStateOf("الكل") } // "الكل", "اليوم", "هذا الأسبوع", "هذا الشهر", "فترة مخصصة"
  var startDateMillis by remember { mutableStateOf<Long?>(null) }
  var endDateMillis by remember { mutableStateOf<Long?>(null) }

  var showExportDialog by remember { mutableStateOf(false) }
  var isExportPdfMode by remember { mutableStateOf(true) }

  val dateSdf = remember { java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.US) }

  val startDateStr = remember(startDateMillis) { if (startDateMillis != null) dateSdf.format(java.util.Date(startDateMillis!!)) else null }
  val endDateStr = remember(endDateMillis) { if (endDateMillis != null) dateSdf.format(java.util.Date(endDateMillis!!)) else null }

  // Filtered lists according to selected period
  val filteredInvoices = remember(invoices, startDateMillis, endDateMillis) {
    if (startDateMillis == null && endDateMillis == null) invoices
    else invoices.filter { inv ->
      (startDateMillis == null || inv.invoiceDate >= startDateMillis!!) &&
      (endDateMillis == null || inv.invoiceDate <= endDateMillis!!)
    }
  }

  val filteredLedgerEntries = remember(ledgerEntries, startDateMillis, endDateMillis) {
    if (startDateMillis == null && endDateMillis == null) ledgerEntries
    else ledgerEntries.filter { le ->
      (startDateMillis == null || le.date >= startDateMillis!!) &&
      (endDateMillis == null || le.date <= endDateMillis!!)
    }
  }

  val filteredExpenses = remember(expenses, startDateMillis, endDateMillis) {
    if (startDateMillis == null && endDateMillis == null) expenses
    else expenses.filter { exp ->
      (startDateMillis == null || exp.date >= startDateMillis!!) &&
      (endDateMillis == null || exp.date <= endDateMillis!!)
    }
  }

  val filteredSupplierTx = remember(supplierTransactions, startDateMillis, endDateMillis) {
    if (startDateMillis == null && endDateMillis == null) supplierTransactions
    else supplierTransactions.filter { st ->
      (startDateMillis == null || st.date >= startDateMillis!!) &&
      (endDateMillis == null || st.date <= endDateMillis!!)
    }
  }

  // Period metrics
  val periodSales = filteredInvoices.filter { !it.isPurchase && !it.isDraft }.sumOf { it.total }
  val todaySales = periodSales
  val periodSalesCount = filteredInvoices.filter { !it.isPurchase && !it.isDraft }.size
  val periodPurchases = filteredSupplierTx.filter { it.transactionType == "شراء" }.sumOf { it.amount } + filteredInvoices.filter { it.isPurchase && !it.isDraft }.sumOf { it.total }
  val periodExpensesTotal = filteredExpenses.sumOf { it.amount } + filteredLedgerEntries.filter { it.debitAccount == "المصاريف" }.sumOf { it.amount }
  val periodNetProfit = (periodSales - periodPurchases - periodExpensesTotal).coerceAtLeast(0.0)

  val lowStockCount = products.count { it.quantity <= it.minQuantityAlert }

  var showAddExpenseDialog by remember { mutableStateOf(false) }


  if (showExportDialog) {
    DateRangeExportDialog(
      initialFromDate = startDateMillis,
      initialToDate = endDateMillis,
      isPdf = isExportPdfMode,
      onDismiss = { showExportDialog = false },
      onConfirm = { fromM, toM, fromStr, toStr ->
        showExportDialog = false
        val exportInvoices = if (fromM == null && toM == null) invoices else invoices.filter { inv ->
          (fromM == null || inv.invoiceDate >= fromM) && (toM == null || inv.invoiceDate <= toM)
        }
        val exportLedger = if (fromM == null && toM == null) ledgerEntries else ledgerEntries.filter { le ->
          (fromM == null || le.date >= fromM) && (toM == null || le.date <= toM)
        }
        val expSupplierTx = if (fromM == null && toM == null) supplierTransactions else supplierTransactions.filter { st ->
          (fromM == null || st.date >= fromM) && (toM == null || st.date <= toM)
        }
        val expExpenses = if (fromM == null && toM == null) expenses else expenses.filter { exp ->
          (fromM == null || exp.date >= fromM) && (toM == null || exp.date <= toM)
        }

        val pSales = exportInvoices.filter { !it.isPurchase && !it.isDraft }.sumOf { it.total }
        val pPurchases = expSupplierTx.filter { it.transactionType == "شراء" }.sumOf { it.amount } + exportInvoices.filter { it.isPurchase && !it.isDraft }.sumOf { it.total }
        val pExpenses = expExpenses.sumOf { it.amount } + exportLedger.filter { it.debitAccount == "المصاريف" }.sumOf { it.amount }
        val pProfit = (pSales - pPurchases - pExpenses).coerceAtLeast(0.0)

        if (isExportPdfMode) {
          viewModel.exportDashboardToPdf(
            context = context,
            generalTreasury = generalTreasury,
            periodSales = pSales,
            periodPurchases = pPurchases,
            periodExpenses = pExpenses,
            periodNetProfit = pProfit,
            periodCount = exportInvoices.size,
            lowStockCount = lowStockCount,
            fromDateStr = fromStr,
            toDateStr = toStr,
            invoices = exportInvoices,
            ledgerEntries = exportLedger
          )
        } else {
          viewModel.exportDashboardToExcel(
            generalTreasury = generalTreasury,
            periodSales = pSales,
            periodPurchases = pPurchases,
            periodExpenses = pExpenses,
            periodNetProfit = pProfit,
            fromDateStr = fromStr,
            toDateStr = toStr,
            invoices = exportInvoices
          )
        }
      }
    )
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isCompact = maxWidth < 600.dp
    
    Column(
      modifier = Modifier
        .fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      if (isCompact) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "الداشبورد المالي والتشغيلي الشامل ",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
          )
          
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Button(
              onClick = {
                isExportPdfMode = false
                showExportDialog = true
              },
              colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("تصدير Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Button(
              onClick = {
                isExportPdfMode = true
                showExportDialog = true
              },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("تصدير PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "الداشبورد المالي والتشغيلي الشامل ",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
          )
          
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Button(
              onClick = {
                isExportPdfMode = false
                showExportDialog = true
              },
              colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("تصدير Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Button(
              onClick = {
                isExportPdfMode = true
                showExportDialog = true
              },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("تصدير PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // --- 3. الصندوق العام (الخزينة العامة بكل الأيام) Card ---
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = EmeraldGreen.copy(alpha = 0.12f)
        ),
        border = BorderStroke(1.5.dp, EmeraldGreen.copy(alpha = 0.35f))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f).padding(end = 8.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Surface(
                shape = CircleShape,
                color = EmeraldGreen,
                modifier = Modifier.size(28.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
              Text(
                text = "الصندوق العام (رصيد الخزينة الكلي) ",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = EmeraldGreen
              )
            }
            Text(
              text = "${formatMoney(generalTreasury)} $currencySymbol",
              fontWeight = FontWeight.ExtraBold,
              fontSize = 22.sp,
              color = EmeraldGreen
            )
            Text(
              text = "إجمالي السيولة النقدية المتوفرة الكلية (بكل الأيام)",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = EmeraldGreen.copy(alpha = 0.2f)
          ) {
            Text(
              text = "تراكمي بكل الأيام",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = EmeraldGreen,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
        }
      }

      // --- 4. Date Filter Selection Bar (تحديد فترة التقرير من تاريخ إلى تاريخ) ---
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
              Text("تحديد فترة التقرير والطباعة:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            if (startDateMillis != null || endDateMillis != null) {
              TextButton(
                onClick = {
                  periodPreset = "الكل"
                  startDateMillis = null
                  endDateMillis = null
                },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text("إعادة ضبط (كافة الأيام)", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
              }
            }
          }

          // Quick Preset Chips
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf("الكل", "اليوم", "هذا الأسبوع", "هذا الشهر", "فترة مخصصة").forEach { preset ->
              FilterChip(
                selected = periodPreset == preset,
                onClick = {
                  periodPreset = preset
                  when (preset) {
                    "الكل" -> {
                      startDateMillis = null
                      endDateMillis = null
                    }
                    "اليوم" -> {
                      val cal = java.util.Calendar.getInstance()
                      cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                      cal.set(java.util.Calendar.MINUTE, 0)
                      cal.set(java.util.Calendar.SECOND, 0)
                      cal.set(java.util.Calendar.MILLISECOND, 0)
                      startDateMillis = cal.timeInMillis

                      cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                      cal.set(java.util.Calendar.MINUTE, 59)
                      cal.set(java.util.Calendar.SECOND, 59)
                      cal.set(java.util.Calendar.MILLISECOND, 999)
                      endDateMillis = cal.timeInMillis
                    }
                    "هذا الأسبوع" -> {
                      val cal = java.util.Calendar.getInstance()
                      cal.set(java.util.Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                      cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                      cal.set(java.util.Calendar.MINUTE, 0)
                      cal.set(java.util.Calendar.SECOND, 0)
                      cal.set(java.util.Calendar.MILLISECOND, 0)
                      startDateMillis = cal.timeInMillis

                      val calEnd = java.util.Calendar.getInstance()
                      calEnd.set(java.util.Calendar.HOUR_OF_DAY, 23)
                      calEnd.set(java.util.Calendar.MINUTE, 59)
                      calEnd.set(java.util.Calendar.SECOND, 59)
                      calEnd.set(java.util.Calendar.MILLISECOND, 999)
                      endDateMillis = calEnd.timeInMillis
                    }
                    "هذا الشهر" -> {
                      val cal = java.util.Calendar.getInstance()
                      cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                      cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                      cal.set(java.util.Calendar.MINUTE, 0)
                      cal.set(java.util.Calendar.SECOND, 0)
                      cal.set(java.util.Calendar.MILLISECOND, 0)
                      startDateMillis = cal.timeInMillis

                      val calEnd = java.util.Calendar.getInstance()
                      calEnd.set(java.util.Calendar.HOUR_OF_DAY, 23)
                      calEnd.set(java.util.Calendar.MINUTE, 59)
                      calEnd.set(java.util.Calendar.SECOND, 59)
                      calEnd.set(java.util.Calendar.MILLISECOND, 999)
                      endDateMillis = calEnd.timeInMillis
                    }
                    "فترة مخصصة" -> {
                      isExportPdfMode = true
                      showExportDialog = true
                    }
                  }
                },
                label = { Text(preset, fontSize = 11.sp) }
              )
            }
          }

          // Selected Date Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = {
                val cal = java.util.Calendar.getInstance()
                if (startDateMillis != null) cal.timeInMillis = startDateMillis!!
                android.app.DatePickerDialog(
                  context,
                  { _, y, m, d ->
                    cal.set(y, m, d, 0, 0, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    startDateMillis = cal.timeInMillis
                    periodPreset = "فترة مخصصة"
                  },
                  cal.get(java.util.Calendar.YEAR),
                  cal.get(java.util.Calendar.MONTH),
                  cal.get(java.util.Calendar.DAY_OF_MONTH)
                ).show()
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("من: ${startDateStr ?: "البداية"}", fontSize = 11.sp)
            }

            OutlinedButton(
              onClick = {
                val cal = java.util.Calendar.getInstance()
                if (endDateMillis != null) cal.timeInMillis = endDateMillis!!
                android.app.DatePickerDialog(
                  context,
                  { _, y, m, d ->
                    cal.set(y, m, d, 23, 59, 59)
                    cal.set(java.util.Calendar.MILLISECOND, 999)
                    endDateMillis = cal.timeInMillis
                    periodPreset = "فترة مخصصة"
                  },
                  cal.get(java.util.Calendar.YEAR),
                  cal.get(java.util.Calendar.MONTH),
                  cal.get(java.util.Calendar.DAY_OF_MONTH)
                ).show()
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("إلى: ${endDateStr ?: "الآن"}", fontSize = 11.sp)
            }
          }
        }
      }

      // --- Period KPI Cards ---
      if (isCompact) {
        KpiCard(
          title = if (startDateMillis == null && endDateMillis == null) "إجمالي المبيعات" else "مبيعات الفترة المحددة",
          value = "${formatMoney(periodSales)} $currencySymbol",
          subtitle = "نشاط المبيعات للعملاء",
          icon = Icons.Default.TrendingUp,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.fillMaxWidth()
        )
        KpiCard(
          title = "صافي ربح الفترة",
          value = "${formatMoney(periodNetProfit)} $currencySymbol",
          subtitle = "المبيعات مخصوماً المصاريف والمشتريات",
          icon = Icons.Default.MonetizationOn,
          color = EmeraldGreen,
          modifier = Modifier.fillMaxWidth()
        )
        KpiCard(
          title = "عدد فواتير الفترة",
          value = periodSalesCount.toString(),
          subtitle = "فواتير مكتملة خلال الفترة",
          icon = Icons.Default.ReceiptLong,
          color = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.fillMaxWidth()
        )
        KpiCard(
          title = "منتجات منخفضة المخزون",
          value = lowStockCount.toString(),
          subtitle = "تتطلب أمر توريد فوراً",
          icon = Icons.Default.WarningAmber,
          color = if (lowStockCount > 0) CoralError else Color.Gray,
          modifier = Modifier.fillMaxWidth()
        )
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          KpiCard(
            title = if (startDateMillis == null && endDateMillis == null) "إجمالي المبيعات" else "مبيعات الفترة المحددة",
            value = "${formatMoney(periodSales)} $currencySymbol",
            subtitle = "نشاط المبيعات للعملاء",
            icon = Icons.Default.TrendingUp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
          )
          
          KpiCard(
            title = "صافي ربح الفترة",
            value = "${formatMoney(periodNetProfit)} $currencySymbol",
            subtitle = "المبيعات مخصوماً التكلفة والمصاريف",
            icon = Icons.Default.MonetizationOn,
            color = EmeraldGreen,
            modifier = Modifier.weight(1f)
          )
        }
        
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          KpiCard(
            title = "عدد فواتير الفترة",
            value = periodSalesCount.toString(),
            subtitle = "فواتير مكتملة خلال الفترة",
            icon = Icons.Default.ReceiptLong,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f)
          )
          
          KpiCard(
            title = "منتجات منخفضة المخزون",
            value = lowStockCount.toString(),
            subtitle = "تتطلب أمر توريد فوراً",
            icon = Icons.Default.WarningAmber,
            color = if (lowStockCount > 0) CoralError else Color.Gray,
            modifier = Modifier.weight(1f)
          )
        }
      }

      // Unified Financial Report and Profits/Losses Section
      val totalSalesInvoices = invoices.sumOf { it.total }
      val totalPurchaseInvoices = supplierTransactions.filter { it.transactionType == "شراء" }.sumOf { it.amount }
      val totalExpenses = ledgerEntries.filter { it.debitAccount == "المصاريف" }.sumOf { it.amount }
      val finalProfit = totalSalesInvoices - totalPurchaseInvoices - totalExpenses

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(2.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Assessment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
              )
              Text(
                text = "التقرير المالي العام والأرباح والخسائر ",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

          // 2x2 Grid for Financial Aggregations
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Sales Card
              Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                )
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Icon(
                      Icons.Default.TrendingUp,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(16.dp)
                    )
                    Text(
                      "إجمالي فواتير المبيعات",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Text(
                    "${formatMoney(totalSalesInvoices)} $currencySymbol",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                  )
                }
              }

              // Purchases Card
              Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                  containerColor = CoralError.copy(alpha = 0.05f)
                )
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Icon(
                      Icons.Default.LocalShipping,
                      contentDescription = null,
                      tint = CoralError,
                      modifier = Modifier.size(16.dp)
                    )
                    Text(
                      "إجمالي المشتريات التراكمية",
                      fontWeight = FontWeight.Medium,
                      fontSize = 12.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Text(
                    "${formatMoney(0.0)} $currencySymbol",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = CoralError
                  )
                }
              }
            }
          }
        }
      }

      // Dialogs
      if (showAddExpenseDialog) {
        var expenseDesc by remember { mutableStateOf("") }
        var expenseAmtStr by remember { mutableStateOf("") }
        var expenseSource by remember { mutableStateOf("الصندوق") } // "الصندوق" or "البنك"
        
        AlertDialog(
          onDismissRequest = { showAddExpenseDialog = false },
          title = { Text("تسجيل مصروف تشغيلي جديد ", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
          text = {
            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedTextField(
                value = expenseDesc,
                onValueChange = { expenseDesc = it },
                label = { Text("البيان / تفصيل المصروف (مثال: كهرباء، رواتب)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
              
              OutlinedTextField(
                value = expenseAmtStr,
                onValueChange = { expenseAmtStr = it },
                label = { Text("المبلغ ($currencySymbol) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
              
              Text("مصدر تمويل المصروف:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  RadioButton(selected = expenseSource == "الصندوق", onClick = { expenseSource = "الصندوق" })
                  Text("الصندوق")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                  RadioButton(selected = expenseSource == "البنك", onClick = { expenseSource = "البنك" })
                  Text("البنك")
                }
              }
            }
          },
          confirmButton = {
            Button(
              onClick = {
                val amt = expenseAmtStr.toDoubleOrNull() ?: 0.0
                if (expenseDesc.trim().isEmpty() || amt <= 0.0) {
                  viewModel.showToast("يرجى إدخال تفاصيل صحيحة ومبلغ أكبر من صفر")
                } else {
                  viewModel.addExpenseInvoice(
                    description = expenseDesc.trim(),
                    amount = amt,
                    creditAccount = expenseSource
                  )
                  showAddExpenseDialog = false
                }
              }
            ) {
              Text("حفظ المصروف ")
            }
          },
          dismissButton = {
            TextButton(onClick = { showAddExpenseDialog = false }) {
              Text("إلغاء")
            }
          }
        )
      }

      if (showAddPurchaseDialog) {
        var isNewSupplier by remember { mutableStateOf(false) }
        var selectedSupplierId by remember { mutableStateOf<Int?>(null) }
        var newSupplierName by remember { mutableStateOf("") }
        var purchaseAmtStr by remember { mutableStateOf("") }
        var purchaseNote by remember { mutableStateOf("") }
        var expandedSupplierDropdown by remember { mutableStateOf(false) }
        
        AlertDialog(
          onDismissRequest = { showAddPurchaseDialog = false },
          title = { Text("شراء بضاعة وتوريد للمخازن ", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
          text = {
            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  RadioButton(selected = !isNewSupplier, onClick = { isNewSupplier = false })
                  Text("مورد مسجل")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                  RadioButton(selected = isNewSupplier, onClick = { isNewSupplier = true })
                  Text("مورد جديد")
                }
              }

              if (!isNewSupplier) {
                val currentSupName = suppliers.find { it.id == selectedSupplierId }?.name ?: "اختر مورد"
                Box(modifier = Modifier.fillMaxWidth()) {
                  OutlinedButton(
                    onClick = { expandedSupplierDropdown = true },
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(currentSupName)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                  }
                  DropdownMenu(
                    expanded = expandedSupplierDropdown,
                    onDismissRequest = { expandedSupplierDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.8f)
                  ) {
                    if (suppliers.isEmpty()) {
                      DropdownMenuItem(
                        text = { Text("لا يوجد موردين مسجلين") },
                        onClick = { expandedSupplierDropdown = false }
                      )
                    } else {
                      suppliers.forEach { sup ->
                        DropdownMenuItem(
                          text = { Text(sup.name) },
                          onClick = {
                            selectedSupplierId = sup.id
                            expandedSupplierDropdown = false
                          }
                        )
                      }
                    }
                  }
                }
              } else {
                OutlinedTextField(
                  value = newSupplierName,
                  onValueChange = { newSupplierName = it },
                  label = { Text("اسم المورد الجديد *") },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth()
                )
              }
              
              OutlinedTextField(
                value = purchaseAmtStr,
                onValueChange = { purchaseAmtStr = it },
                label = { Text("مبلغ فاتورة الشراء ($currencySymbol) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
              
              OutlinedTextField(
                value = purchaseNote,
                onValueChange = { purchaseNote = it },
                label = { Text("البيان / تفصيل المشتريات (مثال: بضاعة صيفية)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
            }
          },
          confirmButton = {
            Button(
              onClick = {
                val amt = purchaseAmtStr.toDoubleOrNull() ?: 0.0
                val noteStr = purchaseNote.trim().ifEmpty { "شراء بضاعة وتوريد للمخزن" }
                
                if (amt <= 0.0) {
                  viewModel.showToast("يرجى إدخال مبلغ صحيح أكبر من صفر")
                  return@Button
                }
                
                val supplierNameFinal = if (isNewSupplier) {
                  if (newSupplierName.trim().isEmpty()) {
                    viewModel.showToast("يرجى إدخال اسم المورد الجديد")
                    return@Button
                  }
                  newSupplierName.trim()
                } else {
                  val matched = suppliers.find { it.id == selectedSupplierId }
                  if (matched == null) {
                    viewModel.showToast("يرجى اختيار مورد مسجل أو تسجيل مورد جديد")
                    return@Button
                  }
                  matched.name
                }
                
                viewModel.addPurchaseInvoice(
                  supplierName = supplierNameFinal,
                  amount = amt,
                  note = noteStr
                )
                showAddPurchaseDialog = false
              }
            ) {
              Text("حفظ فاتورة الشراء ")
            }
          },
          dismissButton = {
            TextButton(onClick = { showAddPurchaseDialog = false }) {
              Text("إلغاء")
            }
          }
        )
      }

      Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(2.dp)
        ) {
        
          Column(modifier = Modifier.padding(12.dp)) {
          Text("منحنى حركة مبيعات الأسبوع المنصرم ", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
          Text("المخطط يوضح المبيعات بالشيكل على مدار الأيام الستة الماضية واليوم", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
          
          Spacer(modifier = Modifier.height(16.dp))
          
          Box(
          modifier = Modifier
          .fillMaxWidth()
          .height(160.dp)
          .padding(horizontal = 8.dp)
            ) {
            val salesData = listOf(1400f, 2100f, 1800f, 3200f, 2900f, 3800f, todaySales.toFloat())
            val days = listOf("الخميس", "الجمعة", "السبت", "الأحد", "الإثنين", "الثلاثاء", "اليوم")
            val primaryColor = MaterialTheme.colorScheme.primary
            
            Canvas(modifier = Modifier.fillMaxSize()) {
              val width = size.width
              val height = size.height
              val spacing = width / (salesData.size - 1)
              val maxVal = (salesData.maxOrNull() ?: 5000f).coerceAtLeast(1000f)
              
              val gridCount = 4
                for (i in 0..gridCount) {
                val y = height - (height / gridCount * i)
                drawLine(
                color = Color.LightGray.copy(alpha = 0.4f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
                )
              }
              
                val points = salesData.mapIndexed { index, value ->
                val x = index * spacing
                val normalizedY = (value / maxVal) * (height - 40f)
                val y = height - normalizedY - 20f
                Offset(x, y)
              }
              
                for (i in 0 until points.size - 1) {
                drawLine(
                color = primaryColor,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 5f,
                cap = StrokeCap.Round
                )
              }
              
                points.forEach { pt ->
                drawCircle(
                color = primaryColor,
                radius = 6f,
                center = pt
                )
                drawCircle(
                color = Color.White,
                radius = 3f,
                center = pt
                )
              }
            }
            
            Row(
            modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
              ) {
                days.forEach { day ->
                Text(day, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
              }
            }
          }
        }
      }
      
      Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(2.dp)
        ) {
        
          Column(modifier = Modifier.padding(12.dp)) {
          Text("الأكثر مبيعاً ومستويات المخزون ", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
          
            products.take(4).forEach { prod ->
            val percent = (prod.quantity.toFloat() / 150f).coerceAtMost(1.0f)
            
              Column(modifier = Modifier.padding(vertical = 6.dp)) {
              Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
                ) {
                Text(prod.nameAr, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("الكمية المتاحة: ${prod.quantity}", fontSize = 12.sp)
              }
              Spacer(modifier = Modifier.height(4.dp))
              LinearProgressIndicator(
              progress = { percent },
              color = if (prod.quantity <= prod.minQuantityAlert) CoralError else MaterialTheme.colorScheme.primary,
              trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
              modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp))
              )
            }
          }
        }
      }
    }
  }
}
  
  // ==========================================
  // 3. INVENTORY MANAGEMENT SCREEN
  // ==========================================
  @Composable
  fun InventoryScreen(
    viewModel: AppViewModel,
    products: List<Product>,
    categories: List<Category>,
    suppliers: List<Supplier>
  ) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showOnlyLowStock by remember { mutableStateOf(false) }
    var inventorySearchQuery by remember { mutableStateOf("") }
    var inventorySelectedCategory by remember { mutableStateOf<String?>(null) }
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        val data = result.data
        val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        if (!matches.isNullOrEmpty()) {
          inventorySearchQuery = matches[0]
        }
      }
    }
    var selectedProductForMovement by remember { mutableStateOf<Product?>(null) }
    val previewFile by viewModel.pdfPreviewFile.collectAsState()

    var productToUpdateImage by remember { mutableStateOf<com.example.data.Product?>(null) }
    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
      contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
      if (uri != null) {
        try {
          context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
          )
        } catch (e: Exception) {
          e.printStackTrace()
        }
        productToUpdateImage?.let { product ->
          viewModel.updateProduct(product.copy(imageResName = uri.toString()), null, 0)
          viewModel.showToast("تم تحديث صورة المنتج")
          productToUpdateImage = null
        }
      } else {
         productToUpdateImage = null
      }
    }

    val lowStockProducts = remember(products) { products.filter { it.quantity <= it.minQuantityAlert } }
    val filteredProductsList = remember(products, inventorySearchQuery, showOnlyLowStock, inventorySelectedCategory) {
      products.filter { prod ->
        val matchesQuery = inventorySearchQuery.isBlank() ||
            prod.nameAr.contains(inventorySearchQuery, ignoreCase = true) ||
            prod.barcode.contains(inventorySearchQuery) ||
            prod.category.contains(inventorySearchQuery, ignoreCase = true)
        val matchesLowStock = if (showOnlyLowStock) prod.quantity <= prod.minQuantityAlert else true
        val matchesCategory = inventorySelectedCategory == null || prod.category == inventorySelectedCategory
        matchesQuery && matchesLowStock && matchesCategory
      }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val isCompact = maxWidth < 600.dp

      Column(
        modifier = Modifier
          .fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)
      ) {
        if (isCompact) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text("إدارة المخازن والمستودع ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = { showCategoryDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.Category, contentDescription = "تصنيفات", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إدارة الأقسام", fontSize = 11.sp, maxLines = 1)
              }

              Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("منتج جديد", fontSize = 11.sp, maxLines = 1)
              }
            }
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("إدارة المخازن والمستودع ", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = { showCategoryDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
              ) {
                Icon(Icons.Default.Category, contentDescription = "تصنيفات", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إدارة الأقسام", fontSize = 12.sp)
              }

              Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
              ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("منتج جديد", fontSize = 12.sp)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Low Stock Alert Banner inside Inventory
        if (lowStockProducts.isNotEmpty()) {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CoralError.copy(alpha = 0.1f)),
            border = BorderStroke(1.dp, CoralError.copy(alpha = 0.35f))
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = Icons.Default.Warning,
                  contentDescription = null,
                  tint = CoralError,
                  modifier = Modifier.size(24.dp)
                )
                Column {
                  Text(
                    text = "تنبيه: وصول ${lowStockProducts.size} منتجات لمستوى 'حد الطلب' المنخفض! ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = CoralError
                  )
                  Text(
                    text = "يرجى مراجعة الكميات المتبقية وتجهيز طلبات توريد جديدة لتجنب نفاد المخزون.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }
              Spacer(modifier = Modifier.width(8.dp))
              FilterChip(
                selected = showOnlyLowStock,
                onClick = { showOnlyLowStock = !showOnlyLowStock },
                label = {
                  Text(
                    if (showOnlyLowStock) "عرض الكل" else "تصفية المنخفض (${lowStockProducts.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                },
                leadingIcon = {
                  Icon(
                    Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = CoralError,
                  selectedLabelColor = MaterialTheme.colorScheme.surface
                )
              )
            }
          }
        }

        // Inventory Search & Filter Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = inventorySearchQuery,
            onValueChange = { inventorySearchQuery = it },
            placeholder = { Text("بحث باسم المنتج أو الباركود...", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                if (inventorySearchQuery.isNotEmpty()) {
                  IconButton(onClick = { inventorySearchQuery = "" }) {
                    Icon(Icons.Default.Clear, contentDescription = "مسح", modifier = Modifier.size(18.dp))
                  }
                }
                IconButton(onClick = {
                  try {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                      putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                      putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                      putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث للبحث عن منتج...")
                    }
                    speechRecognizerLauncher.launch(intent)
                  } catch (e: Exception) {
                    viewModel.showToast("عذراً، ميزة البحث الصوتي غير مدعومة في جهازك")
                  }
                }) {
                  Icon(Icons.Default.Mic, contentDescription = "بحث صوتي", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          )

          FilterChip(
            selected = showOnlyLowStock,
            onClick = { showOnlyLowStock = !showOnlyLowStock },
            label = { Text("حد الطلب (${lowStockProducts.size}) ", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = CoralError,
              selectedLabelColor = MaterialTheme.colorScheme.surface
            )
          )
        }

        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
          item {
            FilterChip(
              selected = inventorySelectedCategory == null,
              onClick = { inventorySelectedCategory = null },
              label = { Text("الكل", fontSize = 11.sp) },
              shape = RoundedCornerShape(20.dp)
            )
          }
          items(categories) { cat ->
            FilterChip(
              selected = inventorySelectedCategory == cat.nameAr,
              onClick = { inventorySelectedCategory = cat.nameAr },
              label = { Text(cat.nameAr, fontSize = 11.sp) },
              shape = RoundedCornerShape(20.dp)
            )
          }
        }

        Card(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(2.dp)
        ) {
          if (isCompact) {
            if (filteredProductsList.isEmpty()) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                  if (showOnlyLowStock) "لا توجد منتجات وصلت حد الطلب المنخفض حالياً " else "لا توجد منتجات مطابقة للبحث.",
                  color = Color.Gray,
                  fontSize = 13.sp
                )
              }
            } else {
              LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                items(filteredProductsList) { prod ->
                  val isLowStock = prod.quantity <= prod.minQuantityAlert
                  Card(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clickable { productToEdit = prod },
                    colors = CardDefaults.cardColors(containerColor = if (isLowStock) CoralError.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, if (isLowStock) CoralError.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                  ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(prod.nameAr, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            if (isLowStock) {
                              Surface(
                                color = CoralError,
                                shape = RoundedCornerShape(6.dp)
                              ) {
                                Text(" حد الطلب", color = MaterialTheme.colorScheme.onPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                              }
                            }
                          }
                          Text("باركود: ${prod.barcode} • حد التنبيه: ${prod.minQuantityAlert}", fontSize = 10.sp, color = Color.Gray)
                        }
                        Surface(
                          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                          contentColor = MaterialTheme.colorScheme.primary,
                          shape = RoundedCornerShape(12.dp)
                        ) {
                          Text(
                            prod.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }
                      }

                      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Column {
                          Text("سعر الشراء: ${formatMoney(prod.pricePurchase)} ", fontSize = 11.sp, color = Color.Gray)
                          Text("سعر البيع: ${formatMoney(prod.priceSale)} ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        Surface(
                          color = if (isLowStock) CoralError.copy(alpha = 0.15f) else EmeraldGreen.copy(alpha = 0.1f),
                          contentColor = if (isLowStock) CoralError else EmeraldGreen,
                          shape = RoundedCornerShape(12.dp)
                        ) {
                          Text(
                            text = if (isLowStock) " المخزون: ${prod.quantity} ${prod.unit} (منخفض!)" else "المخزون: ${prod.quantity} ${prod.unit}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                          )
                        }
                      }

                      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                          OutlinedButton(
                            onClick = { selectedProductForMovement = prod },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                          ) {
                            Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حركة المادة ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                          }
                          OutlinedButton(
                            onClick = { 
                              productToUpdateImage = prod
                              imagePickerLauncher.launch("image/*")
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                          ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                          }
                        }

                        Row {
                          IconButton(onClick = { productToEdit = prod }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary)
                          }
                          Spacer(modifier = Modifier.width(4.dp))
                          IconButton(onClick = { viewModel.deleteProduct(prod) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = CoralError)
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          } else {
            Column(modifier = Modifier.fillMaxSize()) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("المنتج / الباركود", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
                Text("القسم", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.8f))
                Text("الشراء/البيع", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("المخزون / حد الطلب", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                Text("الإجراء", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
              }

              if (filteredProductsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  Text(
                    if (showOnlyLowStock) "لا توجد منتجات وصلت حد الطلب المنخفض حالياً " else "لا توجد منتجات مطابقة للبحث.",
                    color = Color.Gray
                  )
                }
              } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                  items(filteredProductsList) { prod ->
                    val isLowStock = prod.quantity <= prod.minQuantityAlert
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .background(if (isLowStock) CoralError.copy(alpha = 0.05f) else Color.Transparent)
                        .clickable { productToEdit = prod }
                        .padding(12.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column(modifier = Modifier.weight(1.5f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                          Text(prod.nameAr, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                          if (isLowStock) {
                            Surface(
                              color = CoralError,
                              shape = RoundedCornerShape(4.dp)
                            ) {
                              Text(" حد الطلب", color = MaterialTheme.colorScheme.onPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                          }
                        }
                        Text("باركود: ${prod.barcode}", fontSize = 10.sp, color = Color.Gray)
                      }

                      Text(prod.category, fontSize = 11.sp, modifier = Modifier.weight(0.8f))

                      Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("شراء: ${formatMoney(prod.pricePurchase)}", fontSize = 10.sp, color = Color.Gray)
                        Text("بيع: ${formatMoney(prod.priceSale)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                      }

                      Box(
                        modifier = Modifier.weight(1.1f),
                        contentAlignment = Alignment.Center
                      ) {
                        Surface(
                          color = if (isLowStock) CoralError.copy(alpha = 0.15f) else EmeraldGreen.copy(alpha = 0.1f),
                          contentColor = if (isLowStock) CoralError else EmeraldGreen,
                          shape = RoundedCornerShape(12.dp)
                        ) {
                          Text(
                            text = "${prod.quantity} ${prod.unit} (حد التنبيه: ${prod.minQuantityAlert})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                          )
                        }
                      }
                      
                      Row(
                      modifier = Modifier.weight(0.7f),
                      horizontalArrangement = Arrangement.End
                        ) {
                          IconButton(onClick = { productToEdit = prod }, modifier = Modifier.size(28.dp)) {
                          Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                          IconButton(onClick = { 
                            productToUpdateImage = prod
                            imagePickerLauncher.launch("image/*")
                          }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Image, contentDescription = "صورة", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                          }
                          IconButton(onClick = { selectedProductForMovement = prod }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Assessment, contentDescription = "حركة المادة", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                          }
                          IconButton(onClick = { viewModel.deleteProduct(prod) }, modifier = Modifier.size(28.dp)) {
                          Icon(Icons.Default.Delete, contentDescription = "حذف", tint = CoralError, modifier = Modifier.size(18.dp))
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
      
      // --- Add Product Dialog ---
      if (showAddDialog) {
        var nameAr by remember { mutableStateOf("") }
        var nameEn by remember { mutableStateOf("") }
        var barcode by remember { mutableStateOf("") }
        var purchasePrice by remember { mutableStateOf("") }
        var salePrice by remember { mutableStateOf("") }
        var quantity by remember { mutableStateOf("") }
        var alertLevel by remember { mutableStateOf("") }
        val unit = "قطعة"
        var selectedCat by remember { mutableStateOf(categories.firstOrNull()?.nameAr ?: "مستلزمات عامة") }
        var selectedSupplier by remember { mutableStateOf<Supplier?>(null) }
        var expandedSupplier by remember { mutableStateOf(false) }
        var imageResName by remember { mutableStateOf("") }

        val productImgPickerLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
          uri?.let {
            imageResName = saveImageToInternalStorage(context, it)
          }
        }

        val purchaseVal = purchasePrice.toDoubleOrNull() ?: 0.0
        val saleVal = salePrice.toDoubleOrNull() ?: 0.0
        val qtyVal = quantity.toIntOrNull() ?: 0
        val profit = saleVal - purchaseVal
        val profitMarginPercent = if (purchaseVal > 0.0) (profit / purchaseVal) * 100.0 else 0.0
        val totalStockValue = purchaseVal * qtyVal

        AlertDialog(
          onDismissRequest = { showAddDialog = false },
          title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.AddBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("إضافة منتج جديد للمستودع ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
          },
          text = {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Text("أدخل تفاصيل المنتج بدقة لتحديث قاعدة المخزون:", fontSize = 11.sp, color = Color.Gray)

              // Product Image Section
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                  .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("صورة المنتج ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                  if (imageResName.isNotEmpty()) {
                    TextButton(onClick = { imageResName = "" }) {
                      Text("حذف الصورة ", color = CoralError, fontSize = 11.sp)
                    }
                  }
                }
                
                if (imageResName.isNotEmpty()) {
                  Box(
                    modifier = Modifier
                      .size(110.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .background(MaterialTheme.colorScheme.surface)
                      .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                      .clickable { productImgPickerLauncher.launch("image/*") }
                  ) {
                    coil.compose.AsyncImage(
                      model = imageResName,
                      contentDescription = "صورة المنتج المعاينة",
                      modifier = Modifier.fillMaxSize(),
                      contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                  }
                } else {
                  Box(
                    modifier = Modifier
                      .size(110.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                      .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                      .clickable { productImgPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                  ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                      Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                      Spacer(modifier = Modifier.height(2.dp))
                      Text("انقر لرفع صورة", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }
                  }
                }

                Button(
                  onClick = { productImgPickerLauncher.launch("image/*") },
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), contentColor = MaterialTheme.colorScheme.primary),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    if (imageResName.isEmpty()) "اختيار صورة للمنتج من معرض الجهاز " else "تغيير صورة المنتج ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }

              OutlinedTextField(
                value = nameAr,
                onValueChange = { nameAr = it },
                label = { Text("اسم المنتج *", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("product_name_input")
              )

              OutlinedTextField(
                value = nameEn,
                onValueChange = { nameEn = it },
                label = { Text("الاسم بالإنجليزي (اختياري)", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Default.Create, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("product_name_en_input")
              )

              // Barcode Input with Auto-Generate option & Camera Scan
              var showCameraForBarcode by remember { mutableStateOf(false) }
              if (showCameraForBarcode) {
                BarcodeScannerDialog(
                  onDismissRequest = { showCameraForBarcode = false },
                  onBarcodeScanned = { scannedCode ->
                    barcode = scannedCode
                    showCameraForBarcode = false
                    viewModel.showToast("تم إلتقاط الباركود: $scannedCode ")
                  },
                  titleText = "مسح باركود المنتج بالكاميرا ",
                  keepOpenOnScan = false
                )
              }

              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                  value = barcode,
                  onValueChange = { barcode = it },
                  label = { Text("الباركود") },
                  leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp)) },
                  trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      IconButton(
                        onClick = { showCameraForBarcode = true },
                        modifier = Modifier.testTag("scan_product_barcode_camera_btn")
                      ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "مسح بالكاميرا", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                      }
                      IconButton(
                        onClick = {
                          val randDigits = (1..9).map { (0..9).random() }.joinToString("")
                          barcode = "628$randDigits"
                        }
                      ) {
                        Icon(Icons.Default.Autorenew, contentDescription = "توليد", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                      }
                    }
                  },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().testTag("barcode_input")
                )
              }

              // Category Selector Chips Row + Dropdown
              var expandedCat by remember { mutableStateOf(false) }
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("التصنيف / القسم:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                LazyRow(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  items(categories.take(4)) { cat ->
                    val isSelected = selectedCat == cat.nameAr
                    FilterChip(
                      selected = isSelected,
                      onClick = { selectedCat = cat.nameAr },
                      label = { Text(cat.nameAr, fontSize = 11.sp) },
                      leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                      } else null
                    )
                  }
                  if (categories.size > 4) {
                    item {
                      FilterChip(
                        selected = !categories.take(4).any { it.nameAr == selectedCat },
                        onClick = { expandedCat = true },
                        label = { Row(verticalAlignment = Alignment.CenterVertically) {
                          Text("أقسام أخرى ", fontSize = 11.sp)
                        }},
                      )
                    }
                  }
                }

                if (expandedCat) {
                  DropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                    categories.forEach { cat ->
                      DropdownMenuItem(
                        text = { Text(cat.nameAr) },
                        onClick = {
                          selectedCat = cat.nameAr
                          expandedCat = false
                        }
                      )
                    }
                  }
                }
              }

              // Supplier Selection
              Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                OutlinedTextField(
                  value = selectedSupplier?.name ?: "",
                  onValueChange = { },
                  label = { Text("المورد (اختياري)") },
                  readOnly = true,
                  modifier = Modifier.fillMaxWidth(),
                  trailingIcon = {
                    IconButton(onClick = { expandedSupplier = true }) {
                      Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                  }
                )
                DropdownMenu(expanded = expandedSupplier, onDismissRequest = { expandedSupplier = false }) {
                  suppliers.forEach { sup ->
                    DropdownMenuItem(
                      text = { Text(sup.name) },
                      onClick = {
                        selectedSupplier = sup
                        expandedSupplier = false
                      }
                    )
                  }
                  DropdownMenuItem(
                    text = { Text("بدون مورد", color = Color.Gray) },
                    onClick = {
                      selectedSupplier = null
                      expandedSupplier = false
                    }
                  )
                }
              }

              // Prices Row
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedTextField(
                  value = purchasePrice,
                  onValueChange = { purchasePrice = it },
                  label = { Text("سعر الشراء", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                  leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp)) },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  modifier = Modifier.weight(1f).testTag("purchase_price_input")
                )
                OutlinedTextField(
                  value = salePrice,
                  onValueChange = { salePrice = it },
                  label = { Text("سعر البيع", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                  leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  modifier = Modifier.weight(1f).testTag("sale_price_input")
                )
              }

              // Quantity & Alarm Level
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedTextField(
                  value = quantity,
                  onValueChange = { quantity = it },
                  label = { Text("الكمية", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                  leadingIcon = { Icon(Icons.Default.Inventory, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  modifier = Modifier.weight(1f).testTag("quantity_input")
                )
                OutlinedTextField(
                  value = alertLevel,
                  onValueChange = { alertLevel = it },
                  label = { Text("حد التنبيه", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                  leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = CoralError.copy(alpha = 0.7f), modifier = Modifier.size(18.dp)) },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  modifier = Modifier.weight(1f).testTag("alert_level_input")
                )
              }

              // Real-time Profitability Display
              if (purchaseVal > 0.0 || saleVal > 0.0) {
                Spacer(modifier = Modifier.height(4.dp))
                if (profit < 0.0) {
                  Card(
                    colors = CardDefaults.cardColors(containerColor = CoralError.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier.padding(10.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(Icons.Default.Warning, contentDescription = null, tint = CoralError, modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = "تنبيه: سعر البيع أقل من سعر الشراء! خسارة ${formatMoney(-profit)} للحبة.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoralError
                      )
                    }
                  }
                } else if (profit > 0.0) {
                  Card(
                    colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          text = "الأرباح والمخزون المتوقع:",
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Bold,
                          color = EmeraldGreen
                        )
                      }
                      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("صافي ربح القطعة:", fontSize = 11.sp, color = Color.Gray)
                        Text("${formatMoney(profit)} (هامش ربح ${String.format("%.1f", profitMarginPercent)}%)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                      }
                      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إجمالي قيمة البضاعة المضافة:", fontSize = 11.sp, color = Color.Gray)
                        Text(formatMoney(totalStockValue), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                      }
                    }
                  }
                }
              }
            }
          },
          confirmButton = {
            val isEnabled = nameAr.isNotBlank() && purchasePrice.isNotBlank() && salePrice.isNotBlank() && quantity.isNotBlank()
            Button(
              onClick = {
                viewModel.addProduct(
                  Product(
                    nameAr = nameAr,
                    nameEn = nameEn,
                    barcode = barcode,
                    pricePurchase = purchasePrice.toDoubleOrNull() ?: 0.0,
                    priceSale = salePrice.toDoubleOrNull() ?: 0.0,
                    quantity = qtyVal,
                    unit = unit,
                    minQuantityAlert = alertLevel.toIntOrNull() ?: 5,
                    category = selectedCat,
                    imageResName = imageResName
                  ),
                  supplier = selectedSupplier,
                  quantityAdded = qtyVal
                )
                showAddDialog = false
              },
              enabled = isEnabled,
              modifier = Modifier.testTag("save_product_button")
            ) {
              Text("حفظ المنتج ")
            }
          },
          dismissButton = {
            TextButton(
              onClick = { showAddDialog = false },
              modifier = Modifier.testTag("cancel_product_button")
            ) {
              Text("إلغاء", color = CoralError)
            }
          }
        )
      }

      // --- Edit Product Dialog ---
      productToEdit?.let { prod ->
        var nameAr by remember { mutableStateOf(prod.nameAr) }
        var nameEn by remember { mutableStateOf(prod.nameEn) }
        var barcode by remember { mutableStateOf(prod.barcode) }
        var purchasePrice by remember { mutableStateOf(prod.pricePurchase.toString()) }
        var salePrice by remember { mutableStateOf(prod.priceSale.toString()) }
        var quantity by remember { mutableStateOf(prod.quantity.toString()) }
        var alertLevel by remember { mutableStateOf(prod.minQuantityAlert.toString()) }
        var selectedCat by remember { mutableStateOf(prod.category) }
        var selectedSupplier by remember { mutableStateOf<Supplier?>(null) }
        var expandedSupplier by remember { mutableStateOf(false) }
        var imageResName by remember { mutableStateOf(prod.imageResName) }

        val editProductImgPickerLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
          uri?.let {
            imageResName = saveImageToInternalStorage(context, it)
          }
        }

        val purchaseVal = purchasePrice.toDoubleOrNull() ?: 0.0
        val saleVal = salePrice.toDoubleOrNull() ?: 0.0
        val qtyVal = quantity.toIntOrNull() ?: 0
        val profit = saleVal - purchaseVal
        val profitMarginPercent = if (purchaseVal > 0.0) (profit / purchaseVal) * 100.0 else 0.0
        val totalStockValue = purchaseVal * qtyVal

        AlertDialog(
          onDismissRequest = { productToEdit = null },
          title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("تعديل بيانات المنتج ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
          },
          text = {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Product Image Section
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                  .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("صورة المنتج ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                  if (imageResName.isNotEmpty()) {
                    TextButton(onClick = { imageResName = "" }) {
                      Text("حذف الصورة ", color = CoralError, fontSize = 11.sp)
                    }
                  }
                }
                
                if (imageResName.isNotEmpty()) {
                  Box(
                    modifier = Modifier
                      .size(110.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .background(MaterialTheme.colorScheme.surface)
                      .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                      .clickable { editProductImgPickerLauncher.launch("image/*") }
                  ) {
                    coil.compose.AsyncImage(
                      model = imageResName,
                      contentDescription = "صورة المنتج المعاينة",
                      modifier = Modifier.fillMaxSize(),
                      contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                  }
                } else {
                  Box(
                    modifier = Modifier
                      .size(110.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                      .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                      .clickable { editProductImgPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                  ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                      Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                      Spacer(modifier = Modifier.height(2.dp))
                      Text("انقر لرفع صورة", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }
                  }
                }

                Button(
                  onClick = { editProductImgPickerLauncher.launch("image/*") },
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), contentColor = MaterialTheme.colorScheme.primary),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    if (imageResName.isEmpty()) "اختيار صورة للمنتج من معرض الجهاز " else "تغيير صورة المنتج ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }

              OutlinedTextField(
                value = nameAr,
                onValueChange = { nameAr = it },
                label = { Text("اسم المنتج *", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("edit_product_name_input")
              )

              OutlinedTextField(
                value = nameEn,
                onValueChange = { nameEn = it },
                label = { Text("الاسم بالإنجليزي (اختياري)", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Default.Create, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("edit_product_name_en_input")
              )

              // Barcode
              var showCameraForEditBarcode by remember { mutableStateOf(false) }
              if (showCameraForEditBarcode) {
                BarcodeScannerDialog(
                  onDismissRequest = { showCameraForEditBarcode = false },
                  onBarcodeScanned = { scannedCode ->
                    barcode = scannedCode
                    showCameraForEditBarcode = false
                    viewModel.showToast("تم تحديث الباركود: $scannedCode ")
                  },
                  titleText = "تحديث باركود المنتج بالكاميرا ",
                  keepOpenOnScan = false
                )
              }

              OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = it },
                label = { Text("الباركود") },
                leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                      onClick = { showCameraForEditBarcode = true },
                      modifier = Modifier.testTag("edit_scan_barcode_camera_btn")
                    ) {
                      Icon(Icons.Default.PhotoCamera, contentDescription = "مسح بالكاميرا", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    TextButton(
                      onClick = {
                        val randDigits = (1..9).map { (0..9).random() }.joinToString("")
                        barcode = "628$randDigits"
                      }
                    ) {
                      Text("توليد ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("edit_barcode_input")
              )

              // Category Selector Chips Row + Dropdown
              var expandedCat by remember { mutableStateOf(false) }
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("التصنيف / القسم:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                LazyRow(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  items(categories.take(4)) { cat ->
                    val isSelected = selectedCat == cat.nameAr
                    FilterChip(
                      selected = isSelected,
                      onClick = { selectedCat = cat.nameAr },
                      label = { Text(cat.nameAr, fontSize = 11.sp) },
                      leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                      } else null
                    )
                  }
                  if (categories.size > 4) {
                    item {
                      FilterChip(
                        selected = !categories.take(4).any { it.nameAr == selectedCat },
                        onClick = { expandedCat = true },
                        label = { Row(verticalAlignment = Alignment.CenterVertically) {
                          Text("أقسام أخرى ", fontSize = 11.sp)
                        }},
                      )
                    }
                  }
                }

                if (expandedCat) {
                  DropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                    categories.forEach { cat ->
                      DropdownMenuItem(
                        text = { Text(cat.nameAr) },
                        onClick = {
                          selectedCat = cat.nameAr
                          expandedCat = false
                        }
                      )
                    }
                  }
                }
              }

              // Supplier Selection
              Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                OutlinedTextField(
                  value = selectedSupplier?.name ?: "",
                  onValueChange = { },
                  label = { Text("المورد (اختياري)") },
                  readOnly = true,
                  modifier = Modifier.fillMaxWidth(),
                  trailingIcon = {
                    IconButton(onClick = { expandedSupplier = true }) {
                      Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                  }
                )
                DropdownMenu(expanded = expandedSupplier, onDismissRequest = { expandedSupplier = false }) {
                  suppliers.forEach { sup ->
                    DropdownMenuItem(
                      text = { Text(sup.name) },
                      onClick = {
                        selectedSupplier = sup
                        expandedSupplier = false
                      }
                    )
                  }
                  DropdownMenuItem(
                    text = { Text("بدون مورد", color = Color.Gray) },
                    onClick = {
                      selectedSupplier = null
                      expandedSupplier = false
                    }
                  )
                }
              }

              // Prices Row
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedTextField(
                  value = purchasePrice,
                  onValueChange = { purchasePrice = it },
                  label = { Text("سعر الشراء", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                  leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp)) },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  modifier = Modifier.weight(1f).testTag("edit_purchase_price_input")
                )
                OutlinedTextField(
                  value = salePrice,
                  onValueChange = { salePrice = it },
                  label = { Text("سعر البيع", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                  leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  modifier = Modifier.weight(1f).testTag("edit_sale_price_input")
                )
              }

              // Quantity & Alarm Level
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedTextField(
                  value = quantity,
                  onValueChange = { quantity = it },
                  label = { Text("الكمية", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                  leadingIcon = { Icon(Icons.Default.Inventory, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  modifier = Modifier.weight(1f).testTag("edit_quantity_input")
                )
                OutlinedTextField(
                  value = alertLevel,
                  onValueChange = { alertLevel = it },
                  label = { Text("حد التنبيه", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                  leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = CoralError.copy(alpha = 0.7f), modifier = Modifier.size(18.dp)) },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  modifier = Modifier.weight(1f).testTag("edit_alert_level_input")
                )
              }

              // Real-time Profitability Display
              if (purchaseVal > 0.0 || saleVal > 0.0) {
                Spacer(modifier = Modifier.height(4.dp))
                if (profit < 0.0) {
                  Card(
                    colors = CardDefaults.cardColors(containerColor = CoralError.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier.padding(10.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(Icons.Default.Warning, contentDescription = null, tint = CoralError, modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = "تنبيه: سعر البيع أقل من سعر الشراء! خسارة ${formatMoney(-profit)} للحبة.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoralError
                      )
                    }
                  }
                } else if (profit > 0.0) {
                  Card(
                    colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          text = "الأرباح والمخزون المتوقع الحالي:",
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Bold,
                          color = EmeraldGreen
                        )
                      }
                      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("صافي ربح القطعة:", fontSize = 11.sp, color = Color.Gray)
                        Text("${formatMoney(profit)} (هامش ربح ${String.format("%.1f", profitMarginPercent)}%)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                      }
                      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إجمالي قيمة مخزون البضاعة:", fontSize = 11.sp, color = Color.Gray)
                        Text(formatMoney(totalStockValue), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                      }
                    }
                  }
                }
              }
            }
          },
          confirmButton = {
            val isEnabled = nameAr.isNotBlank() && purchasePrice.isNotBlank() && salePrice.isNotBlank() && quantity.isNotBlank()
            Button(
              onClick = {
                val newQty = quantity.toIntOrNull() ?: 0
                viewModel.updateProduct(
                  prod.copy(
                    nameAr = nameAr,
                    nameEn = nameEn,
                    barcode = barcode,
                    pricePurchase = purchasePrice.toDoubleOrNull() ?: 0.0,
                    priceSale = salePrice.toDoubleOrNull() ?: 0.0,
                    quantity = newQty,
                    minQuantityAlert = alertLevel.toIntOrNull() ?: 5,
                    category = selectedCat,
                    imageResName = imageResName
                  ),
                  supplier = selectedSupplier,
                  quantityAdded = newQty - prod.quantity
                )
                productToEdit = null
              },
              enabled = isEnabled,
              modifier = Modifier.testTag("edit_save_product_button")
            ) {
              Text("تعديل وحفظ ")
            }
          },
          dismissButton = {
            TextButton(
              onClick = { productToEdit = null },
              modifier = Modifier.testTag("edit_cancel_product_button")
            ) {
              Text("إلغاء", color = CoralError)
            }
          }
        )
      }
      
      // --- Category Management Dialog ---
        if (showCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        var newCatNameEn by remember { mutableStateOf("") }
        
        AlertDialog(
        onDismissRequest = { showCategoryDialog = false },
        title = { Text("إدارة أقسام وتصنيفات المنتجات ", fontWeight = FontWeight.Bold) },
          text = {
          
          Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            OutlinedTextField(
            value = newCatName,
            onValueChange = { newCatName = it },
            label = { Text("اسم القسم (عربي):") },
            singleLine = true
            )
            OutlinedTextField(
            value = newCatNameEn,
            onValueChange = { newCatNameEn = it },
            label = { Text("اسم القسم (إنجليزي):") },
            singleLine = true
            )
            Button(
              onClick = {
                if (newCatName.isNotEmpty()) {
                viewModel.addCategory(Category(nameAr = newCatName, nameEn = newCatNameEn))
                newCatName = ""
                newCatNameEn = ""
              }
            },
            modifier = Modifier.fillMaxWidth()
              ) {
              Text("إضافة قسم جديد")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("الأقسام الحالية:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
              LazyColumn(modifier = Modifier.height(140.dp)) {
                items(categories) { cat ->
                Row(
                modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
                  ) {
                  Text("${cat.nameAr} / ${cat.nameEn}", fontSize = 13.sp)
                    IconButton(onClick = { viewModel.deleteCategory(cat) }) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = CoralError)
                  }
                }
              }
            }
          }
        },
          confirmButton = {
            Button(onClick = { showCategoryDialog = false }) {
            Text("إغلاق")
          }
        }
        )
      }

      if (selectedProductForMovement != null) {
        ProductMovementDialog(
          viewModel = viewModel,
          product = selectedProductForMovement!!,
          onDismiss = { selectedProductForMovement = null }
        )
      }
    }
      if (previewFile != null) {
      androidx.compose.ui.window.Dialog(
        onDismissRequest = { viewModel.pdfPreviewFile.value = null },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
      ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          Column(modifier = Modifier.fillMaxSize()) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("معاينة الكشف", fontSize = 20.sp, fontWeight = FontWeight.Bold)
              IconButton(onClick = { viewModel.pdfPreviewFile.value = null }) {
                Icon(Icons.Default.Close, contentDescription = "Close")
              }
            }
            HorizontalDivider()
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
              PdfViewer(previewFile!!)
            }
          }
        }
      }
    }
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  fun ProductMovementDialog(
    viewModel: AppViewModel,
    product: Product,
    onDismiss: () -> Unit
  ) {
    val invoices by viewModel.allInvoices.collectAsState(initial = emptyList())
    val productInvoiceItems by viewModel.getInvoiceItemsForProduct(product.id).collectAsState(initial = emptyList())
    val customers by viewModel.allCustomers.collectAsState(initial = emptyList())
    val suppliers by viewModel.allSuppliers.collectAsState(initial = emptyList())
    val auditLogs by viewModel.getAuditLogsForProduct(product.nameAr).collectAsState(initial = emptyList())
    val supplierTransactions by viewModel.allSupplierTransactions.collectAsState(initial = emptyList())
    val ledgerEntries by viewModel.allLedgerEntries.collectAsState(initial = emptyList())
    val allAuditLogs by viewModel.allAuditLogs.collectAsState(initial = emptyList())

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar")) }

    var filterType by remember { mutableStateOf("الكل") }
    var selectedInvoiceForDetails by remember { mutableStateOf<com.example.data.Invoice?>(null) }

    val allMovements = remember(invoices, productInvoiceItems, customers, suppliers, auditLogs, supplierTransactions, ledgerEntries, allAuditLogs, product) {
      val list = mutableListOf<MovementRecord>()

      productInvoiceItems.forEach { item ->
        val inv = invoices.find { it.id == item.invoiceId }
        if (inv != null) {
          val isSupplierInv = isPurchaseInvoice(inv, supplierTransactions, ledgerEntries, allAuditLogs, customers, suppliers)
          val isReturn = inv.total < 0 || item.total < 0 || item.quantity < 0

          val partyName = if (isSupplierInv) {
            suppliers.find { it.id == inv.customerId }?.name ?: "مورد عام"
          } else {
            customers.find { it.id == inv.customerId }?.name ?: "زبون عام"
          }

          val absQty = kotlin.math.abs(item.quantity)
          val unitPrice = item.price

          val (mType, isIncoming) = when {
            isReturn && isSupplierInv -> Pair("مرتجع مشتريات ↪", false)
            isReturn -> Pair("مرتجع مبيعات ", true)
            isSupplierInv -> Pair("مشتريات ", true)
            else -> Pair("مبيعات ", false)
          }

          list.add(
            MovementRecord(
              id = "inv_${item.id}",
              date = inv.invoiceDate,
              type = mType,
              isIncoming = isIncoming,
              quantity = absQty,
              price = unitPrice,
              total = absQty * unitPrice,
              partyName = partyName,
              reference = "فاتورة #${inv.id} (${inv.paymentMethod})",
              invoiceId = inv.id
            )
          )
        }
      }

      auditLogs.forEach { log ->
        if (log.action.contains("إضافة منتج") || log.action.contains("تعديل منتج")) {
          if (log.details.contains("IMPORT|") || log.details.contains("كمية")) {
            val parts = log.details.split("|")
            val addedQty = if (parts.size >= 6) parts[5].toIntOrNull() ?: 0 else 0
            if (addedQty > 0) {
              list.add(
                MovementRecord(
                  id = "audit_${log.id}",
                  date = log.timestamp,
                  type = "توريد / تسوية مخزون ",
                  isIncoming = true,
                  quantity = addedQty,
                  price = product.pricePurchase,
                  total = addedQty * product.pricePurchase,
                  partyName = if (log.employeeName.isNotEmpty()) log.employeeName else "النظام",
                  reference = "سجل تعديل مخزون"
                )
              )
            }
          }
        }
      }

      list.sortedByDescending { it.date }
    }

    val filteredMovements = remember(allMovements, filterType) {
      when (filterType) {
        "وارد " -> allMovements.filter { it.isIncoming }
        "صادر " -> allMovements.filter { !it.isIncoming }
        "مبيعات " -> allMovements.filter { it.type.contains("مبيعات") }
        "مشتريات " -> allMovements.filter { it.type.contains("مشتريات") }
        "مرتجعات " -> allMovements.filter { it.type.contains("مرتجع") }
        else -> allMovements
      }
    }

    val totalIncomingQty = remember(allMovements) { allMovements.filter { it.isIncoming }.sumOf { it.quantity } }
    val totalOutgoingQty = remember(allMovements) { allMovements.filter { !it.isIncoming }.sumOf { it.quantity } }
    val netMovementQty = totalIncomingQty - totalOutgoingQty

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
      Card(
        modifier = Modifier
          .fillMaxWidth(0.96f)
          .fillMaxHeight(0.88f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(8.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 24.dp, bottom = 12.dp)
        ) {
          // Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  "حركة المادة (كشف حساب أصناف)",
                  fontWeight = FontWeight.Bold,
                  fontSize = 16.sp,
                  color = MaterialTheme.colorScheme.primary
                )
              }
              Text(
                "${product.nameAr} • باركود: ${product.barcode.ifEmpty { "بدون" }} • القسم: ${product.category}",
                fontSize = 12.sp,
                color = Color.Gray
              )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              val context = androidx.compose.ui.platform.LocalContext.current
              
              OutlinedButton(
                onClick = { viewModel.exportItemMovementToPdf(context, product, allMovements, netMovementQty, isPreview = true) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
              ) {
                Text("معاينة", fontSize = 11.sp)
              }
              
              Button(
                onClick = { viewModel.exportItemMovementToPdf(context, product, allMovements, netMovementQty) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
              ) {
                Text("PDF", fontSize = 11.sp)
              }
              
              IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "إغلاق", modifier = Modifier.size(20.dp))
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Summary Chips Row
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              // Total Incoming Card
              Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(12.dp)
              ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("إجمالي الوارد", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text("+$totalIncomingQty ${product.unit}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
              }

              // Total Outgoing Card
              Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp)
              ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("إجمالي الصادر", fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text("-$totalOutgoingQty ${product.unit}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                }
              }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              // Current Stock Card
              Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
              ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("المخزون الحالي", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text("${product.quantity} ${product.unit}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }

              // Net Movement Card
              Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
              ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("رصيد الحركة", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text("${if (netMovementQty >= 0) "+$netMovementQty" else "$netMovementQty"} ${product.unit}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(10.dp))

          // Filters Bar
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            val filterOptions = listOf("الكل", "وارد", "صادر", "مبيعات", "مشتريات", "مرتجعات")
            items(filterOptions) { opt ->
              FilterChip(
                selected = filterType == opt,
                onClick = { filterType = opt },
                label = { Text(opt, fontSize = 11.sp, fontWeight = if (filterType == opt) FontWeight.Bold else FontWeight.Normal) },
                shape = RoundedCornerShape(20.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Movements List Header
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
              .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("المرجع", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("التاريخ والطرف", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
            Text("الكمية والسعر", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
          }

          Spacer(modifier = Modifier.height(4.dp))

          if (filteredMovements.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
              Text("لا توجد حركات مسجلة لهذا المنتج حتى الآن.", color = Color.Gray, fontSize = 12.sp)
            }
          } else {
            LazyColumn(
              modifier = Modifier.weight(1f).fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              items(filteredMovements) { move ->
                val bg = if (move.isIncoming) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
                val borderColor = if (move.isIncoming) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                val textColor = if (move.isIncoming) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer

                Card(
                  modifier = Modifier.fillMaxWidth().clickable(enabled = move.invoiceId != null) {
                    move.invoiceId?.let { id ->
                      invoices.find { it.id == id }?.let { inv ->
                        selectedInvoiceForDetails = inv
                      }
                    }
                  },
                  shape = RoundedCornerShape(10.dp),
                  colors = CardDefaults.cardColors(containerColor = bg),
                  border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f))
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    // Column 1: Type & Reference
                    Column(modifier = Modifier.weight(1.5f)) {
                      Text(
                        move.type,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = textColor
                      )
                      Text(
                        move.reference,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }

                    // Column 2: Party & Date
                    Column(modifier = Modifier.weight(1.5f)) {
                      Text(
                        move.partyName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                      )
                      Text(
                        dateFormat.format(java.util.Date(move.date)),
                        fontSize = 10.sp,
                        color = Color.Gray
                      )
                    }

                    // Column 3: Quantity & Price
                    Column(
                      modifier = Modifier.weight(1.2f),
                      horizontalAlignment = Alignment.End
                    ) {
                      Text(
                        text = if (move.isIncoming) "+${move.quantity} ${product.unit}" else "-${move.quantity} ${product.unit}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = textColor
                      )
                      Text(
                        text = "@ ${formatMoney(move.price)} = ${formatMoney(move.total)}",
                        fontSize = 10.sp,
                        color = Color.Gray
                      )
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Footer Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              "إجمالي العمليات: ${filteredMovements.size}",
              fontSize = 12.sp,
              color = Color.Gray
            )

            Button(
              onClick = onDismiss,
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("إغلاق", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
    
    selectedInvoiceForDetails?.let { inv ->
      InvoiceDetailsDialog(
        inv = inv,
        customers = customers,
        viewModel = viewModel,
        onDismiss = { selectedInvoiceForDetails = null }
      )
    }
  }


  
  // ==========================================
  // 4. ACCOUNTING & LEDGER SCREEN
  // ==========================================
  @Composable
  fun AccountingScreen(
  viewModel: AppViewModel,
  ledgerEntries: List<LedgerEntry>
    ) {
    val balances = viewModel.calculateAccountBalances(ledgerEntries)
    val context = LocalContext.current
    var showJournalEntryDialog by remember { mutableStateOf(false) }
    var accountingTab by remember { mutableStateOf("journal") }
    
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val isCompact = maxWidth < 600.dp
      
      Column(
      modifier = Modifier
      .fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (isCompact) {
          
          Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            Text("الدفتر المحاسبي والتقارير ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Button(
              onClick = { showJournalEntryDialog = true },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.PostAdd, contentDescription = "إضافة")
              Spacer(modifier = Modifier.width(6.dp))
              Text("إنشاء سند قيد ", fontSize = 12.sp)
            }
          }
          } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("الدفتر المحاسبي والتقارير الختامية ", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            Button(
              onClick = { showJournalEntryDialog = true },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
              Icon(Icons.Default.PostAdd, contentDescription = "إضافة")
              Spacer(modifier = Modifier.width(6.dp))
              Text("إنشاء سند قيد ")
            }
          }
        }
        
        TabRow(
          selectedTabIndex = when (accountingTab) {
            "journal" -> 0
            "balances" -> 1
            else -> 2
          },
          containerColor = MaterialTheme.colorScheme.surface
        ) {
          Tab(
            selected = accountingTab == "journal",
            onClick = { accountingTab = "journal" },
            text = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isCompact) "اليومية" else "دفتر اليومية المزدوج", fontSize = 12.sp)
              }
            }
          )
          Tab(
            selected = accountingTab == "balances",
            onClick = { accountingTab = "balances" },
            text = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Scale, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isCompact) "الميزان" else "ميزان الحسابات", fontSize = 12.sp)
              }
            }
          )
          Tab(
            selected = accountingTab == "reports",
            onClick = { accountingTab = "reports" },
            text = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isCompact) "التقارير" else "قائمة الدخل والميزانية", fontSize = 12.sp)
              }
            }
          )
        }
        
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(2.dp)
        ) {
          Box(modifier = Modifier.fillMaxSize()) {
            when (accountingTab) {
              "journal" -> JournalBookTab(viewModel, ledgerEntries)
              "balances" -> TrialBalanceTab(balances)
              "reports" -> FinancialStatementsTab(balances)
            }
          }
        }
      }
    }
    
    if (showJournalEntryDialog) {
      CreateJournalVoucherDialog(
        viewModel = viewModel,
        onDismiss = { showJournalEntryDialog = false }
      )
    }
  }
  
  // ==========================================
  // 5. CRM CUSTOMER & SUPPLIER MANAGEMENT SCREEN
  // ==========================================
  @Composable
  fun CrmScreen(
    viewModel: AppViewModel,
    customers: List<Customer>,
    invoices: List<Invoice>
  ) {
    val allInvoiceItemsState by viewModel.allInvoiceItems.collectAsState(emptyList())
    val allInvoicesState by viewModel.allInvoices.collectAsState(emptyList())
    val context = androidx.compose.ui.platform.LocalContext.current
    var showPrintTypeDialogForCust by remember { mutableStateOf(false) }
    var pendingPrintActionForCust by remember { mutableStateOf("") }
    var showPrintTypeDialogForSup by remember { mutableStateOf(false) }
    var pendingPrintActionForSup by remember { mutableStateOf("") }
    var crmTab by remember { mutableStateOf("customers") } // "customers" or "suppliers"
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    var selectedCustomerForStatement by remember { mutableStateOf<Customer?>(null) }
    var selectedSupplierForStatement by remember { mutableStateOf<Supplier?>(null) }

    val suppliers by viewModel.allSuppliers.collectAsState(initial = emptyList())
    val supplierTransactions by viewModel.allSupplierTransactions.collectAsState(initial = emptyList())
    val customerTransactions by viewModel.allCustomerTransactions.collectAsState(initial = emptyList())

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val isCompact = maxWidth < 720.dp
      
      Column(
        modifier = Modifier
          .fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)
      ) {
        // Header with Tabs
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = if (crmTab == "customers") "إضافة عملاء وإدارة الحسابات " else "إضافة موردين وإدارة التوريد ",
              fontWeight = FontWeight.Bold,
              fontSize = if (isCompact) 16.sp else 20.sp,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = if (crmTab == "customers") "تسجيل العملاء ومتابعة أرصدتهم وكشوفات الحسابات" else "متابعة حسابات الموردين، عمليات الشراء، والتسديدات الآجلة",
              fontSize = 11.sp,
              color = Color.Gray
            )
          }
          
          // Action Buttons (Add Customer / Add Supplier)
          Button(
            onClick = {
              if (crmTab == "customers") showAddCustomerDialog = true else showAddSupplierDialog = true
            },
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(
              imageVector = if (crmTab == "customers") Icons.Default.PersonAdd else Icons.Default.Business,
              contentDescription = "إضافة"
            )
            if (!isCompact) {
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (crmTab == "customers") "تسجيل عميل جديد" else "إضافة مورد جديد")
            }
          }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Tabs selection Row
        TabRow(
          selectedTabIndex = if (crmTab == "customers") 0 else 1,
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
        ) {
          Tab(
            selected = crmTab == "customers",
            onClick = { crmTab = "customers"; searchQuery = "" },
            text = {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("العملاء (${customers.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }
            }
          )
          Tab(
            selected = crmTab == "suppliers",
            onClick = { crmTab = "suppliers"; searchQuery = "" },
            text = {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("الموردون والتوريد (${suppliers.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }
            }
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text(if (crmTab == "customers") "بحث باسم العميل أو الجوال..." else "بحث باسم المورد أو الجوال...", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(2.dp)
        ) {
          if (crmTab == "customers") {
            // Customers CRM Tab
            val filteredCustomers = customers.filter {
              it.name.contains(searchQuery, ignoreCase = true) ||
              it.phone.contains(searchQuery)
            }

            if (filteredCustomers.isEmpty()) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا يوجد عملاء مطبقين للبحث.", color = Color.Gray, fontSize = 13.sp)
              }
            } else {
              LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                items(filteredCustomers) { cust ->
                  Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                  ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                          Text("الجوال: ${cust.phone}", fontSize = 11.sp, color = Color.Gray)
                        }
                        Surface(
                          color = if (cust.type == "VIP") AmberAlert.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                          contentColor = if (cust.type == "VIP") AmberAlert else MaterialTheme.colorScheme.onSurfaceVariant,
                          shape = RoundedCornerShape(12.dp)
                        ) {
                          Text(
                            cust.type,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                          )
                        }
                      }
                      
                      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                      
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Column {
                          Text("البريد الإلكتروني:", fontSize = 10.sp, color = Color.Gray)
                          Text(cust.email.ifEmpty { "غير محدد" }, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        
                        Surface(
                          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                          contentColor = MaterialTheme.colorScheme.primary,
                          shape = RoundedCornerShape(12.dp)
                        ) {
                          Text(
                            "${cust.points} نقطة ولاء",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                          )
                        }
                      }
                      
                      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                      
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        // Statement of Account Button
                        Button(
                          onClick = { selectedCustomerForStatement = cust },
                          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary),
                          shape = RoundedCornerShape(8.dp),
                          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                          modifier = Modifier.height(32.dp)
                        ) {
                          Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                          Spacer(modifier = Modifier.width(4.dp))
                          Text("كشف الحساب التفصيلي ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(onClick = { viewModel.deleteCustomer(cust) }, modifier = Modifier.size(32.dp)) {
                          Icon(Icons.Default.Delete, contentDescription = "حذف", tint = CoralError)
                        }
                      }
                    }
                  }
                }
              }
            }
          } else {
            // Suppliers Tab Content
            val filteredSuppliers = suppliers.filter {
              it.name.contains(searchQuery, ignoreCase = true) ||
              it.companyName.contains(searchQuery, ignoreCase = true) ||
              it.phone.contains(searchQuery)
            }

            if (filteredSuppliers.isEmpty()) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا يوجد موردين مسجلين أو مطبقين للبحث حالياً.", color = Color.Gray, fontSize = 13.sp)
              }
            } else {
              LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                items(filteredSuppliers) { sup ->
                  Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                  ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Text(sup.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                          Text("الشركة: ${sup.companyName.ifEmpty { "غير حددّة" }}", fontSize = 11.sp, color = Color.Gray)
                        }
                        // Balance indicator
                        val bal = sup.balance
                        Surface(
                          color = when {
                            bal > 0 -> CoralError.copy(alpha = 0.15f)
                            bal < 0 -> EmeraldGreen.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                          },
                          contentColor = when {
                            bal > 0 -> CoralError
                            bal < 0 -> EmeraldGreen
                            else -> Color.Gray
                          },
                          shape = RoundedCornerShape(12.dp)
                        ) {
                          Text(
                            text = when {
                              bal > 0 -> "مستحق له: ${formatMoney(bal)} شيكل"
                              bal < 0 -> "مستحق لنا (مقدم): ${formatMoney(-bal)} شيكل"
                              else -> "متزن: 0.00 شيكل"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                          )
                        }
                      }
                      
                      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                      
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Column {
                          Text("مسؤول التواصل والجوال:", fontSize = 10.sp, color = Color.Gray)
                          Text("${sup.contactName} (${sup.phone})", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                          Text("البريد الإلكتروني:", fontSize = 10.sp, color = Color.Gray)
                          Text(sup.email.ifEmpty { "غير محدد" }, fontSize = 11.sp)
                        }
                      }

                      Text("العنوان: ${sup.address.ifEmpty { "غير محدد" }}", fontSize = 11.sp, color = Color.Gray)
                      
                      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                      
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        // Statement & Supply details
                        Button(
                          onClick = { selectedSupplierForStatement = sup },
                          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary),
                          shape = RoundedCornerShape(8.dp),
                          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                          modifier = Modifier.height(32.dp)
                        ) {
                          Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                          Spacer(modifier = Modifier.width(4.dp))
                          Text("كشف الحساب والتوريد ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(onClick = { viewModel.deleteSupplier(sup) }, modifier = Modifier.size(32.dp)) {
                          Icon(Icons.Default.Delete, contentDescription = "حذف", tint = CoralError)
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

      // Customer Add Dialog
      if (showAddCustomerDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf("عادي") }
        var initialBalanceThemStr by remember { mutableStateOf("") }
        var initialBalanceUsStr by remember { mutableStateOf("") }
        
        AlertDialog(
          onDismissRequest = { showAddCustomerDialog = false },
          title = { Text("تسجيل عميل جديد ", fontWeight = FontWeight.Bold) },
          text = {
            Column(
              modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("الاسم الكامل للعميل:") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
              OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("رقم الجوال:") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
              OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("البريد الإلكتروني:") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
              OutlinedTextField(
                value = initialBalanceThemStr,
                onValueChange = { initialBalanceThemStr = it },
                label = { Text("الرصيد الافتتاحي له (شيكل):") },
                placeholder = { Text("نحن مدينون له. مثال: 500 أو اتركه 0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
              OutlinedTextField(
                value = initialBalanceUsStr,
                onValueChange = { initialBalanceUsStr = it },
                label = { Text("رصيد افتتاحي عليه (شيكل):") },
                placeholder = { Text("هو مدين لنا. مثال: 200 أو اتركه 0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
              
              var expandedType by remember { mutableStateOf(false) }
              Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { expandedType = true }, modifier = Modifier.fillMaxWidth()) {
                  Text("التصنيف: $selectedType")
                }
                DropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                  listOf("عادي", "VIP", "جملة").forEach { type ->
                    DropdownMenuItem(text = { Text(type) }, onClick = {
                      selectedType = type
                      expandedType = false
                    })
                  }
                }
              }
            }
          },
          confirmButton = {
            Button(onClick = {
              if (name.isNotEmpty() && phone.isNotEmpty()) {
                val initialThem = initialBalanceThemStr.toDoubleOrNull() ?: 0.0
                val initialUs = initialBalanceUsStr.toDoubleOrNull() ?: 0.0
                viewModel.addCustomerWithInitialBalance(
                  Customer(name = name, phone = phone, email = email, type = selectedType),
                  initialOwedToUs = initialUs,
                  initialOwedToThem = initialThem
                )
                showAddCustomerDialog = false
              } else {
                viewModel.showToast("خطأ: يرجى كتابة الاسم والجوال!")
              }
            }) {
              Text("حفظ وتسجيل")
            }
          },
          dismissButton = {
            TextButton(onClick = { showAddCustomerDialog = false }) { Text("إلغاء", color = CoralError) }
          }
        )
      }

      // Supplier Add Dialog
      if (showAddSupplierDialog) {
        var name by remember { mutableStateOf("") }
        var companyName by remember { mutableStateOf("") }
        var contactName by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var initialBalanceThemStr by remember { mutableStateOf("") }
        var initialBalanceUsStr by remember { mutableStateOf("") }

        AlertDialog(
          onDismissRequest = { showAddSupplierDialog = false },
          title = { Text("تسجيل مورد جديد ", fontWeight = FontWeight.Bold) },
          text = {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("اسم المورد / الجهة الموردة:") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
              OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("اسم الشركة التابعة:") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
              OutlinedTextField(
                value = contactName,
                onValueChange = { contactName = it },
                label = { Text("اسم مسؤول المبيعات:") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
              OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("رقم الجوال:") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
              OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("البريد الإلكتروني:") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
              OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("العنوان الرئيسي:") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
              OutlinedTextField(
                value = initialBalanceThemStr,
                onValueChange = { initialBalanceThemStr = it },
                label = { Text("الرصيد الافتتاحي له (شيكل):") },
                placeholder = { Text("نحن مدينون له. مثال: 500 أو اتركه 0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
              OutlinedTextField(
                value = initialBalanceUsStr,
                onValueChange = { initialBalanceUsStr = it },
                label = { Text("رصيد افتتاحي عليه (شيكل):") },
                placeholder = { Text("هو مدين لنا. مثال: 200 أو اتركه 0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
            }
          },
          confirmButton = {
            Button(onClick = {
              if (name.isNotEmpty() && phone.isNotEmpty()) {
                val initialThem = initialBalanceThemStr.toDoubleOrNull() ?: 0.0
                val initialUs = initialBalanceUsStr.toDoubleOrNull() ?: 0.0
                viewModel.addSupplierWithInitialBalance(
                  Supplier(
                    name = name,
                    companyName = companyName,
                    contactName = contactName,
                    phone = phone,
                    email = email,
                    address = address,
                    balance = 0.0
                  ),
                  initialThem,
                  initialUs
                )
                showAddSupplierDialog = false
              } else {
                viewModel.showToast("خطأ: يرجى كتابة اسم المورد ورقم الجوال!")
              }
            }) {
              Text("حفظ وتسجيل")
            }
          },
          dismissButton = {
            TextButton(onClick = { showAddSupplierDialog = false }) { Text("إلغاء", color = CoralError) }
          }
        )
      }

      // Customer Statement of Account Dialog
      selectedCustomerForStatement?.let { customer ->
        // Gather invoices related to this customer
        val customerInvoices = invoices.filter { it.customerId == customer.id && !it.isPurchase && !it.isDraft }
        // Gather manual customer transactions
        val relatedCustomerTx = customerTransactions.filter { it.customerId == customer.id }

        // Build a combined list of transaction entries
        val combinedTxList = remember(customerInvoices, relatedCustomerTx) {
          val list = mutableListOf<CombinedCustomerTx>()
          // Add invoices as debits (what they owe) and their cash paidAmount as immediate credits
          customerInvoices.forEach { inv ->
            // Debit for the invoice total
            list.add(
              CombinedCustomerTx(
                date = inv.invoiceDate,
                description = "فاتورة مبيعات رقم #${inv.id}",
                debit = inv.total,
                credit = 0.0,
                refId = inv.id
              )
            )
            // Credit for any amount paid at the time of invoice
            if (inv.paidAmount > 0) {
              list.add(
                CombinedCustomerTx(
                  date = inv.invoiceDate + 1000,
                  description = "دفعة مسددة على فاتورة #${inv.id}",
                  debit = 0.0,
                  credit = inv.paidAmount,
                  refId = inv.id
                )
              )
            }
          }
          // Add manual transactions
          relatedCustomerTx.forEach { tx ->
            list.add(
              CombinedCustomerTx(
                date = tx.date,
                description = tx.note.ifEmpty { if (tx.transactionType == "بيع") "فاتورة آجلة" else "سند قبض مسدد" },
                debit = if (tx.transactionType == "بيع") tx.amount else 0.0,
                credit = if (tx.transactionType == "دفعة") tx.amount else 0.0,
                refId = tx.id
              )
            )
          }
          // Sort chronologically ascending to calculate running balance
          list.sortBy { it.date }
          list
        }

        // Calculate cumulative summary
        val totalDebit = combinedTxList.sumOf { it.debit }
        val totalCredit = combinedTxList.sumOf { it.credit }
        val outstandingBalance = totalDebit - totalCredit

        var showAddPaymentFields by remember { mutableStateOf(false) }
        var paymentAmountStr by remember { mutableStateOf("") }
        var paymentNote by remember { mutableStateOf("سند قبض نقدي سداد رصيد") }

        AlertDialog(
          onDismissRequest = { selectedCustomerForStatement = null },
          properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
          modifier = Modifier
            .fillMaxWidth(if (isCompact) 0.95f else 0.85f)
            .fillMaxHeight(0.85f),
          title = {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("كشف حساب العميل: ${customer.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
              }
              IconButton(onClick = { selectedCustomerForStatement = null }) {
                Icon(Icons.Default.Close, contentDescription = "إغلاق")
              }
            }
          },
          text = {
            Column(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // Quick info card
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column {
                    Text("رقم الجوال: ${customer.phone}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("البريد: ${customer.email.ifEmpty { "لا يوجد" }}", fontSize = 11.sp, color = Color.Gray)
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text("نقاط الولاء: ${customer.points} نقطة", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("تصنيف العميل: ${customer.type}", fontSize = 11.sp, color = Color.Gray)
                  }
                }
              }

              // Summary of balances
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Card(
                  modifier = Modifier.weight(1f),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                  Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("إجمالي المبيعات (مدين)", fontSize = 10.sp, color = Color.Gray)
                    Text("${formatMoney(totalDebit)} شيكل", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                  }
                }
                Card(
                  modifier = Modifier.weight(1f),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                  Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("إجمالي المدفوعات (دائن)", fontSize = 10.sp, color = Color.Gray)
                    Text("${formatMoney(totalCredit)} شيكل", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                  }
                }
                Card(
                  modifier = Modifier.weight(1.2f),
                  colors = CardDefaults.cardColors(
                    containerColor = if (outstandingBalance > 0) CoralError.copy(alpha = 0.1f) else EmeraldGreen.copy(alpha = 0.1f)
                  ),
                  border = BorderStroke(1.dp, if (outstandingBalance > 0) CoralError else EmeraldGreen)
                ) {
                  Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("الرصيد المتبقي المستحق", fontSize = 10.sp, color = Color.Gray)
                    Text(
                      "${formatMoney(outstandingBalance)} شيكل",
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (outstandingBalance > 0) CoralError else EmeraldGreen
                    )
                  }
                }
              }

              // Ledger Entries Table
              Text("القيود والحركات التفصيلية للعميل:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(1f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
              ) {
                Column(modifier = Modifier.fillMaxSize()) {
                  // Table Header
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                      .padding(vertical = 8.dp, horizontal = 10.dp)
                  ) {
                    Text("التاريخ", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.1f))
                    Text("البيان", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2.2f))
                    Text("مدين (+)", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
                    Text("دائن (-)", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center, color = EmeraldGreen)
                    Text("الرصيد", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
                  }

                  if (combinedTxList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                      Text("لا توجد حركات مالية مسجلة للعميل حتى الآن.", color = Color.Gray, fontSize = 12.sp)
                    }
                  } else {
                    var runningBal = 0.0
                    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                    
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                      items(combinedTxList) { tx ->
                        runningBal += (tx.debit - tx.credit)
                        Row(
                          modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                            .padding(vertical = 8.dp, horizontal = 10.dp),
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Text(sdf.format(Date(tx.date)), fontSize = 10.sp, color = Color.Gray, modifier = Modifier.weight(1.1f))
                          Text(tx.description, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(2.2f))
                          Text(if (tx.debit > 0) formatMoney(tx.debit) else "-", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.9f))
                          Text(if (tx.credit > 0) formatMoney(tx.credit) else "-", fontSize = 11.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.9f))
                          Text(formatMoney(runningBal), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f), color = if (runningBal > 0) CoralError else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                      }
                    }
                  }
                }
              }

              // Add Payment Action Form
              if (showAddPaymentFields) {
                Card(
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تسجيل سند قبض جديد (سداد مالي) ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      OutlinedTextField(
                        value = paymentAmountStr,
                        onValueChange = { paymentAmountStr = it },
                        label = { Text("المبلغ (شيكل):") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                      )
                      OutlinedTextField(
                        value = paymentNote,
                        onValueChange = { paymentNote = it },
                        label = { Text("البيان:") },
                        singleLine = true,
                        modifier = Modifier.weight(1.7f)
                      )
                    }
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.End,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      TextButton(onClick = { showAddPaymentFields = false }) {
                        Text("إلغاء", color = CoralError)
                      }
                      Spacer(modifier = Modifier.width(8.dp))
                      Button(
                        onClick = {
                          val amt = paymentAmountStr.toDoubleOrNull() ?: 0.0
                          if (amt > 0) {
                            viewModel.addCustomerTransaction(
                              CustomerTransaction(
                                customerId = customer.id,
                                transactionType = "دفعة",
                                amount = amt,
                                note = paymentNote
                              )
                            )
                            paymentAmountStr = ""
                            showAddPaymentFields = false
                          } else {
                            viewModel.showToast("يرجى كتابة مبلغ سداد صحيح!")
                          }
                        }
                      ) {
                        Text("تأكيد وحفظ الدفعة")
                      }
                    }
                  }
                }
              } else {
                Button(
                  onClick = { showAddPaymentFields = true; paymentAmountStr = outstandingBalance.let { if (it > 0) it.toString() else "" } },
                  modifier = Modifier.fillMaxWidth(),
                  colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                  Icon(Icons.Default.AttachMoney, contentDescription = null)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("تسجيل سند قبض / تحصيل دفعة سداد جديدة ", fontWeight = FontWeight.Bold)
                }
              }
            }
          },
          confirmButton = {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              TextButton(onClick = { selectedCustomerForStatement = null }) { Text("إغلاق") }
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = {
                      pendingPrintActionForCust = "whatsapp"
                      showPrintTypeDialogForCust = true
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                  Text("واتساب", color = MaterialTheme.colorScheme.onPrimary)
                }
                Button(
                  onClick = {
                      pendingPrintActionForCust = "pdf"
                      showPrintTypeDialogForCust = true
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                  Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("PDF", fontSize = 12.sp)
                }
              }
            }
          }
        )
      }

      // Supplier Statement of Account Dialog
      selectedSupplierForStatement?.let { supplier ->
        // Gather supplier transactions
        val relatedSupplierTx = supplierTransactions.filter { it.supplierId == supplier.id }

        // Sort chronologically ascending to calculate running balance
        val sortedTxList = remember(relatedSupplierTx) {
          relatedSupplierTx.sortedBy { it.date }
        }

        val totalPurchases = sortedTxList.filter { it.transactionType == "شراء" }.sumOf { it.amount }
        val totalPayments = sortedTxList.filter { it.transactionType == "دفعة" }.sumOf { it.amount }
        val supplierBalance = totalPurchases - totalPayments

        var showAddTxFields by remember { mutableStateOf(false) }
        var txTypeSelected by remember { mutableStateOf("شراء") } // "شراء" (Purchase) or "دفعة" (Payment)
        var txAmountStr by remember { mutableStateOf("") }
        var txNote by remember { mutableStateOf("") }

        AlertDialog(
          onDismissRequest = { selectedSupplierForStatement = null },
          properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
          modifier = Modifier
            .fillMaxWidth(if (isCompact) 0.95f else 0.85f)
            .fillMaxHeight(0.85f),
          title = {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("كشف حساب وتوريدات المورد: ${supplier.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
              }
              IconButton(onClick = { selectedSupplierForStatement = null }) {
                Icon(Icons.Default.Close, contentDescription = "إغلاق")
              }
            }
          },
          text = {
            Column(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // Quick info card
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column {
                    Text("الشركة: ${supplier.companyName.ifEmpty { "غير محدد" }}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("مسؤول التواصل: ${supplier.contactName}", fontSize = 11.sp, color = Color.Gray)
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text("رقم الجوال: ${supplier.phone}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("العنوان: ${supplier.address.ifEmpty { "غير محدد" }}", fontSize = 11.sp, color = Color.Gray)
                  }
                }
              }

              // Summary of balances
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Card(
                  modifier = Modifier.weight(1f),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                  Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("إجمالي التوريد (دائن)", fontSize = 10.sp, color = Color.Gray)
                    Text("${formatMoney(totalPurchases)} شيكل", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CoralError)
                  }
                }
                Card(
                  modifier = Modifier.weight(1f),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                  Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("إجمالي المسدد له (مدين)", fontSize = 10.sp, color = Color.Gray)
                    Text("${formatMoney(totalPayments)} شيكل", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                  }
                }
                Card(
                  modifier = Modifier.weight(1.2f),
                  colors = CardDefaults.cardColors(
                    containerColor = if (supplierBalance > 0) CoralError.copy(alpha = 0.1f) else EmeraldGreen.copy(alpha = 0.1f)
                  ),
                  border = BorderStroke(1.dp, if (supplierBalance > 0) CoralError else EmeraldGreen)
                ) {
                  Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("الرصيد المستحق للمورد", fontSize = 10.sp, color = Color.Gray)
                    Text(
                      "${formatMoney(supplierBalance)} شيكل",
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (supplierBalance > 0) CoralError else EmeraldGreen
                    )
                  }
                }
              }

              // Ledger Table
              Text("سجل حركات وعمليات التوريد والدفعات المتبادلة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(1f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
              ) {
                Column(modifier = Modifier.fillMaxSize()) {
                  // Table Header
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                      .padding(vertical = 8.dp, horizontal = 10.dp)
                  ) {
                    Text("التاريخ", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.1f))
                    Text("البيان", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2.2f))
                    Text("مدين (مسدد له)", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center, color = EmeraldGreen)
                    Text("دائن (شراء/توريد)", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center, color = CoralError)
                    Text("الرصيد المستحق", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
                  }

                  if (sortedTxList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                      Text("لا توجد توريدات أو سندات صرف مسجلة لهذا المورد.", color = Color.Gray, fontSize = 12.sp)
                    }
                  } else {
                    var runningBal = 0.0
                    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                      items(sortedTxList) { tx ->
                        val isPurchase = tx.transactionType == "شراء"
                        val deb = if (!isPurchase) tx.amount else 0.0
                        val cred = if (isPurchase) tx.amount else 0.0
                        runningBal += (cred - deb)

                        Row(
                          modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                            .padding(vertical = 8.dp, horizontal = 10.dp),
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Text(sdf.format(Date(tx.date)), fontSize = 10.sp, color = Color.Gray, modifier = Modifier.weight(1.1f))
                          Text(tx.note.ifEmpty { if (isPurchase) "فاتورة توريد بضاعة" else "سند صرف دفعة مالية للمورد" }, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(2.2f))
                          Text(if (deb > 0) formatMoney(deb) else "-", fontSize = 11.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.9f))
                          Text(if (cred > 0) formatMoney(cred) else "-", fontSize = 11.sp, color = CoralError, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.9f))
                          Text(formatMoney(runningBal), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f), color = if (runningBal > 0) CoralError else EmeraldGreen)
                        }
                      }
                    }
                  }
                }
              }

              // Add Transaction Action Form
              if (showAddTxFields) {
                Card(
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تسجيل حركة توريد أو دفعة مالية جديدة ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    
                    // Default type is "دفعة"
                    txTypeSelected = "دفعة"

                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      OutlinedTextField(
                        value = txAmountStr,
                        onValueChange = { txAmountStr = it },
                        label = { Text("المبلغ (شيكل):") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1.1f)
                      )
                      OutlinedTextField(
                        value = txNote,
                        onValueChange = { txNote = it },
                        label = { Text("البيان / تفصيل الشراء:") },
                        singleLine = true,
                        modifier = Modifier.weight(1.9f)
                      )
                    }
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.End,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      TextButton(onClick = { showAddTxFields = false }) {
                        Text("إلغاء", color = CoralError)
                      }
                      Spacer(modifier = Modifier.width(8.dp))
                      Button(
                        onClick = {
                          val amt = txAmountStr.toDoubleOrNull() ?: 0.0
                          if (amt > 0) {
                            viewModel.addSupplierTransaction(
                              SupplierTransaction(
                                supplierId = supplier.id,
                                transactionType = txTypeSelected,
                                amount = amt,
                                note = txNote.ifEmpty { if (txTypeSelected == "شراء") "توريد بضاعة للمستودع" else "تسديد دفعة نقدية تحت الحساب" }
                              )
                            )
                            txAmountStr = ""
                            txNote = ""
                            showAddTxFields = false
                          } else {
                            viewModel.showToast("يرجى كتابة مبلغ صحيح!")
                          }
                        }
                      ) {
                        Text("تأكيد وتسجيل الحركة")
                      }
                    }
                  }
                }
              } else {
                Button(
                  onClick = { showAddTxFields = true; txTypeSelected = "دفعة"; txNote = "سند صرف دفعة مالية للمورد" },
                  modifier = Modifier.fillMaxWidth(),
                  colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                  Icon(Icons.Default.AttachMoney, contentDescription = null)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("سداد دفعة للمورد ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          },
          confirmButton = {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              TextButton(onClick = { selectedSupplierForStatement = null }) { Text("إغلاق") }
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = {
                      pendingPrintActionForSup = "whatsapp"
                      showPrintTypeDialogForSup = true
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                  Text("واتساب", color = MaterialTheme.colorScheme.onPrimary)
                }
                Button(
                  onClick = {
                      pendingPrintActionForSup = "pdf"
                      showPrintTypeDialogForSup = true
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                  Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("PDF", fontSize = 12.sp)
                }
              }
            }
          }
        )
      
        if (showPrintTypeDialogForSup) {
            val executePrintAction = { isDetailed: Boolean ->
                val mappedRows = sortedTxList.map { tx ->
                    var desc = tx.note.ifEmpty { if (tx.transactionType == "شراء") "فاتورة توريد بضاعة" else "سند صرف دفعة مالية" }
                    if (isDetailed && tx.transactionType == "شراء") {
                        val relatedInv = allInvoicesState.find { it.customerId == supplier.id && it.isPurchase && kotlin.math.abs(it.total - tx.amount) < 0.1 }
                        if (relatedInv != null) {
                            val items = allInvoiceItemsState.filter { it.invoiceId == relatedInv.id }
                            if (items.isNotEmpty()) {
                                desc += "\\n" + items.joinToString("\\n") { "- ${it.productNameAr} (${it.quantity} x ${formatMoney(it.price)})" }
                            }
                        }
                    }
                    com.example.ui.StatementRow(
                        date = tx.date,
                        description = desc,
                        debit = if (tx.transactionType != "شراء") tx.amount else 0.0,
                        credit = if (tx.transactionType == "شراء") tx.amount else 0.0
                    )
                }
                val summaryText = "إجمالي التوريد (دائن): ${formatMoney(totalPurchases)} | إجمالي المسدد (مدين): ${formatMoney(totalPayments)} | الرصيد المتبقي: ${formatMoney(supplierBalance)}"
                val entityDetails = "الشركة: ${supplier.companyName} | جوال مسؤول الاتصال: ${supplier.phone}"
                val finalTitle = "كشف حساب مورد: ${supplier.name}" + if (isDetailed) " (تفصيلي)" else " (مجمل)"
                
                if (pendingPrintActionForSup == "pdf") {
                    viewModel.exportStatementToPdf(context, finalTitle, mappedRows, summaryText, entityDetails)
                } else if (pendingPrintActionForSup == "whatsapp") {
                    val cleanPhone = supplier.phone.replace("+", "").replace(" ", "").filter { it.isDigit() }
                    val pdfFile = com.example.ui.PdfGenerator.generateStatementPdfFile(
                        context = context,
                        storeName = viewModel.storeName.value,
                        storePhone = viewModel.storePhone.value,
                        title = finalTitle,
                        bodyRows = mappedRows,
                        summaryText = summaryText,
                        entityDetails = entityDetails
                    )
                    if (pdfFile != null) {
                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            putExtra(android.content.Intent.EXTRA_TEXT, "مرفق كشف الحساب من ${viewModel.storeName.value}")
                            if (cleanPhone.isNotEmpty()) {
                                putExtra("jid", "$cleanPhone@s.whatsapp.net")
                            }
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try {
                            shareIntent.setPackage("com.whatsapp")
                            context.startActivity(shareIntent)
                        } catch (e: Exception) {
                            try {
                                shareIntent.setPackage("com.whatsapp.w4b")
                                context.startActivity(shareIntent)
                            } catch (e2: Exception) {
                                shareIntent.setPackage(null)
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "مشاركة كشف الحساب"))
                            }
                        }
                    }
                }
            }

            AlertDialog(
                onDismissRequest = { showPrintTypeDialogForSup = false },
                title = { Text("نوع كشف الحساب", fontWeight = FontWeight.Bold) },
                text = { Text("هل تريد إنشاء كشف حساب مجمل أم تفصيلي؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            showPrintTypeDialogForSup = false
                            executePrintAction(true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) { Text("تفصيلي") }
                },
                dismissButton = {
                    Button(onClick = {
                        showPrintTypeDialogForSup = false
                        executePrintAction(false)
                    }) { Text("مجمل") }
                }
            )
        }
      }
    }
  }


  // Helper data class for Customer combined transaction row
  

  @Composable
  fun BasicTextFieldHelper(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
  ) {
    androidx.compose.foundation.text.BasicTextField(
      value = value,
      onValueChange = { newValue ->
        if (newValue.all { it.isDigit() } && newValue.length <= 4) {
          onValueChange(newValue)
        }
      },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      singleLine = true,
      modifier = modifier
        .border(
          width = 1.dp,
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
          shape = RoundedCornerShape(4.dp)
        )
        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
        .padding(vertical = 4.dp, horizontal = 6.dp),
      textStyle = androidx.compose.ui.text.TextStyle(
        fontSize = 11.sp, 
        textAlign = TextAlign.Center, 
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold
      )
    )
  }

  @Composable
  fun DenominationCounter(
    onTotalChanged: (Double) -> Unit
  ) {
    var count200 by remember { mutableStateOf("") }
    var count100 by remember { mutableStateOf("") }
    var count50 by remember { mutableStateOf("") }
    var count20 by remember { mutableStateOf("") }
    var count10 by remember { mutableStateOf("") }
    var count5 by remember { mutableStateOf("") }
    var count2 by remember { mutableStateOf("") }
    var count1 by remember { mutableStateOf("") }
    var countHalf by remember { mutableStateOf("") }
    var countAgorot10 by remember { mutableStateOf("") }

    val total = (count200.toIntOrNull() ?: 0) * 200.0 +
        (count100.toIntOrNull() ?: 0) * 100.0 +
        (count50.toIntOrNull() ?: 0) * 50.0 +
        (count20.toIntOrNull() ?: 0) * 20.0 +
        (count10.toIntOrNull() ?: 0) * 10.0 +
        (count5.toIntOrNull() ?: 0) * 5.0 +
        (count2.toIntOrNull() ?: 0) * 2.0 +
        (count1.toIntOrNull() ?: 0) * 1.0 +
        (countHalf.toIntOrNull() ?: 0) * 0.50 +
        (countAgorot10.toIntOrNull() ?: 0) * 0.10

    LaunchedEffect(total) {
      onTotalChanged(total)
    }

    var isExpanded by remember { mutableStateOf(false) }

    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
      shape = RoundedCornerShape(8.dp)
    ) {
      Column(modifier = Modifier.padding(10.dp)) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.AttachMoney,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = " مساعد حساب فئات العملات النقدية والقطع (بالشيكل)",
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.primary
            )
          }
          Icon(
            imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
        }

        if (isExpanded) {
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "أدخل عدد الأوراق والقطع النقدية المتوفرة بالدرج لتسهيل الجرد المطابق:",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
          )
          Spacer(modifier = Modifier.height(8.dp))

          val items = listOf(
            Triple("200 شيكل", count200) { v: String -> count200 = v },
            Triple("100 شيكل", count100) { v: String -> count100 = v },
            Triple("50 شيكل", count50) { v: String -> count50 = v },
            Triple("20 شيكل", count20) { v: String -> count20 = v },
            Triple("10 شيكل", count10) { v: String -> count10 = v },
            Triple("5 شيكل", count5) { v: String -> count5 = v },
            Triple("2 شيكل", count2) { v: String -> count2 = v },
            Triple("1 شيكل", count1) { v: String -> count1 = v },
            Triple("نص شيكل", countHalf) { v: String -> countHalf = v },
            Triple("10 أغورات", countAgorot10) { v: String -> countAgorot10 = v }
          )

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in items.indices step 2) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                // Column 1
                val item1 = items[i]
                Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = item1.first,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(55.dp)
                  )
                  BasicTextFieldHelper(
                    value = item1.second,
                    onValueChange = item1.third,
                    modifier = Modifier.width(60.dp)
                  )
                }

                // Column 2
                if (i + 1 < items.size) {
                  val item2 = items[i + 1]
                  Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(
                      text = item2.first,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Medium,
                      modifier = Modifier.width(55.dp)
                    )
                    BasicTextFieldHelper(
                      value = item2.second,
                      onValueChange = item2.third,
                      modifier = Modifier.width(60.dp)
                    )
                  }
                } else {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                RoundedCornerShape(6.dp)
              )
              .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("مجموع الجرد التلقائي:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
              "${String.format("%.2f", total)} شيكل",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }
    }
  }
  
  // ==========================================
  // 6. SHIFTS & AUDIT LOGS SCREEN
  // ==========================================
  @Composable
    fun SettingsScreen(viewModel: AppViewModel) {
    val storeName by viewModel.storeName.collectAsState("")
    val userAccount by viewModel.currentUserAccount.collectAsState()
    val storePhone by viewModel.storePhone.collectAsState("")
    val storeVatNumber by viewModel.storeVatNumber.collectAsState("")
    val supportWhatsappNumber by viewModel.supportWhatsappNumber.collectAsState()
    val currentBranch by viewModel.currentBranch.collectAsState("")
    val cashierPhone by viewModel.cashierPhone.collectAsState("")
    val cashierAddress by viewModel.cashierAddress.collectAsState("")
    val currentUser by viewModel.currentUser.collectAsState("")
    val branchesList by viewModel.branches.collectAsState(emptyList())
    
    Column(
    modifier = Modifier
    .fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)
    .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
      Text("إعدادات المتجر وبيئة العمل والفرع ", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
      
      Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
        
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("تفعيل الوضع الليلي (Dark Mode)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            androidx.compose.material3.Switch(
              checked = isDarkMode,
              onCheckedChange = { viewModel.toggleDarkMode(it) }
            )
          }
          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
          Text("إعدادات الفرع والشركة وطباعة الفواتير:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
          OutlinedTextField(
          value = storeName,
          onValueChange = { viewModel.storeName.value = it },
          label = { Text("اسم المتجر التجاري:") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
          value = storePhone,
          onValueChange = { viewModel.storePhone.value = it },
          label = { Text("هاتف الاتصال للمتجر:") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
          value = storeVatNumber,
          onValueChange = { viewModel.storeVatNumber.value = it },
          label = { Text("الرقم الضريبي:") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
          )
          if (userAccount?.role == "ADMIN") {
            OutlinedTextField(
              value = supportWhatsappNumber,
              onValueChange = { viewModel.updateSupportWhatsappNumber(it) },
              label = { Text("رقم واتساب الدعم الفني وتنشيط الاشتراكات:") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )

            val currentBannerText by viewModel.subscriptionBannerText.collectAsState()
            var editBannerInput by remember(currentBannerText) { mutableStateOf(currentBannerText) }
            OutlinedTextField(
              value = editBannerInput,
              onValueChange = {
                editBannerInput = it
                viewModel.updateSubscriptionBannerText(it)
              },
              label = { Text("نص شريط تفعيل الاشتراك المتحرك (تحت الهيدر):") },
              placeholder = { Text("أدخل الجملة المتحركة التي تظهر للمستخدمين...") },
              modifier = Modifier.fillMaxWidth(),
              supportingText = { Text("يظهر فوراً لجميع المستخدمين تحت الهيدر ويُحفظ تلقائياً.") }
            )
          }
          
          var expandedBranch by remember { mutableStateOf(false) }
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text("الفرع ومنافذ البيع:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            Box(modifier = Modifier.fillMaxWidth()) {
              OutlinedButton(onClick = { expandedBranch = true }, modifier = Modifier.fillMaxWidth()) {
                Text("الفرع النشط الحالي: $currentBranch ")
              }
              DropdownMenu(expanded = expandedBranch, onDismissRequest = { expandedBranch = false }) {
                branchesList.forEach { b ->
                  DropdownMenuItem(text = { Text(b) }, onClick = {
                    viewModel.currentBranch.value = b
                    expandedBranch = false
                    viewModel.showToast("تم تحويل نقطة البيع لـ: $b ")
                  })
                }
              }
            }

            // Branch CRUD Management UI
            var showManageBranches by remember { mutableStateOf(false) }
            OutlinedButton(
              onClick = { showManageBranches = !showManageBranches },
              modifier = Modifier.fillMaxWidth(),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
              Icon(if (showManageBranches) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text(if (showManageBranches) "إخفاء لوحة الفروع" else "إدارة وتعديل الفروع (إضافة/تعديل/حذف) ")
            }

            if (showManageBranches) {
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Text("قائمة الفروع المسجلة (${branchesList.size}):", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                  branchesList.forEach { b ->
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Text(b, fontSize = 12.sp, modifier = Modifier.weight(1f))

                      var showRenameDialog by remember { mutableStateOf(false) }
                      if (showRenameDialog) {
                        var tempName by remember { mutableStateOf(b) }
                        AlertDialog(
                          onDismissRequest = { showRenameDialog = false },
                          title = { Text("تعديل اسم الفرع ", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                          text = {
                            OutlinedTextField(
                              value = tempName,
                              onValueChange = { tempName = it },
                              label = { Text("اسم الفرع الجديد") },
                              singleLine = true
                            )
                          },
                          confirmButton = {
                            Button(
                              onClick = {
                                viewModel.editBranch(b, tempName)
                                showRenameDialog = false
                              },
                              enabled = tempName.isNotBlank() && tempName != b
                            ) {
                              Text("تعديل")
                            }
                          },
                          dismissButton = {
                            TextButton(onClick = { showRenameDialog = false }) {
                              Text("إلغاء", color = CoralError)
                            }
                          }
                        )
                      }

                      IconButton(onClick = { showRenameDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                      }

                      IconButton(
                        onClick = {
                          viewModel.deleteBranch(b)
                          viewModel.showToast("تم حذف فرع $b ")
                        },
                        enabled = branchesList.size > 1
                      ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = if (branchesList.size > 1) CoralError else Color.Gray, modifier = Modifier.size(18.dp))
                      }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                  }

                  // Add New Branch
                  var newBranchName by remember { mutableStateOf("") }
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    OutlinedTextField(
                      value = newBranchName,
                      onValueChange = { newBranchName = it },
                      label = { Text("اسم فرع جديد ", fontSize = 11.sp) },
                      singleLine = true,
                      modifier = Modifier.weight(1f)
                    )
                    Button(
                      onClick = {
                        if (newBranchName.isNotBlank()) {
                          viewModel.addBranch(newBranchName)
                          viewModel.showToast("تم إضافة الفرع بنجاح ")
                          newBranchName = ""
                        }
                      },
                      enabled = newBranchName.isNotBlank() && !branchesList.contains(newBranchName)
                    ) {
                      Text("إضافة")
                    }
                  }
                }
              }
            }
          }

          if (userAccount?.role != "ADMIN") {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Text("بيانات الكاشير النشط (تظهر في الفاتورة):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
              value = currentUser,
              onValueChange = { viewModel.currentUser.value = it },
              label = { Text("اسم الكاشير:") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = cashierPhone,
              onValueChange = { viewModel.cashierPhone.value = it },
              label = { Text("رقم هاتف الكاشير:") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = cashierAddress,
              onValueChange = { viewModel.cashierAddress.value = it },
              label = { Text("عنوان الفرع / الكاشير:") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      }
      
      if (userAccount?.role != "ADMIN") {
        Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
          ) {
          
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Text removed
            // Text removed
            
            val roles = emptyList<Triple<String, String, String>>()
            
              roles.forEach { (user, role, desc) ->
              OutlinedButton(
              onClick = { viewModel.currentUser.value = user; viewModel.currentUserRole.value = role },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(
              1.dp,
              if (viewModel.currentUser.value == user) MaterialTheme.colorScheme.primary else Color.LightGray
              )
                ) {
                
                  Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                  Text(user, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                  Text(desc, fontSize = 10.sp, color = Color.Gray)
                }
              }
            }
          }
        }
      }
      
      val currentSymbol by viewModel.currencySymbol.collectAsState()
      var expandedCurrency by remember { mutableStateOf(false) }

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(
          modifier = Modifier.padding(12.dp).fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "إعدادات العملة والرمز المالي (شيكل كإعداد افتراضي) :",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
          )
          Text(
            text = "اختر العملة المستخدمة في عرض أسعار المنتجات والفواتير وتقارير الحسابات الختامية:",
            fontSize = 11.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
          )
          
          Box(modifier = Modifier.fillMaxWidth(0.9f)) {
            OutlinedButton(
              onClick = { expandedCurrency = true },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp)
            ) {
              Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
              ) {
                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("العملة النشطة: $currentSymbol", fontWeight = FontWeight.Bold)
              }
            }
            DropdownMenu(
              expanded = expandedCurrency, 
              onDismissRequest = { expandedCurrency = false },
              modifier = Modifier.fillMaxWidth(0.8f).height(240.dp)
            ) {
              listOf(
                "شيكل (شيكل اسرائيلي) ",
                "ج.م (جنيه مصري) ",
                "ش.ج (شيكل جديد) ",
                "د.إ (درهم إماراتي) ",
                "د.ك (دينار كويتي) ",
                "ر.ق (ريال قطر) ",
                "د.ب (دينار بحريني) ",
                "ر.ع (ريال عماني) ",
                "د.أ (دينار أردني) ",
                "ل.ل (ليرة لبنانية) ",
                "$ (دولار أمريكي) ",
                "€ (يورو) "
              ).forEach { curr ->
                val symbol = curr.substringBefore(" ")
                DropdownMenuItem(
                  text = { Text(curr) },
                  onClick = {
                    viewModel.updateCurrencySymbol(symbol)
                    expandedCurrency = false
                    viewModel.showToast("تم تغيير العملة الافتراضية للنظام إلى: $symbol ")
                  }
                )
              }
            }
          }
        }
      }

      Button(
        onClick = { viewModel.saveUserSettings() },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
      ) {
        Icon(Icons.Default.Save, contentDescription = "حفظ", tint = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Text("حفظ التغييرات وكافة الإعدادات المخصصة ", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
      }
    }
  }
  
  // ==========================================
  // Sub-components & Helpers
  // ==========================================
  data class TabItem(
  val id: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector
  )
  
  @Composable
    fun TrialBalanceTab(balances: Map<String, Double>) {
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val isCompact = maxWidth < 600.dp
      
        if (isCompact) {
          if (balances.isEmpty()) {
          Box(
          modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp),
          contentAlignment = Alignment.Center
            ) {
            Text("لا توجد حسابات نشطة", color = Color.Gray, fontSize = 13.sp)
          }
          } else {
          LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              items(balances.toList()) { (account, balance) ->
              Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
                  ) {
                  Text(account, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                  Text(
                  text = "${formatMoney(balance)} ",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = if (balance >= 0) MaterialTheme.colorScheme.primary else CoralError
                  )
                }
              }
            }
          }
        }
        } else {
        
          Column(modifier = Modifier.fillMaxSize()) {
          Row(
          modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
          .padding(12.dp)
            ) {
            Text("الحساب الرئيسي", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
            Text("الرصيد المحاسبي", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
          }
          
            if (balances.isEmpty()) {
            Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
              ) {
              Text("لا توجد حسابات نشطة", color = Color.Gray, fontSize = 13.sp)
            }
            } else {
              LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(balances.toList()) { (account, balance) ->
                Row(
                modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                .padding(12.dp)
                  ) {
                  Text(account, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1.5f))
                  Text(
                  text = "${formatMoney(balance)} ",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = if (balance >= 0) MaterialTheme.colorScheme.primary else CoralError,
                  modifier = Modifier.weight(1.5f),
                  textAlign = TextAlign.End
                  )
                }
              }
            }
          }
        }
      }
    }
  }
  
  @Composable
  fun JournalBookTab(viewModel: AppViewModel, ledgerEntries: List<LedgerEntry>) {
    val context = LocalContext.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val isCompact = maxWidth < 600.dp
      
      if (isCompact) {
        if (ledgerEntries.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
          ) {
            Text("لا توجد قيود محاسبية مسجلة", color = Color.Gray, fontSize = 13.sp)
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(ledgerEntries) { entry ->
              Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(entry.description, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                    Text(sdf.format(Date(entry.date)), fontSize = 10.sp, color = Color.Gray)
                  }
                  HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                      Text("المدين (من):", fontSize = 10.sp, color = Color.Gray)
                      Text(entry.debitAccount, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                      Text("الدائن (إلى):", fontSize = 10.sp, color = Color.Gray)
                      Text(entry.creditAccount, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CoralError)
                    }
                  }
                  HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text("المبلغ الإجمالي:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Text("${formatMoney(entry.amount)} ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                      IconButton(
                        onClick = { viewModel.exportJournalVoucherToPdf(context, entry) },
                        modifier = Modifier.size(32.dp)
                      ) {
                        Icon(
                          Icons.Default.Print,
                          contentDescription = "طباعة سند القيد",
                          tint = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.size(18.dp)
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      } else {
        Column(modifier = Modifier.fillMaxSize()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
              .padding(12.dp)
          ) {
            Text("البيان / التاريخ", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
            Text("المدين (من)", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
            Text("الدائن (إلى)", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), color = CoralError)
            Text("المبلغ", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
            Text("سند القيد", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
          }
          
          if (ledgerEntries.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp),
              contentAlignment = Alignment.Center
            ) {
              Text("لا توجد قيود محاسبية مسجلة", color = Color.Gray, fontSize = 13.sp)
            }
          } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
              items(ledgerEntries) { entry ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                    .padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1.5f)) {
                    Text(entry.description, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                    Text(sdf.format(Date(entry.date)), fontSize = 9.sp, color = Color.Gray)
                  }
                  Text(entry.debitAccount, fontSize = 12.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
                  Text(entry.creditAccount, fontSize = 12.sp, modifier = Modifier.weight(1f), color = CoralError)
                  Text("${formatMoney(entry.amount)} ", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                  Box(modifier = Modifier.weight(0.5f), contentAlignment = Alignment.Center) {
                    IconButton(
                      onClick = { viewModel.exportJournalVoucherToPdf(context, entry) },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(
                        Icons.Default.Print,
                        contentDescription = "طباعة سند القيد",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
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
  
  

@Composable
fun FeatureRequestsScreen(viewModel: AppViewModel, onBackClick: () -> Unit = {}) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("طلبات الميزات")
    }
}

fun isPurchaseInvoice(
  inv: com.example.data.Invoice,
  supplierTransactions: List<com.example.data.SupplierTransaction> = emptyList(),
  ledgerEntries: List<com.example.data.LedgerEntry> = emptyList(),
  auditLogs: List<com.example.data.AuditLog> = emptyList(),
  customers: List<com.example.data.Customer> = emptyList(),
  suppliers: List<com.example.data.Supplier> = emptyList()
): Boolean {
  return suppliers.any { it.id == inv.customerId }
}

@Composable
fun FinancialStatementsTab(balances: Map<String, Double>) {
    val cash = balances["الصندوق"] ?: 0.0
    val bank = balances["البنك"] ?: 0.0
    val inventory = balances["مخزون مستودع 1"] ?: 0.0
    val capital = balances["رأس المال"] ?: 0.0
    val vat = balances["ضريبة القيمة المضافة"] ?: 0.0
    val revenue = balances["إيرادات المبيعات"] ?: 0.0
    val cogs = balances["تكلفة البضاعة المباعة"] ?: 0.0
    val expenses = balances["المصاريف التشغيلية"] ?: 0.0

    val netProfit = revenue - cogs - expenses
    val totalAssets = cash + bank + inventory
    val totalLiabilitiesAndEquity = capital + vat + netProfit

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Income Statement
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.02f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("قائمة الدخل التقديرية (Income Statement)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                HorizontalDivider()
                FinancialRow("إيرادات المبيعات والخدمات (النشاط):", revenue)
                FinancialRow("تكلفة البضاعة المباعة (COGS):", -cogs)
                FinancialRow("إجمالي المصاريف التشغيلية (Expenses):", -expenses)
                HorizontalDivider()
                FinancialRow("صافي الأرباح التشغيلية:", netProfit, isTotal = true)
            }
        }

        // Balance Sheet
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.02f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الميزانية العمومية المبسطة (Balance Sheet)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                HorizontalDivider()
                Text("الأصول (Assets):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                FinancialRow("النقدية بالصندوق (الدرج):", cash)
                FinancialRow("النقدية بالحساب البنكي:", bank)
                FinancialRow("مخزون مستودع البضاعة المتاحة:", inventory)
                FinancialRow("إجمالي الأصول المتداولة:", totalAssets, isTotal = true)

                HorizontalDivider()
                Text("الخصوم وحقوق الملكية (Liabilities & Equity):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                FinancialRow("رأس المال التأسيسي المستثمر:", capital)
                FinancialRow("أمانات ضريبة القيمة المضافة (15%):", vat)
                FinancialRow("الأرباح المرحلة الحالية للشركة:", netProfit)
                FinancialRow("إجمالي الخصوم وحقوق الملكية:", totalLiabilitiesAndEquity, isTotal = true)
            }
        }
    }
}
