package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class TxCategory(val label: String, val icon: ImageVector, val color: Color) {
    ALL("كافة المعاملات", Icons.Default.ReceiptLong, Color(0xFF2563EB)),
    SALES("فواتير المبيعات", Icons.Default.ShoppingCart, Color(0xFF10B981)),
    PURCHASES("فواتير المشتريات", Icons.Default.LocalShipping, Color(0xFF0284C7)),
    CUSTOMER_PAYMENTS("مقبوضات ودفعات العملاء", Icons.Default.Payments, Color(0xFF059669)),
    SUPPLIER_PAYMENTS("سداد دفعات الموردين", Icons.Default.PriceCheck, Color(0xFFD97706)),
    EXPENSES("المصروفات والرواتب", Icons.Default.AccountBalanceWallet, Color(0xFFDC2626)),
    AUDIT("سجل العمليات والمخزون", Icons.Default.Security, Color(0xFF64748B))
}

data class UnifiedTxItem(
    val id: String,
    val category: TxCategory,
    val title: String,
    val partyName: String,
    val amount: Double,
    val isIncoming: Boolean?, // true = +ve in, false = -ve out, null = neutral/audit
    val timestamp: Long,
    val paymentMethod: String,
    val branchName: String,
    val cashierName: String,
    val notes: String,
    val rawInvoice: Invoice? = null,
    val rawCustomerTx: CustomerTransaction? = null,
    val rawSupplierTx: SupplierTransaction? = null,
    val rawExpense: Expense? = null,
    val rawAuditLog: AuditLog? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsHistoryScreen(
    viewModel: AppViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val invoices by viewModel.allInvoices.collectAsState(emptyList())
    val customerTxs by viewModel.allCustomerTransactions.collectAsState(emptyList())
    val supplierTxs by viewModel.allSupplierTransactions.collectAsState(emptyList())
    val expenses by viewModel.allExpenses.collectAsState(emptyList())
    val auditLogs by viewModel.allAuditLogs.collectAsState(emptyList())
    val customers by viewModel.allCustomers.collectAsState(emptyList())
    val suppliers by viewModel.allSuppliers.collectAsState(emptyList())
    val employees by viewModel.allEmployees.collectAsState(emptyList())
    val currencySymbol by viewModel.currencySymbol.collectAsState("شيكل")
    val storeName by viewModel.storeName.collectAsState("مؤسسة الكنز للمحاسبة التجارية")

    var selectedCategory by remember { mutableStateOf(TxCategory.ALL) }
    var selectedDatePeriod by remember { mutableStateOf("الكل") }
    var searchQuery by remember { mutableStateOf("") }
    var sortByLatest by remember { mutableStateOf(true) }

    var selectedInvoiceForDialog by remember { mutableStateOf<Invoice?>(null) }
    var selectedItemForDetailDialog by remember { mutableStateOf<UnifiedTxItem?>(null) }

    // Build unified transactions list
    val allUnifiedItems = remember(invoices, customerTxs, supplierTxs, expenses, auditLogs, customers, suppliers, employees) {
        val list = mutableListOf<UnifiedTxItem>()

        // 1. Invoices (Sales & Purchases)
        invoices.filter { !it.isDraft }.forEach { inv ->
            if (inv.isPurchase) {
                val sup = suppliers.find { it.id == inv.customerId }
                list.add(
                    UnifiedTxItem(
                        id = "INV-P-${inv.id}",
                        category = TxCategory.PURCHASES,
                        title = "فاتورة مشتريات #${inv.id}",
                        partyName = sup?.name ?: "مورد غير محدد",
                        amount = inv.total,
                        isIncoming = false,
                        timestamp = inv.invoiceDate,
                        paymentMethod = inv.paymentMethod.ifBlank { "نقدي" },
                        branchName = inv.branchName,
                        cashierName = inv.cashierName,
                        notes = "الضريبة: ${String.format(Locale.US, "%.2f", inv.taxAmount)} • الخصم: ${String.format(Locale.US, "%.2f", inv.discount)}",
                        rawInvoice = inv
                    )
                )
            } else {
                val cust = customers.find { it.id == inv.customerId }
                list.add(
                    UnifiedTxItem(
                        id = "INV-S-${inv.id}",
                        category = TxCategory.SALES,
                        title = "فاتورة مبيعات #${inv.id}",
                        partyName = cust?.name ?: "زبون نقدي عام",
                        amount = inv.total,
                        isIncoming = true,
                        timestamp = inv.invoiceDate,
                        paymentMethod = inv.paymentMethod.ifBlank { "نقدي" },
                        branchName = inv.branchName,
                        cashierName = inv.cashierName,
                        notes = "الضريبة: ${String.format(Locale.US, "%.2f", inv.taxAmount)} • الخصم: ${String.format(Locale.US, "%.2f", inv.discount)}",
                        rawInvoice = inv
                    )
                )
            }
        }

        // 2. Customer Transactions (Receipts / Payments)
        customerTxs.filter { it.transactionType == "دفعة" || it.invoiceId == null }.forEach { ctx ->
            val cust = customers.find { it.id == ctx.customerId }
            list.add(
                UnifiedTxItem(
                    id = "CTX-${ctx.id}",
                    category = TxCategory.CUSTOMER_PAYMENTS,
                    title = "سند قبض / دفعة عميل #${ctx.id}",
                    partyName = cust?.name ?: "عميل",
                    amount = ctx.amount,
                    isIncoming = true,
                    timestamp = ctx.date,
                    paymentMethod = "سند قبض نقدي",
                    branchName = "",
                    cashierName = "",
                    notes = ctx.note.ifBlank { "تحصيل دفعة نقدية من الحساب" },
                    rawCustomerTx = ctx
                )
            )
        }

        // 3. Supplier Transactions (Payments)
        supplierTxs.filter { it.transactionType == "دفعة" }.forEach { stx ->
            val sup = suppliers.find { it.id == stx.supplierId }
            list.add(
                UnifiedTxItem(
                    id = "STX-${stx.id}",
                    category = TxCategory.SUPPLIER_PAYMENTS,
                    title = "سند صرف / دفعة مورد #${stx.id}",
                    partyName = sup?.name ?: "مورد",
                    amount = stx.amount,
                    isIncoming = false,
                    timestamp = stx.date,
                    paymentMethod = "سند صرف / سداد",
                    branchName = "",
                    cashierName = "",
                    notes = stx.note.ifBlank { "سداد دفعة للمورد" },
                    rawSupplierTx = stx
                )
            )
        }

        // 4. Expenses & Salaries
        expenses.forEach { exp ->
            val emp = employees.find { it.id == exp.employeeId }
            val empText = if (emp != null) " (الموظف: ${emp.name})" else ""
            list.add(
                UnifiedTxItem(
                    id = "EXP-${exp.id}",
                    category = TxCategory.EXPENSES,
                    title = "مصروف: ${exp.type}$empText #${exp.id}",
                    partyName = emp?.name ?: "المصروفات العامة",
                    amount = exp.amount,
                    isIncoming = false,
                    timestamp = exp.date,
                    paymentMethod = "نقدية المصروفات",
                    branchName = "",
                    cashierName = "",
                    notes = exp.notes.ifBlank { "مصروف تشغيلي / إداري" },
                    rawExpense = exp
                )
            )
        }

        // 5. System Audit Logs
        auditLogs.forEach { log ->
            list.add(
                UnifiedTxItem(
                    id = "AUDIT-${log.id}",
                    category = TxCategory.AUDIT,
                    title = "حركة نظام: ${log.action}",
                    partyName = "المستخدم: ${log.employeeName}",
                    amount = 0.0,
                    isIncoming = null,
                    timestamp = log.timestamp,
                    paymentMethod = "سجل إداري",
                    branchName = "",
                    cashierName = log.employeeName,
                    notes = log.details,
                    rawAuditLog = log
                )
            )
        }

        list
    }

    // Date filtering logic
    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance()
    
    val filteredByDateItems = remember(allUnifiedItems, selectedDatePeriod) {
        when (selectedDatePeriod) {
            "اليوم" -> {
                cal.timeInMillis = now
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startOfDay = cal.timeInMillis
                allUnifiedItems.filter { it.timestamp >= startOfDay }
            }
            "أمس" -> {
                cal.timeInMillis = now
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val startOfYesterday = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val endOfYesterday = cal.timeInMillis
                allUnifiedItems.filter { it.timestamp in startOfYesterday..endOfYesterday }
            }
            "آخر 7 أيام" -> {
                val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000)
                allUnifiedItems.filter { it.timestamp >= sevenDaysAgo }
            }
            "هذا الشهر" -> {
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val startOfMonth = cal.timeInMillis
                allUnifiedItems.filter { it.timestamp >= startOfMonth }
            }
            else -> allUnifiedItems
        }
    }

    // Category & Search filter
    val displayedItems = remember(filteredByDateItems, selectedCategory, searchQuery, sortByLatest) {
        var res = if (selectedCategory == TxCategory.ALL) {
            filteredByDateItems
        } else {
            filteredByDateItems.filter { it.category == selectedCategory }
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            res = res.filter { item ->
                item.title.lowercase().contains(q) ||
                item.partyName.lowercase().contains(q) ||
                item.notes.lowercase().contains(q) ||
                item.paymentMethod.lowercase().contains(q) ||
                item.branchName.lowercase().contains(q) ||
                item.cashierName.lowercase().contains(q) ||
                item.amount.toString().contains(q)
            }
        }

        if (sortByLatest) {
            res.sortedByDescending { it.timestamp }
        } else {
            res.sortedBy { it.timestamp }
        }
    }

    // Financial KPI stats
    val totalSales = remember(filteredByDateItems) {
        filteredByDateItems.filter { it.category == TxCategory.SALES }.sumOf { it.amount }
    }
    val totalPurchases = remember(filteredByDateItems) {
        filteredByDateItems.filter { it.category == TxCategory.PURCHASES }.sumOf { it.amount }
    }
    val totalCustomerReceipts = remember(filteredByDateItems) {
        filteredByDateItems.filter { it.category == TxCategory.CUSTOMER_PAYMENTS }.sumOf { it.amount }
    }
    val totalExpenses = remember(filteredByDateItems) {
        filteredByDateItems.filter { it.category == TxCategory.EXPENSES }.sumOf { it.amount }
    }
    val netCashFlow = (totalSales + totalCustomerReceipts) - (totalPurchases + totalExpenses)

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd  hh:mm a", Locale("ar")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.padding(horizontal = 2.dp)) {
                        Text(
                            text = "سجل كافة معاملات النظام",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "$storeName • ${displayedItems.size} حركة مسجلة",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    FilledTonalButton(
                        onClick = onBackClick,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                modifier = Modifier.size(16.dp)
                            )
                            Text("رجوع", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = {
                            exportTransactionsPdf(
                                context = context,
                                storeName = storeName,
                                currency = currencySymbol,
                                period = selectedDatePeriod,
                                items = displayedItems,
                                totalSales = totalSales,
                                totalPurchases = totalPurchases,
                                totalReceipts = totalCustomerReceipts,
                                totalExpenses = totalExpenses,
                                netFlow = netCashFlow
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(onClick = { sortByLatest = !sortByLatest }) {
                        Icon(
                            imageVector = if (sortByLatest) Icons.Default.SwapVert else Icons.Default.FilterList,
                            contentDescription = "ترتيب",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (displayedItems.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        exportTransactionsPdf(
                            context = context,
                            storeName = storeName,
                            currency = currencySymbol,
                            period = selectedDatePeriod,
                            items = displayedItems,
                            totalSales = totalSales,
                            totalPurchases = totalPurchases,
                            totalReceipts = totalCustomerReceipts,
                            totalExpenses = totalExpenses,
                            netFlow = netCashFlow
                        )
                    },
                    icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                    text = { Text("تصدير وطباعة PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            // Top Financial Metrics Strip
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    MetricMiniCard(
                        title = "المبيعات",
                        amount = totalSales,
                        currency = currencySymbol,
                        count = filteredByDateItems.count { it.category == TxCategory.SALES },
                        color = Color(0xFF10B981),
                        icon = Icons.Default.TrendingUp
                    )
                }
                item {
                    MetricMiniCard(
                        title = "المشتريات",
                        amount = totalPurchases,
                        currency = currencySymbol,
                        count = filteredByDateItems.count { it.category == TxCategory.PURCHASES },
                        color = Color(0xFF0284C7),
                        icon = Icons.Default.LocalShipping
                    )
                }
                item {
                    MetricMiniCard(
                        title = "المقبوضات",
                        amount = totalCustomerReceipts,
                        currency = currencySymbol,
                        count = filteredByDateItems.count { it.category == TxCategory.CUSTOMER_PAYMENTS },
                        color = Color(0xFF059669),
                        icon = Icons.Default.Payments
                    )
                }
                item {
                    MetricMiniCard(
                        title = "المصروفات",
                        amount = totalExpenses,
                        currency = currencySymbol,
                        count = filteredByDateItems.count { it.category == TxCategory.EXPENSES },
                        color = Color(0xFFDC2626),
                        icon = Icons.Default.AccountBalanceWallet
                    )
                }
                item {
                    MetricMiniCard(
                        title = "صافي الحركة",
                        amount = netCashFlow,
                        currency = currencySymbol,
                        count = displayedItems.size,
                        color = if (netCashFlow >= 0) Color(0xFF16A34A) else Color(0xFFDC2626),
                        icon = Icons.Default.Analytics
                    )
                }
            }

            // Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("بحث برقم الفاتورة، العميل، المورد، الكاشير، المبلغ...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.Gray)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Date Range Filter Chips
            val periods = listOf("الكل", "اليوم", "أمس", "آخر 7 أيام", "هذا الشهر")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                periods.forEach { period ->
                    val isSelected = selectedDatePeriod == period
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedDatePeriod = period },
                        label = { Text(period, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Category Tabs
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TxCategory.values()) { cat ->
                    val isSelected = selectedCategory == cat
                    val count = remember(filteredByDateItems, cat) {
                        if (cat == TxCategory.ALL) filteredByDateItems.size
                        else filteredByDateItems.count { it.category == cat }
                    }
                    Surface(
                        onClick = { selectedCategory = cat },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) cat.color else MaterialTheme.colorScheme.surface,
                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) cat.color else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = cat.icon, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text(cat.label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) Color.White.copy(alpha = 0.25f) else cat.color.copy(alpha = 0.15f),
                                modifier = Modifier.height(18.dp)
                            ) {
                                Text(
                                    text = "$count",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else cat.color,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Transactions List
            if (displayedItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Text(
                            text = "لا توجد معاملات مسجلة تطابق التصفية الحالية",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "جرب تغيير الفترة الزمنية أو نوع المعاملة أو عبارة البحث.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp)
                ) {
                    items(displayedItems, key = { it.id }) { item ->
                        TransactionItemCard(
                            item = item,
                            currency = currencySymbol,
                            dateFormat = dateFormat,
                            onClick = {
                                if (item.rawInvoice != null) {
                                    selectedInvoiceForDialog = item.rawInvoice
                                } else {
                                    selectedItemForDetailDialog = item
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // 1. Invoice Dialog Viewer
    if (selectedInvoiceForDialog != null) {
        InvoiceDetailsDialog(
            inv = selectedInvoiceForDialog!!,
            customers = customers,
            viewModel = viewModel,
            onDismiss = { selectedInvoiceForDialog = null }
        )
    }

    // 2. Custom Voucher / Detail Modal
    if (selectedItemForDetailDialog != null) {
        TransactionDetailModal(
            item = selectedItemForDetailDialog!!,
            currency = currencySymbol,
            dateFormat = dateFormat,
            onDismiss = { selectedItemForDetailDialog = null }
        )
    }
}

@Composable
fun MetricMiniCard(
    title: String,
    amount: Double,
    currency: String,
    count: Int,
    color: Color,
    icon: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        tonalElevation = 2.dp,
        modifier = Modifier.width(130.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
                    }
                }
            }
            Text(
                text = "${String.format(Locale.US, "%,.2f", amount)} $currency",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$count حركة",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun TransactionItemCard(
    item: UnifiedTxItem,
    currency: String,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon + Main Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = item.category.color.copy(alpha = 0.14f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = item.category.icon,
                            contentDescription = null,
                            tint = item.category.color,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = item.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.height(18.dp)
                        ) {
                            Text(
                                text = item.paymentMethod,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = item.partyName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = dateFormat.format(Date(item.timestamp)),
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }

                    if (item.notes.isNotBlank()) {
                        Text(
                            text = item.notes,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Amount & Action Indicator
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (item.isIncoming != null) {
                    val isPos = item.isIncoming == true
                    val amountColor = if (isPos) Color(0xFF10B981) else Color(0xFFDC2626)
                    val prefix = if (isPos) "+ " else "- "
                    Text(
                        text = "$prefix${String.format(Locale.US, "%,.2f", item.amount)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = amountColor
                    )
                    Text(
                        text = currency,
                        fontSize = 10.sp,
                        color = amountColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "عملية إدارية",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "عرض التفاصيل",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionDetailModal(
    item: UnifiedTxItem,
    currency: String,
    dateFormat: SimpleDateFormat,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = item.category.color.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(item.category.icon, contentDescription = null, tint = item.category.color, modifier = Modifier.size(20.dp))
                    }
                }
                Column {
                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(item.category.label, fontSize = 11.sp, color = item.category.color)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HorizontalDivider()

                DetailInfoRow(label = "الطرف / الحساب:", value = item.partyName)
                if (item.amount > 0) {
                    DetailInfoRow(
                        label = "المبلغ:",
                        value = "${String.format(Locale.US, "%,.2f", item.amount)} $currency",
                        valueColor = if (item.isIncoming == true) Color(0xFF10B981) else Color(0xFFDC2626)
                    )
                }
                DetailInfoRow(label = "طريقة الدفع / القيد:", value = item.paymentMethod)
                DetailInfoRow(label = "التاريخ والوقت:", value = dateFormat.format(Date(item.timestamp)))
                if (item.cashierName.isNotBlank()) {
                    DetailInfoRow(label = "المستخدم / الكاشير:", value = item.cashierName)
                }
                if (item.branchName.isNotBlank()) {
                    DetailInfoRow(label = "الفرع:", value = item.branchName)
                }
                if (item.notes.isNotBlank()) {
                    DetailInfoRow(label = "البيان والملاحظات:", value = item.notes)
                }

                HorizontalDivider()
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
fun DetailInfoRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 12.sp, color = valueColor, fontWeight = FontWeight.Bold)
    }
}

// PDF Exporter for Full Transactions Report with Multi-Page Support & High-Fidelity Design
fun exportTransactionsPdf(
    context: Context,
    storeName: String,
    currency: String,
    period: String,
    items: List<UnifiedTxItem>,
    totalSales: Double,
    totalPurchases: Double,
    totalReceipts: Double,
    totalExpenses: Double,
    netFlow: Double
) {
    try {
        Toast.makeText(context, "جاري إعداد وتنزيل ملف PDF...", Toast.LENGTH_SHORT).show()

        val pdfDoc = PdfDocument()
        val pageW = 595 // Standard A4 width (72 dpi)
        val pageH = 842 // Standard A4 height (72 dpi)
        val marginLeft = 35f
        val marginRight = 560f
        val contentW = marginRight - marginLeft // 525f

        // App Fonts
        val fontBold = try {
            androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.ibm_plex_sans_arabic_bold)
                ?: androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.ibm_plex_sans_arabic_regular)
                ?: Typeface.DEFAULT_BOLD
        } catch (e: Exception) { Typeface.DEFAULT_BOLD }
        val fontSemiBold = try {
            androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.ibm_plex_sans_arabic_semibold)
                ?: fontBold
        } catch (e: Exception) { fontBold }
        val fontMedium = try {
            androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.ibm_plex_sans_arabic_medium)
                ?: Typeface.DEFAULT
        } catch (e: Exception) { Typeface.DEFAULT }
        val fontRegular = try {
            androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.ibm_plex_sans_arabic_regular)
                ?: Typeface.DEFAULT
        } catch (e: Exception) { Typeface.DEFAULT }

        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 15f
            typeface = fontBold
            textAlign = Paint.Align.RIGHT
            color = android.graphics.Color.WHITE
        }
        val subTitlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            typeface = fontMedium
            textAlign = Paint.Align.RIGHT
            color = android.graphics.Color.rgb(224, 231, 255)
        }
        val headerColPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            typeface = fontBold
            textAlign = Paint.Align.RIGHT
            color = android.graphics.Color.WHITE
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            typeface = fontRegular
            textAlign = Paint.Align.RIGHT
            color = android.graphics.Color.rgb(30, 41, 59)
        }
        val boldTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            typeface = fontSemiBold
            textAlign = Paint.Align.RIGHT
            color = android.graphics.Color.rgb(15, 23, 42)
        }
        val greenTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            typeface = fontBold
            textAlign = Paint.Align.RIGHT
            color = android.graphics.Color.rgb(5, 150, 105)
        }
        val redTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            typeface = fontBold
            textAlign = Paint.Align.RIGHT
            color = android.graphics.Color.rgb(220, 38, 38)
        }
        val footerPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8f
            typeface = fontRegular
            textAlign = Paint.Align.RIGHT
            color = android.graphics.Color.rgb(100, 116, 139)
        }
        val footerLeftPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8f
            typeface = fontRegular
            textAlign = Paint.Align.LEFT
            color = android.graphics.Color.rgb(100, 116, 139)
        }
        val footerCenterPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8f
            typeface = fontMedium
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.rgb(100, 116, 139)
        }
        val bgPaint = Paint().apply { isAntiAlias = true }
        val linePaint = Paint().apply {
            strokeWidth = 0.75f
            color = android.graphics.Color.rgb(226, 232, 240)
        }

        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        val itemSdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        val nowStr = sdf.format(Date())

        // Calculate pages pagination
        val page1Limit = 21
        val subsequentLimit = 28
        val totalItems = items.size
        val totalPages = if (totalItems <= page1Limit) 1 else 1 + Math.ceil((totalItems - page1Limit).toDouble() / subsequentLimit).toInt()

        var currentItemIdx = 0

        for (pageIdx in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, pageIdx).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            var y = 30f

            if (pageIdx == 1) {
                // Page 1 Header Banner (Navy gradient-like block)
                bgPaint.color = android.graphics.Color.rgb(30, 58, 138) // Deep Royal Navy
                val headerRect = RectF(marginLeft, y, marginRight, y + 60f)
                canvas.drawRoundRect(headerRect, 8f, 8f, bgPaint)

                // Store name & Report title
                canvas.drawText(storeName, marginRight - 16f, y + 24f, titlePaint)
                canvas.drawText("تقرير سجل المعاملات والحركات المالية الشامل  •  الفترة: $period  •  العملة: $currency", marginRight - 16f, y + 44f, subTitlePaint)
                y += 72f

                // Financial Summary Cards Box
                bgPaint.color = android.graphics.Color.rgb(248, 250, 252)
                val summaryRect = RectF(marginLeft, y, marginRight, y + 54f)
                canvas.drawRoundRect(summaryRect, 6f, 6f, bgPaint)

                // Summary border
                linePaint.color = android.graphics.Color.rgb(203, 213, 225)
                canvas.drawLine(marginLeft, y, marginRight, y, linePaint)
                canvas.drawLine(marginLeft, y + 54f, marginRight, y + 54f, linePaint)
                canvas.drawLine(marginLeft, y, marginLeft, y + 54f, linePaint)
                canvas.drawLine(marginRight, y, marginRight, y + 54f, linePaint)

                // 5 KPIs in a neat row
                val kpiW = contentW / 5f
                val kpis = listOf(
                    Triple("المبيعات", String.format(Locale.US, "%,.2f", totalSales), android.graphics.Color.rgb(5, 150, 105)),
                    Triple("المشتريات", String.format(Locale.US, "%,.2f", totalPurchases), android.graphics.Color.rgb(2, 132, 199)),
                    Triple("المقبوضات", String.format(Locale.US, "%,.2f", totalReceipts), android.graphics.Color.rgb(13, 148, 136)),
                    Triple("المصروفات", String.format(Locale.US, "%,.2f", totalExpenses), android.graphics.Color.rgb(220, 38, 38)),
                    Triple("صافي الحركة", String.format(Locale.US, "%,.2f", netFlow), if (netFlow >= 0) android.graphics.Color.rgb(22, 163, 74) else android.graphics.Color.rgb(220, 38, 38))
                )

                for (k in kpis.indices) {
                    val kpi = kpis[k]
                    val kpiRight = marginRight - (k * kpiW)
                    val kpiPaint = Paint().apply {
                        isAntiAlias = true
                        textSize = 8f
                        textAlign = Paint.Align.RIGHT
                        color = android.graphics.Color.rgb(100, 116, 139)
                    }
                    val kpiValPaint = Paint().apply {
                        isAntiAlias = true
                        textSize = 9.5f
                        typeface = fontBold
                        textAlign = Paint.Align.RIGHT
                        color = kpi.third
                    }
                    canvas.drawText(kpi.first, kpiRight - 10f, y + 20f, kpiPaint)
                    canvas.drawText("${kpi.second} $currency", kpiRight - 10f, y + 40f, kpiValPaint)

                    if (k < kpis.size - 1) {
                        canvas.drawLine(kpiRight - kpiW, y + 8f, kpiRight - kpiW, y + 46f, linePaint)
                    }
                }
                y += 66f

                // Subtitle metadata line
                val metaPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 8.5f
                    textAlign = Paint.Align.RIGHT
                    color = android.graphics.Color.rgb(71, 85, 105)
                }
                canvas.drawText("إجمالي الحركات: ${items.size} معاملة  •  تاريخ التقرير: $nowStr", marginRight - 4f, y, metaPaint)
                y += 12f

            } else {
                // Subsequent Pages Minimal Header
                bgPaint.color = android.graphics.Color.rgb(30, 58, 138)
                val headerRect = RectF(marginLeft, y, marginRight, y + 32f)
                canvas.drawRoundRect(headerRect, 6f, 6f, bgPaint)

                canvas.drawText("$storeName  •  تابع تقرير المعاملات ($period)", marginRight - 12f, y + 21f, titlePaint.apply { textSize = 11f })
                y += 42f
            }

            // Table Header Bar (Dark Indigo)
            bgPaint.color = android.graphics.Color.rgb(37, 99, 235)
            val tblHeaderRect = RectF(marginLeft, y, marginRight, y + 24f)
            canvas.drawRoundRect(tblHeaderRect, 4f, 4f, bgPaint)

            // Columns Header text
            canvas.drawText("رقم ونوع المعاملة", marginRight - 10f, y + 16f, headerColPaint)
            canvas.drawText("الطرف / الحساب", marginRight - 145f, y + 16f, headerColPaint)
            canvas.drawText("طريقة الدفع / القيد", marginRight - 275f, y + 16f, headerColPaint)
            canvas.drawText("المبلغ ($currency)", marginRight - 385f, y + 16f, headerColPaint)
            canvas.drawText("التاريخ والوقت", marginRight - 460f, y + 16f, headerColPaint)
            y += 24f

            // Table Rows
            val limitThisPage = if (pageIdx == 1) page1Limit else subsequentLimit
            val endIdx = Math.min(currentItemIdx + limitThisPage, totalItems)
            val rowHeight = 22f

            if (totalItems == 0) {
                bgPaint.color = android.graphics.Color.WHITE
                canvas.drawRect(marginLeft, y, marginRight, y + 35f, bgPaint)
                val emptyPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 10f
                    textAlign = Paint.Align.CENTER
                    color = android.graphics.Color.GRAY
                }
                canvas.drawText("لا توجد حركات أو معاملات مسجلة في هذه الفترة", (marginLeft + marginRight) / 2f, y + 22f, emptyPaint)
                y += 35f
            } else {
                for (i in currentItemIdx until endIdx) {
                    val item = items[i]
                    val isEven = (i % 2 == 0)

                    // Zebra Background
                    bgPaint.color = if (isEven) android.graphics.Color.WHITE else android.graphics.Color.rgb(248, 250, 252)
                    canvas.drawRect(marginLeft, y, marginRight, y + rowHeight, bgPaint)

                    // Bottom divider line
                    linePaint.color = android.graphics.Color.rgb(226, 232, 240)
                    canvas.drawLine(marginLeft, y + rowHeight, marginRight, y + rowHeight, linePaint)

                    // 1. Title / ID
                    val cleanTitle = item.title.take(24)
                    canvas.drawText(cleanTitle, marginRight - 10f, y + 15f, boldTextPaint)

                    // 2. Party Name
                    val cleanParty = item.partyName.take(20)
                    canvas.drawText(cleanParty, marginRight - 145f, y + 15f, textPaint)

                    // 3. Payment method
                    val cleanMethod = item.paymentMethod.take(16)
                    canvas.drawText(cleanMethod, marginRight - 275f, y + 15f, textPaint)

                    // 4. Amount with formatting and colored sign
                    if (item.amount > 0) {
                        val isPos = item.isIncoming == true
                        val amtFormatted = (if (isPos) "+ " else "- ") + String.format(Locale.US, "%,.2f", item.amount)
                        canvas.drawText(amtFormatted, marginRight - 385f, y + 15f, if (isPos) greenTextPaint else redTextPaint)
                    } else {
                        canvas.drawText("إداري", marginRight - 385f, y + 15f, textPaint)
                    }

                    // 5. Date & Time
                    val dateFormatted = itemSdf.format(Date(item.timestamp))
                    canvas.drawText(dateFormatted, marginRight - 460f, y + 15f, textPaint)

                    y += rowHeight
                }
                currentItemIdx = endIdx
            }

            // Table Border Enclosing
            linePaint.color = android.graphics.Color.rgb(203, 213, 225)
            canvas.drawLine(marginLeft, y, marginRight, y, linePaint)

            // Page Footer
            val footerY = pageH - 35f
            canvas.drawLine(marginLeft, footerY - 10f, marginRight, footerY - 10f, linePaint)

            canvas.drawText("تاريخ الطباعة والتوليد: $nowStr", marginRight - 4f, footerY + 5f, footerPaint)
            canvas.drawText("نظام الكنز للمحاسبة السحابية وإدارة المبيعات", (marginLeft + marginRight) / 2f, footerY + 5f, footerCenterPaint)
            canvas.drawText("صفحة $pageIdx من $totalPages", marginLeft + 4f, footerY + 5f, footerLeftPaint)

            pdfDoc.finishPage(page)
        }

        // Save PDF to Documents Directory
        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(outputDir, "سجل_المعاملات_المالية_${timeStamp}.pdf")
        val fos = FileOutputStream(file)
        pdfDoc.writeTo(fos)
        pdfDoc.close()
        fos.close()

        Toast.makeText(context, "تم حفظ كشف المعاملات PDF بنجاح", Toast.LENGTH_SHORT).show()

        // Launch Viewer / Share Intent with Chooser
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(intent, "عرض وطباعة كشف المعاملات PDF")
        chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(chooser)

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "حدث خطأ أثناء إنشاء ملف PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
