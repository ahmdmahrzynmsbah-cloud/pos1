package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.CoralError
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBranchesScreen(viewModel: AppViewModel, onBackClick: () -> Unit) {
  val branchesList by viewModel.branches.collectAsState(emptyList())
  val invoices by viewModel.allInvoices.collectAsState(emptyList())
  val currentBranch by viewModel.currentBranch.collectAsState()
  
  val userAccount by viewModel.currentUserAccount.collectAsState()

  var showAddDialog by remember { mutableStateOf(false) }
  var branchToEdit by remember { mutableStateOf<String?>(null) }
  
  // Add / Edit form fields
  var nameField by remember { mutableStateOf("") }
  var managerField by remember { mutableStateOf("") }
  var phoneField by remember { mutableStateOf("") }
  var addressField by remember { mutableStateOf("") }
  var statusField by remember { mutableStateOf(true) }

  // Search and filter
  var searchQuery by remember { mutableStateOf("") }

  val filteredBranches = remember(branchesList, searchQuery) {
    if (searchQuery.isBlank()) {
      branchesList
    } else {
      branchesList.filter { it.contains(searchQuery, ignoreCase = true) }
    }
  }

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    CenterAlignedTopAppBar(
      title = { Text("إدارة وتخصيص الفروع ", fontWeight = FontWeight.Bold) },
      navigationIcon = {
        IconButton(onClick = onBackClick) {
          Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "رجوع")
        }
      },
      colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Introductory alert
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
          )
          Text(
            "تتيح لك هذه الواجهة إدارة فروع النظام ومنافذ البيع بالتفصيل، وتخصيص بيانات كل فرع (المدير، الهاتف، العنوان) ومتابعة مبيعاته الإجمالية.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            lineHeight = 18.sp
          )
        }
      }

      // Stats row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Total branches card
        Card(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text("إجمالي الفروع", fontSize = 11.sp, color = Color.Gray)
            Text("${branchesList.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
          }
        }

        // Active branches card
        val activeCount = branchesList.count { viewModel.getBranchStatus(it) }
        Card(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text("الفروع النشطة", fontSize = 11.sp, color = Color.Gray)
            Text("$activeCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
          }
        }

        // Current active POS branch card
        Card(
          modifier = Modifier.weight(1.2f),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text("الفرع النشط حالياً", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
            Text(
              text = currentBranch.split(" ").firstOrNull() ?: currentBranch,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.secondary,
              textAlign = TextAlign.Center
            )
          }
        }
      }

      // Search Bar and Add Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("بحث عن فرع...", fontSize = 13.sp) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
          singleLine = true,
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(10.dp),
          colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.Transparent)
        )

        Button(
          onClick = {
            nameField = ""
            managerField = ""
            phoneField = ""
            addressField = ""
            statusField = true
            branchToEdit = null
            showAddDialog = true
          },
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("فرع جديد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }

      // Branches List
      if (filteredBranches.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Storefront, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              if (searchQuery.isNotBlank()) "لا توجد نتائج مطابقة للبحث." else "لم يتم العثور على فروع مسجلة.",
              color = Color.Gray,
              fontSize = 13.sp
            )
          }
        }
      } else {
        filteredBranches.forEach { branch ->
          val manager = viewModel.getBranchManager(branch)
          val phone = viewModel.getBranchPhone(branch)
          val address = viewModel.getBranchAddress(branch)
          val isActive = viewModel.getBranchStatus(branch)

          // Calculate branch statistics dynamically
          val branchInvoices = invoices.filter { it.branchName == branch || (branch == "الفرع الرئيسي" && it.branchName.isBlank()) }
          val branchSalesTotal = branchInvoices.sumOf { it.total }

          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              // Branch Name & Active Status Indicator
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
                    imageVector = Icons.Default.Store,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                  )
                  Text(
                    text = branch,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  if (currentBranch == branch) {
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                      Text(
                        text = "النشط حالياً ",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                      )
                    }
                  }
                }

                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = if (isActive) EmeraldGreen.copy(alpha = 0.12f) else CoralError.copy(alpha = 0.12f)
                ) {
                  Text(
                    text = if (isActive) "نشط" else "غير نشط",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) EmeraldGreen else CoralError
                  )
                }
              }

              HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

              // Branch Details Information
              Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                  Text("مدير الفرع:", fontSize = 12.sp, color = Color.Gray)
                  Text(
                    text = manager.ifBlank { "غير محدد" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                  Text("رقم الهاتف:", fontSize = 12.sp, color = Color.Gray)
                  Text(
                    text = phone.ifBlank { "غير محدد" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                  Text("العنوان:", fontSize = 12.sp, color = Color.Gray)
                  Text(
                    text = address.ifBlank { "غير محدد" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                  )
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Branch Mini Statistics
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                    Text("الفواتير: ${branchInvoices.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }

                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Icon(Icons.Default.AttachMoney, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(15.dp))
                    Text(
                      text = "المبيعات: ${String.format("%.2f", branchSalesTotal)} ${viewModel.currencySymbol.collectAsState().value}",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = EmeraldGreen
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Action buttons
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
              ) {
                OutlinedButton(
                  onClick = {
                    branchToEdit = branch
                    nameField = branch
                    managerField = manager
                    phoneField = phone
                    addressField = address
                    statusField = isActive
                    showAddDialog = true
                  },
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier.height(36.dp)
                ) {
                  Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("تعديل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                  onClick = {
                    if (branchesList.size <= 1) {
                      viewModel.showToast("لا يمكن حذف الفرع الوحيد في النظام! ")
                    } else {
                      viewModel.deleteBranch(branch)
                      viewModel.showToast("تم حذف الفرع '$branch' بنجاح. ")
                    }
                  },
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralError),
                  border = BorderStroke(1.dp, CoralError.copy(alpha = 0.5f)),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  enabled = branchesList.size > 1,
                  modifier = Modifier.height(36.dp)
                ) {
                  Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("حذف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }

  // Add / Edit Branch Dialog
  if (showAddDialog) {
    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = {
        Text(
          text = if (branchToEdit != null) "تعديل بيانات الفرع " else "إضافة فرع جديد للمؤسسة ",
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )
      },
      text = {
        Column(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
        ) {
          OutlinedTextField(
            value = nameField,
            onValueChange = { nameField = it },
            label = { Text("اسم الفرع:") },
            placeholder = { Text("مثال: فرع مكة المكرمة") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          )

          OutlinedTextField(
            value = managerField,
            onValueChange = { managerField = it },
            label = { Text("اسم المدير المسؤول:") },
            placeholder = { Text("مثال: الأستاذ محمد أحمد") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          )

          OutlinedTextField(
            value = phoneField,
            onValueChange = { phoneField = it },
            label = { Text("رقم هاتف الفرع:") },
            placeholder = { Text("مثال: 059xxxxxxxx") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          )

          OutlinedTextField(
            value = addressField,
            onValueChange = { addressField = it },
            label = { Text("عنوان الفرع بالكامل:") },
            placeholder = { Text("مثال: حي الشوقية، مكة المكرمة") },
            singleLine = false,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          )

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("حالة الفرع (نشط):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Switch(
              checked = statusField,
              onCheckedChange = { statusField = it },
              colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreen)
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val name = nameField.trim()
            if (name.isBlank()) {
              viewModel.showToast("اسم الفرع مطلوب! ")
              return@Button
            }

            if (branchToEdit != null) {
              val old = branchToEdit!!
              if (old != name) {
                if (branchesList.contains(name)) {
                  viewModel.showToast("الاسم الجديد مستخدم لفرع آخر! ")
                  return@Button
                }
                viewModel.editBranch(old, name)
              }
              viewModel.saveBranchDetails(
                branch = name,
                address = addressField.trim(),
                phone = phoneField.trim(),
                manager = managerField.trim(),
                active = statusField
              )
              viewModel.showToast("تم تحديث بيانات الفرع '$name' بنجاح! ")
            } else {
              if (branchesList.contains(name)) {
                viewModel.showToast("هذا الفرع مسجل بالفعل! ")
                return@Button
              }
              viewModel.addBranch(name)
              viewModel.saveBranchDetails(
                branch = name,
                address = addressField.trim(),
                phone = phoneField.trim(),
                manager = managerField.trim(),
                active = statusField
              )
              viewModel.showToast("تم تسجيل الفرع '$name' بنجاح! ")
            }
            showAddDialog = false
          },
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(if (branchToEdit != null) "حفظ التغييرات" else "إضافة الفرع", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) {
          Text("إلغاء", color = CoralError)
        }
      }
    )
  }
}
