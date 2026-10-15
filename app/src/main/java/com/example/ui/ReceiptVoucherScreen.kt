package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

data class ReceiptVoucherItem(
  val id: String,
  val numericId: Int,
  val date: Long,
  val partyName: String,
  val customerId: Int?,
  val customerPhone: String?,
  val amount: Double,
  val paymentMethod: String,
  val notes: String,
  val receiptType: String, // "قبض من عميل" أو "إيراد عام"
  val isCustomerTx: Boolean,
  val originalCustomerTx: CustomerTransaction? = null,
  val originalLedgerEntry: LedgerEntry? = null,
  val remainingBalance: Double? = null,
  val checkNumber: String? = null,
  val checkBankName: String? = null,
  val checkDueDate: Long? = null,
  val checkDrawer: String? = null,
  val checkStatus: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptVoucherScreen(
  viewModel: AppViewModel,
  onBackClick: (() -> Unit)? = null
) {
  val context = LocalContext.current
  val customers by viewModel.allCustomers.collectAsState(initial = emptyList())
  val invoices by viewModel.allInvoices.collectAsState(initial = emptyList())
  val customerTransactions by viewModel.allCustomerTransactions.collectAsState(initial = emptyList())
  val ledgerEntries by viewModel.allLedgerEntries.collectAsState(initial = emptyList())
  val banks by viewModel.allBanks.collectAsState(initial = emptyList())
  val currencySymbol by viewModel.currencySymbol.collectAsState()
  val storeName by viewModel.storeName.collectAsState()
  val currentUser by viewModel.currentUser.collectAsState()

  // Calculate Balances Map for customers (positive = owes us / مدين)
  val customerBalances = remember(customers, invoices, customerTransactions) {
    customers.associate { cust ->
      val custInvoices = invoices.filter { it.customerId == cust.id && !it.isPurchase && !it.isDraft }
      val custTxs = customerTransactions.filter { it.customerId == cust.id }
      val totalDebit = custInvoices.sumOf { it.total } + custTxs.filter { it.transactionType in listOf("بيع", "مدين") }.sumOf { it.amount }
      val totalCredit = custInvoices.sumOf { it.paidAmount } + custTxs.filter { it.transactionType in listOf("سداد", "دفعة", "تحصيل", "إيداع", "دائن") }.sumOf { it.amount }
      cust.id to (totalDebit - totalCredit)
    }
  }

  // Unified list of receipt voucher items
  val allReceipts = remember(customerTransactions, ledgerEntries, customers, customerBalances, invoices) {
    val items = mutableListOf<ReceiptVoucherItem>()

    // 1. Customer Payment Transactions (سندات قبض دفعات العملاء)
    customerTransactions.filter { it.transactionType in listOf("دفعة", "سداد", "تحصيل", "دائن") }.forEach { ctx ->
      val customer = customers.find { it.id == ctx.customerId }
      val custName = customer?.name ?: "عميل رقم #${ctx.customerId}"
      val phone = customer?.phone
      val remBal = customerBalances[ctx.customerId]
      
      // Extract payment method if present in note
      val paymentMethod = when {
        ctx.note.contains("بنك", ignoreCase = true) || ctx.note.contains("تحويل", ignoreCase = true) -> "تحويل بنكي"
        ctx.note.contains("شيك", ignoreCase = true) -> "شيك"
        ctx.note.contains("بطاقة", ignoreCase = true) || ctx.note.contains("فيزا", ignoreCase = true) -> "بطاقة بنكية"
        else -> "نقداً (كاش)"
      }

      val matchingCheckInv = if (paymentMethod == "شيك") {
        invoices.find { it.paymentMethod == "شيك" && it.customerId == ctx.customerId && Math.abs(it.invoiceDate - ctx.date) < 180000 && Math.abs(it.total - ctx.amount) < 0.01 }
      } else null

      val parsedCheckNum = if (ctx.note.contains("شيك رقم")) {
        Regex("""شيك رقم:?\s*#?([^\s\-\]\[\(\)]+)""").find(ctx.note)?.groupValues?.getOrNull(1)
      } else null

      val parsedBankName = if (ctx.note.contains("بنك:")) {
        Regex("""بنك:\s*([^\-\]\[\(\)]+)""").find(ctx.note)?.groupValues?.getOrNull(1)?.trim()
      } else null

      val parsedDueStr = if (ctx.note.contains("استحقاق:")) {
        Regex("""استحقاق:\s*([^\-\]\[\(\)]+)""").find(ctx.note)?.groupValues?.getOrNull(1)?.trim()
      } else null

      val parsedDueDate = try {
        if (!parsedDueStr.isNullOrBlank()) SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).parse(parsedDueStr)?.time else null
      } catch (e: Exception) { null }

      val parsedDrawer = if (ctx.note.contains("الساحب:")) {
        Regex("""الساحب:\s*([^\-\]\[\(\)]+)""").find(ctx.note)?.groupValues?.getOrNull(1)?.trim()
      } else null

      items.add(
        ReceiptVoucherItem(
          id = "CTX-${ctx.id}",
          numericId = ctx.id,
          date = ctx.date,
          partyName = custName,
          customerId = ctx.customerId,
          customerPhone = phone,
          amount = ctx.amount,
          paymentMethod = paymentMethod,
          notes = ctx.note.ifBlank { "سند قبض دفعة حساب" },
          receiptType = "قبض من عميل",
          isCustomerTx = true,
          originalCustomerTx = ctx,
          remainingBalance = remBal,
          checkNumber = matchingCheckInv?.checkNumber ?: parsedCheckNum,
          checkBankName = matchingCheckInv?.checkBankName ?: parsedBankName,
          checkDueDate = matchingCheckInv?.checkDate ?: matchingCheckInv?.dueDate ?: parsedDueDate,
          checkDrawer = parsedDrawer ?: custName,
          checkStatus = matchingCheckInv?.checkStatus ?: if (paymentMethod == "شيك") "تحت التحصيل" else null
        )
      )
    }

    // 2. General Ledger Receipt Entries (سندات القبض المسجلة في القيود العامة للإيرادات)
    ledgerEntries.filter { it.description.contains("سند قبض") || (it.debitAccount.contains("الصندوق") && it.creditAccount.contains("إيراد")) || (it.debitAccount.contains("شيكات") && it.creditAccount.contains("إيراد")) }.forEach { le ->
      // Avoid duplicates if customer transaction also created a ledger entry
      val isDuplicate = items.any { Math.abs(it.date - le.date) < 2000 && Math.abs(it.amount - le.amount) < 0.01 }
      if (!isDuplicate) {
        val paymentMethod = when {
          le.debitAccount.contains("شيك") || le.description.contains("شيك") -> "شيك"
          le.debitAccount.contains("بنك") -> "تحويل بنكي"
          else -> "نقداً (كاش)"
        }

        val matchingCheckInv = if (paymentMethod == "شيك") {
          invoices.find { it.paymentMethod == "شيك" && Math.abs(it.invoiceDate - le.date) < 180000 && Math.abs(it.total - le.amount) < 0.01 }
        } else null

        val parsedCheckNum = if (le.description.contains("شيك رقم")) {
          Regex("""شيك رقم:?\s*#?([^\s\-\]\[\(\)]+)""").find(le.description)?.groupValues?.getOrNull(1)
        } else null

        val parsedBankName = if (le.description.contains("بنك:")) {
          Regex("""بنك:\s*([^\-\]\[\(\)]+)""").find(le.description)?.groupValues?.getOrNull(1)?.trim()
        } else null

        val parsedDueStr = if (le.description.contains("استحقاق:")) {
          Regex("""استحقاق:\s*([^\-\]\[\(\)]+)""").find(le.description)?.groupValues?.getOrNull(1)?.trim()
        } else null

        val parsedDueDate = try {
          if (!parsedDueStr.isNullOrBlank()) SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).parse(parsedDueStr)?.time else null
        } catch (e: Exception) { null }

        val partyNameClean = le.creditAccount.replace("حساب الإيرادات: ", "").replace("حساب الإيراد: ", "").replace("حساب العملاء: ", "")

        items.add(
          ReceiptVoucherItem(
            id = "LE-${le.id}",
            numericId = le.id,
            date = le.date,
            partyName = partyNameClean,
            customerId = null,
            customerPhone = null,
            amount = le.amount,
            paymentMethod = paymentMethod,
            notes = le.description.ifBlank { "سند إيراد عام" },
            receiptType = if (le.creditAccount.contains("عميل") || le.creditAccount.contains("العملاء")) "قبض من عميل" else "إيراد عام",
            isCustomerTx = false,
            originalLedgerEntry = le,
            checkNumber = matchingCheckInv?.checkNumber ?: parsedCheckNum,
            checkBankName = matchingCheckInv?.checkBankName ?: parsedBankName,
            checkDueDate = matchingCheckInv?.checkDate ?: matchingCheckInv?.dueDate ?: parsedDueDate,
            checkDrawer = partyNameClean,
            checkStatus = matchingCheckInv?.checkStatus ?: if (paymentMethod == "شيك") "تحت التحصيل" else null
          )
        )
      }
    }

    items.sortedByDescending { it.date }
  }

  // Filter and Search states
  var searchQuery by remember { mutableStateOf("") }
  var selectedTab by remember { mutableStateOf(0) } // 0: الكل, 1: تحصيل عملاء, 2: إيرادات عامة, 3: عملاء عليهم مديونيات
  var showAddDialog by remember { mutableStateOf(false) }
  var isTableView by remember { mutableStateOf(false) }
  var preSelectedCustomer by remember { mutableStateOf<Customer?>(null) }
  var itemToDelete by remember { mutableStateOf<ReceiptVoucherItem?>(null) }

  // Filtered List
  val filteredReceipts = remember(allReceipts, searchQuery, selectedTab) {
    var list = allReceipts
    if (selectedTab == 1) {
      list = list.filter { it.receiptType == "قبض من عميل" }
    } else if (selectedTab == 2) {
      list = list.filter { it.receiptType != "قبض من عميل" }
    }

    if (searchQuery.isNotBlank()) {
      val query = searchQuery.trim().lowercase()
      list = list.filter {
        it.partyName.lowercase().contains(query) ||
        it.notes.lowercase().contains(query) ||
        it.id.lowercase().contains(query) ||
        it.amount.toString().contains(query) ||
        (it.customerPhone?.contains(query) == true)
      }
    }
    list
  }

  // Debtor Customers (العملاء الذين عليهم مديونيات)
  val debtorCustomers = remember(customers, customerBalances, searchQuery) {
    var list = customers.mapNotNull { cust ->
      val bal = customerBalances[cust.id] ?: 0.0
      if (bal > 0.01) cust to bal else null
    }.sortedByDescending { it.second }

    if (searchQuery.isNotBlank()) {
      val q = searchQuery.trim().lowercase()
      list = list.filter { (cust, _) ->
        cust.name.lowercase().contains(q) || cust.phone.contains(q)
      }
    }
    list
  }

  // Quick stats
  val totalCollectedAllTime = remember(allReceipts) { allReceipts.sumOf { it.amount } }
  val todayStartMillis = remember {
    val cal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    cal.timeInMillis
  }
  val todayCollected = remember(allReceipts, todayStartMillis) {
    allReceipts.filter { it.date >= todayStartMillis }.sumOf { it.amount }
  }
  val totalOutstandingDebts = remember(debtorCustomers) {
    debtorCustomers.sumOf { it.second }
  }

  Scaffold(
    topBar = {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            if (onBackClick != null) {
              IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(36.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.ArrowForward,
                  contentDescription = "رجوع",
                  tint = MaterialTheme.colorScheme.onSurface
                )
              }
            }
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF16A34A).copy(alpha = 0.12f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.CallReceived,
                contentDescription = null,
                tint = Color(0xFF16A34A),
                modifier = Modifier.size(22.dp)
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "سندات القبض",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "تحصيل العملاء والإيرادات",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          Button(
            onClick = {
              preSelectedCustomer = null
              showAddDialog = true
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.testTag("new_receipt_voucher_button")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "سند قبض جديد",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = Color.White,
              maxLines = 1,
              softWrap = false
            )
          }
        }
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

      // Summary Metric Cards
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        ReceiptMetricCard(
          title = "إجمالي المقبوضات",
          value = "${String.format("%.2f", totalCollectedAllTime)} $currencySymbol",
          subtitle = "${allReceipts.size} سند مسجل",
          icon = Icons.Default.AccountBalanceWallet,
          color = Color(0xFF16A34A),
          modifier = Modifier.weight(1f)
        )
        ReceiptMetricCard(
          title = "تحصيلات اليوم",
          value = "${String.format("%.2f", todayCollected)} $currencySymbol",
          subtitle = "اليوم الحالي",
          icon = Icons.Default.TrendingUp,
          color = Color(0xFF2563EB),
          modifier = Modifier.weight(1f)
        )
        ReceiptMetricCard(
          title = "ديون للتحصيل",
          value = "${String.format("%.2f", totalOutstandingDebts)} $currencySymbol",
          subtitle = "${debtorCustomers.size} عميل مدين",
          icon = Icons.Default.Warning,
          color = if (totalOutstandingDebts > 0) Color(0xFFDC2626) else Color(0xFF16A34A),
          modifier = Modifier.weight(1f)
        )
      }

      // Search & Tabs
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
      ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = { Text("بحث برقم السند، العميل، المبلغ...", fontSize = 12.sp) },
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
              trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                  IconButton(onClick = { searchQuery = "" }) {
                    Icon(Icons.Default.Clear, contentDescription = "مسح", modifier = Modifier.size(18.dp))
                  }
                }
              },
              shape = RoundedCornerShape(10.dp),
              singleLine = true,
              modifier = Modifier.weight(1f)
            )

            // View Mode Toggle
            if (selectedTab != 3) {
              Box(
                modifier = Modifier
                  .size(54.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant)
                  .clickable { isTableView = !isTableView },
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = if (isTableView) Icons.Default.ViewAgenda else Icons.Default.TableChart,
                  contentDescription = "تبديل العرض",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(24.dp)
                )
              }
            }
          }

          ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            divider = {},
            containerColor = Color.Transparent
          ) {
            Tab(
              selected = selectedTab == 0,
              onClick = { selectedTab = 0 },
              text = { Text("كافة المقبوضات (${allReceipts.size})", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) }
            )
            Tab(
              selected = selectedTab == 1,
              onClick = { selectedTab = 1 },
              text = { Text("تحصيل عملاء (${allReceipts.count { it.receiptType == "قبض من عميل" }})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) }
            )
            Tab(
              selected = selectedTab == 2,
              onClick = { selectedTab = 2 },
              text = { Text("إيرادات عامة (${allReceipts.count { it.receiptType != "قبض من عميل" }})", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) }
            )
            Tab(
              selected = selectedTab == 3,
              onClick = { selectedTab = 3 },
              text = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text("عملاء عليهم ديون (${debtorCustomers.size})", fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
                  if (debtorCustomers.isNotEmpty()) {
                    Box(
                      modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDC2626))
                    )
                  }
                }
              }
            )
          }
        }
      }

      // Content List
      if (selectedTab == 3) {
        // Debtor Customers List with 1-Click "Quick Collect"
        if (debtorCustomers.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize().padding(30.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(54.dp))
              Spacer(modifier = Modifier.height(10.dp))
              Text("رائع! لا يوجد أي عملاء عليهم مديونيات حالياً", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
              Text("كافة حسابات العملاء مسددة ومتزنة.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(debtorCustomers) { (customer, debt) ->
              DebtorCustomerCard(
                customer = customer,
                debtAmount = debt,
                currencySymbol = currencySymbol,
                onQuickCollect = {
                  preSelectedCustomer = customer
                  showAddDialog = true
                },
                onShareWhatsAppReminder = {
                  val cleanPhone = customer.phone.filter { it.isDigit() }
                  if (cleanPhone.isNotBlank()) {
                    val msg = """
                      السلام عليكم أخي الكريم ${customer.name} 
                      نود تذكيركم بأن الرصيد المستحق في حسابكم لدى *$storeName* هو:
                       *${String.format("%.2f", debt)} $currencySymbol*
                      شاكرين لكم حسن تعاونكم الدائم 
                    """.trimIndent()
                    try {
                      val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanPhone?text=${Uri.encode(msg)}"))
                      context.startActivity(intent)
                    } catch (e: Exception) {
                      viewModel.showToast("تعذر فتح تطبيق واتساب.")
                    }
                  } else {
                    viewModel.showToast("لا يوجد رقم هاتف مسجل لهذا العميل.")
                  }
                }
              )
            }
          }
        }
      } else {
        // Receipt Vouchers List
        if (filteredReceipts.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize().padding(30.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.size(54.dp))
              Spacer(modifier = Modifier.height(10.dp))
              Text("لا توجد سندات قبض مطابقة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
              Text("اضغط على زر 'سند قبض جديد' بالأعلى لتسجيل أي عملية تحصيل أو قبض.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
          }
        } else {
          if (isTableView) {
            ReceiptVouchersTable(
              receipts = filteredReceipts,
              currencySymbol = currencySymbol,
              onPrint = { receipt ->
                viewModel.exportReceiptVoucherToPdf(
                  context = context,
                  voucherNo = receipt.id,
                  dateMillis = receipt.date,
                  receivedFrom = receipt.partyName,
                  amount = receipt.amount,
                  paymentMethod = receipt.paymentMethod,
                  notes = receipt.notes,
                  receiptType = receipt.receiptType,
                  remainingBalance = receipt.remainingBalance,
                  checkNumber = receipt.checkNumber,
                  checkBankName = receipt.checkBankName,
                  checkDueDate = receipt.checkDueDate,
                  checkDrawer = receipt.checkDrawer,
                  checkStatus = receipt.checkStatus
                )
              },
              onShareWhatsApp = { receipt ->
                val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(receipt.date))
                val cleanPhone = receipt.customerPhone?.filter { it.isDigit() } ?: ""
                val remainingStr = if (receipt.remainingBalance != null) "\n *الرصيد المتبقي:* ${String.format("%.2f", Math.abs(receipt.remainingBalance))} $currencySymbol" else ""
                val checkInfoStr = if (receipt.paymentMethod.contains("شيك") || !receipt.checkNumber.isNullOrBlank()) {
                  val chkNum = receipt.checkNumber?.let { "\n *رقم الشيك:* $it" } ?: ""
                  val chkBank = receipt.checkBankName?.let { "\n *البنك:* $it" } ?: ""
                  val chkDue = receipt.checkDueDate?.let { "\n *تاريخ الاستحقاق:* ${SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(it))}" } ?: ""
                  val chkStatus = receipt.checkStatus?.let { "\n *حالة الشيك:* $it" } ?: ""
                  "$chkNum$chkBank$chkDue$chkStatus"
                } else ""
                val msg = """
                   *سند قبض مالي - $storeName*
                  
                   *رقم السند:* ${receipt.id}
                   *التاريخ:* $dateStr
                   *وصلنا من:* ${receipt.partyName}
                   *المبلغ المقبوض:* ${String.format("%.2f", receipt.amount)} $currencySymbol
                   *طريقة القبض:* ${receipt.paymentMethod}$checkInfoStr
                   *البيان:* ${receipt.notes}$remainingStr
                  
                   *المستلم:* $currentUser
                  شكراً لتعاملكم معنا! 
                """.trimIndent()
                try {
                  val url = if (cleanPhone.isNotBlank()) "https://wa.me/$cleanPhone?text=${Uri.encode(msg)}" else "https://wa.me/?text=${Uri.encode(msg)}"
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                  context.startActivity(intent)
                } catch (e: Exception) {
                  viewModel.showToast("تعذر فتح تطبيق واتساب.")
                }
              },
              onDelete = { receipt -> itemToDelete = receipt }
            )
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              items(filteredReceipts) { receipt ->
                ReceiptVoucherCard(
                  receipt = receipt,
                  currencySymbol = currencySymbol,
                  onPrint = {
                    viewModel.exportReceiptVoucherToPdf(
                      context = context,
                      voucherNo = receipt.id,
                      dateMillis = receipt.date,
                      receivedFrom = receipt.partyName,
                      amount = receipt.amount,
                      paymentMethod = receipt.paymentMethod,
                      notes = receipt.notes,
                      receiptType = receipt.receiptType,
                      remainingBalance = receipt.remainingBalance,
                      checkNumber = receipt.checkNumber,
                      checkBankName = receipt.checkBankName,
                      checkDueDate = receipt.checkDueDate,
                      checkDrawer = receipt.checkDrawer,
                      checkStatus = receipt.checkStatus
                    )
                  },
                  onShareWhatsApp = {
                    val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(receipt.date))
                    val cleanPhone = receipt.customerPhone?.filter { it.isDigit() } ?: ""
                    val remainingStr = if (receipt.remainingBalance != null) "\n *الرصيد المتبقي:* ${String.format("%.2f", Math.abs(receipt.remainingBalance))} $currencySymbol" else ""
                    val checkInfoStr = if (receipt.paymentMethod.contains("شيك") || !receipt.checkNumber.isNullOrBlank()) {
                      val chkNum = receipt.checkNumber?.let { "\n *رقم الشيك:* $it" } ?: ""
                      val chkBank = receipt.checkBankName?.let { "\n *البنك:* $it" } ?: ""
                      val chkDue = receipt.checkDueDate?.let { "\n *تاريخ الاستحقاق:* ${SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(it))}" } ?: ""
                      val chkStatus = receipt.checkStatus?.let { "\n *حالة الشيك:* $it" } ?: ""
                      "$chkNum$chkBank$chkDue$chkStatus"
                    } else ""
                    
                    val msg = """
                      *سند قبض مالي - $storeName*
                      ------------------------------------
                      *رقم السند:* ${receipt.id}
                      *التاريخ:* $dateStr
                      *وصلنا من:* ${receipt.partyName}
                      *المبلغ المقبوض:* ${String.format("%.2f", receipt.amount)} $currencySymbol
                      *طريقة القبض:* ${receipt.paymentMethod}$checkInfoStr
                      *البيان:* ${receipt.notes}$remainingStr
                      ------------------------------------
                      *المستلم:* $currentUser
                      شكراً لتعاملكم معنا.
                    """.trimIndent()

                    try {
                      val url = if (cleanPhone.isNotBlank()) "https://wa.me/$cleanPhone?text=${Uri.encode(msg)}" else "https://wa.me/?text=${Uri.encode(msg)}"
                      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                      context.startActivity(intent)
                    } catch (e: Exception) {
                      viewModel.showToast("تعذر فتح تطبيق واتساب.")
                    }
                  },
                  onDelete = {
                    itemToDelete = receipt
                  }
                )
              }
            }
          }
        }
      }
    }
  }

  // Delete Confirmation Dialog
  if (itemToDelete != null) {
    val item = itemToDelete!!
    AlertDialog(
      onDismissRequest = { itemToDelete = null },
      title = { Text("حذف سند القبض", fontWeight = FontWeight.Bold) },
      text = {
        Text("هل أنت متأكد من حذف سند القبض رقم ${item.id} بمبلغ ${String.format("%.2f", item.amount)} $currencySymbol للطرف (${item.partyName})؟ سيتم عكس وتعديل الرصيد تلقائياً.")
      },
      confirmButton = {
        Button(
          onClick = {
            if (item.isCustomerTx && item.originalCustomerTx != null) {
              viewModel.deleteCustomerTransaction(item.originalCustomerTx)
            } else if (item.originalLedgerEntry != null) {
              viewModel.deleteLedgerEntry(item.originalLedgerEntry)
            }
            itemToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("تأكيد الحذف")
        }
      },
      dismissButton = {
        TextButton(onClick = { itemToDelete = null }) {
          Text("إلغاء")
        }
      }
    )
  }

  // New Receipt Voucher Dialog
  if (showAddDialog) {
    NewReceiptVoucherDialog(
      viewModel = viewModel,
      customers = customers,
      customerBalances = customerBalances,
      banks = banks,
      currencySymbol = currencySymbol,
      initialCustomer = preSelectedCustomer,
      onDismiss = {
        showAddDialog = false
        preSelectedCustomer = null
      }
    )
  }
}

@Composable
fun ReceiptMetricCard(
  title: String,
  value: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  color: Color,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(1.dp),
    border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
      }
      Text(
        text = value,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
fun ReceiptVoucherCard(
  receipt: ReceiptVoucherItem,
  currencySymbol: String,
  onPrint: () -> Unit,
  onShareWhatsApp: () -> Unit,
  onDelete: () -> Unit
) {
  val dateStr = remember(receipt.date) {
    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(receipt.date))
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(1.5.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      // Header Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(Color(0xFF16A34A).copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.CallReceived, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
          }
          Column {
            Text(
              text = receipt.partyName,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (receipt.receiptType == "قبض من عميل") Color(0xFF2563EB).copy(alpha = 0.1f) else Color(0xFF9333EA).copy(alpha = 0.1f)
              ) {
                Text(
                  text = receipt.receiptType,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (receipt.receiptType == "قبض من عميل") Color(0xFF2563EB) else Color(0xFF9333EA),
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
              Text("• $dateStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }

        // Amount Box
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "+${String.format("%.2f", receipt.amount)} $currencySymbol",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF16A34A)
          )
          Text(
            text = receipt.paymentMethod,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // Notes & Statement
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            text = receipt.notes,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      // Check Details Card if check payment or check info exists
      if (receipt.paymentMethod.contains("شيك") || !receipt.checkNumber.isNullOrBlank()) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFF0284C7).copy(alpha = 0.08f),
          border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF0284C7))
              Text("بيانات الشيك البنكي:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
              if (receipt.checkStatus != null) {
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = Color(0xFF0284C7).copy(alpha = 0.15f)
                ) {
                  Text(
                    text = receipt.checkStatus,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0369A1),
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                  )
                }
              }
            }
            
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              val chkNum = receipt.checkNumber ?: "غير محدد"
              val chkBank = receipt.checkBankName ?: "شيك بنكي"
              Text(" رقم: #$chkNum", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF334155))
              Text(" البنك: $chkBank", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF334155))
            }

            if (receipt.checkDueDate != null) {
              val dueStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(receipt.checkDueDate))
              Text(" تاريخ الاستحقاق / الصرف: $dueStr", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }

      // Remaining Balance badge if available
      if (receipt.remainingBalance != null) {
        val rem = receipt.remainingBalance
        val isDebt = rem > 0.01
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "الرصيد المتبقي على العميل:",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "${String.format("%.2f", Math.abs(rem))} $currencySymbol ${if (isDebt) "(مدين)" else if (rem < -0.01) "(دائن)" else "(متزن)"}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDebt) Color(0xFFDC2626) else Color(0xFF16A34A)
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

      // Action Buttons Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "رقم السند: #${receipt.numericId}",
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          // WhatsApp Share
          IconButton(
            onClick = onShareWhatsApp,
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(Color(0xFF25D366).copy(alpha = 0.12f))
          ) {
            Icon(Icons.Default.Share, contentDescription = "واتساب", tint = Color(0xFF128C7E), modifier = Modifier.size(16.dp))
          }

          // Print PDF
          IconButton(
            onClick = onPrint,
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(Color(0xFF2563EB).copy(alpha = 0.12f))
          ) {
            Icon(Icons.Default.Print, contentDescription = "طباعة", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
          }

          // Delete
          IconButton(
            onClick = onDelete,
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
          ) {
            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
          }
        }
      }
    }
  }
}

@Composable
fun DebtorCustomerCard(
  customer: Customer,
  debtAmount: Double,
  currencySymbol: String,
  onQuickCollect: () -> Unit,
  onShareWhatsAppReminder: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(1.5.dp),
    border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.3f))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color(0xFFDC2626).copy(alpha = 0.1f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(22.dp))
        }
        Column {
          Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
          if (customer.phone.isNotBlank()) {
            Text(customer.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Text(
            text = "المديونية المستحقة: ${String.format("%.2f", debtAmount)} $currencySymbol",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color(0xFFDC2626)
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (customer.phone.isNotBlank()) {
          IconButton(
            onClick = onShareWhatsAppReminder,
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF25D366).copy(alpha = 0.12f))
          ) {
            Icon(Icons.Default.Chat, contentDescription = "تذكير واتساب", tint = Color(0xFF128C7E), modifier = Modifier.size(18.dp))
          }
        }

        Button(
          onClick = onQuickCollect,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Icon(Icons.Default.CallReceived, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
          Spacer(modifier = Modifier.width(4.dp))
          Text("قبض الآن", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReceiptVoucherDialog(
  viewModel: AppViewModel,
  customers: List<Customer>,
  customerBalances: Map<Int, Double>,
  banks: List<Bank>,
  currencySymbol: String,
  initialCustomer: Customer? = null,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  var selectedTypeIndex by remember { mutableStateOf(if (initialCustomer != null) 0 else 0) } // 0: قبض من عميل, 1: إيراد عام
  
  // Customer selection
  var selectedCustomer by remember { mutableStateOf(initialCustomer) }
  var customerSearchText by remember { mutableStateOf(initialCustomer?.name ?: "") }
  var isCustomerDropdownExpanded by remember { mutableStateOf(false) }

  // Other party name
  var manualPartyName by remember { mutableStateOf("") }
  var revenueType by remember { mutableStateOf("إيرادات مبيعات وخدمات") }
  val revenueTypes = listOf("إيرادات مبيعات وخدمات", "إيرادات صيانة وتصليح", "إيرادات تأجير واستثمار", "استرداد مصاريف وتأمينات", "إيرادات متنوعة أخرى")

  // Amount & payment
  val customerDebt = remember(selectedCustomer, customerBalances) {
    if (selectedCustomer != null) customerBalances[selectedCustomer!!.id] ?: 0.0 else 0.0
  }
  var amountText by remember {
    mutableStateOf(if (initialCustomer != null && customerDebt > 0) String.format("%.2f", customerDebt) else "")
  }
  var paymentMethod by remember { mutableStateOf("نقداً (كاش)") }
  val paymentMethods = listOf("نقداً (كاش)", "تحويل بنكي", "شيك", "محفظة إلكترونية")
  var selectedBankName by remember { mutableStateOf(banks.firstOrNull()?.name ?: "") }
  var notesText by remember { mutableStateOf(if (initialCustomer != null) "سداد حساب عميل" else "سند قبض مالي") }

  // Check details state (بيانات الشيك البنكي)
  var checkNumber by remember { mutableStateOf("") }
  var checkNumberError by remember { mutableStateOf<String?>(null) }
  var checkBankName by remember { mutableStateOf(banks.firstOrNull()?.name ?: "بنك فلسطين") }
  var checkBankError by remember { mutableStateOf<String?>(null) }
  var checkDueDateMillis by remember {
    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 30) }
    mutableStateOf(cal.timeInMillis)
  }
  var checkDueDateText by remember {
    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 30) }
    mutableStateOf(sdf.format(cal.time))
  }
  var checkDrawerName by remember { mutableStateOf(initialCustomer?.name ?: "") }
  var checkBranchName by remember { mutableStateOf("") }
  var checkStatus by remember { mutableStateOf("تحت التحصيل") }
  var showDatePickerDialog by remember { mutableStateOf(false) }

  // DatePickerDialog
  if (showDatePickerDialog) {
    val cal = Calendar.getInstance().apply { timeInMillis = checkDueDateMillis }
    android.app.DatePickerDialog(
      context,
      { _, year, month, dayOfMonth ->
        val selectedCal = Calendar.getInstance().apply {
          set(Calendar.YEAR, year)
          set(Calendar.MONTH, month)
          set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }
        checkDueDateMillis = selectedCal.timeInMillis
        checkDueDateText = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(selectedCal.time)
        showDatePickerDialog = false
      },
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH),
      cal.get(Calendar.DAY_OF_MONTH)
    ).apply {
      setOnDismissListener { showDatePickerDialog = false }
      show()
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF16A34A).copy(alpha = 0.12f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.CallReceived, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
        }
        Text("إنشاء سند قبض جديد", fontWeight = FontWeight.Bold, fontSize = 17.sp)
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Tab selector: قبض من عميل vs إيراد عام
        TabRow(
          selectedTabIndex = selectedTypeIndex,
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          divider = {}
        ) {
          Tab(
            selected = selectedTypeIndex == 0,
            onClick = { selectedTypeIndex = 0 },
            text = { Text("قبض من عميل (تحصيل مديونية)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
          )
          Tab(
            selected = selectedTypeIndex == 1,
            onClick = { selectedTypeIndex = 1 },
            text = { Text("إيراد عام / أخرى", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
          )
        }

        if (selectedTypeIndex == 0) {
          // Customer Picker
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("اختر العميل المستلم منه:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            
            ExposedDropdownMenuBox(
              expanded = isCustomerDropdownExpanded,
              onExpandedChange = { isCustomerDropdownExpanded = !isCustomerDropdownExpanded }
            ) {
              OutlinedTextField(
                value = if (selectedCustomer != null) selectedCustomer!!.name else customerSearchText,
                onValueChange = {
                  customerSearchText = it
                  selectedCustomer = null
                  isCustomerDropdownExpanded = true
                },
                placeholder = { Text("ابحث عن العميل بالاسم أو الهاتف...") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCustomerDropdownExpanded) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().menuAnchor()
              )

              val filteredCusts = customers.filter {
                it.name.contains(customerSearchText, ignoreCase = true) || it.phone.contains(customerSearchText)
              }

              ExposedDropdownMenu(
                expanded = isCustomerDropdownExpanded,
                onDismissRequest = { isCustomerDropdownExpanded = false }
              ) {
                if (filteredCusts.isEmpty()) {
                  DropdownMenuItem(
                    text = { Text("لا يوجد عميل مطابق", color = Color.Gray, fontSize = 12.sp) },
                    onClick = {}
                  )
                } else {
                  filteredCusts.forEach { cust ->
                    val bal = customerBalances[cust.id] ?: 0.0
                    DropdownMenuItem(
                      text = {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Column {
                            Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            if (cust.phone.isNotBlank()) Text(cust.phone, fontSize = 10.sp, color = Color.Gray)
                          }
                          Text(
                            text = "${String.format("%.2f", bal)} $currencySymbol",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (bal > 0.01) Color(0xFFDC2626) else Color(0xFF16A34A)
                          )
                        }
                      },
                      onClick = {
                        selectedCustomer = cust
                        customerSearchText = cust.name
                        if (checkDrawerName.isBlank() || checkDrawerName == "عميل نقدي") {
                          checkDrawerName = cust.name
                        }
                        isCustomerDropdownExpanded = false
                        val debt = customerBalances[cust.id] ?: 0.0
                        if (debt > 0 && amountText.isBlank()) {
                          amountText = String.format("%.2f", debt)
                        }
                      }
                    )
                  }
                }
              }
            }
          }

          // Customer balance summary banner
          if (selectedCustomer != null) {
            val bal = customerBalances[selectedCustomer!!.id] ?: 0.0
            val isDebt = bal > 0.01
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isDebt) Color(0xFFDC2626).copy(alpha = 0.08f) else Color(0xFF16A34A).copy(alpha = 0.08f),
              border = BorderStroke(1.dp, if (isDebt) Color(0xFFDC2626).copy(alpha = 0.3f) else Color(0xFF16A34A).copy(alpha = 0.3f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text("الرصيد الحالي للعميل:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text(
                    text = "${String.format("%.2f", Math.abs(bal))} $currencySymbol ${if (isDebt) "(مدين - عليه مبلغ)" else if (bal < -0.01) "(دائن - له رصيد)" else "(خالص)"}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isDebt) Color(0xFFDC2626) else Color(0xFF16A34A)
                  )
                }

                if (isDebt) {
                  Button(
                    onClick = { amountText = String.format("%.2f", bal) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                  ) {
                    Text("سداد كامل المديونية", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                  }
                }
              }
            }
          }
        } else {
          // General / Other revenue
          OutlinedTextField(
            value = manualPartyName,
            onValueChange = { 
              manualPartyName = it
              if (checkDrawerName.isBlank()) checkDrawerName = it
            },
            label = { Text("المستلم منه (اسم الشخص أو الجهة - اختياري)") },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          )

          var revExpanded by remember { mutableStateOf(false) }
          ExposedDropdownMenuBox(
            expanded = revExpanded,
            onExpandedChange = { revExpanded = !revExpanded }
          ) {
            OutlinedTextField(
              value = revenueType,
              onValueChange = {},
              readOnly = true,
              label = { Text("نوع الإيراد / القبض") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = revExpanded) },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = revExpanded, onDismissRequest = { revExpanded = false }) {
              revenueTypes.forEach { t ->
                DropdownMenuItem(
                  text = { Text(t) },
                  onClick = {
                    revenueType = t
                    revExpanded = false
                  }
                )
              }
            }
          }
        }

        // Amount Field
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it },
          label = { Text("المبلغ المقبوض ($currencySymbol) *") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          shape = RoundedCornerShape(10.dp),
          leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = Color(0xFF16A34A)) },
          modifier = Modifier.fillMaxWidth()
        )

        // Payment Method Selector
        var payExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
          expanded = payExpanded,
          onExpandedChange = { payExpanded = !payExpanded }
        ) {
          OutlinedTextField(
            value = paymentMethod,
            onValueChange = {},
            readOnly = true,
            label = { Text("طريقة القبض / الاستلام") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payExpanded) },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().menuAnchor()
          )
          ExposedDropdownMenu(expanded = payExpanded, onDismissRequest = { payExpanded = false }) {
            paymentMethods.forEach { method ->
              DropdownMenuItem(
                text = { Text(method) },
                onClick = {
                  paymentMethod = method
                  payExpanded = false
                  if (method == "شيك" && notesText == "سند قبض مالي") {
                    notesText = "سند قبض شيك"
                  }
                }
              )
            }
          }
        }

        // If bank transfer -> show bank selector
        if (paymentMethod == "تحويل بنكي" && banks.isNotEmpty()) {
          var bankExpanded by remember { mutableStateOf(false) }
          ExposedDropdownMenuBox(
            expanded = bankExpanded,
            onExpandedChange = { bankExpanded = !bankExpanded }
          ) {
            OutlinedTextField(
              value = selectedBankName,
              onValueChange = {},
              readOnly = true,
              label = { Text("الحساب البنكي المستلم") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bankExpanded) },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = bankExpanded, onDismissRequest = { bankExpanded = false }) {
              banks.forEach { b ->
                val codeInfo = if (b.code.isNotBlank()) " (${b.code})" else ""
                DropdownMenuItem(
                  text = { Text("${b.name}$codeInfo") },
                  onClick = {
                    selectedBankName = b.name
                    bankExpanded = false
                  }
                )
              }
            }
          }
        }

        // Check Details Section (عند اختيار الدفع بالشيك)
        AnimatedVisibility(
          visible = paymentMethod == "شيك",
          enter = expandVertically() + fadeIn(),
          exit = shrinkVertically() + fadeOut()
        ) {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("بيانات وتفاصيل الشيك البنكي", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
              }

              // 1. Check Number Field
              OutlinedTextField(
                value = checkNumber,
                onValueChange = { 
                  checkNumber = it
                  checkNumberError = null
                },
                label = { Text("رقم الشيك *") },
                placeholder = { Text("مثال: 108429") },
                leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(10.dp),
                isError = checkNumberError != null,
                supportingText = { if (checkNumberError != null) Text(checkNumberError!!, color = MaterialTheme.colorScheme.error) },
                modifier = Modifier.fillMaxWidth()
              )

              // 2. Bank Name with suggestions & custom typing
              var bankDropdownExpanded by remember { mutableStateOf(false) }
              val popularBanks = remember {
                listOf(
                  "بنك فلسطين",
                  "البنك العربي",
                  "بنك القدس",
                  "بنك الإسكان للتجارة والتمويل",
                  "بنك القاهرة عمان",
                  "البنك الإسلامي العربي",
                  "البنك الإسلامي الفلسطيني",
                  "بنك الأردن",
                  "بنك الاستثمار الفلسطيني",
                  "بنك صفا",
                  "البنك التجاري الأردني",
                  "البنك الأهلي الأردني",
                  "مصرف الراجحي",
                  "البنك الأهلي السعودي",
                  "بنك الرياض",
                  "بنك مصر",
                  "البنك الأهلي المصري",
                  "بنك أبوظبي الأول"
                )
              }
              val allBankChoices = remember(banks) {
                (banks.map { it.name } + popularBanks).distinct().filter { it.isNotBlank() }
              }

              ExposedDropdownMenuBox(
                expanded = bankDropdownExpanded,
                onExpandedChange = { bankDropdownExpanded = !bankDropdownExpanded }
              ) {
                OutlinedTextField(
                  value = checkBankName,
                  onValueChange = { 
                    checkBankName = it
                    checkBankError = null
                    bankDropdownExpanded = true
                  },
                  label = { Text("اسم البنك المسحوب عليه *") },
                  placeholder = { Text("اختر أو اكتب اسم البنك...") },
                  leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                  trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bankDropdownExpanded) },
                  shape = RoundedCornerShape(10.dp),
                  isError = checkBankError != null,
                  supportingText = { if (checkBankError != null) Text(checkBankError!!, color = MaterialTheme.colorScheme.error) },
                  modifier = Modifier.fillMaxWidth().menuAnchor()
                )

                val filteredBanks = allBankChoices.filter { it.contains(checkBankName, ignoreCase = true) }
                ExposedDropdownMenu(
                  expanded = bankDropdownExpanded,
                  onDismissRequest = { bankDropdownExpanded = false }
                ) {
                  if (filteredBanks.isEmpty()) {
                    DropdownMenuItem(
                      text = { Text("استخدم \"$checkBankName\" كاسم للبنك", fontSize = 12.sp) },
                      onClick = { bankDropdownExpanded = false }
                    )
                  } else {
                    filteredBanks.forEach { bName ->
                      DropdownMenuItem(
                        text = { Text(bName, fontSize = 13.sp) },
                        onClick = {
                          checkBankName = bName
                          checkBankError = null
                          bankDropdownExpanded = false
                        }
                      )
                    }
                  }
                }
              }

              // 3. Due Date / Check Maturity Date with interactive picker & quick chips
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("تاريخ استحقاق الشيك (تاريخ الصرف):", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                
                OutlinedTextField(
                  value = checkDueDateText,
                  onValueChange = { checkDueDateText = it },
                  label = { Text("تاريخ الاستحقاق (سنة/شهر/يوم)") },
                  leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                  trailingIcon = {
                    IconButton(onClick = { showDatePickerDialog = true }) {
                      Icon(Icons.Default.DateRange, contentDescription = "اختر التاريخ", tint = MaterialTheme.colorScheme.primary)
                    }
                  },
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.fillMaxWidth()
                )

                // Quick date offset chips
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  listOf(
                    "اليوم" to 0,
                    "بعد أسبوع" to 7,
                    "بعد 15 يوم" to 15,
                    "بعد 30 يوم" to 30,
                    "بعد شهرين" to 60,
                    "بعد 3 أشهر" to 90,
                    "بعد 6 أشهر" to 180
                  ).forEach { (label, days) ->
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = MaterialTheme.colorScheme.surface,
                      border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                      modifier = Modifier.clickable {
                        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, days) }
                        checkDueDateMillis = cal.timeInMillis
                        checkDueDateText = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(cal.time)
                      }
                    ) {
                      Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                      )
                    }
                  }
                }
              }

              // 4. Drawer / Check Issuer Name
              OutlinedTextField(
                value = checkDrawerName,
                onValueChange = { checkDrawerName = it },
                label = { Text("اسم الساحب / صاحب الشيك") },
                placeholder = { Text("اسم صاحب الحساب البنكي المصدر للشيك") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
              )

              // 5. Branch & Status in a Row
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedTextField(
                  value = checkBranchName,
                  onValueChange = { checkBranchName = it },
                  label = { Text("فرع البنك") },
                  placeholder = { Text("مثال: الفرع الرئيسي") },
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.weight(1f)
                )

                var statusExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                  expanded = statusExpanded,
                  onExpandedChange = { statusExpanded = !statusExpanded },
                  modifier = Modifier.weight(1f)
                ) {
                  OutlinedTextField(
                    value = checkStatus,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("حالة الشيك") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                  )
                  ExposedDropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false }
                  ) {
                    listOf("تحت التحصيل", "قيد الانتظار", "تم التحصيل").forEach { st ->
                      DropdownMenuItem(
                        text = { Text(st, fontSize = 12.sp) },
                        onClick = {
                          checkStatus = st
                          statusExpanded = false
                        }
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // Notes / Statement
        OutlinedTextField(
          value = notesText,
          onValueChange = { notesText = it },
          label = { Text("البيان / ملاحظات السند") },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        )

        // Quick suggestions chips
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf("دفعة تحت الحساب", "سداد حساب قديم", "تحصيل مديونية", "دفعة نقدية").forEach { chip ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
              modifier = Modifier.clickable { notesText = chip }
            ) {
              Text(
                text = chip,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amt = amountText.toDoubleOrNull()
          if (amt == null || amt <= 0) {
            viewModel.showToast("يرجى إدخال مبلغ صحيح أكبر من الصفر.")
            return@Button
          }

          if (paymentMethod == "شيك") {
            if (checkNumber.trim().isBlank()) {
              checkNumberError = "يرجى كتابة رقم الشيك"
              viewModel.showToast("يرجى كتابة رقم الشيك البنكي.")
              return@Button
            }
            if (checkBankName.trim().isBlank()) {
              checkBankError = "يرجى تحديد أو كتابة اسم البنك"
              viewModel.showToast("يرجى إدخال اسم البنك المسحوب عليه.")
              return@Button
            }
          }

          if (selectedTypeIndex == 0) {
            if (selectedCustomer == null) {
              viewModel.showToast("يرجى اختيار العميل أولاً.")
              return@Button
            }
            viewModel.addReceiptVoucher(
              customerId = selectedCustomer!!.id,
              customerName = selectedCustomer!!.name,
              amount = amt,
              paymentMethod = paymentMethod,
              receiptType = "قبض من عميل",
              bankName = if (paymentMethod == "تحويل بنكي") selectedBankName else if (paymentMethod == "شيك") checkBankName.trim() else null,
              notes = notesText,
              checkNumber = if (paymentMethod == "شيك") checkNumber.trim() else null,
              checkBankName = if (paymentMethod == "شيك") checkBankName.trim() else null,
              checkDueDate = if (paymentMethod == "شيك") checkDueDateMillis else null,
              checkDrawer = if (paymentMethod == "شيك") checkDrawerName.trim().ifEmpty { selectedCustomer!!.name } else null,
              checkStatus = if (paymentMethod == "شيك") checkStatus else null,
              checkBranch = if (paymentMethod == "شيك") checkBranchName.trim().ifEmpty { null } else null,
              onSuccess = onDismiss
            )
          } else {
            val party = manualPartyName.trim().ifEmpty { revenueType }
            viewModel.addReceiptVoucher(
              customerId = null,
              customerName = party,
              amount = amt,
              paymentMethod = paymentMethod,
              receiptType = revenueType,
              bankName = if (paymentMethod == "تحويل بنكي") selectedBankName else if (paymentMethod == "شيك") checkBankName.trim() else null,
              notes = notesText,
              checkNumber = if (paymentMethod == "شيك") checkNumber.trim() else null,
              checkBankName = if (paymentMethod == "شيك") checkBankName.trim() else null,
              checkDueDate = if (paymentMethod == "شيك") checkDueDateMillis else null,
              checkDrawer = if (paymentMethod == "شيك") checkDrawerName.trim().ifEmpty { party } else null,
              checkStatus = if (paymentMethod == "شيك") checkStatus else null,
              checkBranch = if (paymentMethod == "شيك") checkBranchName.trim().ifEmpty { null } else null,
              onSuccess = onDismiss
            )
          }
        },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
      ) {
        Text("حفظ سند القبض ", fontWeight = FontWeight.Bold, color = Color.White)
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
fun ReceiptVouchersTable(
  receipts: List<ReceiptVoucherItem>,
  currencySymbol: String,
  onPrint: (ReceiptVoucherItem) -> Unit,
  onShareWhatsApp: (ReceiptVoucherItem) -> Unit,
  onDelete: (ReceiptVoucherItem) -> Unit
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(1.5.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
      // Header
      Row(
        modifier = Modifier
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
          .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("رقم السند", fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
        Text("التاريخ", fontWeight = FontWeight.Bold, modifier = Modifier.width(110.dp))
        Text("العميل / المستلم منه", fontWeight = FontWeight.Bold, modifier = Modifier.width(150.dp))
        Text("المبلغ", fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp))
        Text("طريقة الدفع", fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp))
        Text("الملاحظات", fontWeight = FontWeight.Bold, modifier = Modifier.width(200.dp))
        Text("إجراءات", fontWeight = FontWeight.Bold, modifier = Modifier.width(120.dp), textAlign = TextAlign.Center)
      }
      
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      
      // Rows
      LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 500.dp)) {
        items(receipts) { receipt ->
          val dateStr = remember(receipt.date) { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(receipt.date)) }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("#${receipt.numericId}", fontSize = 13.sp, modifier = Modifier.width(70.dp))
            Text(dateStr, fontSize = 13.sp, modifier = Modifier.width(110.dp))
            Text(receipt.partyName, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(150.dp))
            Text("${String.format("%.2f", receipt.amount)} $currencySymbol", fontSize = 13.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp))
            Column(modifier = Modifier.width(100.dp)) {
              Text(receipt.paymentMethod, fontSize = 13.sp, fontWeight = FontWeight.Medium)
              if (!receipt.checkNumber.isNullOrBlank()) {
                Text("#${receipt.checkNumber}", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
              }
            }
            Text(receipt.notes.ifBlank { "-" }, fontSize = 12.sp, modifier = Modifier.width(200.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            
            Row(modifier = Modifier.width(120.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
               IconButton(onClick = { onShareWhatsApp(receipt) }, modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF25D366).copy(alpha = 0.12f))) {
                 Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF128C7E), modifier = Modifier.size(14.dp))
               }
               IconButton(onClick = { onPrint(receipt) }, modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF2563EB).copy(alpha = 0.12f))) {
                 Icon(Icons.Default.Print, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(14.dp))
               }
               IconButton(onClick = { onDelete(receipt) }, modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))) {
                 Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
               }
            }
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        }
      }
    }
  }
}