package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Customer
import com.example.data.CustomerTransaction
import com.example.data.Supplier
import com.example.data.SupplierTransaction
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.CoralError
import com.example.ui.theme.AmberAlert
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountStatementsScreen(viewModel: AppViewModel) {
  val context = androidx.compose.ui.platform.LocalContext.current
  var activeSubTab by remember { mutableStateOf("customers") } // "customers" or "suppliers"
  var searchQuery by remember { mutableStateOf("") }
  
  // Selection states
  var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
  var selectedSupplier by remember { mutableStateOf<Supplier?>(null) }
  
  // Data flows from ViewModel
  val customers by viewModel.allCustomers.collectAsState(emptyList())
  val suppliers by viewModel.allSuppliers.collectAsState(emptyList())
  val invoices by viewModel.allInvoices.collectAsState(emptyList())
  val customerTransactions by viewModel.allCustomerTransactions.collectAsState(emptyList())
  val supplierTransactions by viewModel.allSupplierTransactions.collectAsState(emptyList())
  val allInvoiceItemsState by viewModel.allInvoiceItems.collectAsState(emptyList())
  var showStatementTypeDialog by remember { mutableStateOf(false) }
  
  // Print / PDF simulation states
  var showPrintSimulation by remember { mutableStateOf(false) }
  var printContentTitle by remember { mutableStateOf("") }
  var printContentBody by remember { mutableStateOf<List<StatementRow>>(emptyList()) }
  var printSummaryText by remember { mutableStateOf("") }
  var entityDetails by remember { mutableStateOf("") }

  val overdueCustomersList = remember(customers, invoices, customerTransactions) {
    calculateOverdueCustomers(customers, invoices, customerTransactions)
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Dynamic Header
    val currentTitle = when {
      selectedCustomer != null -> "كشف حساب العميل: ${selectedCustomer?.name}"
      selectedSupplier != null -> "كشف حساب المورد: ${selectedSupplier?.name}"
      activeSubTab == "overdue_alerts" -> "تنبيهات وإشعارات الدفعات المتأخرة "
      else -> "كشوفات حسابات العملاء والموردين "
    }
    

    val generateStatementPreview = { isDetailed: Boolean ->
        if (selectedCustomer != null) {
                val cust = selectedCustomer!!
                val relatedInvoices = invoices.filter { it.customerId == cust.id }
                val relatedTxs = customerTransactions.filter { it.customerId == cust.id }
                
                val combinedList = mutableListOf<StatementRow>()
                relatedInvoices.forEach { inv ->
                  var desc = "فاتورة مبيعات رقم #${inv.id}"
                  if (isDetailed) {
                      val items = allInvoiceItemsState.filter { it.invoiceId == inv.id }
                      if (items.isNotEmpty()) {
                          desc += "\n" + items.joinToString("\n") { "- ${it.productNameAr} (${it.quantity} x ${formatLocalMoney(it.price)})" }
                      }
                  }
                  combinedList.add(StatementRow(inv.invoiceDate, desc, inv.total, inv.paidAmount, inv.dueDate))
                }
                relatedTxs.forEach { tx ->
                  combinedList.add(StatementRow(tx.date, tx.note.ifEmpty { if (tx.transactionType == "بيع") "فاتورة آجلة" else "سند قبض مسدد" }, if (tx.transactionType == "بيع") tx.amount else 0.0, if (tx.transactionType == "دفعة") tx.amount else 0.0, tx.dueDate))
                }
                combinedList.sortBy { it.date }
                
                val totalDeb = combinedList.sumOf { it.debit }
                val totalCred = combinedList.sumOf { it.credit }
                
                printContentTitle = "كشف حساب عميل: ${cust.name}" + if (isDetailed) " (تفصيلي)" else " (مجمل)"
                printContentBody = combinedList
                printSummaryText = "إجمالي المبيعات: ${formatLocalMoney(totalDeb)} | إجمالي السداد: ${formatLocalMoney(totalCred)} | المتبقي المستحق: ${formatLocalMoney(totalDeb - totalCred)}"
                entityDetails = "رقم الجوال: ${cust.phone} | التصنيف: ${cust.type}"
                showPrintSimulation = true
        } else if (selectedSupplier != null) {
                val sup = selectedSupplier!!
                val relatedTxs = supplierTransactions.filter { it.supplierId == sup.id }
                val combinedList = relatedTxs.sortedBy { it.date }.map { tx ->
                  StatementRow(
                    date = tx.date,
                    description = tx.note.ifEmpty { if (tx.transactionType == "شراء") "فاتورة توريد بضاعة" else "سند صرف دفعة مالية" },
                    debit = if (tx.transactionType != "شراء") tx.amount else 0.0,
                    credit = if (tx.transactionType == "شراء") tx.amount else 0.0
                  )
                }
                
                val totalDeb = combinedList.sumOf { it.debit } // payments
                val totalCred = combinedList.sumOf { it.credit } // purchases
                
                printContentTitle = "كشف حساب مورد: ${sup.name}" + if (isDetailed) " (تفصيلي)" else " (مجمل)"
                printContentBody = combinedList
                printSummaryText = "إجمالي التوريد (دائن): ${formatLocalMoney(totalCred)} | إجمالي المسدد (مدين): ${formatLocalMoney(totalDeb)} | الرصيد المتبقي: ${formatLocalMoney(totalCred - totalDeb)}"
                entityDetails = "الشركة: ${sup.companyName} | جوال مسؤول الاتصال: ${sup.phone}"
                showPrintSimulation = true
        }
    }

    CenterAlignedTopAppBar(
      title = {
        Text(
          text = currentTitle,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      },
      navigationIcon = {
        if (selectedCustomer != null || selectedSupplier != null) {
          IconButton(onClick = {
            selectedCustomer = null
            selectedSupplier = null
          }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
          }
        }
      },
      actions = {
        if (selectedCustomer != null || selectedSupplier != null) {
          IconButton(
            onClick = {
              if (selectedCustomer != null || selectedSupplier != null) {
                  showStatementTypeDialog = true
              }
            }
          ) {
            Icon(Icons.Default.Print, contentDescription = "طباعة ومشاركة الكشف", tint = MaterialTheme.colorScheme.primary)
          }
        }
      },
      colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface
      )
    )

    if (showStatementTypeDialog) {
      AlertDialog(
        onDismissRequest = { showStatementTypeDialog = false },
        title = { Text("نوع كشف الحساب", fontWeight = FontWeight.Bold) },
        text = { Text("هل تريد إنشاء كشف حساب مجمل أم تفصيلي؟") },
        confirmButton = {
          Button(
            onClick = {
              showStatementTypeDialog = false
              generateStatementPreview(true)
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
          ) { Text("تفصيلي") }
        },
        dismissButton = {
          Button(onClick = {
            showStatementTypeDialog = false
            generateStatementPreview(false)
          }) { Text("مجمل") }
        }
      )
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

    if (selectedCustomer == null && selectedSupplier == null) {
      // Tab Selector
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
          .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        val overdueBadgeCount = overdueCustomersList.size
        TabButton(
          text = if (overdueBadgeCount > 0) "تنبيهات المتأخرين ($overdueBadgeCount)" else "تنبيهات المتأخرين ",
          selected = activeSubTab == "overdue_alerts",
          modifier = Modifier.weight(1.2f),
          onClick = {
            activeSubTab = "overdue_alerts"
            searchQuery = ""
          }
        )
        TabButton(
          text = "كشوف الموردين ",
          selected = activeSubTab == "suppliers",
          modifier = Modifier.weight(1f),
          onClick = {
            activeSubTab = "suppliers"
            searchQuery = ""
          }
        )
        TabButton(
          text = "كشوف العملاء ",
          selected = activeSubTab == "customers",
          modifier = Modifier.weight(1f),
          onClick = {
            activeSubTab = "customers"
            searchQuery = ""
          }
        )
      }

      if (activeSubTab != "overdue_alerts") {
        // Search Bar for Customers and Suppliers
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("statement_search_query"),
          placeholder = {
            Text(
              if (activeSubTab == "customers") "ابحث عن عميل بالاسم أو رقم الجوال..."
              else "ابحث عن مورد بالاسم، الشركة أو الجوال..."
            )
          },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Close, contentDescription = "مسح")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))
      }

      // Main Lists
      if (activeSubTab == "overdue_alerts") {
        OverdueAlertsView(
          overdueList = overdueCustomersList,
          viewModel = viewModel,
          onSelectCustomer = { cust -> selectedCustomer = cust }
        )
      } else if (activeSubTab == "customers") {
        val filteredCustomers = customers.filter {
          it.name.contains(searchQuery, ignoreCase = true) ||
          it.phone.contains(searchQuery)
        }
        
        if (filteredCustomers.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .weight(1f),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Default.PeopleOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
              Text("لا يوجد عملاء مسجلين يطابقون البحث حالياً.", color = Color.Gray, fontSize = 13.sp)
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(filteredCustomers) { cust ->
              // Calculate balance
              val relatedInvoices = invoices.filter { it.customerId == cust.id }
              val relatedTxs = customerTransactions.filter { it.customerId == cust.id }
              val totalDebit = relatedInvoices.sumOf { it.total } + relatedTxs.filter { tx -> tx.transactionType == "بيع" }.sumOf { tx -> tx.amount }
              val totalCredit = relatedInvoices.sumOf { it.paidAmount } + relatedTxs.filter { tx -> tx.transactionType == "دفعة" }.sumOf { tx -> tx.amount }
              val outstanding = totalDebit - totalCredit

              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { selectedCustomer = cust }
                  .testTag("statement_customer_card_${cust.id}"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Text("جوال: ${cust.phone}", fontSize = 12.sp, color = Color.Gray)
                      Text("•", fontSize = 12.sp, color = Color.Gray)
                      Text(cust.type, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                  }
                  
                  Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("الرصيد المستحق", fontSize = 10.sp, color = Color.Gray)
                    Text(
                      text = "${formatLocalMoney(outstanding)} شيكل",
                      fontWeight = FontWeight.Bold,
                      fontSize = 14.sp,
                      color = if (outstanding > 0) CoralError else EmeraldGreen
                    )
                  }
                }
              }
            }
          }
        }
      } else {
        val filteredSuppliers = suppliers.filter {
          it.name.contains(searchQuery, ignoreCase = true) ||
          it.companyName.contains(searchQuery, ignoreCase = true) ||
          it.phone.contains(searchQuery)
        }

        if (filteredSuppliers.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .weight(1f),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Default.BusinessCenter, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
              Text("لا يوجد موردين مسجلين يطابقون البحث حالياً.", color = Color.Gray, fontSize = 13.sp)
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(filteredSuppliers) { sup ->
              val relatedTxs = supplierTransactions.filter { it.supplierId == sup.id }
              val totalPurchases = relatedTxs.filter { it.transactionType == "شراء" }.sumOf { it.amount }
              val totalPayments = relatedTxs.filter { it.transactionType == "دفعة" }.sumOf { it.amount }
              val outstanding = totalPurchases - totalPayments

              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { selectedSupplier = sup }
                  .testTag("statement_supplier_card_${sup.id}"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(sup.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Text("الشركة: ${sup.companyName.ifEmpty { "غير محددة" }}", fontSize = 12.sp, color = Color.Gray)
                      Text("•", fontSize = 12.sp, color = Color.Gray)
                      Text("جوال: ${sup.phone}", fontSize = 12.sp, color = Color.Gray)
                    }
                  }
                  
                  Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("مستحق للمورد", fontSize = 10.sp, color = Color.Gray)
                    Text(
                      text = "${formatLocalMoney(outstanding)} شيكل",
                      fontWeight = FontWeight.Bold,
                      fontSize = 14.sp,
                      color = if (outstanding > 0) CoralError else EmeraldGreen
                    )
                  }
                }
              }
            }
          }
        }
      }
    } else {
      // Detailed Statement is open!
      if (selectedCustomer != null) {
        CustomerStatementDetail(
          customer = selectedCustomer!!,
          invoices = invoices,
          customerTransactions = customerTransactions,
          viewModel = viewModel
        )
      } else if (selectedSupplier != null) {
        SupplierStatementDetail(
          supplier = selectedSupplier!!,
          supplierTransactions = supplierTransactions,
          viewModel = viewModel
        )
      }
    }
  }

  // PDF / Print Simulation Dialog
  if (showPrintSimulation) {
    AlertDialog(
      onDismissRequest = { showPrintSimulation = false },
      confirmButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = {
              val isSupplier = selectedSupplier != null
              val phone = if (isSupplier) selectedSupplier?.phone ?: "" else selectedCustomer?.phone ?: ""
              val cleanPhone = phone.replace("+", "").replace(" ", "").filter { it.isDigit() }
              
              val pdfFile = com.example.ui.PdfGenerator.generateStatementPdfFile(
                context = context,
                storeName = viewModel.storeName.value,
                storePhone = viewModel.storePhone.value,
                title = printContentTitle,
                bodyRows = printContentBody,
                summaryText = printSummaryText,
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
                
                // Try personal WhatsApp first
                shareIntent.setPackage("com.whatsapp")
                try {
                  context.startActivity(shareIntent)
                  viewModel.showToast("تم فتح واتساب للمشاركة ")
                } catch (e: Exception) {
                  // Try Business WhatsApp
                  shareIntent.setPackage("com.whatsapp.w4b")
                  try {
                    context.startActivity(shareIntent)
                    viewModel.showToast("تم فتح واتساب للأعمال للمشاركة ")
                  } catch (e2: Exception) {
                    // Fallback to chooser
                    shareIntent.setPackage(null)
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "مشاركة كشف الحساب"))
                  }
                }
              } else {
                viewModel.showToast("حدث خطأ أثناء إنشاء ملف PDF ")
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
          ) {
            Text("مشاركة واتساب", color = Color.White)
          }
          Button(
            onClick = {
              viewModel.exportStatementToPdf(context, printContentTitle, printContentBody, printSummaryText, entityDetails)
              showPrintSimulation = false
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
          ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("طباعة / PDF")
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { showPrintSimulation = false }) {
          Text("إغلاق")
        }
      },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = CoralError)
          Text("معاينة كشف الحساب والطباعة ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f)
            .verticalScroll(rememberScrollState())
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Header simulation
          Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("مؤسسة الكنز للمحاسبة التجارية", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 16.sp)
            Text("نظام الفوترة وكشوف الحسابات المتكامل v2.5", color = Color.Gray, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.LightGray)
          }

          // Statement Title
          Text(printContentTitle, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
          Text(entityDetails, color = Color.DarkGray, fontSize = 11.sp)
          Text("تاريخ استخراج الكشف: ${SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())}", color = Color.Gray, fontSize = 10.sp)

          HorizontalDivider(color = Color.LightGray)

          // Body
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF3F4F6)).padding(6.dp)) {
              Text("التاريخ", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Black, modifier = Modifier.weight(1.2f))
              Text("البيان", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Black, modifier = Modifier.weight(2f))
              Text("مدين (+)", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
              Text("دائن (-)", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }

            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            printContentBody.forEach { row ->
              Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp)) {
                Text(sdf.format(Date(row.date)), fontSize = 10.sp, color = Color.Black, modifier = Modifier.weight(1.2f))
                Text(row.description, fontSize = 10.sp, color = Color.Black, modifier = Modifier.weight(2f))
                Text(if (row.debit > 0) formatLocalMoney(row.debit) else "-", fontSize = 10.sp, color = Color.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text(if (row.credit > 0) formatLocalMoney(row.credit) else "-", fontSize = 10.sp, color = Color.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
              }
            }
          }

          HorizontalDivider(color = Color.LightGray)

          // Summary
          Text(printSummaryText, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 12.sp)
        }
      }
    )
  }
}

@Composable
fun TabButton(
  text: String,
  selected: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Surface(
    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
    shape = RoundedCornerShape(8.dp),
    modifier = modifier.clickable { onClick() }
  ) {
    Box(
      modifier = Modifier.padding(vertical = 10.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(text = text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
  }
}

@Composable
fun CustomerStatementDetail(
  customer: Customer,
  invoices: List<com.example.data.Invoice>,
  customerTransactions: List<CustomerTransaction>,
  viewModel: AppViewModel
) {
  val customerInvoices = invoices.filter { it.customerId == customer.id && !it.isPurchase && !it.isDraft }
  val relatedCustomerTx = customerTransactions.filter { it.customerId == customer.id }

  val combinedTxList = remember(customerInvoices, relatedCustomerTx) {
    val list = mutableListOf<StatementRow>()
    customerInvoices.forEach { inv ->
      // Debit for the invoice total
      list.add(
        StatementRow(
          date = inv.invoiceDate,
          description = "فاتورة مبيعات رقم #${inv.id}",
          debit = inv.total,
          credit = 0.0,
          dueDate = inv.dueDate
        )
      )
      // Credit for any amount paid at the time of invoice
      if (inv.paidAmount > 0) {
        list.add(
          StatementRow(
            date = inv.invoiceDate + 1000,
            description = "دفعة مسددة على فاتورة #${inv.id}",
            debit = 0.0,
            credit = inv.paidAmount,
            dueDate = inv.dueDate
          )
        )
      }
    }
    relatedCustomerTx.forEach { tx ->
      list.add(
        StatementRow(
          date = tx.date,
          description = tx.note.ifEmpty { if (tx.transactionType == "بيع") "فاتورة آجلة / دَيْن" else "سند قبض مسدد" },
          debit = if (tx.transactionType == "بيع") tx.amount else 0.0,
          credit = if (tx.transactionType == "دفعة") tx.amount else 0.0,
          dueDate = tx.dueDate
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
  var selectedTxType by remember { mutableStateOf("دفعة") } // "دفعة" or "بيع"
  var paymentAmountStr by remember { mutableStateOf("") }
  var paymentNote by remember { mutableStateOf("سند قبض نقدي سداد حساب") }
  var selectedDueDays by remember { mutableStateOf(14) } // Default 14 days due

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Customer Quick Summary Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Column {
            Text("الاسم: ${customer.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("رقم الجوال: ${customer.phone}", fontSize = 13.sp, color = Color.Gray)
          }
          Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            contentColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(
              "${customer.points} نقطة",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
        
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          SummaryStatCard(
            title = "إجمالي المبيعات (+)",
            value = totalDebit,
            textColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
          )
          SummaryStatCard(
            title = "إجمالي السداد (-)",
            value = totalCredit,
            textColor = EmeraldGreen,
            modifier = Modifier.weight(1f)
          )
          SummaryStatCard(
            title = "المتبقي المستحق",
            value = outstandingBalance,
            textColor = if (outstandingBalance > 0) CoralError else EmeraldGreen,
            modifier = Modifier.weight(1.2f),
            border = BorderStroke(1.dp, if (outstandingBalance > 0) CoralError.copy(alpha = 0.5f) else EmeraldGreen.copy(alpha = 0.5f))
          )
        }
      }
    }

    // Ledger Table
    Text("القيود والمعاملات التفصيلية للعميل:", fontSize = 13.sp, fontWeight = FontWeight.Bold)

    Card(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Table Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(vertical = 10.dp, horizontal = 12.dp)
        ) {
          Text("التاريخ", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.2f))
          Text("البيان والطلب", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2f))
          Text("مدين (+)", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
          Text("دائن (-)", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center, color = EmeraldGreen)
          Text("الرصيد", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }

        if (combinedTxList.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد حركات مالية مسجلة للعميل حتى الآن.", color = Color.Gray, fontSize = 12.sp)
          }
        } else {
          var runningBal = 0.0
          val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
          val currentTime = System.currentTimeMillis()
          
          LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(combinedTxList) { tx ->
              runningBal += (tx.debit - tx.credit)
              val isOverdue = tx.debit > 0 && (tx.dueDate ?: (tx.date + 14L * 86400000L)) < currentTime
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                  .padding(vertical = 10.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(sdf.format(Date(tx.date)), fontSize = 10.sp, color = Color.Gray, modifier = Modifier.weight(1.2f))
                Column(modifier = Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(tx.description, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                  if (tx.debit > 0) {
                    val dueTime = tx.dueDate ?: (tx.date + 14L * 86400000L)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                      Text("استحقاق: ${sdf.format(Date(dueTime))}", fontSize = 9.sp, color = if (isOverdue) CoralError else Color.Gray)
                      if (isOverdue) {
                        Surface(color = CoralError.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                          Text(" متأخر", fontSize = 8.sp, color = CoralError, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                      }
                    }
                  }
                }
                Text(if (tx.debit > 0) formatLocalMoney(tx.debit) else "-", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.9f))
                Text(if (tx.credit > 0) formatLocalMoney(tx.credit) else "-", fontSize = 11.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.9f))
                Text(formatLocalMoney(runningBal), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f), color = if (runningBal > 0) CoralError else Color.DarkGray)
              }
            }
          }
        }
      }
    }

    // Add Transaction Section
    if (showAddPaymentFields) {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("تسجيل حركة مالية جديدة للعميل ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          
          // Transaction Type Selector
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
              .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            FilterChip(
              selected = selectedTxType == "دفعة",
              onClick = {
                selectedTxType = "دفعة"
                paymentNote = "سند قبض نقدي سداد حساب"
              },
              label = { Text("سند قبض / سداد (-)") },
              modifier = Modifier.weight(1f)
            )
            FilterChip(
              selected = selectedTxType == "بيع",
              onClick = {
                selectedTxType = "بيع"
                paymentNote = "إثبات دَيْن / فاتورة آجلة"
              },
              label = { Text("إثبات دَيْن / بيع آجل (+)") },
              modifier = Modifier.weight(1f)
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = paymentAmountStr,
              onValueChange = { paymentAmountStr = it },
              label = { Text("المبلغ (شيكل):") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1.2f)
            )
            OutlinedTextField(
              value = paymentNote,
              onValueChange = { paymentNote = it },
              label = { Text("البيان / الملاحظة:") },
              singleLine = true,
              modifier = Modifier.weight(1.8f)
            )
          }

          if (selectedTxType == "بيع") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("تاريخ استحقاق الدفعة / المهلة المحدد للسداد:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                  selected = selectedDueDays == 7,
                  onClick = { selectedDueDays = 7 },
                  label = { Text("بعد 7 أيام") }
                )
                FilterChip(
                  selected = selectedDueDays == 14,
                  onClick = { selectedDueDays = 14 },
                  label = { Text("بعد 14 يوماً") }
                )
                FilterChip(
                  selected = selectedDueDays == 30,
                  onClick = { selectedDueDays = 30 },
                  label = { Text("بعد 30 يوماً") }
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            TextButton(onClick = { showAddPaymentFields = false }) {
              Text("إلغاء", color = CoralError)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                val amt = paymentAmountStr.toDoubleOrNull() ?: 0.0
                if (amt > 0) {
                  val computedDueDate = if (selectedTxType == "بيع") {
                    System.currentTimeMillis() + (selectedDueDays.toLong() * 86400000L)
                  } else null

                  viewModel.addCustomerTransaction(
                    CustomerTransaction(
                      customerId = customer.id,
                      transactionType = selectedTxType,
                      amount = amt,
                      note = paymentNote,
                      dueDate = computedDueDate
                    )
                  )
                  paymentAmountStr = ""
                  showAddPaymentFields = false
                } else {
                  viewModel.showToast("يرجى كتابة مبلغ صحيح!")
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = if (selectedTxType == "دفعة") EmeraldGreen else MaterialTheme.colorScheme.primary)
            ) {
              Text("تسجيل الحركة")
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
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
      ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        Text("تسجيل حركة مالية / سداد أو دَيْن جديد ", fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
fun SupplierStatementDetail(
  supplier: Supplier,
  supplierTransactions: List<SupplierTransaction>,
  viewModel: AppViewModel
) {
  val relatedSupplierTx = supplierTransactions.filter { it.supplierId == supplier.id }
  
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

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Supplier Summary Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Column {
            Text("اسم المورد: ${supplier.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("الشركة: ${supplier.companyName.ifEmpty { "غير محددة" }}", fontSize = 13.sp, color = Color.Gray)
          }
          Column(horizontalAlignment = Alignment.End) {
            Text("الجوال: ${supplier.phone}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("العنوان: ${supplier.address.ifEmpty { "غير محدد" }}", fontSize = 11.sp, color = Color.Gray)
          }
        }
        
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          SummaryStatCard(
            title = "إجمالي التوريدات (دائن)",
            value = totalPurchases,
            textColor = CoralError,
            modifier = Modifier.weight(1f)
          )
          SummaryStatCard(
            title = "إجمالي المدفوعات (مدين)",
            value = totalPayments,
            textColor = EmeraldGreen,
            modifier = Modifier.weight(1f)
          )
          SummaryStatCard(
            title = "الرصيد المتبقي للمورد",
            value = supplierBalance,
            textColor = if (supplierBalance > 0) CoralError else EmeraldGreen,
            modifier = Modifier.weight(1.2f),
            border = BorderStroke(1.dp, if (supplierBalance > 0) CoralError.copy(alpha = 0.5f) else EmeraldGreen.copy(alpha = 0.5f))
          )
        }
      }
    }

    // Ledger Table
    Text("سجل حركات وعمليات التوريد والدفعات المتبادلة:", fontSize = 13.sp, fontWeight = FontWeight.Bold)

    Card(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Table Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(vertical = 10.dp, horizontal = 12.dp)
        ) {
          Text("التاريخ", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.2f))
          Text("البيان", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2f))
          Text("مدين (مسدد له)", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = EmeraldGreen)
          Text("دائن (توريد)", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = CoralError)
          Text("الرصيد المستحق", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
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
                  .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                  .padding(vertical = 10.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(sdf.format(Date(tx.date)), fontSize = 10.sp, color = Color.Gray, modifier = Modifier.weight(1.2f))
                Text(tx.note.ifEmpty { if (isPurchase) "فاتورة توريد بضاعة" else "سند صرف دفعة مالية للمورد" }, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(2f))
                Text(if (deb > 0) formatLocalMoney(deb) else "-", fontSize = 11.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Text(if (cred > 0) formatLocalMoney(cred) else "-", fontSize = 11.sp, color = CoralError, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Text(formatLocalMoney(runningBal), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f), color = if (runningBal > 0) CoralError else EmeraldGreen)
              }
            }
          }
        }
      }
    }

    // Add Transaction Forms
    if (showAddTxFields) {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("تسجيل سداد دفعة نقدية ", fontWeight = FontWeight.Bold, fontSize = 13.sp)

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = txAmountStr,
              onValueChange = { txAmountStr = it },
              label = { Text("المبلغ:") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1.2f)
            )
            OutlinedTextField(
              value = txNote,
              onValueChange = { txNote = it },
              label = { Text("البيان / تفصيل الشراء:") },
              singleLine = true,
              modifier = Modifier.weight(1.8f)
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
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
              Text("تسجيل الحركة")
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
        Text("سداد دفعة للمورد ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
fun SummaryStatCard(
  title: String,
  value: Double,
  textColor: Color,
  modifier: Modifier = Modifier,
  border: BorderStroke? = null
) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = border ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(title, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Text(
        "${formatLocalMoney(value)} شيكل",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

data class StatementRow(
  val date: Long,
  val description: String,
  val debit: Double,
  val credit: Double,
  val dueDate: Long? = null
)

data class OverdueCustomerInfo(
  val customer: Customer,
  val totalOutstanding: Double,
  val totalOverdueAmount: Double,
  val earliestDueDate: Long,
  val daysOverdue: Int,
  val overdueItemsCount: Int
)

fun calculateOverdueCustomers(
  customers: List<Customer>,
  invoices: List<com.example.data.Invoice>,
  customerTransactions: List<CustomerTransaction>
): List<OverdueCustomerInfo> {
  val currentTime = System.currentTimeMillis()
  val overdueList = mutableListOf<OverdueCustomerInfo>()

  customers.forEach { cust ->
    val custInvoices = invoices.filter { it.customerId == cust.id }
    val custTxs = customerTransactions.filter { it.customerId == cust.id }

    val totalDebit = custInvoices.sumOf { it.total } + custTxs.filter { it.transactionType == "بيع" }.sumOf { it.amount }
    val totalCredit = custInvoices.sumOf { it.paidAmount } + custTxs.filter { it.transactionType == "دفعة" }.sumOf { it.amount }
    val outstanding = totalDebit - totalCredit

    if (outstanding > 0.01) {
      val debitItemsWithDueDate = mutableListOf<Pair<Long, Double>>() // Pair(dueDate, amount)
      
      custTxs.filter { it.transactionType == "بيع" }.forEach { tx ->
        val due = tx.dueDate ?: (tx.date + 14L * 86400000L)
        debitItemsWithDueDate.add(Pair(due, tx.amount))
      }
      
      custInvoices.forEach { inv ->
        val unpaid = inv.total - inv.paidAmount
        if (unpaid > 0) {
          val due = inv.dueDate ?: (inv.invoiceDate + 14L * 86400000L)
          debitItemsWithDueDate.add(Pair(due, unpaid))
        }
      }

      val overdueItems = debitItemsWithDueDate.filter { it.first < currentTime }
      
      if (overdueItems.isNotEmpty() || (debitItemsWithDueDate.isEmpty() && outstanding > 0)) {
        val earliestDue = overdueItems.minOfOrNull { it.first } ?: (currentTime - 7L * 86400000L)
        val daysOverdue = maxOf(1, ((currentTime - earliestDue) / (1000 * 60 * 60 * 24)).toInt())
        val overdueSum = overdueItems.sumOf { it.second }.let { if (it > 0) minOf(it, outstanding) else outstanding }

        overdueList.add(
          OverdueCustomerInfo(
            customer = cust,
            totalOutstanding = outstanding,
            totalOverdueAmount = overdueSum,
            earliestDueDate = earliestDue,
            daysOverdue = daysOverdue,
            overdueItemsCount = overdueItems.size.coerceAtLeast(1)
          )
        )
      }
    }
  }

  return overdueList.sortedByDescending { it.daysOverdue }
}

@Composable
fun OverdueAlertsView(
  overdueList: List<OverdueCustomerInfo>,
  viewModel: AppViewModel,
  onSelectCustomer: (Customer) -> Unit
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  var searchQuery by remember { mutableStateOf("") }
  var filterMode by remember { mutableStateOf("all") } // "all", "critical", "vip"
  var showBulkDialog by remember { mutableStateOf(false) }

  val filteredList = overdueList.filter { item ->
    val matchesSearch = item.customer.name.contains(searchQuery, ignoreCase = true) ||
              item.customer.phone.contains(searchQuery)
    val matchesFilter = when (filterMode) {
      "critical" -> item.daysOverdue >= 15
      "vip" -> item.customer.type == "VIP"
      else -> true
    }
    matchesSearch && matchesFilter
  }

  val totalOverdueSum = overdueList.sumOf { it.totalOverdueAmount }
  val maxDays = overdueList.maxOfOrNull { it.daysOverdue } ?: 0

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Overdue Summary Header Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
      border = BorderStroke(1.dp, CoralError.copy(alpha = 0.3f)),
      shape = RoundedCornerShape(12.dp)
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = CoralError)
            Text("مركز إشعارات وتنبيهات المتأخرات", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
          }
          Surface(color = CoralError, shape = RoundedCornerShape(16.dp)) {
            Text("${overdueList.size} عميل", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
          }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          SummaryStatCard(
            title = "عملاء متأخرون بالسداد",
            value = overdueList.size.toDouble(),
            textColor = CoralError,
            modifier = Modifier.weight(1f)
          )
          SummaryStatCard(
            title = "إجمالي مبالغ التأخير",
            value = totalOverdueSum,
            textColor = CoralError,
            modifier = Modifier.weight(1.3f)
          )
          SummaryStatCard(
            title = "أقصى تأخير (أيام)",
            value = maxDays.toDouble(),
            textColor = AmberAlert,
            modifier = Modifier.weight(1f)
          )
        }

        Button(
          onClick = { showBulkDialog = true },
          modifier = Modifier.fillMaxWidth(),
          colors = ButtonDefaults.buttonColors(containerColor = CoralError),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("إطلاق إشعارات وتنبيهات تذكيرية جماعية لكافة المتأخرين ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }

    // Search & Filter Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        modifier = Modifier.weight(1f),
        placeholder = { Text("بحث باسم العميل أو الجوال...", fontSize = 11.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(10.dp)
      )

      FilterChip(
        selected = filterMode == "critical",
        onClick = { filterMode = if (filterMode == "critical") "all" else "critical" },
        label = { Text("حرج (> 15 يوم) ", fontSize = 11.sp) }
      )
      FilterChip(
        selected = filterMode == "vip",
        onClick = { filterMode = if (filterMode == "vip") "all" else "vip" },
        label = { Text("VIP ", fontSize = 11.sp) }
      )
    }

    // Overdue Customer Cards
    if (filteredList.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(48.dp), tint = EmeraldGreen)
          Text("ممتاز! لا يوجد عملاء متأخرين يطابقون خيارات البحث.", color = Color.Gray, fontSize = 13.sp)
        }
      }
    } else {
      val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
      LazyColumn(
        modifier = Modifier.fillMaxSize().weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(filteredList) { item ->
          val cust = item.customer
          val dueDateStr = sdf.format(Date(item.earliestDueDate))

          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, CoralError.copy(alpha = 0.4f))
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              // Row 1: Header & Badge
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp)) {
                      Text(cust.type, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                  }
                  Text("جوال: ${cust.phone}", fontSize = 12.sp, color = Color.Gray)
                }

                Surface(color = CoralError, shape = RoundedCornerShape(8.dp)) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Text("متأخر ${item.daysOverdue} يوماً", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }

              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

              // Row 2: Amounts
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                  Text("الرصيد المستحق الكلي", fontSize = 10.sp, color = Color.Gray)
                  Text("${formatLocalMoney(item.totalOutstanding)} شيكل", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CoralError)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("المبلغ المتأخر سداده", fontSize = 10.sp, color = Color.Gray)
                  Text("${formatLocalMoney(item.totalOverdueAmount)} شيكل", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AmberAlert)
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text("تاريخ الاستحقاق", fontSize = 10.sp, color = Color.Gray)
                  Text(dueDateStr, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
              }

              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

              // Row 3: Action Buttons
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                val messageText = "السلام عليكم السيد/ة ${cust.name}، نود تذكيركم بلطف بوجود دفعة مالية متأخرة بقيمة ${formatLocalMoney(item.totalOverdueAmount)} شيكل، وكان تاريخ استحقاقها المسجل $dueDateStr. نرجو المبادرة بالسداد. شكراً لكم."

                // WhatsApp Button
                Button(
                  onClick = {
                    clipboardManager.setText(AnnotatedString(messageText))
                    val cleanPhone = cust.phone.replace("+", "").replace(" ", "").filter { it.isDigit() }
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                      data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${URLEncoder.encode(messageText, "UTF-8")}")
                    }
                    try {
                      context.startActivity(intent)
                      viewModel.showToast("تم فتح واتساب وتجهيز رسالة التذكير للعميل ")
                    } catch (e: Exception) {
                      viewModel.showToast("تم نسخ نص التنبيه للعميل إلى الحافظة ")
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                  modifier = Modifier.weight(1f),
                  contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                  Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("واتساب", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                // SMS Button
                OutlinedButton(
                  onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                      data = Uri.parse("smsto:${cust.phone}")
                      putExtra("sms_body", messageText)
                    }
                    try {
                      context.startActivity(intent)
                      viewModel.showToast("تم فتح تطبيق الرسائل SMS للعميل ")
                    } catch (e: Exception) {
                      viewModel.showToast("تعذر فتح تطبيق الرسائل النصية.")
                    }
                  },
                  modifier = Modifier.weight(1f),
                  contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                  Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("SMS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // System Alert Button
                Button(
                  onClick = {
                    viewModel.sendCustomerOverdueAlertNotification(
                      customerName = cust.name,
                      phone = cust.phone,
                      overdueAmount = item.totalOverdueAmount,
                      dueDateStr = dueDateStr
                    )
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                  modifier = Modifier.weight(1.2f),
                  contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                  Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("إشعار نظامي", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // Statement Button
                OutlinedButton(
                  onClick = { onSelectCustomer(cust) },
                  modifier = Modifier.weight(1f),
                  contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                  Text("كشف الحساب", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }

  // Bulk Reminder Dialog
  if (showBulkDialog) {
    AlertDialog(
      onDismissRequest = { showBulkDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = CoralError)
          Text("تأكيد إرسال إشعارات جماعية للمتأخرين ", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            "سيتم إطلاق وإرسال إشعارات نظامية وتسجيل تنبيهات إدارة وتحديث سجل التنبيهات لعدد ${overdueList.size} عميل متأخر بإجمالي مبالغ ${formatLocalMoney(totalOverdueSum)} شيكل.",
            fontSize = 13.sp
          )
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("تتضمن الحملة التذكيرية:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              Text("• تسجيل إشعار لوحة تحكم الإدارة لجميع الحسابات", fontSize = 11.sp, color = Color.Gray)
              Text("• توثيق عملية التذكير الجماعي في سجل التدقيق المالي", fontSize = 11.sp, color = Color.Gray)
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.sendBulkCustomerOverdueAlerts(overdueList.size, totalOverdueSum)
            showBulkDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = CoralError)
        ) {
          Text("تأكيد وإرسال الإشعارات ")
        }
      },
      dismissButton = {
        TextButton(onClick = { showBulkDialog = false }) {
          Text("إلغاء")
        }
      }
    )
  }
}

private fun formatLocalMoney(amount: Double): String {
  val df = DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols(Locale.US))
  return df.format(amount)
}
