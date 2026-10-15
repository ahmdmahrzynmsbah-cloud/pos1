package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
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


@Composable
fun EmployeesScreen(viewModel: AppViewModel, onPrintStatement: (Employee, List<com.example.data.Expense>) -> Unit) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val employees by viewModel.allEmployees.collectAsState()
  val expenses by viewModel.allExpenses.collectAsState()
  var showAddDialog by remember { mutableStateOf(false) }
  var selectedEmployeeForStatement by remember { mutableStateOf<Employee?>(null) }
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

  if (selectedEmployeeForStatement != null) {
    val emp = selectedEmployeeForStatement!!
    val empExpenses = expenses.filter { it.employeeId == emp.id }
    
    AlertDialog(
      onDismissRequest = { selectedEmployeeForStatement = null },
      title = { Text("كشف حساب الموظف: ${emp.name}") },
      text = {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f)) {
          Text("الراتب الأساسي: ${emp.salary} ر.س", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.height(16.dp))
          Text("المسحوبات والسلف / سندات الصرف:", fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(8.dp))
          
          if (empExpenses.isEmpty()) {
            Text("لا يوجد سندات صرف مسجلة لهذا الموظف.", color = Color.Gray)
          } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              items(empExpenses) { exp ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                  Column(modifier = Modifier.padding(8.dp)) {
                    Text("${exp.amount} ر.س", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text(exp.type + " - " + exp.notes, fontSize = 12.sp)
                    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                    Text(sdf.format(Date(exp.date)), fontSize = 12.sp, color = Color.Gray)
                  }
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(onClick = { onPrintStatement(emp, empExpenses) }) {
          Icon(Icons.Default.PictureAsPdf, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("تنزيل PDF")
        }
      },
      dismissButton = {
        TextButton(onClick = { selectedEmployeeForStatement = null }) {
          Text("إغلاق")
        }
      }
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
            onPreviewClick = { emp -> 
              val empExpenses = expenses.filter { it.employeeId == emp.id }
              viewModel.exportEmployeeStatementToPdf(context, emp, empExpenses, true)
            },
            onDownloadClick = { emp -> 
              val empExpenses = expenses.filter { it.employeeId == emp.id }
              onPrintStatement(emp, empExpenses) 
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
fun EmployeeCard(employee: Employee, onPreviewClick: (Employee) -> Unit, onDownloadClick: (Employee) -> Unit) {
  Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
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
          Text("${employee.role} • الراتب: ${employee.salary}", color = Color.Gray, fontSize = 12.sp)
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
