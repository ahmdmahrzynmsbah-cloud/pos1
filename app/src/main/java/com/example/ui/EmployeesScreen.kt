package com.example.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Employee
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

fun getEmployeePeriodDateRange(periodKey: String): Pair<Long?, Long?> {
  return when (periodKey) {
    "today" -> {
      val start = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }.timeInMillis
      val end = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
      }.timeInMillis
      Pair(start, end)
    }
    "this_week" -> {
      val cal = Calendar.getInstance()
      cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
      cal.set(Calendar.HOUR_OF_DAY, 0)
      cal.set(Calendar.MINUTE, 0)
      cal.set(Calendar.SECOND, 0)
      cal.set(Calendar.MILLISECOND, 0)
      val start = cal.timeInMillis
      val end = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
      }.timeInMillis
      Pair(start, end)
    }
    "this_month" -> {
      val start = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }.timeInMillis
      val end = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
      }.timeInMillis
      Pair(start, end)
    }
    else -> Pair(null, null)
  }
}

@Composable
fun EmployeesScreen(viewModel: AppViewModel, onPrintStatement: (Employee, List<com.example.data.Expense>) -> Unit) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val employees by viewModel.allEmployees.collectAsState()
  val expenses by viewModel.allExpenses.collectAsState()
  var showAddDialog by remember { mutableStateOf(false) }
  var selectedEmployeeForStatement by remember { mutableStateOf<Employee?>(null) }
  var printDateRangeEmployee by remember { mutableStateOf<Pair<Employee, Boolean>?>(null) } // Pair(Employee, isPreview)
  val previewFile by viewModel.pdfPreviewFile.collectAsState()
  
  if (previewFile != null) {
    androidx.compose.ui.window.Dialog(
      onDismissRequest = { viewModel.pdfPreviewFile.value = null },
      properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
      Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("معاينة الكشف", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { viewModel.pdfPreviewFile.value = null }) {
              Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Close")
            }
          }
          Divider()
          Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
            PdfViewer(previewFile!!)
          }
        }
      }
    }
  }

  val ledgerEntries by viewModel.allLedgerEntries.collectAsState(initial = emptyList())

  if (selectedEmployeeForStatement != null) {
    EmployeeStatementDialog(
      employee = selectedEmployeeForStatement!!,
      expenses = expenses,
      ledgerEntries = ledgerEntries,
      viewModel = viewModel,
      onDismiss = { selectedEmployeeForStatement = null }
    )
  }

  if (printDateRangeEmployee != null) {
    EmployeePrintDateRangeDialog(
      employee = printDateRangeEmployee!!.first,
      initialIsPreview = printDateRangeEmployee!!.second,
      viewModel = viewModel,
      onDismiss = { printDateRangeEmployee = null }
    )
  }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("إدارة الموظفين", fontSize = 24.sp, fontWeight = FontWeight.Bold)
      Button(onClick = { showAddDialog = true }) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("إضافة موظف")
      }
    }

    if (employees.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("لا يوجد موظفين حالياً", color = Color.Gray)
      }
    } else {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(employees) { employee ->
          EmployeeCard(
            employee = employee,
            onCardClick = { emp -> selectedEmployeeForStatement = emp },
            onPreviewClick = { emp -> 
              printDateRangeEmployee = Pair(emp, true)
            },
            onDownloadClick = { emp -> 
              printDateRangeEmployee = Pair(emp, false)
            }
          )
        }
      }
    }
  }

  if (showAddDialog) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text("إضافة موظف جديد") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("الاسم") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("رقم الهاتف") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = role,
            onValueChange = { role = it },
            label = { Text("المسمى الوظيفي") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = salary,
            onValueChange = { salary = it },
            label = { Text("الراتب الأساسي") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(onClick = {
          if (name.isNotBlank()) {
            viewModel.addEmployee(name, phone, role, salary.toDoubleOrNull() ?: 0.0)
            showAddDialog = false
          }
        }) {
          Text("حفظ")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) {
          Text("إلغاء")
        }
      }
    )
  }
}

@Composable
fun EmployeeCard(
  employee: Employee,
  onCardClick: (Employee) -> Unit = {},
  onPreviewClick: (Employee) -> Unit,
  onDownloadClick: (Employee) -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    onClick = { onCardClick(employee) },
    elevation = CardDefaults.cardElevation(2.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.Default.Person, 
            contentDescription = null, 
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(employee.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
          Text("${employee.role} • الراتب: ${employee.salary} ر.س", color = Color.Gray, fontSize = 12.sp)
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(onClick = { onPreviewClick(employee) }, modifier = Modifier.size(36.dp)) {
          Icon(Icons.Default.Visibility, contentDescription = "معاينة", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = { onDownloadClick(employee) }, modifier = Modifier.size(36.dp)) {
          Icon(Icons.Default.PictureAsPdf, contentDescription = "تنزيل PDF", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        }
      }
    }
  }
}


@Composable
fun PdfViewer(file: java.io.File) {
  var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
  LaunchedEffect(file) {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
      try {
        val fileDescriptor = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
        val pdfRenderer = android.graphics.pdf.PdfRenderer(fileDescriptor)
        if (pdfRenderer.pageCount > 0) {
          val page = pdfRenderer.openPage(0)
          val newBitmap = android.graphics.Bitmap.createBitmap(page.width * 2, page.height * 2, android.graphics.Bitmap.Config.ARGB_8888)
          newBitmap.eraseColor(android.graphics.Color.WHITE)
          page.render(newBitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
          page.close()
          bitmap = newBitmap
        }
        pdfRenderer.close()
        fileDescriptor.close()
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }
  bitmap?.let { b ->
    
    Image(
      bitmap = b.asImageBitmap(), 
      contentDescription = "PDF Preview", 
      modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
    )
  } ?: run {
    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
    }
  }
}

@Composable
fun EmployeePrintDateRangeDialog(
  employee: Employee,
  initialIsPreview: Boolean,
  viewModel: AppViewModel,
  onDismiss: () -> Unit
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  var selectedPeriod by remember { mutableStateOf("all") }
  var customStartDate by remember { mutableStateOf<Long?>(null) }
  var customEndDate by remember { mutableStateOf<Long?>(null) }

  val sdfDisplay = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

  val openStartDatePicker = {
    val cal = Calendar.getInstance()
    if (customStartDate != null) cal.timeInMillis = customStartDate!!
    DatePickerDialog(
      context,
      { _, y, m, d ->
        val newCal = Calendar.getInstance().apply {
          set(y, m, d, 0, 0, 0)
          set(Calendar.MILLISECOND, 0)
        }
        customStartDate = newCal.timeInMillis
        selectedPeriod = "custom"
      },
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH),
      cal.get(Calendar.DAY_OF_MONTH)
    ).show()
  }

  val openEndDatePicker = {
    val cal = Calendar.getInstance()
    if (customEndDate != null) cal.timeInMillis = customEndDate!!
    DatePickerDialog(
      context,
      { _, y, m, d ->
        val newCal = Calendar.getInstance().apply {
          set(y, m, d, 23, 59, 59)
          set(Calendar.MILLISECOND, 999)
        }
        customEndDate = newCal.timeInMillis
        selectedPeriod = "custom"
      },
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH),
      cal.get(Calendar.DAY_OF_MONTH)
    ).show()
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
          Text("طباعة كشف حساب موظف", fontWeight = FontWeight.Bold, fontSize = 17.sp)
          Text(employee.name, fontSize = 13.sp, color = Color.Gray)
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          "حدد الفترة الزمنية التي تريد استخراج وطباعة الكشف خلالها (من - إلى):",
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Quick Period Chips
        val periods = listOf(
          "all" to "كافة الحركات",
          "this_month" to "هذا الشهر",
          "this_week" to "هذا الأسبوع",
          "today" to "اليوم",
          "custom" to "مخصص (من - إلى)"
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(periods) { (key, label) ->
            val isSelected = selectedPeriod == key
            FilterChip(
              selected = isSelected,
              onClick = {
                selectedPeriod = key
                if (key != "custom") {
                  val range = getEmployeePeriodDateRange(key)
                  customStartDate = range.first
                  customEndDate = range.second
                } else {
                  if (customStartDate == null) {
                    val range = getEmployeePeriodDateRange("this_month")
                    customStartDate = range.first
                    customEndDate = range.second
                  }
                }
              },
              label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
            )
          }
        }

        // Custom Date Pickers
        Surface(
          shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("تحديد التواريخ (من - إلى):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
              if (customStartDate != null || customEndDate != null) {
                TextButton(
                  onClick = {
                    selectedPeriod = "all"
                    customStartDate = null
                    customEndDate = null
                  },
                  contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                  Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text("عرض الكل", fontSize = 11.sp)
                }
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = { openStartDatePicker() },
                modifier = Modifier.weight(1f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (customStartDate != null) "من: ${sdfDisplay.format(Date(customStartDate!!))}" else "من تاريخ: الكل",
                  fontSize = 11.sp
                )
              }

              OutlinedButton(
                onClick = { openEndDatePicker() },
                modifier = Modifier.weight(1f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (customEndDate != null) "إلى: ${sdfDisplay.format(Date(customEndDate!!))}" else "إلى تاريخ: الكل",
                  fontSize = 11.sp
                )
              }
            }
          }
        }

        // Summary of selected period
        val periodSummaryText = when {
          customStartDate != null && customEndDate != null -> "سيتم استخراج الحركات من ${sdfDisplay.format(Date(customStartDate!!))} إلى ${sdfDisplay.format(Date(customEndDate!!))}"
          customStartDate != null -> "سيتم استخراج الحركات من ${sdfDisplay.format(Date(customStartDate!!))} حتى الآن"
          customEndDate != null -> "سيتم استخراج الحركات حتى تاريخ ${sdfDisplay.format(Date(customEndDate!!))}"
          else -> "سيتم استخراج كافة حركات وسندات الموظف المسجلة"
        }
        Text(
          text = periodSummaryText,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Medium
        )
      }
    },
    confirmButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = {
            onDismiss()
            viewModel.exportEmployeeStatementToPdf(
              context = context,
              employee = employee,
              isPreview = true,
              startDate = customStartDate,
              endDate = customEndDate
            )
          }
        ) {
          Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("معاينة", fontSize = 12.sp)
        }

        Button(
          onClick = {
            onDismiss()
            viewModel.exportEmployeeStatementToPdf(
              context = context,
              employee = employee,
              isPreview = false,
              startDate = customStartDate,
              endDate = customEndDate
            )
          }
        ) {
          Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("تنزيل PDF", fontSize = 12.sp)
        }
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
fun EmployeeStatementDialog(
  employee: Employee,
  expenses: List<com.example.data.Expense>,
  ledgerEntries: List<com.example.data.LedgerEntry>,
  viewModel: AppViewModel,
  onDismiss: () -> Unit
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  var filterType by remember { mutableStateOf("all") } // "all", "expense", "journal"

  // Date filtering state
  var selectedPeriod by remember { mutableStateOf("all") }
  var customStartDate by remember { mutableStateOf<Long?>(null) }
  var customEndDate by remember { mutableStateOf<Long?>(null) }
  val sdfDisplay = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
  val currencySymbol by viewModel.currencySymbol.collectAsState("ر.س")

  val openStartDatePicker = {
    val cal = Calendar.getInstance()
    if (customStartDate != null) cal.timeInMillis = customStartDate!!
    DatePickerDialog(
      context,
      { _, y, m, d ->
        val newCal = Calendar.getInstance().apply {
          set(y, m, d, 0, 0, 0)
          set(Calendar.MILLISECOND, 0)
        }
        customStartDate = newCal.timeInMillis
        selectedPeriod = "custom"
      },
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH),
      cal.get(Calendar.DAY_OF_MONTH)
    ).show()
  }

  val openEndDatePicker = {
    val cal = Calendar.getInstance()
    if (customEndDate != null) cal.timeInMillis = customEndDate!!
    DatePickerDialog(
      context,
      { _, y, m, d ->
        val newCal = Calendar.getInstance().apply {
          set(y, m, d, 23, 59, 59)
          set(Calendar.MILLISECOND, 999)
        }
        customEndDate = newCal.timeInMillis
        selectedPeriod = "custom"
      },
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH),
      cal.get(Calendar.DAY_OF_MONTH)
    ).show()
  }

  val emp = employee
  val empName = emp.name.trim()

  // 1. Employee Expenses (سندات صرف)
  val empExpenses = expenses.filter { 
    it.employeeId == emp.id || 
    (it.partyType == "موظف" && it.partyName?.trim()?.equals(empName, ignoreCase = true) == true) ||
    it.type.contains(empName) ||
    it.notes.contains(empName)
  }

  // 2. Employee Ledger Entries (سندات قيد / قيود محاسبية)
  val empLedgers = ledgerEntries.filter { entry ->
    entry.employeeId == emp.id ||
    entry.debitAccount.trim().equals(empName, ignoreCase = true) ||
    entry.creditAccount.trim().equals(empName, ignoreCase = true) ||
    entry.debitAccount.contains(empName) ||
    entry.creditAccount.contains(empName) ||
    (entry.employeeId == null && entry.description.contains(empName))
  }

  // Unified statement item data class
  data class UnifiedEmpStatement(
    val id: String,
    val date: Long,
    val voucherType: String, // "سند صرف", "سند قيد", "سند قبض"
    val description: String,
    val debit: Double,
    val credit: Double,
    val isDebit: Boolean
  )

  val statementItems = remember(emp, empExpenses, empLedgers) {
    val list = mutableListOf<UnifiedEmpStatement>()

    empExpenses.forEach { exp ->
      val vType = if (exp.type.contains("قيد")) "سند قيد" else "سند صرف"
      list.add(
        UnifiedEmpStatement(
          id = "exp_${exp.id}",
          date = exp.date,
          voucherType = vType,
          description = exp.notes.ifBlank { exp.type.ifEmpty { "سند صرف نقدي للموظف" } },
          debit = exp.amount,
          credit = 0.0,
          isDebit = true
        )
      )
    }

    empLedgers.forEach { entry ->
      val isDuplicate = empExpenses.any { exp ->
        kotlin.math.abs(exp.amount - entry.amount) < 0.01 && kotlin.math.abs(exp.date - entry.date) < 15000
      }
      if (!isDuplicate) {
        val vType = when {
          entry.description.contains("سند صرف") -> "سند صرف"
          entry.description.contains("سند قبض") -> "سند قبض"
          else -> "سند قيد"
        }
        val isDebit = entry.debitAccount.contains(empName) ||
                      entry.debitAccount.trim().equals(empName, ignoreCase = true) ||
                      (entry.employeeId == emp.id && !entry.creditAccount.contains(empName))
        list.add(
          UnifiedEmpStatement(
            id = "ledg_${entry.id}",
            date = entry.date,
            voucherType = vType,
            description = entry.description.ifEmpty { "$vType للموظف" },
            debit = if (isDebit) entry.amount else 0.0,
            credit = if (!isDebit) entry.amount else 0.0,
            isDebit = isDebit
          )
        )
      }
    }
    list.sortedByDescending { it.date }
  }

  // Calculate prior balance if startDate is selected
  val priorItems = remember(statementItems, customStartDate) {
    if (customStartDate != null) {
      statementItems.filter { it.date < customStartDate!! }
    } else {
      emptyList()
    }
  }
  val priorDebit = remember(priorItems) { priorItems.sumOf { it.debit } }
  val priorCredit = remember(priorItems) { priorItems.sumOf { it.credit } }
  val priorBalance = remember(priorDebit, priorCredit) { priorCredit - priorDebit }
  val hasPrior = customStartDate != null && (priorDebit > 0 || priorCredit > 0)

  // Period filtered items
  val periodItems = remember(statementItems, customStartDate, customEndDate) {
    statementItems.filter { item ->
      val afterStart = customStartDate == null || item.date >= customStartDate!!
      val beforeEnd = customEndDate == null || item.date <= customEndDate!!
      afterStart && beforeEnd
    }
  }

  val filteredItems = remember(periodItems, filterType) {
    when (filterType) {
      "expense" -> periodItems.filter { it.voucherType == "سند صرف" }
      "journal" -> periodItems.filter { it.voucherType == "سند قيد" }
      else -> periodItems
    }
  }

  val totalPeriodDebit = remember(periodItems) { periodItems.sumOf { it.debit } }
  val totalPeriodCredit = remember(periodItems) { periodItems.sumOf { it.credit } }
  val grandNetSalary = remember(emp.salary, priorDebit, priorCredit, totalPeriodDebit, totalPeriodCredit) {
    emp.salary + (priorCredit + totalPeriodCredit) - (priorDebit + totalPeriodDebit)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("كشف حساب الموظف: ${emp.name}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
          Text("${emp.role} • جوال: ${emp.phone.ifEmpty { "غير محدد" }}", fontSize = 12.sp, color = Color.Gray)
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "إغلاق")
        }
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f)) {
        // Salary and Balance info card
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("الراتب الأساسي:", fontSize = 12.sp, color = Color.Gray)
              Text("${emp.salary} $currencySymbol", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            if (hasPrior) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("رصيد مرحل قبل الفترة:", fontSize = 12.sp, color = Color.Gray)
                Text(
                  "${if (priorBalance >= 0) "+ $priorBalance" else "- ${-priorBalance}"} $currencySymbol",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (priorBalance >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                )
              }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("سندات الصرف (خلال الفترة):", fontSize = 12.sp, color = Color.Gray)
              Text("- $totalPeriodDebit $currencySymbol", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("سندات القيد (خلال الفترة):", fontSize = 12.sp, color = Color.Gray)
              Text("+ $totalPeriodCredit $currencySymbol", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
            }
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("صافي المستحق للموظف:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
              Text(
                "$grandNetSalary $currencySymbol",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (grandNetSalary >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Date Range Selector Card
        Card(
          shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
          Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                Text("تحديد الفترة الزمنية للكشف:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
              if (customStartDate != null || customEndDate != null) {
                TextButton(
                  onClick = {
                    selectedPeriod = "all"
                    customStartDate = null
                    customEndDate = null
                  },
                  contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                  Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                  Spacer(modifier = Modifier.width(2.dp))
                  Text("عرض الكل", fontSize = 10.sp)
                }
              }
            }

            // Period Chips
            val periods = listOf(
              "all" to "الكل",
              "this_month" to "هذا الشهر",
              "this_week" to "هذا الأسبوع",
              "today" to "اليوم",
              "custom" to "مخصص"
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
              items(periods) { (key, label) ->
                val isSelected = selectedPeriod == key
                FilterChip(
                  selected = isSelected,
                  onClick = {
                    selectedPeriod = key
                    if (key != "custom") {
                      val range = getEmployeePeriodDateRange(key)
                      customStartDate = range.first
                      customEndDate = range.second
                    } else {
                      if (customStartDate == null) {
                        val range = getEmployeePeriodDateRange("this_month")
                        customStartDate = range.first
                        customEndDate = range.second
                      }
                    }
                  },
                  label = { Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                  )
                )
              }
            }

            // Date Pickers row (From / To)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              OutlinedButton(
                onClick = { openStartDatePicker() },
                modifier = Modifier.weight(1f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
              ) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = if (customStartDate != null) "من: ${sdfDisplay.format(Date(customStartDate!!))}" else "من: البداية",
                  fontSize = 10.sp
                )
              }

              OutlinedButton(
                onClick = { openEndDatePicker() },
                modifier = Modifier.weight(1f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
              ) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = if (customEndDate != null) "إلى: ${sdfDisplay.format(Date(customEndDate!!))}" else "إلى: اليوم",
                  fontSize = 10.sp
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Voucher Filter chips
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          FilterChip(
            selected = filterType == "all",
            onClick = { filterType = "all" },
            label = { Text("الكل (${periodItems.size})", fontSize = 11.sp) }
          )
          FilterChip(
            selected = filterType == "journal",
            onClick = { filterType = "journal" },
            label = { Text("سندات القيد (${periodItems.count { it.voucherType == "سند قيد" }})", fontSize = 11.sp) }
          )
          FilterChip(
            selected = filterType == "expense",
            onClick = { filterType = "expense" },
            label = { Text("سندات الصرف (${periodItems.count { it.voucherType == "سند صرف" }})", fontSize = 11.sp) }
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Transactions list
        if (filteredItems.isEmpty()) {
          Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
          ) {
            Text("لا توجد حركات مسجلة لهذا الموظف خلال الفترة المحددة.", color = Color.Gray, fontSize = 13.sp)
          }
        } else {
          LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(filteredItems) { item ->
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                  containerColor = if (item.voucherType == "سند قيد") Color(0xFFEFF6FF) else Color(0xFFFFFBEB)
                ),
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (item.voucherType == "سند قيد") Color(0xFFBFDBFE) else Color(0xFFFDE68A)
                )
              ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    // Voucher type badge
                    Surface(
                      shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                      color = if (item.voucherType == "سند قيد") Color(0xFF2563EB) else Color(0xFFD97706)
                    ) {
                      Text(
                        text = item.voucherType,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                      )
                    }

                    // Amount
                    if (item.isDebit) {
                      Text(
                        text = "- ${item.debit} $currencySymbol",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFFDC2626)
                      )
                    } else {
                      Text(
                        text = "+ ${item.credit} $currencySymbol",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF16A34A)
                      )
                    }
                  }

                  // Description / Notes
                  Text(
                    text = item.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                  )

                  // Date & Time
                  val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                  Text(
                    text = sdf.format(Date(item.date)),
                    fontSize = 11.sp,
                    color = Color.Gray
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = {
            viewModel.exportEmployeeStatementToPdf(
              context = context,
              employee = emp,
              isPreview = true,
              startDate = customStartDate,
              endDate = customEndDate
            )
          }
        ) {
          Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("معاينة", fontSize = 12.sp)
        }

        Button(
          onClick = {
            viewModel.exportEmployeeStatementToPdf(
              context = context,
              employee = emp,
              isPreview = false,
              startDate = customStartDate,
              endDate = customEndDate
            )
          }
        ) {
          Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("تنزيل PDF", fontSize = 12.sp)
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("إغلاق")
      }
    }
  )
}
