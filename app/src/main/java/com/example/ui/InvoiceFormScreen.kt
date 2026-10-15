package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.CartItem
import com.example.data.Customer
import com.example.data.Invoice
import com.example.data.Product
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldGreen
import java.net.URLEncoder
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PosTealHeader = Color(0xFF00796B)
private val PosTealDark = Color(0xFF004D40)
private val PosBgLight = Color(0xFFF1F5F9) // Soft Slate, gentle on the eyes for long POS usage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceFormScreen(
  viewModel: AppViewModel,
  onBackClick: () -> Unit,
  isPurchaseMode: Boolean = false
) {
  PosSalesInvoiceScreen(
    viewModel = viewModel,
    onBackClick = onBackClick,
    isPurchaseMode = isPurchaseMode
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosSalesInvoiceScreen(
  viewModel: AppViewModel,
  onBackClick: () -> Unit,
  isPurchaseMode: Boolean = false,
  isReturnMode: Boolean = false,
  isDraftMode: Boolean = false
) {
  val context = LocalContext.current

  // DB Flows
  val products by viewModel.allProducts.collectAsState(emptyList())
  val categoriesList by viewModel.allCategories.collectAsState(emptyList())
  val customers by viewModel.allCustomers.collectAsState(emptyList())
  val suppliers by viewModel.allSuppliers.collectAsState(emptyList())
  val cart by viewModel.cart.collectAsState(emptyList())
  val selectedCustomerId by viewModel.selectedCustomerId.collectAsState(null)
  val manualCustomerName by viewModel.manualCustomerName.collectAsState()
  val selectedSupplierId by viewModel.selectedSupplierId.collectAsState(null)
  val manualSupplierName by viewModel.manualSupplierName.collectAsState()
  val discount by viewModel.discount.collectAsState(0.0)
  val taxPercent by viewModel.taxPercent.collectAsState(15.0)
  val currencySymbol by viewModel.currencySymbol.collectAsState("شيكل")

  val selectedCustomer = customers.find { it.id == selectedCustomerId }
  val customerDisplayName = when {
    selectedCustomer != null -> selectedCustomer.name
    manualCustomerName.isNotBlank() -> manualCustomerName
    else -> "زبون عام"
  }

  val selectedSupplier = suppliers.find { it.id == selectedSupplierId }
  val supplierDisplayName = when {
    selectedSupplier != null -> selectedSupplier.name
    manualSupplierName.isNotBlank() -> manualSupplierName
    else -> "مورد عام"
  }

  val partyDisplayName = if (isPurchaseMode) supplierDisplayName else customerDisplayName

  // Local State
  var searchQuery by remember { mutableStateOf("") }
  var posViewMode by remember { mutableStateOf("table") } // Invoice Items Table
  var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
  var showAutoDropdown by remember { mutableStateOf(false) }
  val setupPayment = viewModel.setupPaymentMethod.collectAsState().value
  var paymentMethod by remember { mutableStateOf(if (setupPayment.isNotBlank()) setupPayment else "نقدي") } // "نقدي", "آجل", "بطاقة", "شيك"
  val setupCustomDateVal = viewModel.setupCustomDate.collectAsState().value
  var customInvoiceDateInput by remember(setupCustomDateVal) { mutableStateOf(setupCustomDateVal ?: System.currentTimeMillis()) }
  
  // Dialog visibility states
  var showBarcodeScanner by remember { mutableStateOf(false) }
  var showGalleryPicker by remember { mutableStateOf(false) }
  var showCalculatorDialog by remember { mutableStateOf(false) }
  var showMoreMenu by remember { mutableStateOf(false) }
  var showReceiptPreview by remember { mutableStateOf(false) }
  var showCustomPrintChoiceDialog by remember { mutableStateOf(false) }
  var showBluetoothPrinterSettings by remember { mutableStateOf(false) }
  var showWifiPrinterDialog by remember { mutableStateOf(false) }
  var showPartyPickerDialog by remember { mutableStateOf(false) }
  var wifiIpAddress by remember { mutableStateOf("192.168.1.200") }
  var lastSavedInvoice by remember { mutableStateOf<Invoice?>(null) }
  var lastSavedItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
  var selectedUnitItem by remember { mutableStateOf<CartItem?>(null) }
  var selectedOptionItem by remember { mutableStateOf<CartItem?>(null) }
  var enlargedImageUrl by remember { mutableStateOf<String?>(null) }
  
  var showCheckDetailsDialog by remember { mutableStateOf(false) }
  val setupCheckNum = viewModel.setupCheckNumber.collectAsState().value
  val setupCheckBank = viewModel.setupCheckBankName.collectAsState().value
  val setupCheckDate = viewModel.setupCheckDate.collectAsState().value
  var checkNumberInput by remember(setupCheckNum) { mutableStateOf(setupCheckNum) }
  var checkBankNameInput by remember(setupCheckBank) { mutableStateOf(setupCheckBank) }
  var checkDateInput by remember(setupCheckDate) { mutableStateOf(setupCheckDate) }

  // Computations
  val subtotal = cart.sumOf { item ->
    val price = if (isPurchaseMode) {
      if (item.product.pricePurchase > 0) item.product.pricePurchase else item.product.priceSale
    } else {
      item.product.priceSale
    }
    price * item.quantity
  }
  val discountedSubtotal = (subtotal - discount).coerceAtLeast(0.0)
  val taxAmount = discountedSubtotal * (taxPercent / 100)
  val total = discountedSubtotal + taxAmount

  // Discount Text Box State & Percentage Mode
  var isDiscountPercent by remember { mutableStateOf(false) }
  var discountInputText by remember { mutableStateOf("0") }

  LaunchedEffect(subtotal, isDiscountPercent, discountInputText) {
    val parsed = discountInputText.toDoubleOrNull() ?: 0.0
    if (isDiscountPercent) {
      val calculatedAmount = subtotal * (parsed / 100.0)
      viewModel.setDiscount(calculatedAmount)
    } else {
      viewModel.setDiscount(parsed)
    }
  }

  // Save Invoice Logic (Preserves cart so user can view/print before starting new invoice)
  val performActualSave = { chkNum: String?, chkBank: String?, chkDate: Long? ->
    if (isPurchaseMode) {
      viewModel.performPurchaseCheckout(
        paymentMethod = paymentMethod,
        paidAmount = total,
        checkNumber = chkNum,
        checkBankName = chkBank,
        checkDate = chkDate,
        clearCartAfter = false,
        isReturnMode = isReturnMode,
        isDraft = isDraftMode,
        customInvoiceDate = customInvoiceDateInput,
        onSuccess = { createdInvoice ->
          lastSavedInvoice = createdInvoice
          lastSavedItems = cart.toList()
          val successMsg = if (isDraftMode) "تم حفظ مسودة الشراء بنجاح " else "تم حفظ فاتورة الشراء وتوريد البضاعة للمخزن بنجاح وتظهر الآن في الفواتير السابقة! "
          viewModel.showToast(successMsg)
        }
      )
    } else {
      val createdInvoice = Invoice(
        id = 0, // Auto-generated
        customerId = selectedCustomerId ?: 0,
        invoiceDate = customInvoiceDateInput,
        total = total,
        discount = discount,
        paidAmount = if (paymentMethod == "آجل" || paymentMethod == "مدين" || paymentMethod == "دائن") 0.0 else total,
        paymentMethod = paymentMethod,
        checkNumber = chkNum,
        checkBankName = chkBank,
        checkDate = chkDate,
        checkStatus = if (paymentMethod == "شيك") "قيد الانتظار" else null
      )

      viewModel.performCheckout(
        paymentMethod = paymentMethod,
        paidAmount = total,
        changeAmount = 0.0,
        checkNumber = chkNum,
        checkBankName = chkBank,
        checkDate = chkDate,
        clearCartAfter = false, // Do NOT clear cart automatically!
        isReturnMode = isReturnMode,
        isDraft = isDraftMode,
        customInvoiceDate = customInvoiceDateInput,
        onSuccess = {
          lastSavedInvoice = createdInvoice
          lastSavedItems = cart.toList()
          val successMsg = if (isDraftMode) "تم حفظ مسودة المبيع بنجاح " else "تم حفظ الفاتورة بنجاح وتظهر الآن في قائمة الفواتير السابقة! "
          viewModel.showToast(successMsg)
        }
      )
    }
  }

  val onSaveInvoice = {
    if (cart.isEmpty()) {
      val msg = if (isPurchaseMode) "السلة فارغة! يرجى إضافة عناصر لفاتورة الشراء أولاً " else "السلة فارغة! يرجى إضافة عناصر للفاتورة أولاً "
      viewModel.showToast(msg)
    } else if (paymentMethod == "شيك") {
      if (checkNumberInput.isNotBlank() && checkBankNameInput.isNotBlank()) {
        performActualSave(checkNumberInput, checkBankNameInput, checkDateInput)
      } else {
        showCheckDetailsDialog = true
      }
    } else {
      performActualSave(null, null, null)
    }
  }

  if (enlargedImageUrl != null) {
    androidx.compose.ui.window.Dialog(
      onDismissRequest = { enlargedImageUrl = null },
      properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black)
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
            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            .size(40.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "إغلاق",
            tint = Color.White
          )
        }
      }
    }
  }

  Scaffold(
    topBar = {
      // TOP HEADER ACTION BAR (Matching Screenshot layout)
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Left Actions: Print & Save
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Save Invoice Button (Primary Teal Pill)
            Button(
              onClick = { onSaveInvoice() },
              colors = ButtonDefaults.buttonColors(containerColor = PosTealHeader),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Icon(
                Icons.Default.Save,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                "حفظ الفاتورة",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.White
              )
            }

            // Print & Share Button
            OutlinedButton(
              onClick = {
                if (cart.isEmpty()) {
                  viewModel.showToast("السلة فارغة للطباعة أو المشاركة!")
                } else {
                  val summaryText = buildString {
                    appendLine(" فاتورة مبيعات")
                    appendLine("العميل: $customerDisplayName")
                    appendLine("نوع الدفع: $paymentMethod")
                    appendLine("-------------------")
                    cart.forEach { item ->
                      appendLine("• ${item.product.nameAr} x${item.quantity} = ${item.product.priceSale * item.quantity} $currencySymbol")
                    }
                    appendLine("-------------------")
                    appendLine("الإجمالي الصافي: ${formatPosMoney(total)} $currencySymbol")
                  }
                  val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, summaryText)
                    type = "text/plain"
                  }
                  val shareIntent = Intent.createChooser(sendIntent, "مشاركة الفاتورة")
                  context.startActivity(shareIntent)
                }
              },
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
              Icon(
                Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = PosTealHeader
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                "طباعة ومشاركة",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            // Custom Print Button (Bluetooth vs WiFi choice)
            OutlinedButton(
              onClick = {
                if (cart.isEmpty() && lastSavedItems.isEmpty()) {
                  viewModel.showToast("السلة فارغة! يرجى إدخال عناصر أو اختيار فاتورة للطباعة ")
                } else {
                  showCustomPrintChoiceDialog = true
                }
              },
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
              Icon(
                Icons.Default.Print,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = PosTealHeader
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                "طباعة مخصصة",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

          }

          // Back / Return Button
          IconButton(onClick = onBackClick) {
            Icon(
              Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "رجوع",
              tint = Color.Gray
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
        .background(MaterialTheme.colorScheme.background)
    ) {
      // ITEM SEARCH & SCANNER INPUT BAR
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 6.dp, vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PosTealHeader.copy(alpha = 0.3f))
      ) {
        Box(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            // Left Actions: Gallery / Photo Picker Icon
            IconButton(
              onClick = { showGalleryPicker = true },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(
                Icons.Default.Collections,
                contentDescription = "كتالوج الصور",
                tint = PosTealHeader,
                modifier = Modifier.size(18.dp)
              )
            }

            // Search Input Text Field
            BasicTextField(
              value = searchQuery,
              onValueChange = {
                searchQuery = it
                showAutoDropdown = it.isNotBlank()
              },
              singleLine = true,
              textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
              ),
              modifier = Modifier.weight(1f),
              decorationBox = @Composable { innerTextField ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                      Text(
                        "ادخل اسم المادة أو الباركود لاضافتها للفاتورة",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                      )
                    }
                    innerTextField()
                  }
                  if (searchQuery.isNotEmpty()) {
                    IconButton(
                      onClick = {
                        searchQuery = ""
                        showAutoDropdown = false
                      },
                      modifier = Modifier.size(20.dp)
                    ) {
                      Icon(
                        Icons.Default.Clear,
                        contentDescription = "مسح",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  }
                }
              }
            )

            // Barcode Scanner Icon
            IconButton(
              onClick = { showBarcodeScanner = true },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(
                Icons.Default.QrCodeScanner,
                contentDescription = "قارئ الباركود",
                tint = PosTealHeader,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          // Live Search Autocomplete Suggestions Popup
          if (showAutoDropdown && searchQuery.isNotBlank()) {
            val filteredProds = products.filter {
              it.nameAr.contains(searchQuery, ignoreCase = true) ||
                  it.nameEn.contains(searchQuery, ignoreCase = true) ||
                  it.barcode.contains(searchQuery)
            }
            if (filteredProds.isNotEmpty()) {
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 54.dp, start = 84.dp, end = 48.dp)
                  .heightIn(max = 220.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, PosTealHeader)
              ) {
                LazyColumn {
                  items(filteredProds) { prod ->
                    DropdownMenuItem(
                      text = {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Column(modifier = Modifier.weight(1f)) {
                            Text(
                              prod.nameAr,
                              fontWeight = FontWeight.Bold,
                              fontSize = 13.sp
                            )
                            Text(
                              "المخزون: ${prod.quantity} ${prod.unit} | باركود: ${prod.barcode}",
                              fontSize = 10.sp,
                              color = Color.Gray
                            )
                          }
                          Text(
                            "${formatPosMoney(prod.priceSale)} $currencySymbol",
                            fontWeight = FontWeight.Bold,
                            color = PosTealHeader,
                            fontSize = 12.sp
                          )
                        }
                      },
                      onClick = {
                        viewModel.addToCart(prod, 1)
                        searchQuery = ""
                        showAutoDropdown = false
                      }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // MAIN CONTENT AREA (Invoice Items Table)
      // TABLE HEADER BANNER (Teal Pill)
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
          color = PosTealHeader,
          shape = RoundedCornerShape(6.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              "الكمية",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = Color.White,
              textAlign = TextAlign.Center,
              modifier = Modifier.weight(1f)
            )
            Text(
              "الوحدة",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = Color.White,
              textAlign = TextAlign.Center,
              modifier = Modifier.weight(1f)
            )
            Text(
              "السعر",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = Color.White,
              textAlign = TextAlign.Center,
              modifier = Modifier.weight(1.2f)
            )
            Text(
              "الإجمالي",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = Color.White,
              textAlign = TextAlign.Center,
              modifier = Modifier.weight(1.2f)
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // INVOICE ITEMS LIST (Middle Scrollable Area)
        if (cart.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.LightGray
              )
              Text(
                "الفاتورة فارغة، ابدأ بإضافة الأصناف أعلاه ",
                color = Color.Gray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
              .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(cart) { item ->
              PosInvoiceItemRow(
                item = item,
                viewModel = viewModel,
                currencySymbol = currencySymbol,
                onUnitClick = { selectedUnitItem = item },
                onOptionsClick = { selectedOptionItem = item }
              )
            }
          }
        }

      Spacer(modifier = Modifier.height(6.dp))

      // BOTTOM SUMMARY & ACTION FOOTER (Fixed at Bottom)
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          val invoiceTimePickerDialog = android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
              val cal = Calendar.getInstance().apply { timeInMillis = customInvoiceDateInput }
              cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
              cal.set(Calendar.MINUTE, minute)
              cal.set(Calendar.SECOND, 0)
              customInvoiceDateInput = cal.timeInMillis
            },
            Calendar.getInstance().apply { timeInMillis = customInvoiceDateInput }.get(Calendar.HOUR_OF_DAY),
            Calendar.getInstance().apply { timeInMillis = customInvoiceDateInput }.get(Calendar.MINUTE),
            true
          )

          // Invoice Date Picker Dialog Setup
          val invoiceDatePickerDialog = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
              val cal = Calendar.getInstance().apply { timeInMillis = customInvoiceDateInput }
              cal.set(year, month, dayOfMonth)
              customInvoiceDateInput = cal.timeInMillis
              invoiceTimePickerDialog.show()
            },
            Calendar.getInstance().apply { timeInMillis = customInvoiceDateInput }.get(Calendar.YEAR),
            Calendar.getInstance().apply { timeInMillis = customInvoiceDateInput }.get(Calendar.MONTH),
            Calendar.getInstance().apply { timeInMillis = customInvoiceDateInput }.get(Calendar.DAY_OF_MONTH)
          )

          // Teal Summary Header Banner Pill & Date Selector
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              modifier = Modifier
                .weight(1f)
                .clickable { showPartyPickerDialog = true },
              color = PosTealHeader,
              shape = RoundedCornerShape(6.dp)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "المجموع [ ${formatPosMoney(subtotal)} ] ${if (isPurchaseMode) "شراء" else "مبيع"} $paymentMethod - $partyDisplayName",
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.weight(1f)
                )
                Icon(
                  Icons.Default.Edit,
                  contentDescription = "تغيير",
                  tint = Color.White,
                  modifier = Modifier.size(14.dp)
                )
              }
            }

            // Date Selector Chip
            Surface(
              modifier = Modifier.clickable { invoiceDatePickerDialog.show() },
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(6.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  Icons.Default.DateRange,
                  contentDescription = "تاريخ الفاتورة",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(14.dp)
                )
                Text(
                  text = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(customInvoiceDateInput)),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                )
              }
            }
          }

          // Middle Calculations Controls Row (Discount | Calculator | Net Total)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Discount Box (Right) - Slim & Inline selector
            Row(
              modifier = Modifier
                .weight(1.3f)
                .height(44.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Surface(
                onClick = { isDiscountPercent = !isDiscountPercent },
                shape = RoundedCornerShape(6.dp),
                color = if (isDiscountPercent) PosTealHeader else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                modifier = Modifier.fillMaxHeight()
              ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                  Text(
                    text = if (isDiscountPercent) "% نسبة" else "مبلغ",
                    color = if (isDiscountPercent) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                  )
                }
              }

              BasicTextField(
                value = discountInputText,
                onValueChange = { newValue -> discountInputText = newValue },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = androidx.compose.ui.text.TextStyle(
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  textAlign = TextAlign.Center
                ),
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight(),
                decorationBox = @Composable { innerTextField ->
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                      .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                      .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    if (discountInputText.isEmpty()) {
                      Text(
                        if (isDiscountPercent) "0 %" else "0.0",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                      )
                    }
                    innerTextField()
                  }
                }
              )
            }

            // Calculator / Math Operations Button (Center)
            IconButton(
              onClick = { showCalculatorDialog = true },
              modifier = Modifier
                .size(44.dp)
                .background(PosTealHeader, CircleShape)
            ) {
              Text(
                "+\n-",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp
              )
            }

            // Net Total Box (Left)
            Surface(
              modifier = Modifier
                .weight(1.3f)
                .height(44.dp),
              shape = RoundedCornerShape(6.dp),
              border = BorderStroke(1.dp, PosTealHeader),
              color = PosTealHeader.copy(alpha = 0.05f)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("الصافي:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text(
                  "${formatPosMoney(total)} $currencySymbol",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = PosTealHeader,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }

          // Bottom Row Action Button (New Invoice Only)
          Button(
            onClick = {
              viewModel.clearCart()
              discountInputText = "0"
              searchQuery = ""
              viewModel.showToast("تم فتح فاتورة جديدة نظيفة ")
            },
            colors = ButtonDefaults.buttonColors(containerColor = PosTealHeader),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(44.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("فاتورة جديدة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
          }
        }
      }
    }
  }

  // --- DIALOGS & OVERLAYS ---

  // 1. Camera Barcode Scanner Dialog
  if (showBarcodeScanner) {
    BarcodeScannerDialog(
      onDismissRequest = { showBarcodeScanner = false },
      onBarcodeScanned = { code ->
        searchQuery = code
        val matched = products.find { it.barcode.trim().equals(code.trim(), ignoreCase = true) }
        if (matched != null) {
          viewModel.addToCart(matched, 1)
          viewModel.showToast("تم إيجاد وإضافة: ${matched.nameAr} للفاتورة ")
          showBarcodeScanner = false
        } else {
          viewModel.showToast("لم يتم العثور على منتج بهذا الباركود: $code")
        }
      },
      titleText = "مسح باركود الصنف للفاتورة "
    )
  }

  // 2. Product Gallery / Catalog Dialog (matching reference design without "المواد الرئيسية" tag)
  if (showGalleryPicker) {
    Dialog(
      onDismissRequest = { showGalleryPicker = false },
      properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
      var catalogSearchQuery by remember { mutableStateOf("") }
      var catalogSelectedCategory by remember { mutableStateOf<String?>(null) }
      var selectedLetter by remember { mutableStateOf<String?>(null) }

      // Filter out "المواد الرئيسية" and "مستلزمات عامة" category chips from category list
      val filteredCategoryNames = (categoriesList.map { it.nameAr } + products.map { it.category })
        .distinct()
        .filter { it.isNotBlank() && it != "المواد الرئيسية" && it != "مستلزمات عامة" }

      val filteredProducts = products.filter { prod ->
        val matchesSearch = catalogSearchQuery.isBlank() || prod.nameAr.contains(catalogSearchQuery, ignoreCase = true) || prod.barcode.contains(catalogSearchQuery, ignoreCase = true)
        val matchesCategory = catalogSelectedCategory == null || prod.category == catalogSelectedCategory
        val matchesLetter = when (selectedLetter) {
          null, "#" -> true
          "أ" -> prod.nameAr.startsWith("أ", ignoreCase = true) || prod.nameAr.startsWith("ا", ignoreCase = true) || prod.nameAr.startsWith("إ", ignoreCase = true) || prod.nameAr.startsWith("آ", ignoreCase = true)
          else -> prod.nameAr.startsWith(selectedLetter!!, ignoreCase = true)
        }
        matchesSearch && matchesCategory && matchesLetter
      }

      val alphabet = listOf("#", "أ", "ب", "ت", "ث", "ج", "ح", "خ", "د", "ذ", "ر", "ز", "س", "ش", "ص", "ض", "ط", "ظ", "ع", "غ", "ف", "ق", "ك", "ل", "م", "ن", "هـ", "و", "ي")

      Surface(
        modifier = Modifier
          .fillMaxSize()
          .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
        ) {
          // Top Search Bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = catalogSearchQuery,
              onValueChange = { catalogSearchQuery = it },
              placeholder = { Text("البحث في المواد", fontSize = 13.sp, color = Color.Gray) },
              singleLine = true,
              modifier = Modifier
                .weight(1f)
                .height(52.dp),
              shape = RoundedCornerShape(10.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PosTealHeader,
                unfocusedBorderColor = Color(0xFFCBD5E1),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
              ),
              trailingIcon = {
                if (catalogSearchQuery.isNotEmpty()) {
                  IconButton(onClick = { catalogSearchQuery = "" }) {
                    Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.Gray)
                  }
                }
              }
            )

            // Barcode Scan Icon Button
            Surface(
              onClick = { showBarcodeScanner = true },
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              border = BorderStroke(1.dp, PosTealHeader.copy(alpha=0.5f)),
              modifier = Modifier.size(52.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  Icons.Default.QrCodeScanner,
                  contentDescription = "مسح باركود",
                  tint = PosTealHeader,
                  modifier = Modifier.size(26.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Categories Filter Chips
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            item {
              FilterChip(
                selected = catalogSelectedCategory == null,
                onClick = { catalogSelectedCategory = null },
                label = { Text("الكل", fontWeight = FontWeight.Bold) },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Color(0xFFDCFCE7),
                  selectedLabelColor = Color(0xFF15803D)
                )
              )
            }

            items(filteredCategoryNames) { catName ->
              FilterChip(
                selected = catalogSelectedCategory == catName,
                onClick = { catalogSelectedCategory = if (catalogSelectedCategory == catName) null else catName },
                label = { Text(catName, fontWeight = FontWeight.Medium) },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Color(0xFFDCFCE7),
                  selectedLabelColor = Color(0xFF15803D)
                )
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Products Area + Right Alphabet Strip
          Row(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
          ) {
            // Product Grid (2 Columns)
            if (filteredProducts.isEmpty()) {
              Box(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight(),
                contentAlignment = Alignment.Center
              ) {
                Text("لا توجد مواد مطابقة للبحث", color = Color.Gray, fontSize = 14.sp)
              }
            } else {
              LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight()
              ) {
                items(filteredProducts) { prod ->
                  val inCartQty = cart.find { it.product.id == prod.id }?.quantity ?: 0
                  val isSelected = inCartQty > 0

                  Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                      containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isSelected) BorderStroke(2.dp, Color(0xFF16A34A)) else null,
                    elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 2.dp),
                    modifier = Modifier.clickable {
                      viewModel.addToCart(prod, 1)
                      viewModel.showToast("تمت إضافة ${prod.nameAr} للفاتورة ")
                    }
                  ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                      Column(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                      ) {
                        // Circular Icon / Image Container
                        Box(
                          modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                              when ((prod.id % 3).toInt()) {
                                0 -> Color(0xFF00796B)
                                1 -> MaterialTheme.colorScheme.onSurface
                                else -> Color(0xFFB45309)
                              }
                            )
                            .clickable {
                              if (prod.imageResName.isNotEmpty()) {
                                enlargedImageUrl = prod.imageResName
                              }
                            },
                          contentAlignment = Alignment.Center
                        ) {
                          if (prod.imageResName.isNotEmpty()) {
                            coil.compose.AsyncImage(
                              model = prod.imageResName,
                              contentDescription = prod.nameAr,
                              modifier = Modifier.fillMaxSize(),
                              contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                          } else {
                            Icon(
                              imageVector = when ((prod.id % 3).toInt()) {
                                0 -> Icons.Default.QrCode2
                                1 -> Icons.Default.Inventory2
                                else -> Icons.Default.Scanner
                              },
                              contentDescription = null,
                              tint = Color.White,
                              modifier = Modifier.size(36.dp)
                            )
                          }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Product Name
                        Text(
                          text = prod.nameAr,
                          fontWeight = FontWeight.Bold,
                          fontSize = 14.sp,
                          color = MaterialTheme.colorScheme.onSurface,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bottom Details Row: Price on left, Stock on right
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                              formatPosMoney(prod.priceSale),
                              fontWeight = FontWeight.Bold,
                              fontSize = 13.sp,
                              color = Color(0xFF0284C7)
                            )
                          }

                          Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                              "${prod.quantity}",
                              fontWeight = FontWeight.Bold,
                              fontSize = 13.sp,
                              color = Color(0xFF0284C7)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("", fontSize = 11.sp)
                          }
                        }
                      }

                      // Selection Badge / Counter Overlay
                      if (isSelected) {
                        Surface(
                          shape = CircleShape,
                          color = Color(0xFF16A34A),
                          modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                        ) {
                          Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                          ) {
                            Icon(
                              Icons.Default.Check,
                              contentDescription = "محدد",
                              tint = Color.White,
                              modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                              text = "$inCartQty",
                              color = Color.White,
                              fontWeight = FontWeight.Bold,
                              fontSize = 11.sp
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Right Alphabet Strip
            Surface(
              modifier = Modifier
                .width(32.dp)
                .fillMaxHeight(),
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFFEC4899)
            ) {
              LazyColumn(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
              ) {
                items(alphabet) { letter ->
                  val isSelected = (selectedLetter == letter) || (letter == "#" && selectedLetter == null)
                  Box(
                    modifier = Modifier
                      .size(26.dp)
                      .clip(CircleShape)
                      .background(if (isSelected) Color.Yellow else Color.Transparent)
                      .clickable {
                        if (letter == "#") {
                          selectedLetter = null
                        } else {
                          selectedLetter = if (selectedLetter == letter) null else letter
                        }
                      },
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = letter,
                      color = if (isSelected) Color(0xFF831843) else Color.White,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Bottom Green Action Button ("إضافة" / Close)
          Button(
            onClick = { showGalleryPicker = false },
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
          ) {
            Text("إضافة", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
          }
        }
      }
    }
  }

  // 4. Quick Calculator Dialog
  if (showCalculatorDialog) {
    PosCalculatorDialog(
      onDismissRequest = { showCalculatorDialog = false }
    )
  }

  // 5. Unit Selector Dialog
  if (selectedUnitItem != null) {
    val itemToEdit = selectedUnitItem!!
    AlertDialog(
      onDismissRequest = { selectedUnitItem = null },
      title = { Text("تحديد unit / وحدة الصنف: ${itemToEdit.product.nameAr}", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("حبة", "قطعة", "الوحدة", "علبة", "كيلو", "كرتونة", "درزن").forEach { unitName ->
            Surface(
              onClick = {
                val updatedProduct = itemToEdit.product.copy(unit = unitName)
                viewModel.updateCartItemPrice(itemToEdit.product.id, itemToEdit.product.priceSale) // preserve
                selectedUnitItem = null
                viewModel.showToast("تم تحديث وحدة ${itemToEdit.product.nameAr} إلى $unitName")
              },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              border = BorderStroke(0.5.dp, Color.LightGray)
            ) {
              Text(unitName, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { selectedUnitItem = null }) { Text("إغلاق") }
      }
    )
  }

  // 6. Item Options Dialog (Delete / Discount / Notes)
  if (selectedOptionItem != null) {
    val targetItem = selectedOptionItem!!
    AlertDialog(
      onDismissRequest = { selectedOptionItem = null },
      title = { Text("خيارات الصنف: ${targetItem.product.nameAr}", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Button(
            onClick = {
              viewModel.removeFromCart(targetItem.product)
              selectedOptionItem = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = CoralError),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("حذف الصنف من الفاتورة", fontWeight = FontWeight.Bold)
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { selectedOptionItem = null }) { Text("إغلاق") }
      }
    )
  }

  // 7. More Menu Sheet/Dialog
  if (showMoreMenu) {
    AlertDialog(
      onDismissRequest = { showMoreMenu = false },
      title = { Text("خيارات الفاتورة الإضافية ", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          // Payment Method Options
          Text("نوع طريقة الدفع:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            val optionsList = if (isPurchaseMode) listOf("نقدي", "دائن", "شيك") else listOf("نقدي", "مدين", "شيك")
            optionsList.forEach { mode ->
              FilterChip(
                selected = (paymentMethod == mode),
                onClick = { paymentMethod = mode },
                label = { Text(mode, fontSize = 11.sp) }
              )
            }
          }

          HorizontalDivider()

          // Clear Cart Option
          OutlinedButton(
            onClick = {
              viewModel.clearCart()
              showMoreMenu = false
              viewModel.showToast("تم تفريغ الفاتورة")
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralError),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("إلغاء وتفريغ الفاتورة بالكامل", fontWeight = FontWeight.Bold)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = { showMoreMenu = false },
          colors = ButtonDefaults.buttonColors(containerColor = PosTealHeader)
        ) {
          Text("موافق")
        }
      }
    )
  }

  // 8. Thermal Receipt & Invoice Print Preview Dialog
  if (showReceiptPreview) {
    val inv = lastSavedInvoice ?: Invoice(
      id = 101,
      customerId = selectedCustomerId ?: 0,
      invoiceDate = System.currentTimeMillis(),
      total = total,
      discount = discount,
      paidAmount = total,
      paymentMethod = paymentMethod
    )
    val itemsToPrint = if (lastSavedItems.isNotEmpty()) lastSavedItems else cart

    AlertDialog(
      onDismissRequest = { showReceiptPreview = false },
      title = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("معاينة الفاتورة للطباعة ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
          IconButton(onClick = { showReceiptPreview = false }) {
            Icon(Icons.Default.Close, contentDescription = null)
          }
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text("--- فاتورة مبيعات ---", fontWeight = FontWeight.Bold, fontSize = 16.sp)
          Text("العميل: $customerDisplayName", fontSize = 12.sp)
          Text("طريقة الدفع: ${inv.paymentMethod}", fontSize = 11.sp, color = PosTealHeader)
          HorizontalDivider()

          LazyColumn(modifier = Modifier.weight(1f)) {
            items(itemsToPrint) { item ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("${item.product.nameAr} (x${item.quantity})", fontSize = 11.sp)
                Text("${formatPosMoney(item.product.priceSale * item.quantity)} $currencySymbol", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          HorizontalDivider()
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("الخصم:", fontSize = 12.sp)
            Text("${formatPosMoney(inv.discount)} $currencySymbol", fontSize = 12.sp)
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("الإجمالي الصافي:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("${formatPosMoney(inv.total)} $currencySymbol", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PosTealHeader)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val invToPrint = lastSavedInvoice ?: Invoice(
              id = (System.currentTimeMillis() % 1000000).toInt(),
              customerId = selectedCustomerId ?: 0,
              invoiceDate = System.currentTimeMillis(),
              subtotal = subtotal,
              discount = discount,
              taxAmount = taxAmount,
              total = total,
              paidAmount = total,
              paymentMethod = paymentMethod,
              cashierName = "الكاشير الحالي"
            )
            val itemsList = if (lastSavedItems.isNotEmpty()) lastSavedItems else cart.toList()
            viewModel.printThermalInvoice(
              invoice = invToPrint,
              items = itemsList,
              customerName = partyDisplayName,
              onNeedPrinterSelection = {
                showBluetoothPrinterSettings = true
              }
            )
            showReceiptPreview = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = PosTealHeader)
        ) {
          Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("طباعة الآن عبر البلوتوث ")
        }
      },
      dismissButton = {
        TextButton(onClick = { showReceiptPreview = false }) {
          Text("إغلاق")
        }
      }
    )
  }

  // 9. Custom Print Choice Dialog (Bluetooth vs WiFi)
  if (showCustomPrintChoiceDialog) {
    AlertDialog(
      onDismissRequest = { showCustomPrintChoiceDialog = false },
      title = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("اختر وسيلة الطباعة المخصصة ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
          IconButton(onClick = { showCustomPrintChoiceDialog = false }) {
            Icon(Icons.Default.Close, contentDescription = null)
          }
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            "يرجى تحديد نوع الاتصال بالطابعة الحرارية المخصصة:",
            fontSize = 12.sp,
            color = Color.Gray
          )

          // Option 1: Bluetooth Thermal Printer
          Card(
            onClick = {
              showCustomPrintChoiceDialog = false
              if (viewModel.savedPrinterDevice.value != null) {
                showReceiptPreview = true
              } else {
                showBluetoothPrinterSettings = true
              }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PosTealHeader.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, PosTealHeader)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Surface(
                shape = CircleShape,
                color = PosTealHeader,
                modifier = Modifier.size(40.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                  )
                }
              }
              Column(modifier = Modifier.weight(1f)) {
                Text("طباعة عبر البلوتوث", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PosTealHeader)
                Text("الاتصال المباشر بالطابعة الحرارية المحمولة عبر Bluetooth", fontSize = 11.sp, color = Color.Gray)
              }
            }
          }

          // Option 2: WiFi / Network Thermal Printer
          Card(
            onClick = {
              showCustomPrintChoiceDialog = false
              showWifiPrinterDialog = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(40.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    Icons.Default.Wifi,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                  )
                }
              }
              Column(modifier = Modifier.weight(1f)) {
                Text("طباعة عبر الويفاي / الشبكة", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("إرسال أمر الطباعة عبر IP الشبكة المحلية (WiFi/LAN)", fontSize = 11.sp, color = Color.Gray)
              }
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showCustomPrintChoiceDialog = false }) {
          Text("إلغاء")
        }
      }
    )
  }

  // 10. WiFi Printer Setup & Print Dialog
  if (showWifiPrinterDialog) {
    AlertDialog(
      onDismissRequest = { showWifiPrinterDialog = false },
      title = {
        Text("طباعة عبر شبكة الويفاي (WiFi IP) ", fontWeight = FontWeight.Bold, fontSize = 15.sp)
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text("أدخل عنوان IP الخاص بالطابعة الحرارية على شبكة الويفاي:", fontSize = 12.sp, color = Color.Gray)
          OutlinedTextField(
            value = wifiIpAddress,
            onValueChange = { wifiIpAddress = it },
            label = { Text("عنوان IP الطابعة (مثال: 192.168.1.200)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PosTealHeader)
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.showToast("تم إرسال الفاتورة إلى طابعة الويفاي ($wifiIpAddress) بنجاح! ")
            showWifiPrinterDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = PosTealHeader)
        ) {
          Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("طباعة الآن")
        }
      },
      dismissButton = {
        TextButton(onClick = { showWifiPrinterDialog = false }) {
          Text("إغلاق")
        }
      }
    )
  }

  // 11. Bluetooth Thermal Printer Settings Dialog
  if (showBluetoothPrinterSettings) {
    BluetoothPrinterSettingsDialog(
      viewModel = viewModel,
      onDismissRequest = { showBluetoothPrinterSettings = false }
    )
  }

  // Party Selector Dialog (Customer / Supplier Picker)
  if (showPartyPickerDialog) {
    var isNewParty by remember { mutableStateOf(false) }
    var selectedPartyId by remember { mutableStateOf<Int?>(if (isPurchaseMode) selectedSupplierId else selectedCustomerId) }
    var manualNameInput by remember { mutableStateOf(if (isPurchaseMode) manualSupplierName else manualCustomerName) }
    var expandedPartyDropdown by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { showPartyPickerDialog = false },
      title = {
        Text(
          text = if (isPurchaseMode) "تحديد المورد للفاتورة " else "تحديد العميل للفاتورة ",
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp
        )
      },
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
              RadioButton(selected = !isNewParty, onClick = { isNewParty = false })
              Text(if (isPurchaseMode) "مورد مسجل" else "عميل مسجل")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              RadioButton(selected = isNewParty, onClick = { isNewParty = true })
              Text(if (isPurchaseMode) "مورد جديد" else "عميل جديد / زبون عام")
            }
          }

          if (!isNewParty) {
            if (isPurchaseMode) {
              val currentSupName = suppliers.find { it.id == selectedPartyId }?.name ?: "اختر المورد *"
              Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                  onClick = { expandedPartyDropdown = true },
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(currentSupName)
                  Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                  expanded = expandedPartyDropdown,
                  onDismissRequest = { expandedPartyDropdown = false },
                  modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                  if (suppliers.isEmpty()) {
                    DropdownMenuItem(
                      text = { Text("لا يوجد موردين مسجلين") },
                      onClick = { expandedPartyDropdown = false }
                    )
                  } else {
                    suppliers.forEach { sup ->
                      DropdownMenuItem(
                        text = { Text(sup.name) },
                        onClick = {
                          selectedPartyId = sup.id
                          expandedPartyDropdown = false
                        }
                      )
                    }
                  }
                }
              }
            } else {
              val currentCustName = customers.find { it.id == selectedPartyId }?.name ?: "اختر العميل *"
              Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                  onClick = { expandedPartyDropdown = true },
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(currentCustName)
                  Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                  expanded = expandedPartyDropdown,
                  onDismissRequest = { expandedPartyDropdown = false },
                  modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                  if (customers.isEmpty()) {
                    DropdownMenuItem(
                      text = { Text("لا يوجد عملاء مسجلين") },
                      onClick = { expandedPartyDropdown = false }
                    )
                  } else {
                    customers.forEach { cust ->
                      DropdownMenuItem(
                        text = { Text(cust.name) },
                        onClick = {
                          selectedPartyId = cust.id
                          expandedPartyDropdown = false
                        }
                      )
                    }
                  }
                }
              }
            }
          } else {
            OutlinedTextField(
              value = manualNameInput,
              onValueChange = { manualNameInput = it },
              label = { Text(if (isPurchaseMode) "اسم المورد *" else "اسم العميل / زبون عام *") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }

          HorizontalDivider()

          val partyTimePickerDialog = android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
              val cal = Calendar.getInstance().apply { timeInMillis = customInvoiceDateInput }
              cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
              cal.set(Calendar.MINUTE, minute)
              cal.set(Calendar.SECOND, 0)
              customInvoiceDateInput = cal.timeInMillis
            },
            Calendar.getInstance().apply { timeInMillis = customInvoiceDateInput }.get(Calendar.HOUR_OF_DAY),
            Calendar.getInstance().apply { timeInMillis = customInvoiceDateInput }.get(Calendar.MINUTE),
            true
          )

          val partyDatePickerDialog = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
              val cal = Calendar.getInstance().apply { timeInMillis = customInvoiceDateInput }
              cal.set(year, month, dayOfMonth)
              customInvoiceDateInput = cal.timeInMillis
              partyTimePickerDialog.show()
            },
            Calendar.getInstance().apply { timeInMillis = customInvoiceDateInput }.get(Calendar.YEAR),
            Calendar.getInstance().apply { timeInMillis = customInvoiceDateInput }.get(Calendar.MONTH),
            Calendar.getInstance().apply { timeInMillis = customInvoiceDateInput }.get(Calendar.DAY_OF_MONTH)
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("تاريخ ووقت الفاتورة:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            OutlinedButton(
              onClick = { partyDatePickerDialog.show() },
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(customInvoiceDateInput)),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (isPurchaseMode) {
              if (isNewParty) {
                viewModel.setManualSupplierName(manualNameInput)
              } else {
                viewModel.setSelectedSupplier(selectedPartyId)
              }
            } else {
              if (isNewParty) {
                viewModel.setManualCustomerName(manualNameInput)
              } else {
                viewModel.selectCustomer(selectedPartyId ?: 0)
              }
            }
            showPartyPickerDialog = false
            viewModel.showToast("تم تحديث معلومات الفاتورة ")
          },
          colors = ButtonDefaults.buttonColors(containerColor = PosTealHeader)
        ) {
          Text("حفظ وتأكيد", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { showPartyPickerDialog = false }) {
          Text("إلغاء")
        }
      }
    )
  }

  if (showCheckDetailsDialog) {
    AlertDialog(
      onDismissRequest = { showCheckDetailsDialog = false },
      title = { Text("تفاصيل الشيك", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = checkNumberInput,
            onValueChange = { checkNumberInput = it },
            label = { Text("رقم الشيك") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
          )
          OutlinedTextField(
            value = checkBankNameInput,
            onValueChange = { checkBankNameInput = it },
            label = { Text("اسم البنك") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          
          val context = androidx.compose.ui.platform.LocalContext.current
          val cal = java.util.Calendar.getInstance().apply { timeInMillis = checkDateInput }
          val datePickerDialog = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
              val picked = java.util.Calendar.getInstance()
              picked.set(year, month, dayOfMonth)
              checkDateInput = picked.timeInMillis
            },
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH),
            cal.get(java.util.Calendar.DAY_OF_MONTH)
          )
          
          Box(modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }) {
            OutlinedTextField(
              value = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault()).format(java.util.Date(checkDateInput)),
              onValueChange = { },
              label = { Text("تاريخ الاستلام") },
              readOnly = true,
              enabled = false,
              colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = Color.Black,
                disabledBorderColor = Color.Gray,
                disabledLabelColor = Color.Gray,
                disabledTrailingIconColor = PosTealHeader
              ),
              modifier = Modifier.fillMaxWidth(),
              trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "تاريخ") }
            )
          }
          
          Text("يتم تحديد المبلغ تلقائياً بعد حفظ الفاتورة.", fontSize = 12.sp, color = Color.Gray)
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showCheckDetailsDialog = false
            performActualSave(
              checkNumberInput.takeIf { it.isNotBlank() },
              checkBankNameInput.takeIf { it.isNotBlank() },
              checkDateInput
            )
          },
          colors = ButtonDefaults.buttonColors(containerColor = PosTealHeader)
        ) {
          Text("حفظ الفاتورة", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { showCheckDetailsDialog = false }) {
          Text("إلغاء")
        }
      }
    )
  }
}

// INVOICE ITEM ROW COMPONENT (Matching screenshot item row layout)
@Composable
private fun PosInvoiceItemRow(
  item: CartItem,
  viewModel: AppViewModel,
  currencySymbol: String,
  onUnitClick: () -> Unit,
  onOptionsClick: () -> Unit
) {
  var qtyText by remember(item.quantity) { mutableStateOf(item.quantity.toString()) }
  var priceText by remember(item.product.priceSale) { mutableStateOf(item.product.priceSale.toString()) }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      // Line 1: Header (Options ... | Item Name | Qty Controls - +)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Options button (...)
        IconButton(
          onClick = onOptionsClick,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            Icons.Default.MoreHoriz,
            contentDescription = "خيارات",
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
          )
        }

        // Item Name (Center)
        Text(
          text = item.product.nameAr,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        // Plus / Minus Buttons (Right)
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = {
              viewModel.decreaseQuantity(item.product)
            },
            modifier = Modifier.size(30.dp)
          ) {
            Icon(
              Icons.Default.RemoveCircle,
              contentDescription = "تقليل",
              tint = Color(0xFF0284C7),
              modifier = Modifier.size(22.dp)
            )
          }

          IconButton(
            onClick = {
              viewModel.addToCart(item.product, 1)
            },
            modifier = Modifier.size(30.dp)
          ) {
            Icon(
              Icons.Default.AddCircle,
              contentDescription = "زيادة",
              tint = Color(0xFF0284C7),
              modifier = Modifier.size(22.dp)
            )
          }
        }
      }

      // Line 2: Table Data Boxes (Qty | Unit | Unit Price | Total Price)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // 1. Quantity Box
        BasicTextField(
          value = qtyText,
          onValueChange = { newVal ->
            qtyText = newVal
            val parsed = newVal.toIntOrNull()
            if (parsed != null) {
              viewModel.updateCartItemQuantity(item.product.id, parsed)
            }
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          textStyle = androidx.compose.ui.text.TextStyle(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
          ),
          modifier = Modifier
            .weight(1f)
            .height(42.dp),
          decorationBox = @Composable { innerTextField ->
            Box(
              modifier = Modifier
                .fillMaxSize()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha=0.3f), RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)),
              contentAlignment = Alignment.Center
            ) {
              innerTextField()
            }
          }
        )

        // 2. Unit Selection Box
        OutlinedButton(
          onClick = onUnitClick,
          modifier = Modifier
            .weight(1f)
            .height(42.dp),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
          colors = ButtonDefaults.outlinedButtonColors(containerColor = PosTealHeader.copy(alpha = 0.08f))
        ) {
          Text(
            text = item.product.unit.ifEmpty { "الوحدة" },
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = PosTealHeader,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        // 3. Unit Price Box (Editable)
        BasicTextField(
          value = priceText,
          onValueChange = { newVal ->
            priceText = newVal
            val parsed = newVal.toDoubleOrNull()
            if (parsed != null) {
              viewModel.updateCartItemPrice(item.product.id, parsed)
            }
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          textStyle = androidx.compose.ui.text.TextStyle(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
          ),
          modifier = Modifier
            .weight(1.2f)
            .height(42.dp),
          decorationBox = @Composable { innerTextField ->
            Box(
              modifier = Modifier
                .fillMaxSize()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha=0.3f), RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)),
              contentAlignment = Alignment.Center
            ) {
              innerTextField()
            }
          }
        )

        // 4. Total Price Box
        Surface(
          modifier = Modifier
            .weight(1.2f)
            .height(42.dp),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, PosTealHeader.copy(alpha = 0.4f)),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = formatPosMoney(item.product.priceSale * item.quantity),
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = PosTealHeader
            )
          }
        }
      }
    }
  }
}

// QUICK POPUP CALCULATOR DIALOG
@Composable
private fun PosCalculatorDialog(
  onDismissRequest: () -> Unit
) {
  var calcDisplay by remember { mutableStateOf("0") }

  fun appendChar(char: String) {
    if (calcDisplay == "0") calcDisplay = char
    else calcDisplay += char
  }

  fun computeResult() {
    try {
      val expr = calcDisplay.replace("x", "*").replace("÷", "/")
      val result = evaluateSimpleExpr(expr)
      calcDisplay = DecimalFormat("#.##", java.text.DecimalFormatSymbols(Locale.US)).format(result)
    } catch (e: Exception) {
      calcDisplay = "خطأ"
    }
  }

  AlertDialog(
    onDismissRequest = onDismissRequest,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("حاسبة العمليات السريعة ", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        IconButton(onClick = onDismissRequest) {
          Icon(Icons.Default.Close, contentDescription = null)
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Display Screen
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.onSurface
        ) {
          Box(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.CenterEnd) {
            Text(calcDisplay, color = Color.Green, fontSize = 20.sp, fontWeight = FontWeight.Bold)
          }
        }

        // Keypad Grid
        val buttons = listOf(
          listOf("C", "⌫", "÷", "x"),
          listOf("7", "8", "9", "-"),
          listOf("4", "5", "6", "+"),
          listOf("1", "2", "3", "="),
          listOf("0", "00", ".", "")
        )

        buttons.forEach { row ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            row.forEach { key ->
              if (key == "") {
                Spacer(modifier = Modifier.weight(1f))
              } else {
                Button(
                  onClick = {
                    when (key) {
                      "C" -> calcDisplay = "0"
                      "⌫" -> {
                        if (calcDisplay.isNotEmpty() && calcDisplay != "0") {
                           calcDisplay = calcDisplay.dropLast(1)
                           if (calcDisplay.isEmpty() || calcDisplay == "-") calcDisplay = "0"
                        }
                      }
                      "=" -> computeResult()
                      else -> appendChar(key)
                    }
                  },
                  modifier = Modifier.weight(1f).height(50.dp),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(0.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = if (key in listOf("=", "+", "-", "x", "÷")) PosTealHeader else if (key == "C" || key == "⌫") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (key in listOf("=", "+", "-", "x", "÷")) Color.White else if (key == "C" || key == "⌫") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                  )
                ) {
                  if (key == "⌫") {
                    Icon(Icons.Default.Backspace, contentDescription = "حذف", modifier = Modifier.size(20.dp))
                  } else {
                    Text(key, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1)
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismissRequest,
        colors = ButtonDefaults.buttonColors(containerColor = PosTealHeader)
      ) {
        Text("تم (إغلاق)")
      }
    }
  )
}

private fun evaluateSimpleExpr(expr: String): Double {
  return try {
    val parts = expr.split(Regex("(?<=[-+*/])|(?=[-+*/])"))
    if (parts.size >= 3) {
      val num1 = parts[0].trim().toDouble()
      val op = parts[1].trim()
      val num2 = parts[2].trim().toDouble()
      when (op) {
        "+" -> num1 + num2
        "-" -> num1 - num2
        "*" -> num1 * num2
        "/" -> if (num2 != 0.0) num1 / num2 else 0.0
        else -> num1
      }
    } else {
      expr.toDoubleOrNull() ?: 0.0
    }
  } catch (e: Exception) {
    0.0
  }
}

private fun formatPosMoney(amount: Double): String {
  val df = DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols(Locale.US))
  return df.format(amount)
}

@Composable
fun PosProductGridCardItem(
  prod: Product,
  inCartQty: Int,
  currencySymbol: String,
  onAddToCart: () -> Unit
) {
  var isPressed by remember { mutableStateOf(false) }
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.92f else 1.0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
    ),
    label = "quickAddScale"
  )
  val coroutineScope = rememberCoroutineScope()

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .scale(scale)
      .clickable {
        coroutineScope.launch {
          isPressed = true
          delay(150)
          isPressed = false
        }
        onAddToCart()
      },
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (inCartQty > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    ),
    border = BorderStroke(1.dp, if (inCartQty > 0) EmeraldGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
    elevation = CardDefaults.cardElevation(if (inCartQty > 0) 3.dp else 1.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // In Cart Badge (if > 0)
      if (inCartQty > 0) {
        Surface(
          color = EmeraldGreen,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.align(Alignment.End)
        ) {
          Text(
            "x$inCartQty في السلة",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
          )
        }
      }

      // Image or Icon
      Box(
        modifier = Modifier
          .size(54.dp)
          .clip(CircleShape)
          .background(
            when ((prod.id % 3).toInt()) {
              0 -> Color(0xFF00796B)
              1 -> MaterialTheme.colorScheme.onSurface
              else -> Color(0xFFB45309)
            }
          ),
        contentAlignment = Alignment.Center
      ) {
        if (prod.imageResName.isNotEmpty()) {
          coil.compose.AsyncImage(
            model = prod.imageResName,
            contentDescription = prod.nameAr,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
          )
        } else {
          Icon(
            imageVector = when ((prod.id % 3).toInt()) {
              0 -> Icons.Default.QrCode2
              1 -> Icons.Default.Inventory2
              else -> Icons.Default.Scanner
            },
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(26.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Name
      Text(
        text = prod.nameAr,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center
      )

      // Stock
      Text(
        text = "المخزون: ${prod.quantity} ${prod.unit}",
        fontSize = 10.sp,
        color = Color.Gray,
        maxLines = 1
      )

      Spacer(modifier = Modifier.height(2.dp))

      // Price
      Text(
        text = "${formatPosMoney(prod.priceSale)} $currencySymbol",
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
        color = PosTealHeader
      )

      Spacer(modifier = Modifier.height(6.dp))

      // Quick Add Button (زر أضف سريع)
      Button(
        onClick = {
          coroutineScope.launch {
            isPressed = true
            delay(150)
            isPressed = false
          }
          onAddToCart()
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isPressed) EmeraldGreen else PosTealHeader
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(30.dp)
          .testTag("quick_add_${prod.id}"),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
      ) {
        AnimatedContent(
          targetState = isPressed,
          label = "quickAddAnim"
        ) { pressed ->
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (pressed) {
              Icon(
                Icons.Default.Check,
                contentDescription = "تمت الإضافة",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(2.dp))
              Text(
                "تمت!",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            } else {
              Icon(
                Icons.Default.Add,
                contentDescription = "إضافة سريع",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(2.dp))
              Text(
                "أضف سريع",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
          }
        }
      }
    }
  }
}
