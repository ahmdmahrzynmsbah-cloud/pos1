package com.example.ui

import androidx.activity.compose.BackHandler
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
import com.example.data.isReturn
import com.example.data.Product
import com.example.data.Supplier
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

val PosTealHeader = Color(0xFF00796B)
val PosTealDark = Color(0xFF004D40)
val PosBgLight = Color(0xFFF1F5F9) // Soft Slate, gentle on the eyes for long POS usage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceFormScreen(
  viewModel: AppViewModel,
  onBackClick: () -> Unit,
  isPurchaseMode: Boolean = false,
  onNavigateToStatements: (() -> Unit)? = null
) {
  PosSalesInvoiceScreen(
    viewModel = viewModel,
    onBackClick = onBackClick,
    isPurchaseMode = isPurchaseMode,
    onNavigateToStatements = onNavigateToStatements
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosSalesInvoiceScreen(
  viewModel: AppViewModel,
  onBackClick: () -> Unit,
  isPurchaseMode: Boolean = false,
  isReturnMode: Boolean = false,
  isDraftMode: Boolean = false,
  onNavigateToStatements: (() -> Unit)? = null
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

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
  val damagedAccState by viewModel.damagedCreditAccount.collectAsState()
  val storeName by viewModel.storeName.collectAsState()
  val storePhone by viewModel.storePhone.collectAsState("")
  val storeVatNumber by viewModel.storeVatNumber.collectAsState("")
  val currentBranch by viewModel.currentBranch.collectAsState("الفرع الرئيسي")
  val currentUser by viewModel.currentUser.collectAsState()

  var showWhatsAppPhonePromptDialog by remember { mutableStateOf(false) }
  var inputWhatsAppPhone by remember { mutableStateOf("") }
  var pendingShareAction by remember { mutableStateOf<((String) -> Unit)?>(null) }

  val selectedSupplierFromVm by viewModel.selectedSupplier.collectAsState()
  val selectedCustomerFromVm by viewModel.selectedCustomer.collectAsState()

  val selectedCustomer = customers.find { it.id == selectedCustomerId } ?: selectedCustomerFromVm
  val selectedSupplier = suppliers.find { it.id == selectedSupplierId } ?: selectedSupplierFromVm

  val partyDisplayName = when {
    selectedSupplier != null && selectedSupplier.name.isNotBlank() -> selectedSupplier.name
    selectedCustomer != null && selectedCustomer.name.isNotBlank() -> selectedCustomer.name
    manualSupplierName.isNotBlank() -> manualSupplierName
    manualCustomerName.isNotBlank() -> manualCustomerName
    isPurchaseMode -> "مورد عام"
    else -> "زبون عام"
  }

  val partyRoleTag = when {
    selectedSupplier != null -> "مورد"
    selectedCustomer != null -> "عميل"
    manualSupplierName.isNotBlank() -> "مورد يدوي"
    manualCustomerName.isNotBlank() -> "عميل يدوي"
    isPurchaseMode -> "مورد عام"
    else -> "زبون عام"
  }

  // Local State
  var searchQuery by remember { mutableStateOf("") }
  var posViewMode by remember { mutableStateOf("table") } // Invoice Items Table
  var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
  var showAutoDropdown by remember { mutableStateOf(false) }
  val setupPayment = viewModel.setupPaymentMethod.collectAsState().value
  var paymentMethod by remember(setupPayment) { mutableStateOf(if (setupPayment.isNotBlank()) setupPayment else "نقدي") } // "نقدي", "آجل", "بطاقة", "شيك"
  val setupCustomDateVal = viewModel.setupCustomDate.collectAsState().value
  var customInvoiceDateInput by remember(setupCustomDateVal) { mutableStateOf(setupCustomDateVal ?: System.currentTimeMillis()) }
  var isReturnCurrent by remember(isReturnMode) { mutableStateOf(isReturnMode) }
  var currentDraftMode by remember(isDraftMode) { mutableStateOf(isDraftMode) }
  val isDarkSales = !isPurchaseMode && !isReturnCurrent
  val invoiceAccentColor = if (isReturnCurrent) Color(0xFFDC2626) else if (isPurchaseMode) PosTealHeader else Color(0xFF0284C7)
  BackHandler {
    onBackClick()
  }
  
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
        clearCartAfter = true,
        isReturnMode = isReturnCurrent,
        isDraft = currentDraftMode,
        customInvoiceDate = customInvoiceDateInput,
        onSuccess = { createdInvoice ->
          lastSavedInvoice = createdInvoice
          lastSavedItems = cart.toList()
          viewModel.clearCart()
          viewModel.selectCustomer(null)
          viewModel.setManualCustomerName("")
          viewModel.setSelectedSupplier(null)
          viewModel.setManualSupplierName("")
          discountInputText = "0"
          searchQuery = ""
          checkNumberInput = ""
          checkBankNameInput = ""
          checkDateInput = System.currentTimeMillis()
          paymentMethod = "نقدي"
          val successMsg = when {
            currentDraftMode -> "تم حفظ مسودة الشراء بنجاح "
            isReturnCurrent -> "تم حفظ فاتورة مرتجع مشتريات وخصم البضاعة من المخزن بنجاح وتظهر الآن كمرتجع! "
            else -> "تم حفظ فاتورة الشراء وتوريد البضاعة للمخزن بنجاح وتظهر الآن في الفواتير السابقة! "
          }
          viewModel.showToast(successMsg)
        }
      )
    } else {
      val effectivePaid = if (paymentMethod == "آجل" || paymentMethod == "مدين" || paymentMethod == "دائن") 0.0 else total

      viewModel.performCheckout(
        paymentMethod = paymentMethod,
        paidAmount = effectivePaid,
        changeAmount = 0.0,
        checkNumber = chkNum,
        checkBankName = chkBank,
        checkDate = chkDate,
        clearCartAfter = true,
        isReturnMode = isReturnCurrent,
        isDraft = currentDraftMode,
        customInvoiceDate = customInvoiceDateInput,
        onSuccess = { createdInvoice ->
          lastSavedInvoice = createdInvoice
          lastSavedItems = cart.toList()
          viewModel.clearCart()
          viewModel.selectCustomer(null)
          viewModel.setManualCustomerName("")
          viewModel.setSelectedSupplier(null)
          viewModel.setManualSupplierName("")
          discountInputText = "0"
          searchQuery = ""
          checkNumberInput = ""
          checkBankNameInput = ""
          checkDateInput = System.currentTimeMillis()
          paymentMethod = "نقدي"
          val successMsg = when {
            currentDraftMode -> "تم حفظ مسودة المبيع بنجاح "
            isReturnCurrent -> "تم حفظ فاتورة مرتجع مبيعات وإرجاع البضاعة للمخزن بنجاح وتظهر الآن كمرتجع! "
            else -> "تم حفظ الفاتورة بنجاح وتظهر الآن في قائمة الفواتير السابقة! "
          }
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
        color = if (isDarkSales) Color(0xFF18181B) else MaterialTheme.colorScheme.surface,
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
            // Save Invoice Button
            Button(
              onClick = { onSaveInvoice() },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (currentDraftMode) Color(0xFFD97706)
                                 else if (isDarkSales) Color(0xFF27272A)
                                 else invoiceAccentColor
              ),
              border = if (isDarkSales && !currentDraftMode) BorderStroke(1.dp, Color(0xFF52525B)) else null,
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Icon(
                if (isReturnCurrent) Icons.Default.PublishedWithChanges
                else if (currentDraftMode) Icons.Default.EditNote
                else Icons.Default.Save,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                when {
                  damagedAccState != null -> "حفظ فاتورة تالف"
                  isReturnCurrent && isPurchaseMode -> "حفظ مرتجع شراء"
                  isReturnCurrent -> "حفظ مرتجع مبيعات"
                  currentDraftMode && isPurchaseMode -> "حفظ مسودة شراء"
                  currentDraftMode -> "حفظ مسودة مبيع"
                  isPurchaseMode -> "حفظ فاتورة شراء"
                  else -> "حفظ الفاتورة"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.White
              )
            }

            // Save & Share Button (حفظ ومشاركة - WhatsApp Direct with PDF Invoice)
            OutlinedButton(
              onClick = {
                val hasCart = cart.isNotEmpty()
                val hasSaved = lastSavedItems.isNotEmpty() && lastSavedInvoice != null

                if (!hasCart && !hasSaved) {
                  val msg = if (isPurchaseMode) "السلة فارغة للحفظ والمشاركة! يرجى إضافة عناصر أولاً " else "السلة فارغة للحفظ والمشاركة! يرجى إضافة عناصر أولاً "
                  viewModel.showToast(msg)
                } else {
                  val targetPhone = if (isPurchaseMode) (selectedSupplier?.phone ?: "") else (selectedCustomer?.phone ?: "")
                  val cleanPhone = targetPhone.replace("+", "").replace(" ", "").filter { it.isDigit() }

                  val executeSaveAndShare: (String) -> Unit = { phoneToSend ->
                    if (hasCart) {
                      val snapshotCart = cart.toList()
                      val snapshotTotal = total

                      if (isPurchaseMode) {
                        viewModel.performPurchaseCheckout(
                          paymentMethod = paymentMethod,
                          paidAmount = snapshotTotal,
                          checkNumber = checkNumberInput.ifBlank { null },
                          checkBankName = checkBankNameInput.ifBlank { null },
                          checkDate = checkDateInput,
                          clearCartAfter = true,
                          isReturnMode = isReturnCurrent,
                          isDraft = currentDraftMode,
                          customInvoiceDate = customInvoiceDateInput,
                          onSuccess = { createdInvoice ->
                            lastSavedInvoice = createdInvoice
                            lastSavedItems = snapshotCart
                            viewModel.clearCart()
                            viewModel.selectCustomer(null)
                            viewModel.setManualCustomerName("")
                            viewModel.setSelectedSupplier(null)
                            viewModel.setManualSupplierName("")
                            discountInputText = "0"
                            searchQuery = ""
                            checkNumberInput = ""
                            checkBankNameInput = ""
                            checkDateInput = System.currentTimeMillis()
                            paymentMethod = "نقدي"
                            viewModel.showToast("تم حفظ الفاتورة بنجاح وجاري فتح واتساب للمشاركة...")

                            coroutineScope.launch {
                              val invTypeHeader = when {
                                createdInvoice.isReturn && createdInvoice.isPurchase -> "فاتورة مرتجع مشتريات"
                                createdInvoice.isReturn -> "فاتورة مرتجع مبيعات"
                                createdInvoice.isDraft && createdInvoice.isPurchase -> "مسودة شراء"
                                createdInvoice.isDraft -> "مسودة مبيع"
                                createdInvoice.isPurchase -> "فاتورة مشتريات وتوريد"
                                else -> "فاتورة مبيعات"
                              }
                              val pdfItems = snapshotCart.map { item ->
                                PdfGenerator.InvoiceItemPdfData(
                                  name = item.product.nameAr,
                                  quantity = item.quantity.toDouble(),
                                  unitPrice = item.product.priceSale,
                                  total = item.product.priceSale * item.quantity.toDouble()
                                )
                              }
                              val pdfFile = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                PdfGenerator.generateInvoicePdfFile(
                                  context = context,
                                  storeName = storeName,
                                  storePhone = storePhone,
                                  storeVatNumber = storeVatNumber,
                                  branchName = currentBranch,
                                  docTitle = invTypeHeader,
                                  invoiceNumber = createdInvoice.id.toString(),
                                  invoiceDate = createdInvoice.invoiceDate,
                                  cashierName = createdInvoice.cashierName.ifBlank { currentUser },
                                  partyName = partyDisplayName,
                                  partyPhone = phoneToSend.ifBlank { targetPhone },
                                  paymentMethod = createdInvoice.paymentMethod,
                                  items = pdfItems,
                                  subtotal = createdInvoice.subtotal,
                                  discount = createdInvoice.discount,
                                  taxAmount = createdInvoice.taxAmount,
                                  total = createdInvoice.total,
                                  currencySymbol = currencySymbol
                                )
                              }
                              if (pdfFile != null) {
                                PdfGenerator.shareInvoicePdfToWhatsApp(
                                  context = context,
                                  pdfFile = pdfFile,
                                  phoneNumber = phoneToSend,
                                  messageText = "مرحباً $partyDisplayName، تم حفظ الفاتورة رقم #${createdInvoice.id} من $storeName ومرفق لكم نسخة PDF. شكراً لتعاملكم معنا.",
                                  onSuccess = { msg -> viewModel.showToast(msg) },
                                  onError = { err -> viewModel.showToast(err) }
                                )
                              } else {
                                viewModel.showToast("حدث خطأ أثناء إنشاء ملف الفاتورة PDF")
                              }
                            }
                          }
                        )
                      } else {
                        val effectivePaid = if (paymentMethod == "آجل" || paymentMethod == "مدين" || paymentMethod == "دائن") 0.0 else snapshotTotal

                        viewModel.performCheckout(
                          paymentMethod = paymentMethod,
                          paidAmount = effectivePaid,
                          changeAmount = 0.0,
                          checkNumber = checkNumberInput.ifBlank { null },
                          checkBankName = checkBankNameInput.ifBlank { null },
                          checkDate = checkDateInput,
                          clearCartAfter = true,
                          isReturnMode = isReturnCurrent,
                          isDraft = currentDraftMode,
                          customInvoiceDate = customInvoiceDateInput,
                          onSuccess = { createdInvoice ->
                            lastSavedInvoice = createdInvoice
                            lastSavedItems = snapshotCart
                            viewModel.clearCart()
                            viewModel.selectCustomer(null)
                            viewModel.setManualCustomerName("")
                            viewModel.setSelectedSupplier(null)
                            viewModel.setManualSupplierName("")
                            discountInputText = "0"
                            searchQuery = ""
                            checkNumberInput = ""
                            checkBankNameInput = ""
                            checkDateInput = System.currentTimeMillis()
                            paymentMethod = "نقدي"
                            viewModel.showToast("تم حفظ الفاتورة بنجاح وجاري فتح واتساب للمشاركة...")

                            coroutineScope.launch {
                              val invTypeHeader = when {
                                createdInvoice.isReturn && createdInvoice.isPurchase -> "فاتورة مرتجع مشتريات"
                                createdInvoice.isReturn -> "فاتورة مرتجع مبيعات"
                                createdInvoice.isDraft && createdInvoice.isPurchase -> "مسودة شراء"
                                createdInvoice.isDraft -> "مسودة مبيع"
                                createdInvoice.isPurchase -> "فاتورة مشتريات وتوريد"
                                else -> "فاتورة مبيعات"
                              }
                              val pdfItems = snapshotCart.map { item ->
                                PdfGenerator.InvoiceItemPdfData(
                                  name = item.product.nameAr,
                                  quantity = item.quantity.toDouble(),
                                  unitPrice = item.product.priceSale,
                                  total = item.product.priceSale * item.quantity.toDouble()
                                )
                              }
                              val pdfFile = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                PdfGenerator.generateInvoicePdfFile(
                                  context = context,
                                  storeName = storeName,
                                  storePhone = storePhone,
                                  storeVatNumber = storeVatNumber,
                                  branchName = currentBranch,
                                  docTitle = invTypeHeader,
                                  invoiceNumber = createdInvoice.id.toString(),
                                  invoiceDate = createdInvoice.invoiceDate,
                                  cashierName = createdInvoice.cashierName.ifBlank { currentUser },
                                  partyName = partyDisplayName,
                                  partyPhone = phoneToSend.ifBlank { targetPhone },
                                  paymentMethod = createdInvoice.paymentMethod,
                                  items = pdfItems,
                                  subtotal = createdInvoice.subtotal,
                                  discount = createdInvoice.discount,
                                  taxAmount = createdInvoice.taxAmount,
                                  total = createdInvoice.total,
                                  currencySymbol = currencySymbol
                                )
                              }
                              if (pdfFile != null) {
                                PdfGenerator.shareInvoicePdfToWhatsApp(
                                  context = context,
                                  pdfFile = pdfFile,
                                  phoneNumber = phoneToSend,
                                  messageText = "مرحباً $partyDisplayName، تم حفظ الفاتورة رقم #${createdInvoice.id} من $storeName ومرفق لكم نسخة PDF. شكراً لتعاملكم معنا.",
                                  onSuccess = { msg -> viewModel.showToast(msg) },
                                  onError = { err -> viewModel.showToast(err) }
                                )
                              } else {
                                viewModel.showToast("حدث خطأ أثناء إنشاء ملف الفاتورة PDF")
                              }
                            }
                          }
                        )
                      }
                    } else if (hasSaved && lastSavedInvoice != null) {
                      val savedInv = lastSavedInvoice!!
                      val savedItems = lastSavedItems
                      coroutineScope.launch {
                        viewModel.showToast("جاري تجهيز الفاتورة المحفوظة PDF والتحويل إلى واتساب...")
                        val invTypeHeader = when {
                          savedInv.isReturn && savedInv.isPurchase -> "فاتورة مرتجع مشتريات"
                          savedInv.isReturn -> "فاتورة مرتجع مبيعات"
                          savedInv.isDraft && savedInv.isPurchase -> "مسودة شراء"
                          savedInv.isDraft -> "مسودة مبيع"
                          savedInv.isPurchase -> "فاتورة مشتريات وتوريد"
                          else -> "فاتورة مبيعات"
                        }
                        val pdfItems = savedItems.map { item ->
                          PdfGenerator.InvoiceItemPdfData(
                            name = item.product.nameAr,
                            quantity = item.quantity.toDouble(),
                            unitPrice = item.product.priceSale,
                            total = item.product.priceSale * item.quantity.toDouble()
                          )
                        }
                        val pdfFile = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                          PdfGenerator.generateInvoicePdfFile(
                            context = context,
                            storeName = storeName,
                            storePhone = storePhone,
                            storeVatNumber = storeVatNumber,
                            branchName = currentBranch,
                            docTitle = invTypeHeader,
                            invoiceNumber = savedInv.id.toString(),
                            invoiceDate = savedInv.invoiceDate,
                            cashierName = savedInv.cashierName.ifBlank { currentUser },
                            partyName = partyDisplayName,
                            partyPhone = phoneToSend.ifBlank { targetPhone },
                            paymentMethod = savedInv.paymentMethod,
                            items = pdfItems,
                            subtotal = savedInv.subtotal,
                            discount = savedInv.discount,
                            taxAmount = savedInv.taxAmount,
                            total = savedInv.total,
                            currencySymbol = currencySymbol
                          )
                        }
                        if (pdfFile != null) {
                          PdfGenerator.shareInvoicePdfToWhatsApp(
                            context = context,
                            pdfFile = pdfFile,
                            phoneNumber = phoneToSend,
                            messageText = "مرحباً $partyDisplayName، مرفق فاتورتكم رقم #${savedInv.id} من $storeName بصيغة PDF. شكراً لتعاملكم معنا.",
                            onSuccess = { msg -> viewModel.showToast(msg) },
                            onError = { err -> viewModel.showToast(err) }
                          )
                        } else {
                          viewModel.showToast("حدث خطأ أثناء إنشاء ملف الفاتورة PDF")
                        }
                      }
                    }
                  }

                  if (cleanPhone.isNotBlank()) {
                    executeSaveAndShare(cleanPhone)
                  } else {
                    inputWhatsAppPhone = ""
                    pendingShareAction = { enteredPhone -> executeSaveAndShare(enteredPhone) }
                    showWhatsAppPhonePromptDialog = true
                  }
                }
              },
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, if (isDarkSales) Color(0xFF52525B) else Color(0xFF25D366)),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
              Icon(
                Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF25D366)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                "حفظ ومشاركة",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkSales) Color.White else MaterialTheme.colorScheme.onSurface
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
              border = BorderStroke(1.dp, if (isDarkSales) Color(0xFF52525B) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
              Icon(
                Icons.Default.Print,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isDarkSales) Color.White else invoiceAccentColor
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                "طباعة مخصصة",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkSales) Color.White else MaterialTheme.colorScheme.onSurface
              )
            }

          }

          // Left Actions: Back Button
          IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "رجوع",
              tint = if (isDarkSales) Color.White else Color(0xFF64748B),
              modifier = Modifier.size(24.dp)
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
        .imePadding()
        .background(if (isDarkSales) Color(0xFF09090B) else MaterialTheme.colorScheme.background)
    ) {
      // Visual Alert Banner if Damaged Mode is Active
      if (damagedAccState != null) {
        Surface(
          color = Color(0xFFEF4444).copy(alpha = 0.15f),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
            Text(
              text = "تنبيه: يتم إعداد (فاتورة تالف) - سيتم خصم المبلغ من ($damagedAccState) وترحيله تلقائياً للمصاريف في الميزان وخصم الأصناف من المخزن.",
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFEF4444)
            )
          }
        }
      }

      // Visual Alert Banner if Return Mode is Active
      if (isReturnCurrent) {
        Surface(
          color = Color(0xFFDC2626).copy(alpha = 0.12f),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.4f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.PublishedWithChanges, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
            Text(
              text = if (isPurchaseMode) "تنبيه: يتم إعداد (فاتورة مرتجع مشتريات) - سيتم خصم الكميات من المخزن وتسجيلها كمرتجع للمورد."
                     else "تنبيه: يتم إعداد (فاتورة مرتجع مبيعات) - سيتم استرجاع الكميات للمخزن وتسجيل الفاتورة كمرتجع مبيعات للعميل.",
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFDC2626)
            )
          }
        }
      }

      // Visual Alert Banner if Draft Mode is Active
      if (currentDraftMode) {
        Surface(
          color = Color(0xFFD97706).copy(alpha = 0.15f),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.5f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.EditNote, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
            Text(
              text = if (isPurchaseMode) "أنت في وضع (مسودة شراء) - لن يتم توريد البضاعة للمخزن حتى اعتماد الفاتورة نهائياً."
                     else "أنت في وضع (مسودة مبيع) - لن يتم خصم البضاعة من المخزن حتى اعتماد الفاتورة نهائياً.",
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFF59E0B)
            )
          }
        }
      }

      val invoiceTimePickerDialog = remember(customInvoiceDateInput) {
        android.app.TimePickerDialog(
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
      }

      val invoiceDatePickerDialog = remember(customInvoiceDateInput) {
        android.app.DatePickerDialog(
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
      }

      // ITEM SEARCH & SCANNER INPUT BAR
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDarkSales) Color(0xFF18181B) else MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isDarkSales) Color(0xFF27272A) else Color(0xFF0D9488).copy(alpha = 0.35f))
      ) {
        Box(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            // Right Icon: Gallery / Photo Picker Icon
            IconButton(
              onClick = { showGalleryPicker = true },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                Icons.Default.Image,
                contentDescription = "كتالوج الصور",
                tint = if (isDarkSales) Color.White else Color(0xFF0D9488),
                modifier = Modifier.size(22.dp)
              )
            }

            // Search Input Text Field (Center)
            BasicTextField(
              value = searchQuery,
              onValueChange = {
                searchQuery = it
                showAutoDropdown = it.isNotBlank()
              },
              singleLine = true,
              textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 12.sp,
                color = if (isDarkSales) Color.White else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
              ),
              modifier = Modifier.weight(1f),
              decorationBox = @Composable { innerTextField ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDarkSales) Color(0xFF27272A) else MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .border(1.dp, if (isDarkSales) Color(0xFF3F3F46) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center
                ) {
                  Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (searchQuery.isEmpty()) {
                      Text(
                        "ادخل اسم المادة أو الباركود لاضافتها للفاتورة",
                        fontSize = 11.5.sp,
                        color = if (isDarkSales) Color(0xFFA1A1AA) else Color(0xFF475569),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
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
                      modifier = Modifier.size(18.dp)
                    ) {
                      Icon(
                        Icons.Default.Clear,
                        contentDescription = "مسح",
                        tint = if (isDarkSales) Color(0xFFA1A1AA) else Color.Gray,
                        modifier = Modifier.size(14.dp)
                      )
                    }
                  }
                }
              }
            )

            // Left Icon: Barcode Scanner Icon
            IconButton(
              onClick = { showBarcodeScanner = true },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                Icons.Default.QrCodeScanner,
                contentDescription = "قارئ الباركود",
                tint = if (isDarkSales) Color(0xFF38BDF8) else Color(0xFF0D9488),
                modifier = Modifier.size(24.dp)
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
      // TABLE HEADER BANNER
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 2.dp),
        color = if (isDarkSales) Color(0xFF18181B) else invoiceAccentColor,
        border = BorderStroke(1.dp, if (isDarkSales) Color(0xFF27272A) else Color.Transparent),
        shape = RoundedCornerShape(8.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp, horizontal = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            "الكمية",
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
          )
          Text(
            "الوحدة",
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
          )
          Text(
            "السعر",
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1.2f)
          )
          Text(
            "الإجمالي",
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1.2f)
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

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
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              Icons.Default.ShoppingCart,
              contentDescription = null,
              modifier = Modifier.size(56.dp),
              tint = if (isDarkSales) Color(0xFF52525B) else Color(0xFFCBD5E1)
            )
            Text(
              if (currentDraftMode) "المسودة فارغة، ابدأ بإضافة الأصناف أعلاه" else "الفاتورة فارغة، ابدأ بإضافة الأصناف أعلاه",
              color = if (isDarkSales) Color(0xFFA1A1AA) else Color(0xFF64748B),
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
              onOptionsClick = { selectedOptionItem = item },
              isDark = isDarkSales
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // BOTTOM SUMMARY & ACTION FOOTER (Fixed at Bottom)
      Surface(
        color = if (isDarkSales) Color(0xFF18181B) else MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Row 1: Top Summary Header Banner Pill & Date Selector (Matching reference screenshot)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Summary Header Banner Pill (Right side in RTL)
            Surface(
              modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .clickable { showPartyPickerDialog = true },
              color = if (isDarkSales) Color(0xFF27272A) else invoiceAccentColor,
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, if (isDarkSales) Color(0xFF3F3F46) else Color.Transparent),
              shadowElevation = 1.dp
            ) {
              Row(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                // In RTL: Right side is Text:
                val displayPartyText = when {
                  isPurchaseMode && selectedCustomer != null -> "${selectedCustomer.name} (عميل)"
                  isPurchaseMode && selectedSupplier != null -> "${selectedSupplier.name} (مورد)"
                  !isPurchaseMode && selectedSupplier != null -> "${selectedSupplier.name} (مورد)"
                  !isPurchaseMode && selectedCustomer != null -> "${selectedCustomer.name} (عميل)"
                  manualCustomerName.isNotBlank() -> "$manualCustomerName (عميل يدوي)"
                  manualSupplierName.isNotBlank() -> "$manualSupplierName (مورد يدوي)"
                  isPurchaseMode -> "اختر المورد أو العميل..."
                  else -> "اختر العميل أو المورد..."
                }
                Text(
                  text = "المجموع [ ${formatPosMoney(subtotal)} ] ${if (currentDraftMode) "مسودة " else ""}${if (isPurchaseMode) "شراء" else "مبيع"} $paymentMethod - $displayPartyText",
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.5.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                // Statement shortcut button for whichever party (Customer or Supplier) is active
                if (selectedCustomer != null) {
                  Surface(
                    onClick = {
                      viewModel.navigateToCustomerStatement(selectedCustomer)
                      onNavigateToStatements?.invoke()
                    },
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.22f),
                    modifier = Modifier.padding(horizontal = 2.dp)
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                      horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                      Icon(Icons.Default.ReceiptLong, contentDescription = "كشف حساب", tint = Color.White, modifier = Modifier.size(13.dp))
                      Text("كشف حساب عميل", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                } else if (selectedSupplier != null) {
                  Surface(
                    onClick = {
                      viewModel.navigateToSupplierStatement(selectedSupplier)
                      onNavigateToStatements?.invoke()
                    },
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.22f),
                    modifier = Modifier.padding(horizontal = 2.dp)
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                      horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                      Icon(Icons.Default.ReceiptLong, contentDescription = "كشف حساب", tint = Color.White, modifier = Modifier.size(13.dp))
                      Text("كشف حساب مورد", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
                Spacer(modifier = Modifier.width(2.dp))
                // Left side in RTL (adjacent to Date chip) is pencil edit icon:
                Icon(
                  Icons.Default.Edit,
                  contentDescription = "تعديل الطرف والدفع",
                  tint = Color.White,
                  modifier = Modifier.size(14.dp)
                )
              }
            }

            // Date Selector Chip (Left side in RTL)
            Surface(
              modifier = Modifier
                .height(38.dp)
                .clickable { invoiceDatePickerDialog.show() },
              color = if (isDarkSales) Color(0xFF27272A) else Color(0xFFEFF6FF),
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.2.dp, if (isDarkSales) Color(0xFF3F3F46) else Color(0xFF2563EB))
            ) {
              Row(
                modifier = Modifier
                  .fillMaxHeight()
                  .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                // In RTL: Icon is on the right, date text is on the left
                Icon(
                  Icons.Default.DateRange,
                  contentDescription = "تاريخ الفاتورة",
                  tint = if (isDarkSales) Color.White else Color(0xFF2563EB),
                  modifier = Modifier.size(15.dp)
                )
                Text(
                  text = SimpleDateFormat("HH:mm yyyy/MM/dd", Locale.getDefault()).format(Date(customInvoiceDateInput)),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isDarkSales) Color.White else Color(0xFF2563EB)
                )
              }
            }
          }

          // Prominent Dedicated Customer / Supplier Selection Bar
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showPartyPickerDialog = true },
            color = if (isDarkSales) Color(0xFF1E293B) else Color(0xFFE0F2FE),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, if (isDarkSales) Color(0xFF334155) else Color(0xFF38BDF8))
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = when {
                    selectedSupplier != null -> Icons.Default.Business
                    selectedCustomer != null -> Icons.Default.Person
                    isPurchaseMode -> Icons.Default.Business
                    else -> Icons.Default.Person
                  },
                  contentDescription = null,
                  tint = when {
                    selectedSupplier != null -> Color(0xFF16A34A)
                    selectedCustomer != null -> Color(0xFF0284C7)
                    isDarkSales -> Color(0xFF38BDF8)
                    else -> Color(0xFF0284C7)
                  },
                  modifier = Modifier.size(18.dp)
                )
                val partyPrefix = when {
                  isPurchaseMode && selectedCustomer != null -> "المشتري منه (عميل): "
                  isPurchaseMode && selectedSupplier != null -> "المورد (مورد): "
                  !isPurchaseMode && selectedSupplier != null -> "العميل (مورد): "
                  !isPurchaseMode && selectedCustomer != null -> "العميل (عميل): "
                  manualCustomerName.isNotBlank() -> if (isPurchaseMode) "المشتري منه (عميل يدوي): " else "العميل (يدوي): "
                  manualSupplierName.isNotBlank() -> if (isPurchaseMode) "المورد (يدوي): " else "العميل (مورد يدوي): "
                  isPurchaseMode -> "المورد / الطرف: "
                  else -> "العميل / الطرف: "
                }
                Text(
                  text = partyPrefix,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isDarkSales) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = partyDisplayName,
                  fontSize = 12.5.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = when {
                    selectedSupplier != null -> Color(0xFF16A34A)
                    selectedCustomer != null -> Color(0xFF0284C7)
                    isDarkSales -> Color(0xFF38BDF8)
                    else -> Color(0xFF0284C7)
                  },
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                if (selectedSupplier != null || manualSupplierName.isNotBlank()) {
                  Surface(
                    color = Color(0xFFDCFCE7),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                    shape = RoundedCornerShape(4.dp)
                  ) {
                    Text(
                      "مورد",
                      color = Color(0xFF15803D),
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                  }
                } else if (selectedCustomer != null || manualCustomerName.isNotBlank()) {
                  Surface(
                    color = Color(0xFFE0F2FE),
                    border = BorderStroke(1.dp, Color(0xFF7DD3FC)),
                    shape = RoundedCornerShape(4.dp)
                  ) {
                    Text(
                      "عميل",
                      color = Color(0xFF0369A1),
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                  }
                }
              }

              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isDarkSales) Color(0xFF0F172A) else Color.White,
                border = BorderStroke(1.dp, if (isDarkSales) Color(0xFF475569) else Color(0xFF0284C7))
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(Icons.Default.Edit, contentDescription = "تغيير الطرف", tint = if (isDarkSales) Color.White else Color(0xFF0284C7), modifier = Modifier.size(13.dp))
                  Text(
                    text = "تغيير الطرف",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkSales) Color.White else Color(0xFF0284C7)
                  )
                }
              }
            }
          }

          // Row 2: Middle Calculations Controls Row (Discount | Calculator | Net Total)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Discount Box (Right side in RTL) - "مبلغ" + input field
            Row(
              modifier = Modifier
                .weight(1.3f)
                .height(42.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              // "مبلغ" Chip
              Surface(
                onClick = { isDiscountPercent = !isDiscountPercent },
                shape = RoundedCornerShape(8.dp),
                color = if (isDiscountPercent) (if (isDarkSales) Color(0xFF3F3F46) else invoiceAccentColor)
                       else (if (isDarkSales) Color(0xFF27272A) else Color(0xFFE2E8F0)),
                border = BorderStroke(1.dp, if (isDarkSales) Color(0xFF52525B) else Color.Transparent),
                modifier = Modifier
                  .fillMaxHeight()
                  .width(52.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = if (isDiscountPercent) "% نسبة" else "مبلغ",
                    color = if (isDiscountPercent) Color.White else (if (isDarkSales) Color.White else Color(0xFF0F172A)),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                  )
                }
              }

              // Discount Input Box
              BasicTextField(
                value = discountInputText,
                onValueChange = { newValue -> discountInputText = newValue },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = androidx.compose.ui.text.TextStyle(
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isDarkSales) Color.White else MaterialTheme.colorScheme.onSurface,
                  textAlign = TextAlign.Center
                ),
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight(),
                decorationBox = @Composable { innerTextField ->
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .background(if (isDarkSales) Color(0xFF27272A) else MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                      .border(1.dp, if (isDarkSales) Color(0xFF3F3F46) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                      .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    if (discountInputText.isEmpty()) {
                      Text(
                        if (isDiscountPercent) "0 %" else "0",
                        fontSize = 14.sp,
                        color = if (isDarkSales) Color(0xFFA1A1AA) else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                      )
                    }
                    innerTextField()
                  }
                }
              )
            }

            // Circular Math / Operations Button (Center)
            IconButton(
              onClick = { showCalculatorDialog = true },
              modifier = Modifier
                .size(42.dp)
                .background(if (isDarkSales) Color(0xFF27272A) else invoiceAccentColor, CircleShape)
                .border(1.dp, if (isDarkSales) Color(0xFF52525B) else Color.Transparent, CircleShape)
            ) {
              Text(
                "+\n-",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 11.sp
              )
            }

            // Net Total Box (Left side in RTL)
            Surface(
              modifier = Modifier
                .weight(1.4f)
                .height(42.dp),
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.2.dp, if (isDarkSales) Color(0xFF3F3F46) else Color(0xFF0D9488)),
              color = if (isDarkSales) Color(0xFF27272A) else Color(0xFFF0FDFA)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  "الصافي:",
                  fontSize = 11.sp,
                  color = if (isDarkSales) Color(0xFFA1A1AA) else Color(0xFF64748B),
                  fontWeight = FontWeight.Bold
                )
                Text(
                  "${formatPosMoney(total)} $currencySymbol",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = if (isDarkSales) Color(0xFF38BDF8) else Color(0xFF0D9488),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }

          // Row 3: Bottom Row Action Button (New Invoice / Draft)
          Button(
            onClick = {
              viewModel.clearCart()
              viewModel.selectCustomer(null)
              viewModel.setManualCustomerName("")
              viewModel.setSelectedSupplier(null)
              viewModel.setManualSupplierName("")
              viewModel.setDiscount(0.0)
              lastSavedInvoice = null
              lastSavedItems = emptyList()
              discountInputText = "0"
              searchQuery = ""
              checkNumberInput = ""
              checkBankNameInput = ""
              checkDateInput = System.currentTimeMillis()
              paymentMethod = "نقدي"
              customInvoiceDateInput = System.currentTimeMillis()
              showPartyPickerDialog = true
              viewModel.showToast(if (currentDraftMode) "تم فتح مسودة جديدة، حدد الطرف " else "تم فتح فاتورة جديدة، حدد الطرف ")
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (currentDraftMode) Color(0xFFD97706)
                               else if (isDarkSales) Color(0xFF27272A)
                               else invoiceAccentColor
            ),
            border = if (isDarkSales && !currentDraftMode) BorderStroke(1.dp, Color(0xFF52525B)) else null,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(46.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Text(
                "+",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                if (currentDraftMode) "مسودة جديدة" else "فاتورة جديدة",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White
              )
            }
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
          val isReturnEffective = isReturnCurrent || (inv.total < 0 || inv.subtotal < 0)
          val previewTypeTitle = when {
            isReturnEffective && isPurchaseMode -> "--- فاتورة مرتجع مشتريات ---"
            isReturnEffective -> "--- فاتورة مرتجع مبيعات ---"
            isPurchaseMode -> "--- فاتورة مشتريات ---"
            else -> "--- فاتورة مبيعات ---"
          }
          Text(previewTypeTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isReturnEffective) Color(0xFFDC2626) else Color.Unspecified)
          Text("${if (isPurchaseMode) "المورد / الطرف" else "العميل / الطرف"}: $partyDisplayName", fontSize = 12.sp)
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

  // WhatsApp Phone Prompt Dialog
  if (showWhatsAppPhonePromptDialog) {
    AlertDialog(
      onDismissRequest = { showWhatsAppPhonePromptDialog = false },
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF25D366))
          Text("إرسال الفاتورة عبر واتساب", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "يرجى إدخال رقم هاتف ($partyDisplayName) للتحويل مباشرة إلى محادثته على واتساب وإرسال الفاتورة بصيغة PDF جاهزة:",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          OutlinedTextField(
            value = inputWhatsAppPhone,
            onValueChange = { inputWhatsAppPhone = it },
            placeholder = { Text("مثال: 05xxxxxxxx أو 01xxxxxxxxx", fontSize = 12.sp) },
            label = { Text("رقم الهاتف (واتساب)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val phone = inputWhatsAppPhone.trim()
            if (phone.isBlank()) {
              viewModel.showToast("يرجى كتابة رقم الهاتف أو اختيار مشاركة عامة")
            } else {
              showWhatsAppPhonePromptDialog = false
              pendingShareAction?.invoke(phone)
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
        ) {
          Text("إرسال للرقم عبر واتساب", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          TextButton(
            onClick = {
              showWhatsAppPhonePromptDialog = false
              pendingShareAction?.invoke("")
            }
          ) {
            Text("مشاركة عامة")
          }
          TextButton(onClick = { showWhatsAppPhonePromptDialog = false }) {
            Text("إلغاء")
          }
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

  // Party Selector Dialog (Customer / Supplier Unified Picker)
  if (showPartyPickerDialog) {
    var partySearchQuery by remember { mutableStateOf("") }
    // Filter Tab: "all" (الكل), "suppliers" (الموردين), "customers" (العملاء), "new_party" (طرف جديد)
    var partyFilterTab by remember(showPartyPickerDialog) {
      mutableStateOf(if (manualSupplierName.isNotBlank() || manualCustomerName.isNotBlank()) "new_party" else "all")
    }
    var newPartyRole by remember(showPartyPickerDialog) {
      mutableStateOf(if (isPurchaseMode) "supplier" else "customer")
    }
    var manualNameInput by remember(showPartyPickerDialog) {
      mutableStateOf(if (manualSupplierName.isNotBlank()) manualSupplierName else manualCustomerName)
    }

    val q = partySearchQuery.trim()

    data class UnifiedPartyItem(
      val id: Int,
      val name: String,
      val phone: String,
      val isSupplier: Boolean,
      val supplierObj: Supplier? = null,
      val customerObj: Customer? = null
    )

    val allPartiesList = remember(suppliers, customers, partyFilterTab, q, isPurchaseMode) {
      val sItems = suppliers.map { UnifiedPartyItem(it.id, it.name, it.phone, isSupplier = true, supplierObj = it) }
      val cItems = customers.map { UnifiedPartyItem(it.id, it.name, it.phone, isSupplier = false, customerObj = it) }

      val rawList = when (partyFilterTab) {
        "suppliers" -> sItems
        "customers" -> cItems
        else -> if (isPurchaseMode) sItems + cItems else cItems + sItems
      }

      if (q.isBlank()) rawList
      else rawList.filter { it.name.contains(q, ignoreCase = true) || it.phone.contains(q) }
    }

    AlertDialog(
      onDismissRequest = { showPartyPickerDialog = false },
      title = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
              imageVector = if (isPurchaseMode) Icons.Default.ShoppingCart else Icons.Default.PointOfSale,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp)
            )
            Text(
              text = if (isPurchaseMode) "تحديد الطرف للشراء (مورد أو عميل)" else "تحديد الطرف للبيع (عميل أو مورد)",
              fontWeight = FontWeight.Bold,
              fontSize = 15.5.sp
            )
          }
          IconButton(onClick = { showPartyPickerDialog = false }) {
            Icon(Icons.Default.Close, contentDescription = "إغلاق")
          }
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 470.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Top Navigation Filter Chips for distinct categorization
          LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            item {
              FilterChip(
                selected = partyFilterTab == "all",
                onClick = { partyFilterTab = "all" },
                label = { Text("الكل (${suppliers.size + customers.size})", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
              )
            }
            item {
              FilterChip(
                selected = partyFilterTab == "suppliers",
                onClick = { partyFilterTab = "suppliers" },
                label = { Text("الموردين (${suppliers.size})", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF16A34A)) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Color(0xFFDCFCE7),
                  selectedLabelColor = Color(0xFF15803D)
                )
              )
            }
            item {
              FilterChip(
                selected = partyFilterTab == "customers",
                onClick = { partyFilterTab = "customers" },
                label = { Text("العملاء (${customers.size})", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF0284C7)) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Color(0xFFE0F2FE),
                  selectedLabelColor = Color(0xFF0369A1)
                )
              )
            }
            item {
              FilterChip(
                selected = partyFilterTab == "new_party",
                onClick = { partyFilterTab = "new_party" },
                label = { Text("طرف جديد", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp)) }
              )
            }
          }

          if (partyFilterTab != "new_party") {
            // Search field
            OutlinedTextField(
              value = partySearchQuery,
              onValueChange = { partySearchQuery = it },
              placeholder = { Text("ابحث باسم المورد أو العميل أو الهاتف...", fontSize = 12.sp) },
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
              trailingIcon = {
                if (partySearchQuery.isNotBlank()) {
                  IconButton(onClick = { partySearchQuery = "" }) {
                    Icon(Icons.Default.Clear, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                  }
                }
              },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp)
            )

            // Dynamic Action Cards when typing directly into search
            if (q.isNotBlank()) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                // Apply as Supplier card
                Surface(
                  onClick = {
                    val matchedSup = suppliers.find { it.name.trim().equals(q, ignoreCase = true) }
                    if (matchedSup != null) {
                      viewModel.setSelectedSupplier(matchedSup.id, matchedSup)
                    } else {
                      viewModel.setManualSupplierName(q)
                      viewModel.setSelectedSupplier(null)
                    }
                    viewModel.selectCustomer(null)
                    viewModel.setManualCustomerName("")
                    showPartyPickerDialog = false
                    viewModel.showToast("تم اختيار المورد: $q")
                  },
                  shape = RoundedCornerShape(8.dp),
                  color = Color(0xFFDCFCE7),
                  border = BorderStroke(1.dp, Color(0xFF16A34A)),
                  modifier = Modifier.weight(1f)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(Icons.Default.Business, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                    Text(
                      text = "تطبيق كـ مورد",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF15803D),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }

                // Apply as Customer card
                Surface(
                  onClick = {
                    val matchedCust = customers.find { it.name.trim().equals(q, ignoreCase = true) }
                    if (matchedCust != null) {
                      viewModel.selectCustomer(matchedCust.id, matchedCust)
                    } else {
                      viewModel.setManualCustomerName(q)
                      viewModel.selectCustomer(null)
                    }
                    viewModel.setSelectedSupplier(null)
                    viewModel.setManualSupplierName("")
                    showPartyPickerDialog = false
                    viewModel.showToast(if (isPurchaseMode) "تم اختيار العميل للشراء منه: $q" else "تم اختيار العميل: $q")
                  },
                  shape = RoundedCornerShape(8.dp),
                  color = Color(0xFFE0F2FE),
                  border = BorderStroke(1.dp, Color(0xFF0284C7)),
                  modifier = Modifier.weight(1f)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                    Text(
                      text = "تطبيق كـ عميل",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF0369A1),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }
            }

            // Parties List (Suppliers & Customers clearly badged)
            LazyColumn(
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(max = 250.dp),
              verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
              // General Party option
              item {
                val isGeneral = (selectedSupplierId == null && selectedCustomerId == null && manualSupplierName.isBlank() && manualCustomerName.isBlank())
                Surface(
                  onClick = {
                    viewModel.setSelectedSupplier(null)
                    viewModel.selectCustomer(null)
                    viewModel.setManualSupplierName("")
                    viewModel.setManualCustomerName("")
                    showPartyPickerDialog = false
                    viewModel.showToast(if (isPurchaseMode) "تم اختيار: مورد عام (نقدي)" else "تم اختيار: زبون عام (نقدي)")
                  },
                  shape = RoundedCornerShape(8.dp),
                  color = if (isGeneral) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  border = BorderStroke(1.dp, if (isGeneral) MaterialTheme.colorScheme.primary else Color.Transparent),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    RadioButton(
                      selected = isGeneral,
                      onClick = {
                        viewModel.setSelectedSupplier(null)
                        viewModel.selectCustomer(null)
                        viewModel.setManualSupplierName("")
                        viewModel.setManualCustomerName("")
                        showPartyPickerDialog = false
                        viewModel.showToast(if (isPurchaseMode) "تم اختيار: مورد عام" else "تم اختيار: زبون عام")
                      }
                    )
                    Text(
                      text = if (isPurchaseMode) "مورد عام (شراء نقدي بدون تسجيل حساب)" else "زبون عام (مبيع نقدي بدون تسجيل حساب)",
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.5.sp
                    )
                  }
                }
              }

              if (allPartiesList.isEmpty() && q.isNotBlank()) {
                item {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Text("لا يوجد طرف مطابق، يمكنك الضغط أعلاه لتطبيقه مباشرة كـ مورد أو عميل", fontSize = 11.5.sp, color = Color.Gray, textAlign = TextAlign.Center)
                  }
                }
              }

              // List of both Suppliers and Customers
              items(allPartiesList) { item ->
                val isSel = if (item.isSupplier) {
                  (selectedSupplierId == item.id || selectedSupplierFromVm?.id == item.id)
                } else {
                  (selectedCustomerId == item.id || selectedCustomerFromVm?.id == item.id)
                }

                Surface(
                  onClick = {
                    if (item.isSupplier) {
                      viewModel.setSelectedSupplier(item.id, item.supplierObj)
                      viewModel.selectCustomer(null)
                      viewModel.setManualSupplierName("")
                      viewModel.setManualCustomerName("")
                      viewModel.showToast(if (isPurchaseMode) "تم اختيار المورد: ${item.name}" else "تم اختيار المورد: ${item.name} للبيع له")
                    } else {
                      viewModel.selectCustomer(item.id, item.customerObj)
                      viewModel.setSelectedSupplier(null)
                      viewModel.setManualSupplierName("")
                      viewModel.setManualCustomerName("")
                      viewModel.showToast(if (isPurchaseMode) "تم اختيار العميل: ${item.name} للشراء منه" else "تم اختيار العميل: ${item.name}")
                    }
                    showPartyPickerDialog = false
                  },
                  shape = RoundedCornerShape(8.dp),
                  color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                  border = BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      modifier = Modifier.weight(1f)
                    ) {
                      RadioButton(
                        selected = isSel,
                        onClick = {
                          if (item.isSupplier) {
                            viewModel.setSelectedSupplier(item.id, item.supplierObj)
                            viewModel.selectCustomer(null)
                            viewModel.setManualSupplierName("")
                            viewModel.setManualCustomerName("")
                          } else {
                            viewModel.selectCustomer(item.id, item.customerObj)
                            viewModel.setSelectedSupplier(null)
                            viewModel.setManualSupplierName("")
                            viewModel.setManualCustomerName("")
                          }
                          showPartyPickerDialog = false
                        }
                      )
                      Icon(
                        imageVector = if (item.isSupplier) Icons.Default.Business else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (item.isSupplier) Color(0xFF16A34A) else Color(0xFF0284C7),
                        modifier = Modifier.size(18.dp)
                      )
                      Column {
                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (item.phone.isNotBlank()) {
                          Text(item.phone, fontSize = 11.sp, color = Color.Gray)
                        }
                      }
                    }

                    // Distinction Badges and Statement Button
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                      TextButton(
                        onClick = {
                          showPartyPickerDialog = false
                          if (item.isSupplier) {
                            item.supplierObj?.let { viewModel.navigateToSupplierStatement(it) }
                          } else {
                            item.customerObj?.let { viewModel.navigateToCustomerStatement(it) }
                          }
                          onNavigateToStatements?.invoke()
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                      ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("كشف حساب", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                      }

                      // Distinctive Badge Pill
                      if (item.isSupplier) {
                        Surface(
                          color = Color(0xFFDCFCE7),
                          border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                          shape = RoundedCornerShape(5.dp)
                        ) {
                          Text(
                            text = "مورد",
                            color = Color(0xFF15803D),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }
                      } else {
                        Surface(
                          color = Color(0xFFE0F2FE),
                          border = BorderStroke(1.dp, Color(0xFF7DD3FC)),
                          shape = RoundedCornerShape(5.dp)
                        ) {
                          Text(
                            text = "عميل",
                            color = Color(0xFF0369A1),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          } else {
            // "new_party" Tab: Enter manual name and choose whether to treat as Supplier or Customer
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              OutlinedTextField(
                value = manualNameInput,
                onValueChange = { manualNameInput = it },
                label = { Text("اسم الطرف الجديد *", fontSize = 12.5.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
              )

              // Role Selection
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("نوع الطرف:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                FilterChip(
                  selected = newPartyRole == "supplier",
                  onClick = { newPartyRole = "supplier" },
                  label = { Text("مورد", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                  leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(15.dp)) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFDCFCE7),
                    selectedLabelColor = Color(0xFF15803D)
                  )
                )
                FilterChip(
                  selected = newPartyRole == "customer",
                  onClick = { newPartyRole = "customer" },
                  label = { Text("عميل", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                  leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(15.dp)) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFE0F2FE),
                    selectedLabelColor = Color(0xFF0369A1)
                  )
                )
              }

              if (manualNameInput.trim().isNotBlank()) {
                val inputName = manualNameInput.trim()
                Button(
                  onClick = {
                    if (newPartyRole == "supplier") {
                      viewModel.setManualSupplierName(inputName)
                      viewModel.setSelectedSupplier(null)
                      viewModel.selectCustomer(null)
                      viewModel.setManualCustomerName("")
                    } else {
                      viewModel.setManualCustomerName(inputName)
                      viewModel.selectCustomer(null)
                      viewModel.setSelectedSupplier(null)
                      viewModel.setManualSupplierName("")
                    }
                    showPartyPickerDialog = false
                    viewModel.showToast("تم تحديد الطرف: $inputName للفاتورة")
                  },
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                  Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("تطبيق \"$inputName\" على هذه الفاتورة فقط", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                  onClick = {
                    if (newPartyRole == "supplier") {
                      viewModel.addSupplierAndSelect(inputName) {
                        showPartyPickerDialog = false
                      }
                    } else {
                      viewModel.addCustomerAndSelect(inputName) {
                        showPartyPickerDialog = false
                      }
                    }
                  },
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (newPartyRole == "supplier") "حفظ في قائمة الموردين واختياره" else "حفظ في قائمة العملاء واختياره",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                  )
                }
              }
            }
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
            Text("تاريخ ووقت الفاتورة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            OutlinedButton(
              onClick = { partyDatePickerDialog.show() },
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(customInvoiceDateInput)),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (partyFilterTab == "new_party") {
              val inputName = manualNameInput.trim()
              if (inputName.isNotBlank()) {
                if (newPartyRole == "supplier") {
                  viewModel.setManualSupplierName(inputName)
                  viewModel.setSelectedSupplier(null)
                  viewModel.selectCustomer(null)
                  viewModel.setManualCustomerName("")
                } else {
                  viewModel.setManualCustomerName(inputName)
                  viewModel.selectCustomer(null)
                  viewModel.setSelectedSupplier(null)
                  viewModel.setManualSupplierName("")
                }
              }
            } else if (partySearchQuery.trim().isNotBlank()) {
              val queryName = partySearchQuery.trim()
              val matchedSup = suppliers.find { it.name.trim().equals(queryName, ignoreCase = true) }
              val matchedCust = customers.find { it.name.trim().equals(queryName, ignoreCase = true) }
              if (matchedSup != null) {
                viewModel.setSelectedSupplier(matchedSup.id, matchedSup)
                viewModel.selectCustomer(null)
                viewModel.setManualSupplierName("")
                viewModel.setManualCustomerName("")
              } else if (matchedCust != null) {
                viewModel.selectCustomer(matchedCust.id, matchedCust)
                viewModel.setSelectedSupplier(null)
                viewModel.setManualSupplierName("")
                viewModel.setManualCustomerName("")
              } else {
                if (isPurchaseMode) {
                  viewModel.setManualSupplierName(queryName)
                  viewModel.setSelectedSupplier(null)
                } else {
                  viewModel.setManualCustomerName(queryName)
                  viewModel.selectCustomer(null)
                }
              }
            }
            showPartyPickerDialog = false
            viewModel.showToast("تم تحديث معلومات الطرف للفاتورة بنجاح")
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
          Text("تأكيد الاختيار", color = Color.White, fontWeight = FontWeight.Bold)
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
  onOptionsClick: () -> Unit,
  isDark: Boolean = false
) {
  var qtyText by remember(item.quantity) { mutableStateOf(item.quantity.toString()) }
  var priceText by remember(item.product.priceSale) { mutableStateOf(item.product.priceSale.toString()) }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF18181B) else MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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
            tint = if (isDark) Color(0xFFA1A1AA) else Color.Gray,
            modifier = Modifier.size(20.dp)
          )
        }

        // Item Name (Center)
        Text(
          text = item.product.nameAr,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp,
          color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
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
              tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
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
              tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
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
            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
          ),
          modifier = Modifier
            .weight(1f)
            .height(42.dp),
          decorationBox = @Composable { innerTextField ->
            Box(
              modifier = Modifier
                .fillMaxSize()
                .border(1.dp, if (isDark) Color(0xFF3F3F46) else MaterialTheme.colorScheme.outline.copy(alpha=0.3f), RoundedCornerShape(8.dp))
                .background(if (isDark) Color(0xFF27272A) else MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)),
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
          colors = ButtonDefaults.outlinedButtonColors(containerColor = if (isDark) Color(0xFF27272A) else PosTealHeader.copy(alpha = 0.08f)),
          border = BorderStroke(1.dp, if (isDark) Color(0xFF3F3F46) else PosTealHeader.copy(alpha = 0.3f))
        ) {
          Text(
            text = item.product.unit.ifEmpty { "الوحدة" },
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color.White else PosTealHeader,
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
            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
          ),
          modifier = Modifier
            .weight(1.2f)
            .height(42.dp),
          decorationBox = @Composable { innerTextField ->
            Box(
              modifier = Modifier
                .fillMaxSize()
                .border(1.dp, if (isDark) Color(0xFF3F3F46) else MaterialTheme.colorScheme.outline.copy(alpha=0.3f), RoundedCornerShape(8.dp))
                .background(if (isDark) Color(0xFF27272A) else MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)),
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
          border = BorderStroke(1.dp, if (isDark) Color(0xFF3F3F46) else PosTealHeader.copy(alpha = 0.4f)),
          color = if (isDark) Color(0xFF27272A) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = formatPosMoney(item.product.priceSale * item.quantity),
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = if (isDark) Color(0xFF38BDF8) else PosTealHeader
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
