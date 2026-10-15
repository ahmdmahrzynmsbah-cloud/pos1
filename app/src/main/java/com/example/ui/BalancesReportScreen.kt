package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Customer
import com.example.data.Supplier
import java.util.Locale

private val CoralError = Color(0xFFEF4444)
private val EmeraldGreen = Color(0xFF10B981)
private val PrimaryBlue = Color(0xFF2563EB)
private val CardBgLight = Color(0xFFF8FAFC)

private fun formatMoneyLocal(amount: Double): String {
  return String.format(Locale.US, "%.2f", amount)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalancesReportScreen(
  viewModel: AppViewModel,
  onBackClick: (() -> Unit)? = null
) {
  val context = LocalContext.current

  // Navigation sub-tab: "customers" or "suppliers"
  var activeSubTab by remember { mutableStateOf("customers") }
  var searchQuery by remember { mutableStateOf("") }
  
  // Filter chip state: "ALL", "NON_ZERO", "DEBIT", "CREDIT", "ZERO"
  var filterType by remember { mutableStateOf("ALL") }
  // Sort option: "NAME", "BALANCE_DESC", "BALANCE_ASC"
  var sortBy by remember { mutableStateOf("BALANCE_DESC") }

  // Dialog state for viewing full detailed statement
  var selectedCustomerForStatement by remember { mutableStateOf<Customer?>(null) }
  var selectedSupplierForStatement by remember { mutableStateOf<Supplier?>(null) }

  // Data from ViewModel
  val customers by viewModel.allCustomers.collectAsState(initial = emptyList())
  val suppliers by viewModel.allSuppliers.collectAsState(initial = emptyList())
  val invoices by viewModel.allInvoices.collectAsState(initial = emptyList())
  val customerTransactions by viewModel.allCustomerTransactions.collectAsState(initial = emptyList())
  val supplierTransactions by viewModel.allSupplierTransactions.collectAsState(initial = emptyList())
  val expenses by viewModel.allExpenses.collectAsState()
  val currencySymbol by viewModel.currencySymbol.collectAsState()

  if (activeSubTab == "expenses") {
    ExpenseBalancesReportScreen(
      viewModel = viewModel,
      onBackClick = { activeSubTab = "customers" }
    )
    return
  }

  // Calculate balances for Customers
  val customerReportItems = remember(customers, invoices, customerTransactions) {
    customers.map { cust ->
      val custInvoices = invoices.filter { it.customerId == cust.id && !it.isPurchase && !it.isDraft }
      val custTxs = customerTransactions.filter { it.customerId == cust.id }
      
      val totalDebit = custInvoices.sumOf { it.total } + custTxs.filter { it.transactionType in listOf("بيع", "مدين") }.sumOf { it.amount }
      val totalCredit = custInvoices.sumOf { it.paidAmount } + custTxs.filter { it.transactionType in listOf("سداد", "دفعة", "تحصيل", "إيداع", "دائن") }.sumOf { it.amount }
      val balance = totalDebit - totalCredit // positive = customer owes us (مدين), negative = customer overpaid (دائن)

      val statusText = when {
        balance > 0.001 -> "مدين (عليه)"
        balance < -0.001 -> "دائن (له)"
        else -> "متزن"
      }

      BalancesReportRowData(
        id = cust.id,
        name = cust.name,
        phone = cust.phone,
        companyOrType = cust.type,
        balance = balance,
        statusText = statusText
      )
    }
  }

  // Calculate balances for Suppliers
  val supplierReportItems = remember(suppliers, supplierTransactions) {
    suppliers.map { sup ->
      val supTxs = supplierTransactions.filter { it.supplierId == sup.id }
      val totalPurchases = supTxs.filter { it.transactionType in listOf("شراء", "توريد", "دائن") }.sumOf { it.amount }
      val totalPayments = supTxs.filter { it.transactionType in listOf("دفعة", "سداد", "مدين") }.sumOf { it.amount }
      val balance = totalPurchases - totalPayments // positive = we owe supplier (دائن), negative = supplier owes us (مدين)

      val statusText = when {
        balance > 0.001 -> "دائن (له علينا)"
        balance < -0.001 -> "مدين (لنا عليه)"
        else -> "متزن"
      }

      BalancesReportRowData(
        id = sup.id,
        name = sup.name,
        phone = sup.phone,
        companyOrType = sup.companyName.ifEmpty { "مورد بضاعة" },
        balance = balance,
        statusText = statusText
      )
    }
  }

  val currentRawItems = if (activeSubTab == "customers") customerReportItems else supplierReportItems

  // Filtered & Sorted items
  val filteredItems = remember(currentRawItems, searchQuery, filterType, sortBy) {
    var list = currentRawItems.filter { item ->
      searchQuery.isBlank() || 
      item.name.contains(searchQuery, ignoreCase = true) ||
      item.phone.contains(searchQuery) ||
      item.companyOrType.contains(searchQuery, ignoreCase = true)
    }

    list = when (filterType) {
      "NON_ZERO" -> list.filter { Math.abs(it.balance) > 0.001 }
      "DEBIT" -> list.filter { it.balance > 0.001 }
      "CREDIT" -> list.filter { it.balance < -0.001 }
      "ZERO" -> list.filter { Math.abs(it.balance) <= 0.001 }
      else -> list
    }

    when (sortBy) {
      "NAME" -> list.sortedBy { it.name }
      "BALANCE_ASC" -> list.sortedBy { Math.abs(it.balance) }
      else -> list.sortedByDescending { Math.abs(it.balance) }
    }
  }

  // Summary statistics for active report
  val totalCount = filteredItems.size
  val totalDebits = filteredItems.filter { it.balance > 0 }.sumOf { it.balance }
  val totalCredits = filteredItems.filter { it.balance < 0 }.sumOf { -it.balance }
  val netBalance = filteredItems.sumOf { it.balance }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // App Bar / Screen Title
    CenterAlignedTopAppBar(
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.AccountBalanceWallet,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "كشف الأرصدة والديون",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
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
        Button(
          onClick = {
            val title = if (activeSubTab == "customers") "كشف أرصدة العملاء" else "كشف أرصدة الموردين"
            viewModel.exportBalancesReportToPdf(
              context = context,
              reportTitle = title,
              isCustomerReport = (activeSubTab == "customers"),
              rows = filteredItems
            )
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.padding(end = 8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.Print, contentDescription = "طباعة", modifier = Modifier.size(16.dp))
            Text("طباعة / PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      },
      colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface
      )
    )

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Section 1: SubTab Segmented Choice (Customers vs Suppliers)
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            // Customers Option Button
            val isCust = (activeSubTab == "customers")
            Surface(
              onClick = { activeSubTab = "customers" },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp),
              color = if (isCust) MaterialTheme.colorScheme.primary else Color.Transparent,
              contentColor = if (isCust) Color.White else MaterialTheme.colorScheme.onSurface
            ) {
              Row(
                modifier = Modifier
                  .padding(vertical = 12.dp)
                  .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  Icons.Default.People,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "كشف أرصدة العملاء (${customerReportItems.size})",
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            // Suppliers Option Button
            val isSup = (activeSubTab == "suppliers")
            Surface(
              onClick = { activeSubTab = "suppliers" },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp),
              color = if (isSup) MaterialTheme.colorScheme.primary else Color.Transparent,
              contentColor = if (isSup) Color.White else MaterialTheme.colorScheme.onSurface
            ) {
              Row(
                modifier = Modifier
                  .padding(vertical = 12.dp)
                  .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  Icons.Default.LocalShipping,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "الموردين (${supplierReportItems.size})",
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            // Expenses Option Button
            val isExp = (activeSubTab == "expenses")
            Surface(
              onClick = { activeSubTab = "expenses" },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp),
              color = if (isExp) MaterialTheme.colorScheme.primary else Color.Transparent,
              contentColor = if (isExp) Color.White else MaterialTheme.colorScheme.onSurface
            ) {
              Row(
                modifier = Modifier
                  .padding(vertical = 12.dp)
                  .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  Icons.Default.Payments,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "المصاريف (${expenses.size})",
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }

      // Section 2: Summary KPI Stat Cards
      item {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Card 1: Total Count
            Card(
              modifier = Modifier.weight(1f),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
              shape = RoundedCornerShape(12.dp)
            ) {
              Column(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(
                  text = if (activeSubTab == "customers") "عدد العملاء" else "عدد الموردين",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "$totalCount",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
            
            // Card 4: Net Balance
            Card(
              modifier = Modifier.weight(1.5f),
              colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.1f)),
              border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f)),
              shape = RoundedCornerShape(12.dp)
            ) {
              Column(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(
                  text = "صافي المبالغ المستحقة",
                  fontSize = 11.sp,
                  color = PrimaryBlue
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "${formatMoneyLocal(netBalance)} $currencySymbol",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = PrimaryBlue
                )
              }
            }
          }
          
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Card 2: Total Debits (مدين)
            Card(
              modifier = Modifier.weight(1f),
              colors = CardDefaults.cardColors(containerColor = CoralError.copy(alpha = 0.1f)),
              border = BorderStroke(1.dp, CoralError.copy(alpha = 0.3f)),
              shape = RoundedCornerShape(12.dp)
            ) {
              Column(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(
                  text = if (activeSubTab == "customers") "ديون العملاء (مدين)" else "مستحق لنا (مدين)",
                  fontSize = 11.sp,
                  color = CoralError,
                  textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "${formatMoneyLocal(totalDebits)} $currencySymbol",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = CoralError
                )
              }
            }
            
            // Card 3: Total Credits (دائن)
            Card(
              modifier = Modifier.weight(1f),
              colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.1f)),
              border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f)),
              shape = RoundedCornerShape(12.dp)
            ) {
              Column(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(
                  text = if (activeSubTab == "customers") "أرصدة فائضة (دائن)" else "ديون الموردين (دائن)",
                  fontSize = 11.sp,
                  color = EmeraldGreen,
                  textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "${formatMoneyLocal(totalCredits)} $currencySymbol",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = EmeraldGreen
                )
              }
            }
          }
        }
      }

      // Section 3: Search Input & Filter Chips
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Search TextField
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = {
                Text(
                  text = if (activeSubTab == "customers") "ابحث عن عميل بالاسم أو رقم الهاتف..." else "ابحث عن مورد بالاسم، الشركة، أو الهاتف...",
                  fontSize = 12.sp
                )
              },
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
              trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                  IconButton(onClick = { searchQuery = "" }) {
                    Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color.Gray)
                  }
                }
              },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp)
            )

            // Filter Chips Row
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("التصفية:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

              listOf(
                "ALL" to "الكل",
                "NON_ZERO" to "غير صفرية ",
                "DEBIT" to "مدين (عليهم)",
                "CREDIT" to "دائن (لهم)",
                "ZERO" to "متزن (0.0)"
              ).forEach { (code, label) ->
                FilterChip(
                  selected = (filterType == code),
                  onClick = { filterType = code },
                  label = { Text(label, fontSize = 11.sp) }
                )
              }
            }
          }
        }
      }

      // Section 4: Data List Header
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (activeSubTab == "customers") "قائمة كشف أرصدة العملاء ($totalCount)" else "قائمة كشف أرصدة الموردين ($totalCount)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary
          )

          // Quick Share / WhatsApp summary of all balances
          TextButton(
            onClick = {
              val summary = buildString {
                appendLine(" ${if (activeSubTab == "customers") "كشف أرصدة العملاء" else "كشف أرصدة الموردين"}")
                appendLine("التاريخ: ${java.text.SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(java.util.Date())}")
                appendLine("-----------------------------")
                filteredItems.take(20).forEach { item ->
                  appendLine("• ${item.name}: ${formatMoneyLocal(Math.abs(item.balance))} $currencySymbol (${item.statusText})")
                }
                appendLine("-----------------------------")
                appendLine("صافي الإجمالي: ${formatMoneyLocal(netBalance)} $currencySymbol")
              }

              val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, summary)
              }
              context.startActivity(Intent.createChooser(shareIntent, "مشاركة كشف الأرصدة"))
            }
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
              Text("مشاركة النص", fontSize = 12.sp)
            }
          }
        }
      }

      // Section 5: List Items
      if (filteredItems.isEmpty()) {
        item {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 30.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                modifier = Modifier.size(48.dp),
                tint = Color.Gray
              )
              Text(
                text = "لا توجد نتائج مطابقة لخيارات البحث أو الفلترة",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.Gray
              )
            }
          }
        }
      } else {
        itemsIndexed(filteredItems) { index, item ->
          val absBalance = Math.abs(item.balance)
          val statusColor = when {
            item.balance > 0.001 -> CoralError
            item.balance < -0.001 -> EmeraldGreen
            else -> Color.Gray
          }

          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              // Left Side: Name, Phone, Type, Status Badge
              Row(
                modifier = Modifier.weight(1.5f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                // Index badge
                Box(
                  modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "${index + 1}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                  )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )

                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    if (item.phone.isNotBlank()) {
                      Text(
                        text = " ${item.phone}",
                        fontSize = 11.sp,
                        color = Color.Gray
                      )
                    }

                    Text(
                      text = "• ${item.companyOrType}",
                      fontSize = 11.sp,
                      color = Color.Gray
                    )
                  }
                }
              }

              // Right Side: Balance Amount, Status Badge, Quick Actions
              Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                // Balance Status Badge
                Surface(
                  color = statusColor.copy(alpha = 0.1f),
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                ) {
                  Text(
                    text = item.statusText,
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                  )
                }

                Text(
                  text = "${formatMoneyLocal(absBalance)} $currencySymbol",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = statusColor
                )

                // Action Buttons (Statement + Contact)
                Row(
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  // Detailed Statement Dialog Button
                  Button(
                    onClick = {
                      if (activeSubTab == "customers") {
                        selectedCustomerForStatement = customers.find { it.id == item.id }
                      } else {
                        selectedSupplierForStatement = suppliers.find { it.id == item.id }
                      }
                    },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                      contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                  ) {
                    Text("كشف حساب ", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                  }

                  // WhatsApp Button if phone exists
                  if (item.phone.isNotBlank()) {
                    IconButton(
                      onClick = {
                        val cleanPhone = item.phone.replace("+", "").replace(" ", "")
                        val url = "https://api.whatsapp.com/send?phone=$cleanPhone"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                      },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(
                        Icons.Default.Phone,
                        contentDescription = "واتساب",
                        tint = EmeraldGreen,
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

  // Detailed Statement Dialog for Customer
  selectedCustomerForStatement?.let { customer ->
    val customerInvoices = invoices.filter { it.customerId == customer.id }
    val relatedCustomerTx = customerTransactions.filter { it.customerId == customer.id }

    AlertDialog(
      onDismissRequest = { selectedCustomerForStatement = null },
      properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .fillMaxHeight(0.88f),
      title = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "كشف حساب تفصيلي: ${customer.name}",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary
          )
          IconButton(onClick = { selectedCustomerForStatement = null }) {
            Icon(Icons.Default.Close, contentDescription = "إغلاق")
          }
        }
      },
      text = {
        CustomerStatementDetail(
          customer = customer,
          invoices = invoices,
          customerTransactions = customerTransactions,
          viewModel = viewModel
        )
      },
      confirmButton = {}
    )
  }

  // Detailed Statement Dialog for Supplier
  selectedSupplierForStatement?.let { supplier ->
    AlertDialog(
      onDismissRequest = { selectedSupplierForStatement = null },
      properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .fillMaxHeight(0.88f),
      title = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "كشف حساب وتوريدات: ${supplier.name}",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary
          )
          IconButton(onClick = { selectedSupplierForStatement = null }) {
            Icon(Icons.Default.Close, contentDescription = "إغلاق")
          }
        }
      },
      text = {
        SupplierStatementDetail(
          supplier = supplier,
          supplierTransactions = supplierTransactions,
          viewModel = viewModel
        )
      },
      confirmButton = {}
    )
  }
}
