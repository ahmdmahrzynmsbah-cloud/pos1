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
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
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

  var showAddDialog by remember { mutableStateOf(false) }

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
                text = "سندات الصرف",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
              )
              Text(
                text = "المصروفات والمدفوعات",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
              )
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                text = "سند صرف جديد",
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
        .padding(16.dp)
    ) {
      if (expenses.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              Icons.Default.AttachMoney,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
              modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text("لا توجد سندات صرف مطابقة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(
              "اضغط على زر 'سند صرف جديد' بالأعلى لتسجيل أي مصروف.",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          items(expenses.reversed()) { expense ->
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
  // Expense Beneficiary State: partyType can be "مصروف عام", "مورد", "موظف", "عميل"
  var partyType by remember { mutableStateOf("مصروف عام") }
  var partyName by remember { mutableStateOf("إيجار") }
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

  // Available checks from invoices
  val checkInvoices = remember(invoices) {
    invoices.filter { it.paymentMethod == "شيك" || !it.checkNumber.isNullOrBlank() }
      .sortedByDescending { it.invoiceDate }
  }

  // Sub-Dialog 1: Beneficiary / Expense Type Picker
  if (showPartyPicker) {
    ExpensePartyPickerDialog(
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

          // 1. Beneficiary / Expense Type Selector
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              "نوع المصروف / جهة الصرف (عملاء، موردين، موظفين، بنود عامة):",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )

            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { showPartyPicker = true }
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
                  val (partyBadgeColor, partyIcon) = when (partyType) {
                    "مورد" -> Pair(Color(0xFF0284C7), Icons.Default.LocalShipping)
                    "موظف" -> Pair(Color(0xFF8B5CF6), Icons.Default.Badge)
                    "عميل" -> Pair(Color(0xFF10B981), Icons.Default.People)
                    else -> Pair(MaterialTheme.colorScheme.primary, Icons.Default.Category)
                  }

                  Box(
                    modifier = Modifier
                      .size(36.dp)
                      .background(partyBadgeColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(partyIcon, contentDescription = null, tint = partyBadgeColor, modifier = Modifier.size(20.dp))
                  }

                  Column {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Surface(
                        color = partyBadgeColor,
                        shape = RoundedCornerShape(6.dp)
                      ) {
                        Text(
                          text = partyType,
                          color = Color.White,
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                      Text(
                        text = partyName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                      )
                    }
                    Text(
                      text = "انقر لتغيير أو اختيار جهة الصرف (عملاء / موردين / موظفين / أخرى)",
                      fontSize = 10.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("تغيير", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                  }
                }
              }
            }
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
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Button(
            onClick = {
              val amt = amount.toDoubleOrNull()
              if (amt == null || amt <= 0) {
                viewModel.showToast("يرجى إدخال مبلغ صحيح للسند!")
                return@Button
              }

              if (paymentMethod == "شيك" && selectedCheck == null) {
                viewModel.showToast("يرجى تحديد الشيك المراد استخدامه للصرف!")
                return@Button
              }

              val fullType = when (partyType) {
                "مورد" -> "مورد: $partyName"
                "موظف" -> "موظف: $partyName"
                "عميل" -> "عميل: $partyName"
                else -> partyName
              }

              viewModel.addExpense(
                type = fullType,
                amount = amt,
                notes = notes.trim(),
                employeeId = selectedEmployeeId,
                paymentMethod = paymentMethod,
                checkNumber = selectedCheck?.checkNumber,
                checkBankName = selectedCheck?.checkBankName,
                checkInvoiceId = selectedCheck?.id,
                partyType = partyType,
                partyName = partyName,
                customerId = selectedCustomerId,
                supplierId = selectedSupplierId
              )
              onDismiss()
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("حفظ سند الصرف", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
  customers: List<Customer>,
  suppliers: List<Supplier>,
  employees: List<Employee>,
  onSelectParty: (partyType: String, partyName: String, customerId: Int?, supplierId: Int?, employeeId: Int?) -> Unit,
  onDismiss: () -> Unit
) {
  var selectedTab by remember { mutableStateOf("الكل") } // "الكل", "موردين", "موظفين", "عملاء", "عام"
  var searchQuery by remember { mutableStateOf("") }

  val generalExpenseTypes = listOf(
    "إيجار المحل / المقر",
    "رواتب وأجور",
    "كهرباء ومياه",
    "صيانة ومعدات",
    "نقل وشحن وبضائع",
    "ضيافة ونثريات وبوفيه",
    "دعاية وإعلان وتسويق",
    "رسوم وتراخيص واشتراكات",
    "مصروفات تشغيلية أخرى"
  )

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
              Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Column {
              Text("تحديد نوع المصروف أو المستفيد", fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Text("اختر المورد، الموظف، العميل، أو البند العام المراد الصرف له", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
          placeholder = { Text("ابحث باسم المورد، الموظف، العميل، أو البند...", fontSize = 12.sp) },
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

        // Category Filter Chips
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          val tabs = listOf(
            "الكل" to "الكل",
            "موردين" to "الموردين (${suppliers.size})",
            "موظفين" to "الموظفين (${employees.size})",
            "عملاء" to "العملاء (${customers.size})",
            "عام" to "مصروفات عامة"
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

        // Beneficiary List
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // 1. Suppliers
          if (selectedTab in listOf("الكل", "موردين")) {
            val filteredSuppliers = suppliers.filter {
              searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) ||
                it.companyName.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
            }
            if (filteredSuppliers.isNotEmpty()) {
              item {
                Text("الموردين", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0284C7), modifier = Modifier.padding(top = 4.dp))
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

          // 2. Employees
          if (selectedTab in listOf("الكل", "موظفين")) {
            val filteredEmployees = employees.filter {
              searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) ||
                it.role.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
            }
            if (filteredEmployees.isNotEmpty()) {
              item {
                Text("الموظفين", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF8B5CF6), modifier = Modifier.padding(top = 4.dp))
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

          // 3. Customers
          if (selectedTab in listOf("الكل", "عملاء")) {
            val filteredCustomers = customers.filter {
              searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
            }
            if (filteredCustomers.isNotEmpty()) {
              item {
                Text("العملاء", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF10B981), modifier = Modifier.padding(top = 4.dp))
              }
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

          // 4. General Expenses
          if (selectedTab in listOf("الكل", "عام")) {
            val filteredGeneral = generalExpenseTypes.filter {
              searchQuery.isBlank() || it.contains(searchQuery, ignoreCase = true)
            }
            if (filteredGeneral.isNotEmpty()) {
              item {
                Text("بنود المصروفات العامة", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
              }
              items(filteredGeneral) { genType ->
                BeneficiaryItemRow(
                  title = genType,
                  subtitle = "بند تشغيلي عام",
                  badgeText = "مصروف عام",
                  badgeColor = MaterialTheme.colorScheme.primary,
                  icon = Icons.Default.Category,
                  onClick = {
                    onSelectParty("مصروف عام", genType, null, null, null)
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
          val (badgeColor, icon) = when (expense.partyType) {
            "مورد" -> Pair(Color(0xFF0284C7), Icons.Default.LocalShipping)
            "موظف" -> Pair(Color(0xFF8B5CF6), Icons.Default.Badge)
            "عميل" -> Pair(Color(0xFF10B981), Icons.Default.People)
            else -> Pair(Color(0xFFDC2626), Icons.Default.AttachMoney)
          }

          Box(
            modifier = Modifier
              .size(34.dp)
              .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(18.dp))
          }

          Column {
            Text(
              text = expense.type + if (employeeName != null && !expense.type.contains(employeeName)) " - $employeeName" else "",
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
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
