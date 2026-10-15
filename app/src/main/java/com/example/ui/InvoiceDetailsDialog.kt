package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.Customer
import com.example.data.Invoice
import com.example.data.InvoiceItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailsDialog(
  inv: Invoice,
  customers: List<Customer>,
  viewModel: AppViewModel,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val invoiceItems by viewModel.getInvoiceItemsForInvoice(inv.id).collectAsState(emptyList())
  val storeName by viewModel.storeName.collectAsState()
  val storePhone by viewModel.storePhone.collectAsState("")
  val storeVatNumber by viewModel.storeVatNumber.collectAsState("")
  val currentBranch by viewModel.currentBranch.collectAsState("الفرع الرئيسي")
  val currencySymbol by viewModel.currencySymbol.collectAsState("ر.س")
  val custName = customers.find { it.id == inv.customerId }?.name ?: "عميل نقدي"

  var showDeleteConfirmDialog by remember { mutableStateOf(false) }
  var previewPdfFile by remember { mutableStateOf<File?>(null) }
  var showPrintMenu by remember { mutableStateOf(false) }

  // 1. Delete Confirmation Dialog
  if (showDeleteConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirmDialog = false },
      title = {
        Text(
          text = "حذف الفاتورة رقم #${inv.id}",
          fontWeight = FontWeight.Bold,
          color = Color(0xFFDC2626),
          fontSize = 16.sp
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "هل أنت متأكد من رغبتك في حذف هذه الفاتورة نهائياً من سجل الفواتير؟",
            fontSize = 14.sp,
            color = Color(0xFF1E293B)
          )
          Text(
            text = "• سيتم إلغاء تأثير الفاتورة من النظام.\n• سيتم تعديل كميات المنتجات في المخزن واسترجاعها تلقائياً.",
            fontSize = 12.sp,
            color = Color(0xFF64748B)
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirmDialog = false
            viewModel.deleteInvoice(inv) {
              onDismiss()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
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

  // 2. In-App PDF Preview Dialog
  previewPdfFile?.let { pdfFile ->
    Dialog(
      onDismissRequest = { previewPdfFile = null },
      properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
      Surface(
        modifier = Modifier
          .fillMaxWidth(0.95f)
          .fillMaxHeight(0.90f),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
        ) {
          // Preview Top Bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
              )
              Text(
                text = "معاينة الفاتورة #${inv.id}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
              )
            }
            IconButton(
              onClick = { previewPdfFile = null },
              modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
              Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Preview Canvas Box
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFFE2E8F0))
              .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
          ) {
            InvoicePdfViewer(file = pdfFile)
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Preview Bottom Actions
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Print Button
            Button(
              onClick = {
                printPdfDocument(context, pdfFile, "فاتورة_${inv.id}")
              },
              modifier = Modifier
                .weight(1f)
                .height(46.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Print,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                  tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "طباعة",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = Color.White,
                  maxLines = 1
                )
              }
            }

            // Share / Export Button
            Button(
              onClick = {
                shareOrViewPdf(context, pdfFile)
              },
              modifier = Modifier
                .weight(1.2f)
                .height(46.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Share,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                  tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "مشاركة PDF",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = Color.White,
                  maxLines = 1
                )
              }
            }

            // Close Button
            Button(
              onClick = { previewPdfFile = null },
              modifier = Modifier
                .weight(0.9f)
                .height(46.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                  tint = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "إغلاق",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = Color.White,
                  maxLines = 1
                )
              }
            }
          }
        }
      }
    }
  }

  // 3. Main Invoice Details Dialog
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      dismissOnClickOutside = true,
      dismissOnBackPress = true,
      usePlatformDefaultWidth = false
    )
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .wrapContentHeight()
        .padding(vertical = 16.dp),
      shape = RoundedCornerShape(24.dp),
      color = Color(0xFFF8FAFC),
      tonalElevation = 8.dp,
      shadowElevation = 10.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Dialog Title & Close Top Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.size(36.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.PictureAsPdf,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
            Text(
              text = "تفاصيل فاتورة #${inv.id}",
              fontWeight = FontWeight.Bold,
              fontSize = 17.sp,
              color = Color(0xFF0F172A)
            )
          }

          // Top Close Action Button
          FilledTonalButton(
            onClick = onDismiss,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = Color(0xFFE2E8F0),
              contentColor = Color(0xFF334155)
            )
          ) {
            Icon(Icons.Default.Close, contentDescription = "إغلاق", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("إغلاق", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Receipt Details Box (White Paper style)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 380.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(14.dp)
        ) {
          LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            item {
              Text(
                text = storeName,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
              )
              if (storePhone.isNotBlank()) {
                Text("الهاتف: $storePhone", color = Color(0xFF64748B), fontSize = 11.sp)
              }
              if (storeVatNumber.isNotBlank()) {
                Text("الرقم الضريبي: $storeVatNumber", color = Color(0xFF64748B), fontSize = 11.sp)
              }
              Text("الفرع: $currentBranch", color = Color(0xFF64748B), fontSize = 11.sp)

              Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color(0xFFE2E8F0),
                thickness = 1.dp
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "رقم الفاتورة: #${inv.id}",
                  color = Color.Black,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
                val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                Text(
                  text = sdf.format(Date(inv.invoiceDate)),
                  color = Color(0xFF475569),
                  fontSize = 11.sp
                )
              }
              Text(
                text = "الكاشير: ${inv.cashierName.ifBlank { "الآدمن" }}",
                color = Color(0xFF334155),
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
              )
              Text(
                text = "العميل: $custName",
                color = Color(0xFF334155),
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
              )

              Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color(0xFFE2E8F0),
                thickness = 1.dp
              )

              // Header row
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                  .padding(vertical = 4.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "الصنف",
                  color = Color(0xFF1E293B),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.weight(1.5f)
                )
                Text(
                  text = "الكمية",
                  color = Color(0xFF1E293B),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.weight(0.7f),
                  textAlign = TextAlign.Center
                )
                Text(
                  text = "الإجمالي",
                  color = Color(0xFF1E293B),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.weight(1f),
                  textAlign = TextAlign.End
                )
              }
            }

            items(invoiceItems) { item ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = item.productNameAr,
                  color = Color(0xFF0F172A),
                  fontSize = 11.sp,
                  modifier = Modifier.weight(1.5f)
                )
                Text(
                  text = "x${item.quantity}",
                  color = Color(0xFF475569),
                  fontSize = 11.sp,
                  modifier = Modifier.weight(0.7f),
                  textAlign = TextAlign.Center
                )
                Text(
                  text = "${formatMoney(item.total)} $currencySymbol",
                  color = Color(0xFF0F172A),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.weight(1f),
                  textAlign = TextAlign.End
                )
              }
            }

            item {
              Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color(0xFFE2E8F0),
                thickness = 1.dp
              )

              Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("المجموع الفرعي:", color = Color(0xFF475569), fontSize = 11.sp)
                Text("${formatMoney(inv.subtotal)} $currencySymbol", color = Color(0xFF0F172A), fontSize = 11.sp)
              }
              if (inv.discount > 0) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("الخصم:", color = Color(0xFFDC2626), fontSize = 11.sp)
                  Text("- ${formatMoney(inv.discount)} $currencySymbol", color = Color(0xFFDC2626), fontSize = 11.sp)
                }
              }
              Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ضريبة القيمة المضافة (15%):", color = Color(0xFF475569), fontSize = 11.sp)
                Text("${formatMoney(inv.taxAmount)} $currencySymbol", color = Color(0xFF0F172A), fontSize = 11.sp)
              }

              Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("المجموع النهائي:", color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                  Text("${formatMoney(inv.total)} $currencySymbol", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
              }

              Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("طريقة الدفع: ${inv.paymentMethod}", color = Color(0xFF475569), fontSize = 11.sp)
                Text("المدفوع: ${formatMoney(inv.paidAmount)} $currencySymbol", color = Color(0xFF0F172A), fontSize = 11.sp)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Actions: 4 Symmetrical Action Buttons in 2x2 Grid + Full Width Close
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Row 1: المعاينة & تصدير PDF
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Button 1: معاينة (Preview)
            Button(
              onClick = {
                coroutineScope.launch {
                  Toast.makeText(context, "جاري إعداد المعاينة...", Toast.LENGTH_SHORT).show()
                  val file = createInvoicePdfFile(
                    context = context,
                    inv = inv,
                    items = invoiceItems,
                    storeName = storeName,
                    storePhone = storePhone,
                    storeVatNumber = storeVatNumber,
                    branch = currentBranch,
                    customerName = custName,
                    currency = currencySymbol
                  )
                  if (file != null) {
                    previewPdfFile = file
                  } else {
                    Toast.makeText(context, "تعذر إنشاء ملف المعاينة", Toast.LENGTH_SHORT).show()
                  }
                }
              },
              modifier = Modifier
                .weight(1f)
                .height(44.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(17.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("معاينة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            // Button 2: PDF (Export / Download)
            Button(
              onClick = {
                coroutineScope.launch {
                  Toast.makeText(context, "جاري تنزيل الفاتورة كملف PDF...", Toast.LENGTH_SHORT).show()
                  val file = createInvoicePdfFile(
                    context = context,
                    inv = inv,
                    items = invoiceItems,
                    storeName = storeName,
                    storePhone = storePhone,
                    storeVatNumber = storeVatNumber,
                    branch = currentBranch,
                    customerName = custName,
                    currency = currencySymbol
                  )
                  if (file != null) {
                    Toast.makeText(context, "تم حفظ الفاتورة PDF بنجاح", Toast.LENGTH_SHORT).show()
                    shareOrViewPdf(context, file)
                  } else {
                    Toast.makeText(context, "حدث خطأ أثناء تنزيل PDF", Toast.LENGTH_SHORT).show()
                  }
                }
              },
              modifier = Modifier
                .weight(1f)
                .height(44.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(17.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("تصدير PDF", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
          }

          // Row 2: طباعة & حذف
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Button 3: Print (طباعة)
            Box(modifier = Modifier.weight(1f)) {
              Button(
                onClick = {
                  showPrintMenu = true
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Print,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("طباعة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
              }

              DropdownMenu(
                expanded = showPrintMenu,
                onDismissRequest = { showPrintMenu = false }
              ) {
                DropdownMenuItem(
                  text = { Text("طباعة عادية / PDF (طابعة النظام)") },
                  leadingIcon = { Icon(Icons.Default.Print, contentDescription = null, tint = Color(0xFF16A34A)) },
                  onClick = {
                    showPrintMenu = false
                    coroutineScope.launch {
                      val file = createInvoicePdfFile(
                        context = context,
                        inv = inv,
                        items = invoiceItems,
                        storeName = storeName,
                        storePhone = storePhone,
                        storeVatNumber = storeVatNumber,
                        branch = currentBranch,
                        customerName = custName,
                        currency = currencySymbol
                      )
                      if (file != null) {
                        printPdfDocument(context, file, "فاتورة_${inv.id}")
                      }
                    }
                  }
                )
                DropdownMenuItem(
                  text = { Text("طابعة بلوتوث حرارية (POS)") },
                  leadingIcon = { Icon(Icons.Default.Print, contentDescription = null, tint = Color(0xFF0284C7)) },
                  onClick = {
                    showPrintMenu = false
                    val cartItems = invoiceItems.map { item ->
                      com.example.data.CartItem(
                        product = com.example.data.Product(
                          id = item.productId,
                          nameAr = item.productNameAr,
                          nameEn = item.productNameEn,
                          priceSale = item.price
                        ),
                        quantity = item.quantity
                      )
                    }
                    viewModel.printThermalInvoice(
                      invoice = inv,
                      items = cartItems,
                      customerName = custName,
                      onNeedPrinterSelection = {
                        viewModel.showToast("يرجى اختيار الطابعة من إعدادات الطابعات أولاً")
                      }
                    )
                  }
                )
              }
            }

            // Button 4: Delete (حذف)
            Button(
              onClick = { showDeleteConfirmDialog = true },
              modifier = Modifier
                .weight(1f)
                .height(44.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(17.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("حذف", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
          }

          // Full-width Close Button (خروج / إغلاق النافذة)
          Button(
            onClick = onDismiss,
            modifier = Modifier
              .fillMaxWidth()
              .height(44.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFFE2E8F0),
              contentColor = Color(0xFF1E293B)
            ),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF475569))
            Spacer(modifier = Modifier.width(6.dp))
            Text("إغلاق النافذة والرجوع للقائمة", color = Color(0xFF1E293B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// PDF Renderer Component for High-Quality in-app preview
// -------------------------------------------------------------
@Composable
fun InvoicePdfViewer(file: File) {
  var bitmap by remember(file) { mutableStateOf<Bitmap?>(null) }
  var isLoading by remember(file) { mutableStateOf(true) }

  LaunchedEffect(file) {
    isLoading = true
    withContext(Dispatchers.IO) {
      try {
        val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val pdfRenderer = PdfRenderer(fileDescriptor)
        if (pdfRenderer.pageCount > 0) {
          val page = pdfRenderer.openPage(0)
          val newBitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
          newBitmap.eraseColor(android.graphics.Color.WHITE)
          page.render(newBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
          page.close()
          bitmap = newBitmap
        }
        pdfRenderer.close()
        fileDescriptor.close()
      } catch (e: Exception) {
        e.printStackTrace()
      } finally {
        isLoading = false
      }
    }
  }

  if (isLoading) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
  } else {
    bitmap?.let { b ->
      Image(
        bitmap = b.asImageBitmap(),
        contentDescription = "معاينة الفاتورة",
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(8.dp)
      )
    } ?: run {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("تعذر عرض معاينة الملف", color = Color.Gray, fontSize = 13.sp)
      }
    }
  }
}

// -------------------------------------------------------------
// High-Fidelity Invoice PDF Generator
// -------------------------------------------------------------
suspend fun createInvoicePdfFile(
  context: Context,
  inv: Invoice,
  items: List<InvoiceItem>,
  storeName: String,
  storePhone: String,
  storeVatNumber: String,
  branch: String,
  customerName: String,
  currency: String
): File? = withContext(Dispatchers.IO) {
  try {
    val pdfDoc = PdfDocument()
    val pageW = 450 // Clean receipt / invoice page width
    val baseH = 520
    val itemsH = (items.size * 26)
    val pageH = baseH + itemsH

    val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, 1).create()
    val page = pdfDoc.startPage(pageInfo)
    val canvas: Canvas = page.canvas

    val marginLeft = 25f
    val marginRight = pageW - 25f

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

    val bgPaint = Paint().apply { isAntiAlias = true }
    val linePaint = Paint().apply {
      isAntiAlias = true
      strokeWidth = 0.8f
      color = android.graphics.Color.rgb(226, 232, 240)
    }

    // Top Header Banner
    bgPaint.color = android.graphics.Color.rgb(30, 58, 138)
    val headerRect = RectF(marginLeft, 20f, marginRight, 80f)
    canvas.drawRoundRect(headerRect, 8f, 8f, bgPaint)

    val titlePaint = Paint().apply {
      isAntiAlias = true
      textSize = 16f
      typeface = fontBold
      textAlign = Paint.Align.CENTER
      color = android.graphics.Color.WHITE
    }
    val subHeaderPaint = Paint().apply {
      isAntiAlias = true
      textSize = 10f
      typeface = fontMedium
      textAlign = Paint.Align.CENTER
      color = android.graphics.Color.rgb(224, 231, 255)
    }

    canvas.drawText(storeName, pageW / 2f, 46f, titlePaint)
    canvas.drawText("فاتورة مبيعات ضريبية  •  $branch", pageW / 2f, 66f, subHeaderPaint)

    var y = 98f

    // Store & Invoice Metadata Card
    bgPaint.color = android.graphics.Color.rgb(248, 250, 252)
    val metaRect = RectF(marginLeft, y, marginRight, y + 68f)
    canvas.drawRoundRect(metaRect, 6f, 6f, bgPaint)
    canvas.drawRect(marginLeft, y, marginRight, y + 68f, Paint().apply {
      style = Paint.Style.STROKE
      strokeWidth = 0.8f
      color = android.graphics.Color.rgb(203, 213, 225)
    })

    val metaLabelPaint = Paint().apply {
      isAntiAlias = true
      textSize = 8.5f
      typeface = fontRegular
      textAlign = Paint.Align.RIGHT
      color = android.graphics.Color.rgb(100, 116, 139)
    }
    val metaValPaint = Paint().apply {
      isAntiAlias = true
      textSize = 9f
      typeface = fontSemiBold
      textAlign = Paint.Align.RIGHT
      color = android.graphics.Color.rgb(15, 23, 42)
    }

    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    val dateFormatted = sdf.format(Date(inv.invoiceDate))

    // Right Column
    canvas.drawText("رقم الفاتورة: #${inv.id}", marginRight - 12f, y + 20f, metaValPaint)
    canvas.drawText("العميل: $customerName", marginRight - 12f, y + 38f, metaValPaint)
    canvas.drawText("الكاشير: ${inv.cashierName.ifBlank { "الآدمن" }}", marginRight - 12f, y + 56f, metaLabelPaint)

    // Left Column
    val col2X = marginRight - 220f
    canvas.drawText("التاريخ: $dateFormatted", col2X, y + 20f, metaLabelPaint)
    if (storeVatNumber.isNotBlank()) {
      canvas.drawText("الرقم الضريبي: $storeVatNumber", col2X, y + 38f, metaLabelPaint)
    }
    if (storePhone.isNotBlank()) {
      canvas.drawText("الهاتف: $storePhone", col2X, y + 56f, metaLabelPaint)
    }

    y += 80f

    // Items Table Header
    bgPaint.color = android.graphics.Color.rgb(37, 99, 235)
    val tblHeaderRect = RectF(marginLeft, y, marginRight, y + 22f)
    canvas.drawRoundRect(tblHeaderRect, 4f, 4f, bgPaint)

    val thPaint = Paint().apply {
      isAntiAlias = true
      textSize = 9f
      typeface = fontBold
      textAlign = Paint.Align.RIGHT
      color = android.graphics.Color.WHITE
    }

    canvas.drawText("الصنف", marginRight - 10f, y + 15f, thPaint)
    canvas.drawText("الكمية", marginRight - 180f, y + 15f, thPaint)
    canvas.drawText("سعر الوحدة", marginRight - 260f, y + 15f, thPaint)
    canvas.drawText("الإجمالي ($currency)", marginRight - 330f, y + 15f, thPaint)

    y += 22f

    // Item Rows
    val rowHeight = 22f
    val tdTextPaint = Paint().apply {
      isAntiAlias = true
      textSize = 8.5f
      typeface = fontRegular
      textAlign = Paint.Align.RIGHT
      color = android.graphics.Color.rgb(30, 41, 59)
    }
    val tdBoldPaint = Paint().apply {
      isAntiAlias = true
      textSize = 8.5f
      typeface = fontSemiBold
      textAlign = Paint.Align.RIGHT
      color = android.graphics.Color.rgb(15, 23, 42)
    }

    items.forEachIndexed { index, item ->
      val isEven = (index % 2 == 0)
      bgPaint.color = if (isEven) android.graphics.Color.WHITE else android.graphics.Color.rgb(248, 250, 252)
      canvas.drawRect(marginLeft, y, marginRight, y + rowHeight, bgPaint)
      canvas.drawLine(marginLeft, y + rowHeight, marginRight, y + rowHeight, linePaint)

      canvas.drawText(item.productNameAr.take(22), marginRight - 10f, y + 15f, tdBoldPaint)
      canvas.drawText("x${item.quantity}", marginRight - 180f, y + 15f, tdTextPaint)
      canvas.drawText(String.format(Locale.US, "%,.2f", item.price), marginRight - 260f, y + 15f, tdTextPaint)
      canvas.drawText(String.format(Locale.US, "%,.2f", item.total), marginRight - 330f, y + 15f, tdBoldPaint)

      y += rowHeight
    }

    y += 10f

    // Totals Breakdown Card
    bgPaint.color = android.graphics.Color.rgb(248, 250, 252)
    val totalBoxRect = RectF(marginLeft, y, marginRight, y + 80f)
    canvas.drawRoundRect(totalBoxRect, 6f, 6f, bgPaint)
    canvas.drawRect(marginLeft, y, marginRight, y + 80f, Paint().apply {
      style = Paint.Style.STROKE
      strokeWidth = 0.8f
      color = android.graphics.Color.rgb(203, 213, 225)
    })

    val totLabelPaint = Paint().apply {
      isAntiAlias = true
      textSize = 9f
      typeface = fontMedium
      textAlign = Paint.Align.RIGHT
      color = android.graphics.Color.rgb(71, 85, 105)
    }
    val totValPaint = Paint().apply {
      isAntiAlias = true
      textSize = 9.5f
      typeface = fontBold
      textAlign = Paint.Align.RIGHT
      color = android.graphics.Color.rgb(15, 23, 42)
    }
    val grandTotalPaint = Paint().apply {
      isAntiAlias = true
      textSize = 12f
      typeface = fontBold
      textAlign = Paint.Align.RIGHT
      color = android.graphics.Color.rgb(37, 99, 235)
    }

    canvas.drawText("المجموع الفرعي:", marginRight - 15f, y + 18f, totLabelPaint)
    canvas.drawText("${String.format(Locale.US, "%,.2f", inv.subtotal)} $currency", marginRight - 130f, y + 18f, totValPaint)

    if (inv.discount > 0) {
      canvas.drawText("الخصم:", marginRight - 15f, y + 34f, totLabelPaint)
      canvas.drawText("- ${String.format(Locale.US, "%,.2f", inv.discount)} $currency", marginRight - 130f, y + 34f, totValPaint)
    }

    canvas.drawText("ضريبة القيمة المضافة (15%):", marginRight - 15f, y + 50f, totLabelPaint)
    canvas.drawText("${String.format(Locale.US, "%,.2f", inv.taxAmount)} $currency", marginRight - 130f, y + 50f, totValPaint)

    canvas.drawLine(marginLeft + 10f, y + 56f, marginRight - 10f, y + 56f, linePaint)

    canvas.drawText("المجموع النهائي:", marginRight - 15f, y + 72f, grandTotalPaint)
    canvas.drawText("${String.format(Locale.US, "%,.2f", inv.total)} $currency", marginRight - 130f, y + 72f, grandTotalPaint)

    y += 92f

    // Footer note
    val footerPaint = Paint().apply {
      isAntiAlias = true
      textSize = 8f
      typeface = fontRegular
      textAlign = Paint.Align.CENTER
      color = android.graphics.Color.rgb(100, 116, 139)
    }
    canvas.drawText("شكراً لتسوقكم معنا  •  طريقة الدفع: ${inv.paymentMethod}", pageW / 2f, y, footerPaint)

    pdfDoc.finishPage(page)

    // Save File in App Documents directory
    val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
    val file = File(outputDir, "فاتورة_${inv.id}_${System.currentTimeMillis()}.pdf")
    val fos = FileOutputStream(file)
    pdfDoc.writeTo(fos)
    pdfDoc.close()
    fos.close()

    file
  } catch (e: Exception) {
    e.printStackTrace()
    null
  }
}

// -------------------------------------------------------------
// Share & View PDF Intent
// -------------------------------------------------------------
fun shareOrViewPdf(context: Context, file: File) {
  try {
    val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, "application/pdf")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(intent, "عرض وطباعة الفاتورة PDF")
    chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(chooser)
  } catch (e: Exception) {
    Toast.makeText(context, "تعذر فتح ملف PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
  }
}

// -------------------------------------------------------------
// Direct Android System Print Manager
// -------------------------------------------------------------
fun printPdfDocument(context: Context, file: File, jobName: String) {
  try {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
    if (printManager != null) {
      val printAdapter = object : PrintDocumentAdapter() {
        override fun onLayout(
          oldAttributes: PrintAttributes?,
          newAttributes: PrintAttributes?,
          cancellationSignal: CancellationSignal?,
          callback: LayoutResultCallback?,
          extras: android.os.Bundle?
        ) {
          if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
          }
          val info = PrintDocumentInfo.Builder("$jobName.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()
          callback?.onLayoutFinished(info, newAttributes != oldAttributes)
        }

        override fun onWrite(
          pages: Array<out PageRange>?,
          destination: ParcelFileDescriptor?,
          cancellationSignal: CancellationSignal?,
          callback: WriteResultCallback?
        ) {
          var input: FileInputStream? = null
          var output: FileOutputStream? = null
          try {
            input = FileInputStream(file)
            output = FileOutputStream(destination?.fileDescriptor)
            val buf = ByteArray(16384)
            var bytesRead: Int
            while (input.read(buf).also { bytesRead = it } >= 0) {
              if (cancellationSignal?.isCanceled == true) {
                callback?.onWriteCancelled()
                return
              }
              output.write(buf, 0, bytesRead)
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
          } catch (e: Exception) {
            callback?.onWriteFailed(e.message)
          } finally {
            try { input?.close() } catch (_: Exception) {}
            try { output?.close() } catch (_: Exception) {}
          }
        }
      }

      val attributes = PrintAttributes.Builder()
        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
        .build()

      printManager.print(jobName, printAdapter, attributes)
    } else {
      shareOrViewPdf(context, file)
    }
  } catch (e: Exception) {
    Toast.makeText(context, "حدث خطأ أثناء إرسال الفاتورة للطباعة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
  }
}
