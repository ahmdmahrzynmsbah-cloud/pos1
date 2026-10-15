package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Invoice
import com.example.ui.theme.EmeraldGreen
import java.text.SimpleDateFormat
import java.util.*

import com.example.data.Customer

@Composable
fun ChecksScreen(viewModel: AppViewModel, invoices: List<Invoice>, customers: List<Customer>) {
  var searchQuery by remember { mutableStateOf("") }
  
  val checkInvoices = invoices.filter { it.paymentMethod == "شيك" }
  
  val filteredChecks = checkInvoices.filter {
    val customerName = customers.find { c -> c.id == it.customerId }?.name ?: "عميل نقدي"
    it.checkNumber?.contains(searchQuery, ignoreCase = true) == true ||
    it.checkBankName?.contains(searchQuery, ignoreCase = true) == true ||
    customerName.contains(searchQuery, ignoreCase = true) ||
    it.id.toString() == searchQuery
  }.sortedByDescending { it.invoiceDate }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Icon(Icons.Default.Article, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        Column {
          Text("سجل الشيكات البنكية", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
          Text("إجمالي عدد الشيكات المستلمة: ${checkInvoices.size}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        }
      }
    }

    // Search Bar
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      label = { Text("البحث برقم الشيك، اسم البنك، اسم العميل، أو رقم الفاتورة") },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      singleLine = true
    )

    // List
    if (filteredChecks.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(if (searchQuery.isEmpty()) "لا توجد شيكات مستلمة بعد." else "لا توجد نتائج مطابقة للبحث.", color = Color.Gray)
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(filteredChecks) { checkInv ->
          val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
          val checkDateFormatted = checkInv.checkDate?.let { sdf.format(Date(it)) } ?: "غير محدد"
          val receivedDateFormatted = sdf.format(Date(checkInv.invoiceDate))
          val customerName = customers.find { c -> c.id == checkInv.customerId }?.name ?: "عميل نقدي"
          var showDeleteConfirm by remember { mutableStateOf(false) }

          if (showDeleteConfirm) {
            AlertDialog(
              onDismissRequest = { showDeleteConfirm = false },
              title = {
                Text(
                  text = "حذف الشيك / الفاتورة #${checkInv.id} ",
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFDC2626)
                )
              },
              text = {
                Text("هل أنت تأكد من رغبتك في حذف هذا الشيك المرتبط بالفاتورة رقم #${checkInv.id}؟")
              },
              confirmButton = {
                Button(
                  onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteInvoice(checkInv)
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Text("تأكيد الحذف ", fontWeight = FontWeight.Bold)
                }
              },
              dismissButton = {
                OutlinedButton(
                  onClick = { showDeleteConfirm = false },
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Text("إلغاء")
                }
              }
            )
          }

          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(12.dp)
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("رقم الشيك: ${checkInv.checkNumber ?: ""}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Text(formatMoney(checkInv.total), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EmeraldGreen)
                  IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(28.dp)
                  ) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف الشيك", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                  }
                }
              }
              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("العميل:", fontSize = 14.sp, color = Color.Gray)
                Text(customerName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              }
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("البنك:", fontSize = 14.sp, color = Color.Gray)
                Text(checkInv.checkBankName ?: "", fontSize = 14.sp, fontWeight = FontWeight.Medium)
              }
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("تاريخ استحقاق الشيك:", fontSize = 14.sp, color = Color.Gray)
                Text(checkDateFormatted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
              }
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("تاريخ استلام الشيك (تاريخ الفاتورة):", fontSize = 14.sp, color = Color.Gray)
                Text(receivedDateFormatted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
              }
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("رقم الفاتورة المرتبطة:", fontSize = 14.sp, color = Color.Gray)
                Text("#${checkInv.id}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
              }
              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
              Column(modifier = Modifier.fillMaxWidth()) {
                Text("حالة الشيك:", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  val statuses = listOf("قيد الانتظار" to Color.Gray, "تم التحصيل" to EmeraldGreen, "مرفوض" to Color.Red)
                  val currentStatus = checkInv.checkStatus ?: "قيد الانتظار"
                  statuses.forEach { (status, color) ->
                    val isSelected = currentStatus == status
                    OutlinedButton(
                      onClick = { viewModel.updateCheckStatus(checkInv.id, status) },
                      colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) color.copy(alpha = 0.1f) else Color.Transparent,
                        contentColor = if (isSelected) color else Color.Gray
                      ),
                      border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) color else Color.LightGray),
                      modifier = Modifier.weight(1f),
                      shape = RoundedCornerShape(8.dp),
                      contentPadding = PaddingValues(4.dp)
                    ) {
                      Text(status, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
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
