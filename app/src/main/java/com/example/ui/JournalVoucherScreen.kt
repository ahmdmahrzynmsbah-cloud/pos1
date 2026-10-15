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
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Customer
import com.example.data.Employee
import com.example.data.LedgerEntry
import com.example.data.Supplier
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldGreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalVoucherScreen(viewModel: AppViewModel, onNavigate: (String) -> Unit = {}) {
  val ledgerEntries by viewModel.allLedgerEntries.collectAsState(initial = emptyList())
  val currencySymbol by viewModel.currencySymbol.collectAsState()
  val context = LocalContext.current
  var showAddDialog by remember { mutableStateOf(true) }
  var searchQuery by remember { mutableStateOf("") }

  // Filter entries
  val filteredEntries = remember(ledgerEntries, searchQuery) {
    if (searchQuery.isBlank()) {
      ledgerEntries
    } else {
      ledgerEntries.filter {
        it.description.contains(searchQuery, ignoreCase = true) ||
          it.debitAccount.contains(searchQuery, ignoreCase = true) ||
          it.creditAccount.contains(searchQuery, ignoreCase = true) ||
          it.id.toString().contains(searchQuery)
      }
    }
  }

  val totalAmount = remember(filteredEntries) {
    filteredEntries.sumOf { it.amount }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Top Bar with Actions
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
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF6366F1).copy(alpha = 0.12f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.ReceiptLong,
            contentDescription = null,
            tint = Color(0xFF6366F1),
            modifier = Modifier.size(24.dp)
          )
        }
        Column {
          Text(
            text = "سندات القيد المحاسبي",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "سندات الصرف والقبض والقيود اليومية المزدوجة",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Button(
        onClick = { showAddDialog = true },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("سند قيد جديد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }

    // Summary Statistics Cards
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text("عدد القيود المسجلة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("${filteredEntries.size} سند قيد", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
      }

      Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(12.dp)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text("إجمالي قيمة القيود", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("${String.format(Locale.US, "%.2f", totalAmount)} $currencySymbol", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
        }
      }
    }

    // Search Bar
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("بحث برقم السند، البيان، أو الحسابات...") },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { searchQuery = "" }) {
            Icon(Icons.Default.Close, contentDescription = "مسح")
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth()
    )

    // Entries List
    if (filteredEntries.isEmpty()) {
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
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            modifier = Modifier.size(54.dp)
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            if (searchQuery.isBlank()) "لا توجد سندات قيد مسجلة حتى الآن." else "لا توجد نتائج مطابقة للبحث.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(filteredEntries.reversed(), key = { it.id }) { entry ->
          JournalVoucherCard(
            entry = entry,
            currencySymbol = currencySymbol,
            onPrintClick = {
              viewModel.exportJournalVoucherToPdf(context, entry, "سند قيد محاسبي")
            },
            onDeleteClick = {
              viewModel.deleteLedgerEntry(entry)
            }
          )
        }
      }
    }
  }

  // Exact Match Modal Dialog from image.png
  if (showAddDialog) {
    CreateJournalVoucherDialog(
      viewModel = viewModel,
      onDismiss = { showAddDialog = false },
      onNavigate = onNavigate
    )
  }
}

// -----------------------------------------------------------------
// Card in the list
// -----------------------------------------------------------------
@Composable
fun JournalVoucherCard(
  entry: LedgerEntry,
  currencySymbol: String,
  onPrintClick: () -> Unit,
  onDeleteClick: () -> Unit
) {
  var showDeleteConfirmDialog by remember { mutableStateOf(false) }

  if (showDeleteConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirmDialog = false },
      title = {
        Text(
          text = "حذف سند القيد",
          fontWeight = FontWeight.Bold,
          color = Color(0xFFDC2626)
        )
      },
      text = {
        Text("هل أنت متأكد من رغبتك في حذف سند القيد المحاسبي (${entry.description})؟")
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirmDialog = false
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
          onClick = { showDeleteConfirmDialog = false },
          shape = RoundedCornerShape(8.dp)
        ) {
          Text("إلغاء")
        }
      }
    )
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Surface(
            color = Color(0xFF6366F1).copy(alpha = 0.12f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = "سند رقم #${entry.id}",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF4F46E5),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
          Text(sdf.format(Date(entry.date)), fontSize = 11.sp, color = Color.Gray)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          Button(
            onClick = onPrintClick,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
          ) {
            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("طباعة", fontSize = 11.sp)
          }

          IconButton(
            onClick = { showDeleteConfirmDialog = true },
            modifier = Modifier.size(32.dp)
          ) {
            Icon(Icons.Default.Delete, contentDescription = "حذف السند", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

      Text(
        text = "البيان: ${entry.description}",
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
          .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text("الجانب المدين (من حـ/):", fontSize = 10.sp, color = Color.Gray)
          Text(entry.debitAccount, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("الجانب الدائن (إلى حـ/):", fontSize = 10.sp, color = Color.Gray)
          Text(entry.creditAccount, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CoralError)
        }

        Column(horizontalAlignment = Alignment.End) {
          Text("المبلغ الإجمالي:", fontSize = 10.sp, color = Color.Gray)
          Text(
            "${String.format(Locale.US, "%.2f", entry.amount)} $currencySymbol",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = EmeraldGreen
          )
        }
      }
    }
  }
}

// -----------------------------------------------------------------
// Account Picker Entry Data Model
// -----------------------------------------------------------------
data class AccountPickerEntry(
  val name: String,
  val type: String, // "حساب عام", "عميل", "مورد", "موظف"
  val subtitle: String,
  val badgeColor: Color,
  val icon: ImageVector
)

// -----------------------------------------------------------------
// Create Journal Voucher Dialog - Exactly as shown in image.png
// -----------------------------------------------------------------
@Composable
fun CreateJournalVoucherDialog(
  viewModel: AppViewModel,
  onDismiss: () -> Unit,
  onNavigate: (String) -> Unit = {}
) {
  val context = LocalContext.current
  val employees by viewModel.allEmployees.collectAsState(initial = emptyList())
  val customers by viewModel.allCustomers.collectAsState(initial = emptyList())
  val suppliers by viewModel.allSuppliers.collectAsState(initial = emptyList())
  val currencySymbol by viewModel.currencySymbol.collectAsState()

  // 1. Voucher Type Selection: "سند صرف", "سند قبض", "سند قيد محاسبي" (in image, "سند صرف" is selected)
  var voucherType by remember { mutableStateOf("سند صرف") }

  // 2. Description
  var description by remember { mutableStateOf("") }

  // 3. Debit & Credit Accounts
  var debitAcc by remember { mutableStateOf("عميل نقدي افتراضي") }
  var creditAcc by remember { mutableStateOf("الصندوق") }

  // 4. Amount
  var amount by remember { mutableStateOf("") }

  // Account Picker Sub-Dialog for either debit or credit side
  var pickerSide by remember { mutableStateOf<String?>(null) } // "debit" or "credit"

  if (pickerSide != null) {
    val title = if (pickerSide == "debit") "اختيار الجانب المدين (من حساب)" else "اختيار الجانب الدائن (إلى حساب)"
    AllAccountsPickerDialog(
      sideTitle = title,
      customers = customers,
      suppliers = suppliers,
      employees = employees,
      onSelect = { selectedName ->
        if (pickerSide == "debit") {
          debitAcc = selectedName
        } else {
          creditAcc = selectedName
        }
        pickerSide = null
      },
      onDismiss = { pickerSide = null }
    )
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(26.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .padding(vertical = 16.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // --- Header (Centered Icon & Title as in image) ---
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF6366F1).copy(alpha = 0.12f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.ReceiptLong,
            contentDescription = null,
            tint = Color(0xFF6366F1),
            modifier = Modifier.size(26.dp)
          )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "إصدار سند قيد / قيد محاسبي",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "سندات الخزينة والقيود المزدوجة",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
        }

        // --- Scrollable Body ---
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // --- Section 1: نوع سند القيد: ---
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "نوع سند القيد:",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 3 Pills: سند قيد محاسبي | سند قبض | سند صرف
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              val voucherTypes = listOf("سند صرف", "سند قبض", "سند قيد محاسبي")
              voucherTypes.forEach { type ->
                val isSelected = voucherType == type
                Surface(
                  onClick = {
                    voucherType = type
                    when (type) {
                      "سند صرف" -> {
                        debitAcc = "المصاريف العامة"
                        creditAcc = "الصندوق"
                      }
                      "سند قبض" -> {
                        debitAcc = "الصندوق"
                        creditAcc = customers.firstOrNull()?.name ?: "عميل نقدي افتراضي"
                      }
                      "سند قيد محاسبي" -> {
                        debitAcc = "عميل نقدي افتراضي"
                        creditAcc = "الصندوق"
                      }
                    }
                  },
                  shape = RoundedCornerShape(20.dp),
                  color = if (isSelected) Color(0xFFDCFCE7) else Color.Transparent,
                  border = BorderStroke(
                    1.dp,
                    if (isSelected) Color(0xFF16A34A).copy(alpha = 0.6f) else Color(0xFFCBD5E1)
                  ),
                  modifier = Modifier.weight(1f)
                ) {
                  Box(
                    modifier = Modifier.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = type,
                      fontSize = 12.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                      color = if (isSelected) Color(0xFF16A34A) else Color(0xFF64748B),
                      textAlign = TextAlign.Center
                    )
                  }
                }
              }
            }
          }

          // --- Section 2: بيان وشرح القيد المحاسبي: ---
          OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("بيان وشرح القيد المحاسبي:", fontSize = 13.sp, color = Color.Gray) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          )

          // --- Section 3: الجانب المدين (من حساب): ---
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "الجانب المدين (من حساب):",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF2563EB)
            )

            // Direct Wide Pill Button for Selected Account
            Surface(
              onClick = { pickerSide = "debit" },
              shape = RoundedCornerShape(24.dp),
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.5.dp, Color(0xFF818CF8)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 11.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = debitAcc,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF4F46E5),
                  textAlign = TextAlign.Center
                )
              }
            }
          }

          // --- Section 4: الجانب الدائن (إلى حساب): ---
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "الجانب الدائن (إلى حساب):",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFDC2626)
            )

            // Direct Wide Pill Button for Selected Credit Account
            Surface(
              onClick = { pickerSide = "credit" },
              shape = RoundedCornerShape(24.dp),
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.5.dp, Color(0xFF818CF8)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 11.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = creditAcc,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF4F46E5),
                  textAlign = TextAlign.Center
                )
              }
            }
          }

          // --- Section 5: المبلغ ---
          OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            placeholder = { Text("المبلغ ($currencySymbol):", fontSize = 13.sp, color = Color.Gray) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          )
        }

        // --- Section 6: Action Buttons at Bottom ---
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Button 1: ترحيل وطباعة السند (Green)
            Button(
              onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                if (amt > 0 && description.isNotBlank()) {
                  viewModel.addManualLedgerEntry(description, debitAcc, creditAcc, amt) { newEntry ->
                    viewModel.exportJournalVoucherToPdf(context, newEntry, voucherType)
                  }
                  onDismiss()
                } else {
                  viewModel.showToast("خطأ: يرجى كتابة بيان القيد وإدخال مبلغ صحيح!")
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1.3f)
            ) {
              Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
              Spacer(modifier = Modifier.width(6.dp))
              Text("ترحيل وطباعة السند", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            // Button 2: ترحيل فقط (Blue)
            Button(
              onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                if (amt > 0 && description.isNotBlank()) {
                  viewModel.addManualLedgerEntry(description, debitAcc, creditAcc, amt)
                  onDismiss()
                } else {
                  viewModel.showToast("خطأ: يرجى كتابة بيان القيد وإدخال مبلغ صحيح!")
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("ترحيل فقط", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
          }

          // Red Text Button: إلغاء
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "إلغاء",
              color = Color(0xFFEF4444),
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
          }
        }
      }
    }
  }
}

// -----------------------------------------------------------------
// Sub-Dialog to pick any account/person directly under each other
// (Includes General Accounts, Customers, Suppliers, and Employees)
// -----------------------------------------------------------------
@Composable
fun AllAccountsPickerDialog(
  sideTitle: String,
  customers: List<Customer>,
  suppliers: List<Supplier>,
  employees: List<Employee>,
  onSelect: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }

  val generalAccountsList = remember {
    listOf(
      AccountPickerEntry("الصندوق", "حساب عام", "الخزينة النقدية الرئيسية", Color(0xFF2563EB), Icons.Default.AccountBalanceWallet),
      AccountPickerEntry("البنك (الحساب الجاري)", "حساب عام", "الحساب البنكي / الإيداعات", Color(0xFF2563EB), Icons.Default.AccountBalance),
      AccountPickerEntry("أوراق قبض (شيكات واردة)", "حساب عام", "شيكات العملاء برسم التحصيل", Color(0xFF0284C7), Icons.Default.Article),
      AccountPickerEntry("أوراق دفع (شيكات صادرة)", "حساب عام", "شيكات محررة للموردين / دائنين", Color(0xFF0284C7), Icons.Default.Article),
      AccountPickerEntry("إيرادات المبيعات", "حساب عام", "حساب المبيعات العامة والدخل", Color(0xFF10B981), Icons.Default.AttachMoney),
      AccountPickerEntry("المصاريف العامة", "حساب عام", "حساب المصروفات التشغيلية", Color(0xFFDC2626), Icons.Default.Payments),
      AccountPickerEntry("رواتب وأجور", "حساب عام", "مستحقات ورواتب العاملين", Color(0xFF8B5CF6), Icons.Default.Badge),
      AccountPickerEntry("إيجار المحل", "حساب عام", "مصروف إيجار المقر / المحل", Color(0xFFDC2626), Icons.Default.Category),
      AccountPickerEntry("أرباح وخسائر / تسوية", "حساب عام", "حساب الأرباح والخسائر والتسويات", Color(0xFF64748B), Icons.Default.AccountBalance),
      AccountPickerEntry("رأس المال / جاري المالك", "حساب عام", "حقوق الملكية وجاري الشركاء", Color(0xFF64748B), Icons.Default.AccountBalance)
    )
  }

  val customerEntries = remember(customers) {
    customers.map { cust ->
      AccountPickerEntry(
        name = cust.name,
        type = "عميل",
        subtitle = "عميل • جوال: ${cust.phone.ifEmpty { "غير محدد" }}",
        badgeColor = EmeraldGreen,
        icon = Icons.Default.People
      )
    }
  }

  val supplierEntries = remember(suppliers) {
    suppliers.map { supp ->
      AccountPickerEntry(
        name = supp.name,
        type = "مورد",
        subtitle = "مورد • ${if (supp.companyName.isNotBlank()) "شركة: ${supp.companyName}" else "جوال: ${supp.phone}"}",
        badgeColor = Color(0xFF0284C7),
        icon = Icons.Default.LocalShipping
      )
    }
  }

  val employeeEntries = remember(employees) {
    employees.map { emp ->
      AccountPickerEntry(
        name = emp.name,
        type = "موظف",
        subtitle = "موظف • وظيفة: ${emp.role.ifEmpty { "غير محدد" }} • جوال: ${emp.phone.ifEmpty { "غير محدد" }}",
        badgeColor = Color(0xFF8B5CF6),
        icon = Icons.Default.Badge
      )
    }
  }

  val allAccounts = remember(generalAccountsList, customerEntries, supplierEntries, employeeEntries) {
    generalAccountsList + customerEntries + supplierEntries + employeeEntries
  }

  val filteredList = remember(allAccounts, searchQuery) {
    if (searchQuery.isBlank()) {
      allAccounts
    } else {
      allAccounts.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
          it.subtitle.contains(searchQuery, ignoreCase = true) ||
          it.type.contains(searchQuery, ignoreCase = true)
      }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .fillMaxHeight(0.82f)
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(sideTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("جميع حسابات التطبيق، العملاء، الموردين، والموظفين (${allAccounts.size})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "إغلاق")
          }
        }

        // Search Bar
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("بحث باسم الحساب، العميل، المورد، الموظف...", fontSize = 12.sp) },
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

        // Unified Vertical List: All accounts, customers, suppliers, and employees together under each other
        if (filteredList.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            contentAlignment = Alignment.Center
          ) {
            Text("لا توجد نتائج مطابقة للبحث", color = Color.Gray, fontSize = 13.sp)
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            items(filteredList, key = { "${it.type}_${it.name}" }) { item ->
              AccountChoiceItem(
                entry = item,
                onClick = { onSelect(item.name) }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun AccountChoiceItem(
  entry: AccountPickerEntry,
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
        .padding(horizontal = 12.dp, vertical = 10.dp),
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
            .background(entry.badgeColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(entry.icon, contentDescription = null, tint = entry.badgeColor, modifier = Modifier.size(18.dp))
        }

        Column {
          Text(entry.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          if (entry.subtitle.isNotBlank()) {
            Text(entry.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }

      Surface(
        color = entry.badgeColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
      ) {
        Text(
          text = entry.type,
          color = entry.badgeColor,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
      }
    }
  }
}
