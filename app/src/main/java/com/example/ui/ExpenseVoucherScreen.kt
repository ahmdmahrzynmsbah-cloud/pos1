package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Customer
import com.example.data.Employee
import com.example.data.Expense
import com.example.data.Invoice
import com.example.data.Supplier
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldGreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseVoucherScreen(
  viewModel: AppViewModel,
  onOpenBalancesReport: (() -> Unit)? = null,
  onBackClick: (() -> Unit)? = null
) {
  val context = LocalContext.current
  val expenses by viewModel.allExpenses.collectAsState()
  val employees by viewModel.allEmployees.collectAsState()
  val suppliers by viewModel.allSuppliers.collectAsState(initial = emptyList())
  val customers by viewModel.allCustomers.collectAsState(initial = emptyList())
  val invoices by viewModel.allInvoices.collectAsState(initial = emptyList())
  val currencySymbol by viewModel.currencySymbol.collectAsState()
  val expenseCategories by viewModel.expenseCategories.collectAsState()

  var showAddDialog by remember { mutableStateOf(false) }
  var showManageCategoriesDialog by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategoryFilter by remember { mutableStateOf("الكل") }

  // Filter out any supplier merchandise purchase invoices that might have slipped into database
  val pureExpenses = remember(expenses) {
    expenses.filter { exp ->
      !exp.type.startsWith("مشتريات بضاعة من مورد") &&
      !exp.type.startsWith("مرتجع مشتريات من مورد") &&
      !(exp.partyType == "مورد" && exp.notes.contains("فاتورة شراء بضاعة وتوريد للمخزن"))
    }
  }

  val filteredExpenses = remember(pureExpenses, searchQuery, selectedCategoryFilter) {
    pureExpenses.filter { exp ->
      val matchesCategory = if (selectedCategoryFilter == "الكل") true else {
        val cleanFilter = selectedCategoryFilter
          .replace("فاتورة ", "")
          .replace("المحل", "")
          .replace("المقر", "")
          .replace(" / ", " ")
          .trim()
        val words = cleanFilter.split(" ").filter { it.length > 2 }
        exp.type.contains(selectedCategoryFilter, ignoreCase = true) ||
        exp.notes.contains(selectedCategoryFilter, ignoreCase = true) ||
        (exp.partyName ?: "").contains(selectedCategoryFilter, ignoreCase = true) ||
        words.any { word ->
          exp.type.contains(word, ignoreCase = true) ||
          exp.notes.contains(word, ignoreCase = true) ||
          (exp.partyName ?: "").contains(word, ignoreCase = true)
        }
      }

      val matchesQuery = if (searchQuery.isBlank()) true else {
        val q = searchQuery.trim()
        val emp = employees.find { it.id == exp.employeeId }?.name ?: ""
        exp.type.contains(q, ignoreCase = true) ||
        exp.notes.contains(q, ignoreCase = true) ||
        (exp.partyName ?: "").contains(q, ignoreCase = true) ||
        emp.contains(q, ignoreCase = true) ||
        exp.amount.toString().contains(q)
      }

      matchesCategory && matchesQuery
    }
  }

  val totalFilteredAmount = remember(filteredExpenses) {
    filteredExpenses.sumOf { it.amount }
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
                  imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                  contentDescription = "رجوع",
                  tint = MaterialTheme.colorScheme.onSurface
                )
              }
            }
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF2563EB).copy(alpha = 0.12f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.AttachMoney,
                contentDescription = null,
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(22.dp)
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "سندات المصروفات",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
              )
              Text(
                text = "كهرباء، مياه، إيجار، والمصروفات التشغيلية",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
              )
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(
              onClick = { showManageCategoriesDialog = true },
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
              Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("بنود المصروفات", fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }

            if (onOpenBalancesReport != null) {
              OutlinedButton(
                onClick = onOpenBalancesReport,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
              ) {
                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
              }
            }

            Button(
              onClick = { showAddDialog = true },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "إضافة مصروف",
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
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // 1. KPI Summary Card
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = if (selectedCategoryFilter == "الكل") "إجمالي المصروفات التشغيلية" else "إجمالي مصروفات ($selectedCategoryFilter)",
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "${String.format(Locale.US, "%.2f", totalFilteredAmount)} $currencySymbol",
              fontSize = 19.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFDC2626)
            )
          }

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          ) {
            Column(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text("عدد السندات", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("${filteredExpenses.size}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
          }
        }
      }

      // 2. Search Box
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("ابحث في المصروفات (كهرباء، مياه، إيجار، صيانة...)", fontSize = 12.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      )

      // 3. Category Filter Chips (Dynamic operating expenses)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        val isAllSelected = selectedCategoryFilter == "الكل"
        FilterChip(
          selected = isAllSelected,
          onClick = { selectedCategoryFilter = "الكل" },
          label = { Text("الكل (${pureExpenses.size})", fontSize = 11.sp, fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal) },
          shape = RoundedCornerShape(8.dp)
        )

        expenseCategories.forEach { cat ->
          val isSelected = selectedCategoryFilter == cat
          FilterChip(
            selected = isSelected,
            onClick = { selectedCategoryFilter = cat },
            label = { Text(cat, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
            shape = RoundedCornerShape(8.dp)
          )
        }

        AssistChip(
          onClick = { showManageCategoriesDialog = true },
          label = { Text("+ إدارة البنود", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
          leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp)) },
          shape = RoundedCornerShape(8.dp)
        )
      }

      // 4. List or Empty State
      if (filteredExpenses.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              Icons.Default.AttachMoney,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
              modifier = Modifier.size(48.dp)
            )
            Text("لا توجد مصروفات مسجلة مطابقة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(
              "اضغط على زر 'إضافة مصروف' بالأعلى لتسجيل فواتير الكهرباء أو المياه أو الإيجار أو غيرها.",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
              onClick = { showAddDialog = true },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("إضافة مصروف الآن", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(filteredExpenses.reversed(), key = { "exp_${it.id}" }) { expense ->
            ExpenseCard(
              expense = expense,
              currencySymbol = currencySymbol,
              employeeName = employees.find { it.id == expense.employeeId }?.name,
              onDeleteClick = { viewModel.deleteExpense(expense) }
            )
          }
        }
      }
    }
  }

  if (showManageCategoriesDialog) {
    ManageExpenseCategoriesDialog(
      viewModel = viewModel,
      onDismiss = { showManageCategoriesDialog = false }
    )
  }

  // --- Add Expense Voucher Dialog ---
  if (showAddDialog) {
    AddExpenseVoucherDialog(
      viewModel = viewModel,
      customers = customers,
      suppliers = suppliers,
      employees = employees,
      invoices = invoices,
      currencySymbol = currencySymbol,
      onDismiss = { showAddDialog = false }
    )
  }
}

// -------------------------------------------------------------
// Comprehensive Add Expense Voucher Dialog
// -------------------------------------------------------------
@Composable
fun AddExpenseVoucherDialog(
  viewModel: AppViewModel,
  customers: List<Customer>,
  suppliers: List<Supplier>,
  employees: List<Employee>,
  invoices: List<Invoice>,
  currencySymbol: String,
  onDismiss: () -> Unit
) {
  val expenseCategories by viewModel.expenseCategories.collectAsState()
  var showManageCategoriesDialog by remember { mutableStateOf(false) }

  // Expense Beneficiary State: partyType can be "مصروف عام", "مورد", "موظف", "عميل"
  var partyType by remember { mutableStateOf("مصروف عام") }
  var partyName by remember(expenseCategories) {
    mutableStateOf(expenseCategories.firstOrNull() ?: "فاتورة كهرباء")
  }
  var selectedCustomerId by remember { mutableStateOf<Int?>(null) }
  var selectedSupplierId by remember { mutableStateOf<Int?>(null) }
  var selectedEmployeeId by remember { mutableStateOf<Int?>(null) }

  var showPartyPicker by remember { mutableStateOf(false) }

  // Payment Method State: "الصندوق" (نقداً) or "شيك" (شيكات موجودة)
  var paymentMethod by remember { mutableStateOf("الصندوق") }
  var selectedCheck by remember { mutableStateOf<Invoice?>(null) }
  var showCheckPicker by remember { mutableStateOf(false) }

  var amount by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }
  var isSaving by remember { mutableStateOf(false) }

  // Available checks from invoices
  val checkInvoices = remember(invoices) {
    invoices.filter { it.paymentMethod == "شيك" || !it.checkNumber.isNullOrBlank() }
      .sortedByDescending { it.invoiceDate }
  }

  // Dialog for managing categories
  if (showManageCategoriesDialog) {
    ManageExpenseCategoriesDialog(
      viewModel = viewModel,
      onDismiss = { showManageCategoriesDialog = false }
    )
  }

  // Sub-Dialog 1: Beneficiary / Expense Type Picker
  if (showPartyPicker) {
    ExpensePartyPickerDialog(
      viewModel = viewModel,
      customers = customers,
      suppliers = suppliers,
      employees = employees,
      onSelectParty = { type, name, custId, suppId, empId ->
        partyType = type
        partyName = name
        selectedCustomerId = custId
        selectedSupplierId = suppId
        selectedEmployeeId = empId
        showPartyPicker = false
      },
      onDismiss = { showPartyPicker = false }
    )
  }

  // Sub-Dialog 2: Check Selection Picker
  if (showCheckPicker) {
    CheckPickerDialog(
      checks = checkInvoices,
      customers = customers,
      currencySymbol = currencySymbol,
      onSelectCheck = { check ->
        selectedCheck = check
        amount = String.format(Locale.US, "%.2f", check.total)
        val bankInfo = check.checkBankName ?: "شيك بنكي"
        val chkNum = check.checkNumber ?: ""
        if (notes.isBlank()) {
          notes = "صرف وتجيير شيك رقم $chkNum ($bankInfo)"
        }
        showCheckPicker = false
      },
      onDismiss = { showCheckPicker = false }
    )
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .padding(vertical = 16.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // --- Header ---
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .background(Color(0xFF2563EB).copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.AttachMoney, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(24.dp))
            }
            Column {
              Text("سند صرف جديد", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
              Text("تسجيل المدفوعات والمصروفات النقدية والشيكات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .size(32.dp)
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
          ) {
            Icon(Icons.Default.Close, contentDescription = "إغلاق", modifier = Modifier.size(18.dp))
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // --- Scrollable Form Body ---
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

          // 1. Beneficiary / Operating Expense Type Selector
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              "اختر بند المصروف التشغيلي السريع (كهرباء، مياه، إيجار...):",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )

            // Quick Operating Chips (Dynamic from expenseCategories)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              expenseCategories.forEach { optionName ->
                val optionIcon = when {
                  optionName.contains("كهرباء") -> Icons.Default.Bolt
                  optionName.contains("مياه") || optionName.contains("ماء") -> Icons.Default.WaterDrop
                  optionName.contains("إيجار") -> Icons.Default.Store
                  optionName.contains("صيانة") -> Icons.Default.Build
                  optionName.contains("نثريات") || optionName.contains("ضيافة") || optionName.contains("بوفيه") -> Icons.Default.Coffee
                  optionName.contains("إنترنت") || optionName.contains("هاتف") || optionName.contains("اتصالات") -> Icons.Default.Wifi
                  optionName.contains("نظافة") -> Icons.Default.CleaningServices
                  optionName.contains("رواتب") || optionName.contains("أجور") -> Icons.Default.Badge
                  optionName.contains("تسويق") || optionName.contains("دعاية") || optionName.contains("إعلان") -> Icons.Default.Campaign
                  optionName.contains("شحن") || optionName.contains("نقل") || optionName.contains("مواصلات") -> Icons.Default.LocalShipping
                  else -> Icons.Default.Category
                }
                val isSelected = partyName == optionName && partyType == "مصروف عام"
                FilterChip(
                  selected = isSelected,
                  onClick = {
                    partyType = "مصروف عام"
                    partyName = optionName
                    selectedCustomerId = null
                    selectedSupplierId = null
                    selectedEmployeeId = null
                  },
                  label = { Text(optionName, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                  leadingIcon = { Icon(optionIcon, contentDescription = null, modifier = Modifier.size(14.dp)) },
                  shape = RoundedCornerShape(8.dp)
                )
              }

              AssistChip(
                onClick = { showManageCategoriesDialog = true },
                label = { Text("+ إدارة البنود", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp)) },
                shape = RoundedCornerShape(8.dp)
              )
            }

            // Editable Text Field for Expense Name
            OutlinedTextField(
              value = partyName,
              onValueChange = {
                partyName = it
                if (partyType != "موظف" && partyType != "مورد" && partyType != "عميل") {
                  partyType = "مصروف عام"
                }
              },
              label = { Text("بيان / مسمى المصروف (كهرباء، مياه، إيجار...)", fontSize = 12.sp) },
              placeholder = { Text("مثال: فاتورة كهرباء شهر 10", fontSize = 11.sp) },
              leadingIcon = {
                val icon = when {
                  partyName.contains("كهرباء") -> Icons.Default.Bolt
                  partyName.contains("مياه") || partyName.contains("ماء") -> Icons.Default.WaterDrop
                  partyName.contains("إيجار") -> Icons.Default.Store
                  partyName.contains("صيانة") -> Icons.Default.Build
                  partyName.contains("إنترنت") || partyName.contains("هاتف") -> Icons.Default.Wifi
                  partyName.contains("نظافة") -> Icons.Default.CleaningServices
                  partyName.contains("ضيافة") || partyName.contains("نثريات") -> Icons.Default.Coffee
                  partyType == "مورد" -> Icons.Default.LocalShipping
                  partyType == "موظف" -> Icons.Default.Badge
                  partyType == "عميل" -> Icons.Default.People
                  else -> Icons.Default.Category
                }
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
              },
              trailingIcon = {
                TextButton(onClick = { showPartyPicker = true }) {
                  Text("القائمة الشاملة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            )
          }

          // 2. Payment Method Selector (الصندوق vs الشيكات الموجودة)
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              "طريقة الدفع ومصدر الصرف:",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Option A: الصندوق (نقداً)
              Surface(
                onClick = {
                  paymentMethod = "الصندوق"
                  selectedCheck = null
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = if (paymentMethod == "الصندوق") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(
                  1.5.dp,
                  if (paymentMethod == "الصندوق") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = if (paymentMethod == "الصندوق") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                  )
                  Column {
                    Text(
                      text = "الصندوق (نقداً)",
                      fontSize = 12.sp,
                      fontWeight = if (paymentMethod == "الصندوق") FontWeight.Bold else FontWeight.Medium,
                      color = if (paymentMethod == "الصندوق") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text("من الخزينة النقدية", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
              }

              // Option B: شيك من المحفظة (شيكات واردة موجودة)
              Surface(
                onClick = {
                  paymentMethod = "شيك"
                  if (selectedCheck == null) {
                    showCheckPicker = true
                  }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = if (paymentMethod == "شيك") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(
                  1.5.dp,
                  if (paymentMethod == "شيك") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(
                    Icons.Default.Article,
                    contentDescription = null,
                    tint = if (paymentMethod == "شيك") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                  )
                  Column {
                    Text(
                      text = "الشيكات الموجودة",
                      fontSize = 12.sp,
                      fontWeight = if (paymentMethod == "شيك") FontWeight.Bold else FontWeight.Medium,
                      color = if (paymentMethod == "شيك") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text("تجيير شيك من المحفظة (${checkInvoices.size})", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
              }
            }
          }

          // 3. If Check is selected: Check Details or Picker Button
          if (paymentMethod == "شيك") {
            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                if (selectedCheck == null) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text("لم يتم تحديد شيك بعد", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CoralError)
                      Text("اختر شيكاً من الشيكات الواردة الموجودة في محفظتك لدفع هذا السند", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                      onClick = { showCheckPicker = true },
                      shape = RoundedCornerShape(10.dp),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                      Icon(Icons.Default.Article, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("عرض الشيكات (${checkInvoices.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                } else {
                  val chk = selectedCheck!!
                  val custName = customers.find { it.id == chk.customerId }?.name ?: "عميل نقدي"
                  val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                  val dueStr = chk.checkDate?.let { sdf.format(Date(it)) } ?: "غير محدد"

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                      Text("الشيك المختار للصرف والتجيير", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(
                      onClick = { showCheckPicker = true },
                      contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                      Text("اختيار شيك آخر", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }

                  HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Column {
                      Text("رقم الشيك: #${chk.checkNumber ?: ""}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                      Text("البنك: ${chk.checkBankName ?: "شيك بنكي"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      Text("الساحب/العميل: $custName", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                      Text(
                        "${String.format(Locale.US, "%.2f", chk.total)} $currencySymbol",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = EmeraldGreen
                      )
                      Text("الاستحقاق: $dueStr", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      Text(chk.checkStatus ?: "قيد الانتظار", fontSize = 10.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }

          // 4. Amount Input Field
          OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("المبلغ المطلوب صرفه", fontSize = 12.sp) },
            placeholder = { Text("0.00", fontSize = 12.sp) },
            leadingIcon = {
              Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            suffix = {
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = currencySymbol,
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
          )

          // 5. Notes / Description Input Field
          OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("البيان والملاحظات", fontSize = 12.sp) },
            placeholder = { Text("مثال: سداد دفعة مورد، سلفة موظف، إيجار شهر...", fontSize = 12.sp) },
            leadingIcon = {
              Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
          )
        }

        // --- Action Buttons Footer ---
        fun cleanExpenseAmount(input: String): Double {
          val clean = input.trim()
            .replace('٠', '0').replace('١', '1').replace('٢', '2').replace('٣', '3').replace('٤', '4')
            .replace('٥', '5').replace('٦', '6').replace('٧', '7').replace('٨', '8').replace('٩', '9')
            .replace('۰', '0').replace('۱', '1').replace('۲', '2').replace('۳', '3').replace('۴', '4')
            .replace('۵', '5').replace('۶', '6').replace('۷', '7').replace('۸', '8').replace('۹', '9')
            .replace(',', '.')
            .replace('،', '.')
          return clean.toDoubleOrNull() ?: 0.0
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Button(
            onClick = {
              if (isSaving) return@Button
              val amt = cleanExpenseAmount(amount)
              if (amt <= 0.0) {
                viewModel.showToast("يرجى إدخال مبلغ صحيح لسند الصرف أكبر من الصفر!")
                return@Button
              }

              if (paymentMethod == "شيك" && selectedCheck == null) {
                viewModel.showToast("يرجى تحديد الشيك المراد استخدامه للصرف!")
                return@Button
              }

              val finalEmpId = selectedEmployeeId ?: if (partyType == "موظف") {
                employees.firstOrNull { 
                  it.name.trim().equals(partyName.trim(), ignoreCase = true) ||
                  partyName.contains(it.name.trim(), ignoreCase = true) ||
                  it.name.trim().contains(partyName.trim(), ignoreCase = true)
                }?.id
              } else null

              val fullType = when (partyType) {
                "مورد" -> "مورد: $partyName"
                "موظف" -> "موظف: $partyName"
                "عميل" -> "عميل: $partyName"
                else -> partyName
              }

              isSaving = true
              viewModel.addExpense(
                type = fullType,
                amount = amt,
                notes = notes.trim(),
                employeeId = finalEmpId,
                paymentMethod = paymentMethod,
                checkNumber = selectedCheck?.checkNumber,
                checkBankName = selectedCheck?.checkBankName,
                checkInvoiceId = selectedCheck?.id,
                partyType = partyType,
                partyName = partyName,
                customerId = selectedCustomerId,
                supplierId = selectedSupplierId,
                onSuccess = {
                  isSaving = false
                  onDismiss()
                },
                onError = {
                  isSaving = false
                }
              )
            },
            enabled = !isSaving,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            modifier = Modifier.weight(1f)
          ) {
            if (isSaving) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(6.dp))
              Text("جاري الحفظ...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            } else {
              Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("حفظ سند الصرف", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }

          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("إلغاء", fontSize = 12.sp, color = CoralError)
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// Sub-Dialog: Beneficiary / Expense Type Picker
// (Includes Customers, Suppliers, Employees, and General Expenses)
// -------------------------------------------------------------
@Composable
fun ExpensePartyPickerDialog(
  viewModel: AppViewModel,
  customers: List<Customer>,
  suppliers: List<Supplier>,
  employees: List<Employee>,
  onSelectParty: (partyType: String, partyName: String, customerId: Int?, supplierId: Int?, employeeId: Int?) -> Unit,
  onDismiss: () -> Unit
) {
  val expenseCategories by viewModel.expenseCategories.collectAsState()
  var showManageCategoriesDialog by remember { mutableStateOf(false) }

  var selectedTab by remember { mutableStateOf("عام") } // "عام", "الكل", "موظفين", "موردين", "عملاء"
  var searchQuery by remember { mutableStateOf("") }

  // Filter out any dummy cash customer from customers list
  val realCustomers = remember(customers) {
    customers.filter {
      !it.name.contains("افتراضي") &&
      it.name.trim() != "عميل نقدي" &&
      it.name.trim() != "عميل نقدي افتراضي"
    }
  }

  if (showManageCategoriesDialog) {
    ManageExpenseCategoriesDialog(
      viewModel = viewModel,
      onDismiss = { showManageCategoriesDialog = false }
    )
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.85f)
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Column {
              Text("تحديد بند المصروف أو المستفيد", fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Text("كهرباء، مياه، إيجار، صيانة، نثريات، أو حسابات أخرى", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "إغلاق")
          }
        }

        // Search Bar
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("ابحث في المصروفات (كهرباء، مياه، إيجار...) أو اكتب اسماً جديداً...", fontSize = 12.sp) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        )

        // Category Filter Chips (Prioritizing "عام")
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          val tabs = listOf(
            "عام" to "المصروفات العامة (${expenseCategories.size})",
            "الكل" to "كافة الفئات",
            "موظفين" to "الموظفين (${employees.size})",
            "موردين" to "الموردين (${suppliers.size})",
            "عملاء" to "العملاء (${realCustomers.size})"
          )
          tabs.forEach { (tabKey, tabLabel) ->
            val isSelected = selectedTab == tabKey
            FilterChip(
              selected = isSelected,
              onClick = { selectedTab = tabKey },
              label = { Text(tabLabel, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
              shape = RoundedCornerShape(8.dp)
            )
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // Beneficiary & Expenses List
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // 1. General Operating Expenses (Rendered FIRST)
          if (selectedTab in listOf("عام", "الكل")) {
            val filteredGeneral = expenseCategories.filter {
              searchQuery.isBlank() || it.contains(searchQuery, ignoreCase = true)
            }

            item {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("المصروفات العامة والتشغيلية", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                TextButton(
                  onClick = { showManageCategoriesDialog = true },
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("إدارة البنود (إضافة / حذف)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }

            // Custom typed item if query not found
            if (searchQuery.isNotBlank() && expenseCategories.none { it.equals(searchQuery.trim(), ignoreCase = true) }) {
              item {
                BeneficiaryItemRow(
                  title = "استخدام \"${searchQuery.trim()}\"",
                  subtitle = "تسجيل كمصروف عام تشغيلي جديد",
                  badgeText = "بند مخصص",
                  badgeColor = MaterialTheme.colorScheme.primary,
                  icon = Icons.Default.AddCircle,
                  onClick = {
                    onSelectParty("مصروف عام", searchQuery.trim(), null, null, null)
                  }
                )
              }
            }

            items(filteredGeneral) { genType ->
              val (genColor, genIcon) = when {
                genType.contains("كهرباء") -> Pair(Color(0xFFD97706), Icons.Default.Bolt)
                genType.contains("مياه") || genType.contains("ماء") -> Pair(Color(0xFF0284C7), Icons.Default.WaterDrop)
                genType.contains("إيجار") -> Pair(Color(0xFF7C3AED), Icons.Default.Store)
                genType.contains("صيانة") -> Pair(Color(0xFF4B5563), Icons.Default.Build)
                genType.contains("إنترنت") || genType.contains("اتصالات") || genType.contains("هاتف") -> Pair(Color(0xFF0D9488), Icons.Default.Wifi)
                genType.contains("نظافة") -> Pair(Color(0xFF059669), Icons.Default.CleaningServices)
                genType.contains("ضيافة") || genType.contains("نثريات") || genType.contains("بوفيه") -> Pair(Color(0xFFB45309), Icons.Default.Coffee)
                genType.contains("تسويق") || genType.contains("دعاية") || genType.contains("إعلان") -> Pair(Color(0xFF7B1FA2), Icons.Default.Campaign)
                genType.contains("رواتب") || genType.contains("أجور") -> Pair(Color(0xFF8B5CF6), Icons.Default.Badge)
                genType.contains("شحن") || genType.contains("نقل") || genType.contains("مواصلات") -> Pair(Color(0xFF0284C7), Icons.Default.LocalShipping)
                else -> Pair(MaterialTheme.colorScheme.primary, Icons.Default.Category)
              }

              BeneficiaryItemRow(
                title = genType,
                subtitle = "بند مصروف تشغيلي",
                badgeText = "مصروف عام",
                badgeColor = genColor,
                icon = genIcon,
                onClick = {
                  onSelectParty("مصروف عام", genType, null, null, null)
                }
              )
            }
          }

          // 2. Employees
          if (selectedTab in listOf("موظفين", "الكل")) {
            val filteredEmployees = employees.filter {
              searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) ||
                it.role.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
            }
            if (filteredEmployees.isNotEmpty()) {
              item {
                Text("الموظفين (رواتب ومستحقات)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF8B5CF6), modifier = Modifier.padding(top = 6.dp))
              }
              items(filteredEmployees, key = { "emp_${it.id}" }) { emp ->
                BeneficiaryItemRow(
                  title = emp.name,
                  subtitle = "وظيفة: ${emp.role} • جوال: ${emp.phone}",
                  badgeText = "موظف",
                  badgeColor = Color(0xFF8B5CF6),
                  icon = Icons.Default.Badge,
                  onClick = {
                    onSelectParty("موظف", emp.name, null, null, emp.id)
                  }
                )
              }
            }
          }

          // 3. Suppliers
          if (selectedTab in listOf("موردين", "الكل")) {
            val filteredSuppliers = suppliers.filter {
              searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) ||
                it.companyName.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
            }
            if (filteredSuppliers.isNotEmpty()) {
              item {
                Text("الموردين (سداد دفعات)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0284C7), modifier = Modifier.padding(top = 6.dp))
              }
              items(filteredSuppliers, key = { "supp_${it.id}" }) { supp ->
                BeneficiaryItemRow(
                  title = supp.name,
                  subtitle = if (supp.companyName.isNotBlank()) "شركة: ${supp.companyName} • جوال: ${supp.phone}" else "جوال: ${supp.phone}",
                  badgeText = "مورد",
                  badgeColor = Color(0xFF0284C7),
                  icon = Icons.Default.LocalShipping,
                  onClick = {
                    onSelectParty("مورد", supp.name, null, supp.id, null)
                  }
                )
              }
            }
          }

          // 4. Customers
          if (selectedTab in listOf("عملاء", "الكل")) {
            val filteredCustomers = realCustomers.filter {
              searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
            }

            item {
              Text("العملاء (رد أمانات أو دفعات)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF10B981), modifier = Modifier.padding(top = 6.dp))
            }

            // Custom typed customer name if query is not already in the list
            if (searchQuery.isNotBlank() && filteredCustomers.none { it.name.trim().equals(searchQuery.trim(), ignoreCase = true) }) {
              item {
                BeneficiaryItemRow(
                  title = "صرف للعميل: \"${searchQuery.trim()}\"",
                  subtitle = "تسجيل السند لاسم العميل المدخل",
                  badgeText = "عميل محدد",
                  badgeColor = Color(0xFF10B981),
                  icon = Icons.Default.PersonAdd,
                  onClick = {
                    onSelectParty("عميل", searchQuery.trim(), null, null, null)
                  }
                )
              }
            }

            if (filteredCustomers.isEmpty() && searchQuery.isBlank()) {
              item {
                Card(
                  shape = RoundedCornerShape(10.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                ) {
                  Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Text(
                      text = "لا توجد أسماء عملاء مسجلة حالياً",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = "يمكنك كتابة اسم العميل مباشرة في خانة البحث بالأعلى لتسجيل المصروف باسمه فوراً.",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                  }
                }
              }
            } else {
              items(filteredCustomers, key = { "cust_${it.id}" }) { cust ->
                BeneficiaryItemRow(
                  title = cust.name,
                  subtitle = "نوع: ${cust.type} • جوال: ${cust.phone}",
                  badgeText = "عميل",
                  badgeColor = Color(0xFF10B981),
                  icon = Icons.Default.People,
                  onClick = {
                    onSelectParty("عميل", cust.name, cust.id, null, null)
                  }
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
fun BeneficiaryItemRow(
  title: String,
  subtitle: String,
  badgeText: String,
  badgeColor: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(18.dp))
        }
        Column {
          Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          if (subtitle.isNotBlank()) {
            Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }

      Surface(
        color = badgeColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
      ) {
        Text(
          text = badgeText,
          color = badgeColor,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
      }
    }
  }
}

// -------------------------------------------------------------
// Sub-Dialog: Check Picker Dialog (Shows on-hand checks)
// -------------------------------------------------------------
@Composable
fun CheckPickerDialog(
  checks: List<Invoice>,
  customers: List<Customer>,
  currencySymbol: String,
  onSelectCheck: (Invoice) -> Unit,
  onDismiss: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  val sdf = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

  val filteredChecks = remember(checks, searchQuery) {
    checks.filter { chk ->
      val custName = customers.find { it.id == chk.customerId }?.name ?: "عميل نقدي"
      searchQuery.isBlank() ||
        (chk.checkNumber?.contains(searchQuery, ignoreCase = true) == true) ||
        (chk.checkBankName?.contains(searchQuery, ignoreCase = true) == true) ||
        custName.contains(searchQuery, ignoreCase = true) ||
        chk.total.toString().contains(searchQuery)
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.85f)
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .background(Color(0xFF0284C7).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Article, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
            }
            Column {
              Text("شيكات المحفظة المتوفرة (${checks.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Text("اختر الشيك المراد استخدامه لتسديد وصرف هذا السند", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "إغلاق")
          }
        }

        // Search Bar
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("ابحث برقم الشيك، البنك، أو اسم الساحب...", fontSize = 12.sp) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // Check List
        if (filteredChecks.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.Article, contentDescription = null, tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                if (searchQuery.isBlank()) "لا توجد شيكات واردة في النظام حالياً" else "لا توجد شيكات مطابقة للبحث",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(filteredChecks, key = { it.id }) { chk ->
              val custName = customers.find { it.id == chk.customerId }?.name ?: "عميل نقدي"
              val dueStr = chk.checkDate?.let { sdf.format(Date(it)) } ?: "غير محدد"
              val isPending = chk.checkStatus == null || chk.checkStatus == "قيد الانتظار"

              Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, if (isPending) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onSelectCheck(chk) }
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
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
                      Surface(
                        color = Color(0xFF0284C7).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                      ) {
                        Text(
                          "شيك #${chk.checkNumber ?: ""}",
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Bold,
                          color = Color(0xFF0284C7),
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                      Text(chk.checkBankName ?: "شيك بنكي", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Text(
                      "${String.format(Locale.US, "%.2f", chk.total)} $currencySymbol",
                      fontWeight = FontWeight.Bold,
                      fontSize = 14.sp,
                      color = EmeraldGreen
                    )
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text("الساحب: $custName", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("الاستحقاق: $dueStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    val statusText = chk.checkStatus ?: "قيد الانتظار"
                    val statusColor = when (statusText) {
                      "تم التحصيل" -> EmeraldGreen
                      "مجير / تم الصرف" -> Color(0xFF8B5CF6)
                      "مرفوض" -> CoralError
                      else -> Color(0xFF0284C7)
                    }
                    Text(
                      "الحالة: $statusText",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = statusColor
                    )

                    Surface(
                      color = MaterialTheme.colorScheme.primaryContainer,
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text(
                        "اختيار هذا الشيك",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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

// -------------------------------------------------------------
// Expense Card for main list
// -------------------------------------------------------------
@Composable
fun ExpenseCard(
  expense: Expense,
  currencySymbol: String,
  employeeName: String?,
  onDeleteClick: () -> Unit
) {
  var showDeleteDialog by remember { mutableStateOf(false) }

  if (showDeleteDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteDialog = false },
      title = {
        Text(
          text = "حذف سند الصرف",
          fontWeight = FontWeight.Bold,
          color = Color(0xFFDC2626)
        )
      },
      text = {
        Text("هل أنت متأكد من رغبتك في حذف هذا السند بقيمة ${expense.amount} $currencySymbol؟")
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteDialog = false
            onDeleteClick()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text("تأكيد الحذف", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { showDeleteDialog = false },
          shape = RoundedCornerShape(8.dp)
        ) {
          Text("إلغاء")
        }
      }
    )
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f)
        ) {
          val (badgeColor, icon, catTag) = when {
            expense.type.contains("كهرباء") -> Triple(Color(0xFFD97706), Icons.Default.Bolt, "كهرباء")
            expense.type.contains("مياه") || expense.type.contains("ماء") -> Triple(Color(0xFF0284C7), Icons.Default.WaterDrop, "مياه")
            expense.type.contains("إيجار") -> Triple(Color(0xFF7C3AED), Icons.Default.Store, "إيجار")
            expense.type.contains("صيانة") -> Triple(Color(0xFF4B5563), Icons.Default.Build, "صيانة")
            expense.type.contains("إنترنت") || expense.type.contains("هاتف") || expense.type.contains("اتصالات") -> Triple(Color(0xFF0D9488), Icons.Default.Wifi, "إنترنت")
            expense.type.contains("نظافة") -> Triple(Color(0xFF059669), Icons.Default.CleaningServices, "نظافة")
            expense.type.contains("ضيافة") || expense.type.contains("نثريات") || expense.type.contains("بوفيه") -> Triple(Color(0xFFB45309), Icons.Default.Coffee, "نثريات")
            expense.type.contains("تسويق") || expense.type.contains("دعاية") -> Triple(Color(0xFF7B1FA2), Icons.Default.Campaign, "تسويق")
            expense.partyType == "مورد" -> Triple(Color(0xFF0284C7), Icons.Default.LocalShipping, "مورد")
            expense.partyType == "موظف" -> Triple(Color(0xFF8B5CF6), Icons.Default.Badge, "موظف")
            expense.partyType == "عميل" -> Triple(Color(0xFF10B981), Icons.Default.People, "عميل")
            else -> Triple(Color(0xFFDC2626), Icons.Default.AttachMoney, "مصروف تشغيلي")
          }

          Box(
            modifier = Modifier
              .size(36.dp)
              .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(19.dp))
          }

          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Surface(
                color = badgeColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
              ) {
                Text(
                  text = catTag,
                  color = badgeColor,
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
              }
              Text(
                text = expense.type + if (employeeName != null && !expense.type.contains(employeeName)) " - $employeeName" else "",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
            Text(sdf.format(Date(expense.date)), color = Color.Gray, fontSize = 11.sp)
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            "${String.format(Locale.US, "%.2f", expense.amount)} $currencySymbol",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFFDC2626)
          )
          IconButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.size(28.dp)
          ) {
            Icon(Icons.Default.Delete, contentDescription = "حذف السند", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Payment Method Badge
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          if (expense.paymentMethod == "شيك") {
            Surface(
              color = Color(0xFF0284C7).copy(alpha = 0.12f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(Icons.Default.Article, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(12.dp))
                Text(
                  "شيك: #${expense.checkNumber ?: ""}${if (!expense.checkBankName.isNullOrBlank()) " (${expense.checkBankName})" else ""}",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF0284C7)
                )
              }
            }
          } else {
            Surface(
              color = EmeraldGreen.copy(alpha = 0.12f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(Icons.Default.Payments, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(12.dp))
                Text(
                  "الصندوق (نقداً)",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = EmeraldGreen
                )
              }
            }
          }
        }

        if (expense.notes.isNotBlank()) {
          Text(
            text = expense.notes,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

// -------------------------------------------------------------
// Manage Expense Categories Dialog (Add / Delete / Reset)
// -------------------------------------------------------------
@Composable
fun ManageExpenseCategoriesDialog(
  viewModel: AppViewModel,
  onDismiss: () -> Unit
) {
  val expenseCategories by viewModel.expenseCategories.collectAsState()
  var newCategoryName by remember { mutableStateOf("") }
  var categoryToDelete by remember { mutableStateOf<String?>(null) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .fillMaxHeight(0.80f)
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .background(Color(0xFF2563EB).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
            }
            Column {
              Text("إدارة بنود المصروفات", fontWeight = FontWeight.Bold, fontSize = 15.sp)
              Text("إضافة بنود جديدة أو حذف البنود القائمة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "إغلاق")
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // Add new category input
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = newCategoryName,
            onValueChange = { newCategoryName = it },
            label = { Text("اسم بند المصروف الجديد", fontSize = 11.5.sp) },
            placeholder = { Text("مثال: وقود وبنزين، صيانة مكيفات...", fontSize = 11.sp) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          )

          Button(
            onClick = {
              val name = newCategoryName.trim()
              if (name.isNotBlank()) {
                viewModel.addExpenseCategory(name) {
                  newCategoryName = ""
                }
              } else {
                viewModel.showToast("يرجى كتابة اسم البند أولاً!")
              }
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("إضافة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }

        Text(
          "البنود الحالية المتاحة (${expenseCategories.size} بند):",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        // Categories List
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          items(expenseCategories, key = { it }) { cat ->
            val icon = when {
              cat.contains("كهرباء") -> Icons.Default.Bolt
              cat.contains("مياه") || cat.contains("ماء") -> Icons.Default.WaterDrop
              cat.contains("إيجار") -> Icons.Default.Store
              cat.contains("صيانة") -> Icons.Default.Build
              cat.contains("نثريات") || cat.contains("ضيافة") || cat.contains("بوفيه") -> Icons.Default.Coffee
              cat.contains("إنترنت") || cat.contains("هاتف") || cat.contains("اتصالات") -> Icons.Default.Wifi
              cat.contains("نظافة") -> Icons.Default.CleaningServices
              cat.contains("رواتب") || cat.contains("أجور") -> Icons.Default.Badge
              cat.contains("تسويق") || cat.contains("دعاية") || cat.contains("إعلان") -> Icons.Default.Campaign
              cat.contains("شحن") || cat.contains("نقل") || cat.contains("مواصلات") -> Icons.Default.LocalShipping
              else -> Icons.Default.Category
            }

            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                  Text(cat, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                }

                IconButton(
                  onClick = { categoryToDelete = cat },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "حذف البند", tint = CoralError, modifier = Modifier.size(18.dp))
                }
              }
            }
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // Footer Actions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(
            onClick = {
              viewModel.resetExpenseCategoriesToDefault()
            }
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("استعادة البنود الافتراضية", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }

          Button(
            onClick = onDismiss,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
          ) {
            Text("تم", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }

  // Delete Confirmation Dialog
  if (categoryToDelete != null) {
    val cat = categoryToDelete!!
    AlertDialog(
      onDismissRequest = { categoryToDelete = null },
      title = { Text("تأكيد حذف بند المصروف", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
      text = { Text("هل أنت متأكد من حذف بند \"$cat\" من قائمة المصروفات المتاحة؟", fontSize = 13.sp) },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteExpenseCategory(cat)
            categoryToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = CoralError)
        ) {
          Text("نعم، حذف", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { categoryToDelete = null }) {
          Text("إلغاء")
        }
      }
    )
  }
}

