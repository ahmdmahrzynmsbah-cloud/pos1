package com.example.ui

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Employee
import com.example.data.Expense
import java.text.SimpleDateFormat
import java.util.*

private fun formatExpenseAmount(amount: Double): String {
  return String.format(Locale.US, "%.2f", amount)
}

private fun getCategoryIcon(type: String): ImageVector {
  return when (type) {
    "رواتب وأجور" -> Icons.Default.People
    "إيجار" -> Icons.Default.Store
    "كهرباء" -> Icons.Default.Bolt
    "مياه" -> Icons.Default.WaterDrop
    "تسويق" -> Icons.Default.Campaign
    "صيانة" -> Icons.Default.Build
    "بضاعة ومشتريات" -> Icons.Default.ShoppingBag
    "نثريات" -> Icons.Default.Receipt
    else -> Icons.Default.Payments
  }
}

private fun getCategoryColor(type: String): Color {
  return when (type) {
    "رواتب وأجور" -> Color(0xFF1976D2)
    "إيجار" -> Color(0xFFE65100)
    "كهرباء" -> Color(0xFFF57F17)
    "مياه" -> Color(0xFF0097A7)
    "تسويق" -> Color(0xFF7B1FA2)
    "صيانة" -> Color(0xFF455A64)
    "بضاعة ومشتريات" -> Color(0xFF2E7D32)
    "نثريات" -> Color(0xFFC2185B)
    else -> Color(0xFF5E35B1)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseBalancesReportScreen(
  viewModel: AppViewModel,
  onBackClick: (() -> Unit)? = null
) {
  val context = LocalContext.current
  val expenses by viewModel.allExpenses.collectAsState()
  val employees by viewModel.allEmployees.collectAsState()
  val currencySymbol by viewModel.currencySymbol.collectAsState()

  // State Filters
  var selectedPeriod by remember { mutableStateOf("THIS_MONTH") } // ALL, TODAY, THIS_WEEK, THIS_MONTH, THIS_YEAR, CUSTOM
  var selectedCategory by remember { mutableStateOf("الكل") }
  var selectedEmployeeId by remember { mutableStateOf<Int?>(null) }
  var searchQuery by remember { mutableStateOf("") }
  var sortBy by remember { mutableStateOf("DATE_DESC") } // DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC

  // Custom date range state
  val calendar = remember { Calendar.getInstance() }
  var customStartDate by remember {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    mutableStateOf(cal.timeInMillis)
  }
  var customEndDate by remember { mutableStateOf(System.currentTimeMillis()) }
  var showCustomDatePickers by remember { mutableStateOf(false) }

  // Dialogs
  var showAddExpenseDialog by remember { mutableStateOf(false) }
  var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
  var showCategoryBreakdownDialog by remember { mutableStateOf(false) }

  val sdfDisplay = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
  val sdfDateTime = remember { SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()) }

  // Calculate Date Boundaries
  val (filterStartMs, filterEndMs, periodTitle) = remember(selectedPeriod, customStartDate, customEndDate) {
    val now = Calendar.getInstance()
    when (selectedPeriod) {
      "TODAY" -> {
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
        Triple(start, end, "اليوم (${sdfDisplay.format(Date(start))})")
      }
      "THIS_WEEK" -> {
        val start = Calendar.getInstance().apply {
          set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = System.currentTimeMillis()
        Triple(start, end, "هذا الأسبوع")
      }
      "THIS_MONTH" -> {
        val start = Calendar.getInstance().apply {
          set(Calendar.DAY_OF_MONTH, 1)
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = System.currentTimeMillis()
        val monthName = SimpleDateFormat("MMMM yyyy", Locale("ar")).format(Date())
        Triple(start, end, "شهر $monthName")
      }
      "THIS_YEAR" -> {
        val start = Calendar.getInstance().apply {
          set(Calendar.DAY_OF_YEAR, 1)
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = System.currentTimeMillis()
        val yearName = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        Triple(start, end, "عام $yearName")
      }
      "CUSTOM" -> {
        val start = Calendar.getInstance().apply {
          timeInMillis = customStartDate
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
        }.timeInMillis
        val end = Calendar.getInstance().apply {
          timeInMillis = customEndDate
          set(Calendar.HOUR_OF_DAY, 23)
          set(Calendar.MINUTE, 59)
          set(Calendar.SECOND, 59)
        }.timeInMillis
        Triple(start, end, "من ${sdfDisplay.format(Date(start))} إلى ${sdfDisplay.format(Date(end))}")
      }
      else -> {
        Triple(0L, Long.MAX_VALUE, "كافة الفترات (شامل)")
      }
    }
  }

  // Filter & Sort Logic
  val filteredExpenses = remember(
    expenses, filterStartMs, filterEndMs, selectedCategory,
    selectedEmployeeId, searchQuery, sortBy
  ) {
    var list = expenses.filter { exp ->
      exp.date in filterStartMs..filterEndMs
    }

    if (selectedCategory != "الكل") {
      list = list.filter { it.type == selectedCategory }
    }

    if (selectedEmployeeId != null) {
      list = list.filter { it.employeeId == selectedEmployeeId }
    }

    if (searchQuery.isNotBlank()) {
      val query = searchQuery.trim()
      list = list.filter { exp ->
        val empName = employees.find { it.id == exp.employeeId }?.name ?: ""
        exp.notes.contains(query, ignoreCase = true) ||
          exp.type.contains(query, ignoreCase = true) ||
          empName.contains(query, ignoreCase = true) ||
          exp.amount.toString().contains(query)
      }
    }

    when (sortBy) {
      "DATE_ASC" -> list.sortedBy { it.date }
      "AMOUNT_DESC" -> list.sortedByDescending { it.amount }
      "AMOUNT_ASC" -> list.sortedBy { it.amount }
      else -> list.sortedByDescending { it.date }
    }
  }

  // Summary Metrics
  val totalExpensesAmount = remember(filteredExpenses) { filteredExpenses.sumOf { it.amount } }
  val totalExpensesCount = filteredExpenses.size
  val avgExpense = if (totalExpensesCount > 0) totalExpensesAmount / totalExpensesCount else 0.0

  // Category Breakdown Data
  val categoryBreakdown = remember(filteredExpenses, totalExpensesAmount) {
    filteredExpenses.groupBy { it.type }.map { (cat, items) ->
      val sum = items.sumOf { it.amount }
      val pct = if (totalExpensesAmount > 0) (sum / totalExpensesAmount) * 100 else 0.0
      Triple(cat, sum, Pair(items.size, pct))
    }.sortedByDescending { it.second }
  }

  val topCategory = categoryBreakdown.firstOrNull()?.first ?: "لا يوجد"

  // Date Pickers
  val openStartDatePicker = {
    val cal = Calendar.getInstance().apply { timeInMillis = customStartDate }
    DatePickerDialog(
      context,
      { _, y, m, d ->
        val newCal = Calendar.getInstance().apply {
          set(y, m, d, 0, 0, 0)
        }
        customStartDate = newCal.timeInMillis
      },
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH),
      cal.get(Calendar.DAY_OF_MONTH)
    ).show()
  }

  val openEndDatePicker = {
    val cal = Calendar.getInstance().apply { timeInMillis = customEndDate }
    DatePickerDialog(
      context,
      { _, y, m, d ->
        val newCal = Calendar.getInstance().apply {
          set(y, m, d, 23, 59, 59)
        }
        customEndDate = newCal.timeInMillis
      },
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH),
      cal.get(Calendar.DAY_OF_MONTH)
    ).show()
  }

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Payments,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
            )
            Text(
              text = "كشف أرصدة المصاريف",
              fontWeight = FontWeight.Bold,
              fontSize = 17.sp
            )
          }
        },
        navigationIcon = {
          if (onBackClick != null) {
            IconButton(onClick = onBackClick) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
            }
          }
        },
        actions = {
          // WhatsApp Share
          IconButton(
            onClick = {
              viewModel.shareExpenseBalancesReportWhatsApp(
                context = context,
                dateRangeText = periodTitle,
                expenses = filteredExpenses,
                employees = employees
              )
            }
          ) {
            Icon(
              Icons.Default.Share,
              contentDescription = "مشاركة واتساب",
              tint = Color(0xFF25D366)
            )
          }

          // PDF Print
          IconButton(
            onClick = {
              viewModel.exportExpenseBalancesReportToPdf(
                context = context,
                reportTitle = "كشف أرصدة وحركات المصاريف ($periodTitle)",
                dateRangeText = periodTitle,
                expenses = filteredExpenses,
                employees = employees
              )
            }
          ) {
            Icon(
              Icons.Default.Print,
              contentDescription = "طباعة PDF",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showAddExpenseDialog = true },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White
      ) {
        Icon(Icons.Default.Add, contentDescription = "سند صرف جديد")
      }
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(MaterialTheme.colorScheme.background),
      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

      // Section 1: Period Selector Chips
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(2.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text("تحديد الفترة الزمنية للتاريخ:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }
              Text(periodTitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              val periods = listOf(
                "THIS_MONTH" to "هذا الشهر",
                "TODAY" to "اليوم",
                "THIS_WEEK" to "هذا الأسبوع",
                "THIS_YEAR" to "هذا العام",
                "CUSTOM" to "مخصص ",
                "ALL" to "الكل"
              )
              items(periods) { (key, label) ->
                val isSelected = selectedPeriod == key
                FilterChip(
                  selected = isSelected,
                  onClick = {
                    selectedPeriod = key
                    if (key == "CUSTOM") showCustomDatePickers = true
                  },
                  label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                  )
                )
              }
            }

            if (selectedPeriod == "CUSTOM") {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = { openStartDatePicker() },
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                  Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("من: ${sdfDisplay.format(Date(customStartDate))}", fontSize = 11.sp)
                }

                OutlinedButton(
                  onClick = { openEndDatePicker() },
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                  Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("إلى: ${sdfDisplay.format(Date(customEndDate))}", fontSize = 11.sp)
                }
              }
            }
          }
        }
      }

      // Section 2: Summary KPI Cards
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Total Card
          Card(
            modifier = Modifier.weight(1.2f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text("إجمالي المصاريف", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "${formatExpenseAmount(totalExpensesAmount)} $currencySymbol",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.error
              )
              Text("الفترة: $periodTitle", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
          }

          // Count Card
          Card(
            modifier = Modifier.weight(0.9f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text("عدد السندات", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "$totalExpensesCount",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
              )
              Text("متوسط: ${formatExpenseAmount(avgExpense)}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
          }

          // Top Category Card
          Card(
            modifier = Modifier.weight(1.1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text("أعلى بند صرف", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = topCategory,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              val topAmount = categoryBreakdown.firstOrNull()?.second ?: 0.0
              Text("${formatExpenseAmount(topAmount)} $currencySymbol", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // Section 3: Category Breakdown Card (Progress bars)
      if (categoryBreakdown.isNotEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Default.PieChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                  Text("أرصدة وتوزيع المصاريف حسب البنود", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Text("${categoryBreakdown.size} بنود", fontSize = 11.sp, color = Color.Gray)
              }

              categoryBreakdown.forEach { (categoryName, sum, details) ->
                val count = details.first
                val pct = details.second
                val catColor = getCategoryColor(categoryName)
                val isFilterActive = selectedCategory == categoryName

                Surface(
                  onClick = {
                    selectedCategory = if (selectedCategory == categoryName) "الكل" else categoryName
                  },
                  shape = RoundedCornerShape(10.dp),
                  color = if (isFilterActive) catColor.copy(alpha = 0.1f) else Color.Transparent,
                  border = if (isFilterActive) BorderStroke(1.dp, catColor) else null
                ) {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
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
                        Box(
                          modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(catColor)
                        )
                        Text(categoryName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("($count سند)", fontSize = 10.sp, color = Color.Gray)
                      }
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                      ) {
                        Text(
                          "${formatExpenseAmount(sum)} $currencySymbol",
                          fontWeight = FontWeight.Bold,
                          fontSize = 12.sp,
                          color = MaterialTheme.colorScheme.error
                        )
                        Text(
                          "(${String.format(Locale.US, "%.1f", pct)}%)",
                          fontSize = 10.sp,
                          color = Color.Gray
                        )
                      }
                    }

                    // Progress bar
                    LinearProgressIndicator(
                      progress = { (pct / 100f).toFloat().coerceIn(0f, 1f) },
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                      color = catColor,
                      trackColor = catColor.copy(alpha = 0.15f)
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Section 4: Filter & Search Bar
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(1.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Search field
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = { Text("بحث بالبيان، الموظف، البند، أو المبلغ...", fontSize = 12.sp) },
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
              trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                  IconButton(onClick = { searchQuery = "" }) {
                    Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                  }
                }
              },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp),
              singleLine = true
            )

            // Category Filter Row
            val allCategories = remember(expenses) {
              listOf("الكل") + expenses.map { it.type }.distinct()
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(allCategories) { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                  onClick = { selectedCategory = cat },
                  shape = RoundedCornerShape(8.dp),
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                ) {
                  Text(
                    text = cat,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                  )
                }
              }
            }
          }
        }
      }

      // Section 5: List Header & Action Buttons
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "سجل حركات وسندات المصاريف (${filteredExpenses.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }

      // Section 6: Expense Items
      if (filteredExpenses.isEmpty()) {
        item {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                Icons.Default.ReceiptLong,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(48.dp)
              )
              Text("لا توجد حركات أو مصاريف مسجلة في هذه الفترة", color = Color.Gray, fontSize = 13.sp)
              Button(
                onClick = { showAddExpenseDialog = true },
                modifier = Modifier.padding(top = 8.dp),
                shape = RoundedCornerShape(10.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("تسجيل سند صرف جديد")
              }
            }
          }
        }
      } else {
        items(filteredExpenses) { expense ->
          val empName = employees.find { it.id == expense.employeeId }?.name
          val catColor = getCategoryColor(expense.type)
          val catIcon = getCategoryIcon(expense.type)
          val formattedDate = sdfDateTime.format(Date(expense.date))

          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // Top Row: Category + Date + Amount
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
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = catColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, catColor.copy(alpha = 0.3f))
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      Icon(catIcon, contentDescription = null, tint = catColor, modifier = Modifier.size(14.dp))
                      Text(expense.type, color = catColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }

                  // Formatted Date
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    Text(formattedDate, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }

                // Amount
                Text(
                  text = "${formatExpenseAmount(expense.amount)} $currencySymbol",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Black,
                  color = MaterialTheme.colorScheme.error
                )
              }

              // Middle: Employee & Notes
              if (!empName.isNullOrBlank() || expense.notes.isNotBlank()) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!empName.isNullOrBlank()) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                      ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Text("المستلم / الموظف: $empName", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                      }
                    }
                    if (expense.notes.isNotBlank()) {
                      Text(
                        text = "البيان: ${expense.notes}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                      )
                    }
                  }
                }
              }

              // Bottom Actions: Single item share & Delete
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Share single voucher to WhatsApp
                TextButton(
                  onClick = {
                    val singleMsg = buildString {
                      appendLine("*سند صرف مصاريف*")
                      appendLine("*التاريخ:* $formattedDate")
                      appendLine("*البند:* ${expense.type}")
                      appendLine("*المبلغ:* ${formatExpenseAmount(expense.amount)} $currencySymbol")
                      if (!empName.isNullOrBlank()) appendLine("*المستلم:* $empName")
                      if (expense.notes.isNotBlank()) appendLine("*البيان:* ${expense.notes}")
                    }
                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                      type = "text/plain"
                      putExtra(android.content.Intent.EXTRA_TEXT, singleMsg)
                    }
                    context.startActivity(android.content.Intent.createChooser(sendIntent, "مشاركة سند الصرف"))
                  },
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                  Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF25D366))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("مشاركة واتساب", fontSize = 11.sp, color = Color(0xFF25D366))
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Delete button
                IconButton(
                  onClick = { expenseToDelete = expense },
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(
                    Icons.Default.Delete,
                    contentDescription = "حذف",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }
        }
      }

      // Bottom Spacer for FAB
      item {
        Spacer(modifier = Modifier.height(70.dp))
      }
    }
  }

  // Delete Confirmation Dialog
  if (expenseToDelete != null) {
    AlertDialog(
      onDismissRequest = { expenseToDelete = null },
      title = { Text("تأكيد حذف سند الصرف", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
      text = {
        val exp = expenseToDelete!!
        Text("هل أنت متأكد من حذف سند صرف ${exp.type} بمبلغ ${formatExpenseAmount(exp.amount)} $currencySymbol؟")
      },
      confirmButton = {
        Button(
          onClick = {
            expenseToDelete?.let { viewModel.deleteExpense(it) }
            expenseToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("نعم، احذف")
        }
      },
      dismissButton = {
        TextButton(onClick = { expenseToDelete = null }) {
          Text("إلغاء")
        }
      }
    )
  }

  // Add Expense Dialog
  if (showAddExpenseDialog) {
    val suppliers by viewModel.allSuppliers.collectAsState(initial = emptyList())
    val customers by viewModel.allCustomers.collectAsState(initial = emptyList())
    val invoices by viewModel.allInvoices.collectAsState(initial = emptyList())

    AddExpenseVoucherDialog(
      viewModel = viewModel,
      customers = customers,
      suppliers = suppliers,
      employees = employees,
      invoices = invoices,
      currencySymbol = currencySymbol,
      onDismiss = { showAddExpenseDialog = false }
    )
  }
}
