package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.printer.BluetoothPrinterDevice

@Composable
fun BluetoothPrinterSettingsDialog(
  viewModel: AppViewModel,
  onDismissRequest: () -> Unit
) {
  val context = LocalContext.current
  val savedPrinter by viewModel.savedPrinterDevice.collectAsState()
  val isPrinting by viewModel.isPrinterPrinting.collectAsState()

  var pairedDevices by remember { mutableStateOf<List<BluetoothPrinterDevice>>(emptyList()) }
  var hasPermission by remember { mutableStateOf(false) }

  // Check Bluetooth Permissions
  fun checkPermissions(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
          ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
    } else {
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val granted = permissions.values.all { it }
    hasPermission = granted
    if (granted) {
      pairedDevices = viewModel.refreshPairedPrinters()
    } else {
      viewModel.showToast("يتطلب التطبيق صلاحة البلوتوث للاتصال بالطابعة الحرارية ")
    }
  }

  LaunchedEffect(Unit) {
    if (checkPermissions()) {
      hasPermission = true
      pairedDevices = viewModel.refreshPairedPrinters()
    } else {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissionLauncher.launch(
          arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
          )
        )
      } else {
        permissionLauncher.launch(
          arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
          )
        )
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismissRequest,
    shape = RoundedCornerShape(20.dp),
    containerColor = Color.White,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(Color(0xFF00796B)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Default.Print,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            "إعدادات الطابعة الحرارية ",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF1E293B)
          )
        }

        IconButton(onClick = onDismissRequest) {
          Icon(Icons.Default.Close, contentDescription = "إغلاق")
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Saved Printer Status Banner
        if (savedPrinter != null) {
          Surface(
            color = Color(0xFFF0FDF4),
            border = BorderStroke(1.dp, Color(0xFF16A34A)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(
                      "الطابعة النشطة حالياً:",
                      fontSize = 11.sp,
                      color = Color.Gray
                    )
                    Text(
                      savedPrinter?.name ?: "",
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                      color = Color(0xFF1E293B)
                    )
                    Text(
                      "MAC: ${savedPrinter?.address}",
                      fontSize = 10.sp,
                      color = Color.DarkGray
                    )
                  }
                }

                TextButton(
                  onClick = { viewModel.clearBluetoothPrinter() },
                  colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626))
                ) {
                  Text("إزالة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              // Test Print Button
              Button(
                onClick = {
                  savedPrinter?.let { viewModel.testBluetoothPrinter(it) }
                },
                enabled = !isPrinting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                if (isPrinting) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("جاري طباعة الاختبار...", fontSize = 12.sp)
                } else {
                  Icon(
                    Icons.Default.Receipt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("طباعة فاتورة تجريبية (Test Print) ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        } else {
          Surface(
            color = Color(0xFFFEF2F2),
            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                "لم يتم اختيار طابعة حرارية بعد. الرجاء تحديد طابعة من القائمة أدناه.",
                fontSize = 11.sp,
                color = Color(0xFF991B1B)
              )
            }
          }
        }

        Divider(color = Color(0xFFE2E8F0))

        // Section Title & Refresh Button
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            "الأجهزة المقترنة بالبلوتوث ",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color(0xFF1E293B)
          )

          IconButton(
            onClick = {
              if (checkPermissions()) {
                pairedDevices = viewModel.refreshPairedPrinters()
                viewModel.showToast("تم تحديث قائمة أجهزة البلوتوث")
              } else {
                viewModel.showToast("الرجاء منح صلاحية البلوتوث أولاً")
              }
            }
          ) {
            Icon(
              Icons.Default.Refresh,
              contentDescription = "تحديث القائمة",
              tint = Color(0xFF00796B)
            )
          }
        }

        // Device List
        if (!hasPermission) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            contentAlignment = Alignment.Center
          ) {
            Button(
              onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                  permissionLauncher.launch(
                    arrayOf(
                      Manifest.permission.BLUETOOTH_CONNECT,
                      Manifest.permission.BLUETOOTH_SCAN
                    )
                  )
                } else {
                  permissionLauncher.launch(
                    arrayOf(
                      Manifest.permission.ACCESS_FINE_LOCATION,
                      Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                  )
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B))
            ) {
              Text("منح صلاحية البلوتوث ")
            }
          }
        } else if (pairedDevices.isEmpty()) {
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                Icons.Default.BluetoothSearching,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(36.dp)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                "لا توجد أجهزة بلوتوث مقترنة!",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFF1E293B)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                "يرجى تشغيل البلوتوث واقتران طابعتك الحرارية (مثال: POS-58 أو PT-210) من إعدادات البلوتوث بالهاتف، ثم اضغط زر التحديث ",
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 220.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(pairedDevices) { device ->
              val isSelected = savedPrinter?.address == device.address
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    viewModel.saveBluetoothPrinter(device)
                  },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                  containerColor = if (isSelected) Color(0xFFE6F4EA) else Color.White
                ),
                border = BorderStroke(
                  1.dp,
                  if (isSelected) Color(0xFF16A34A) else Color(0xFFCBD5E1)
                )
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      Icons.Default.Print,
                      contentDescription = null,
                      tint = if (isSelected) Color(0xFF16A34A) else Color(0xFF64748B),
                      modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text(
                        device.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1E293B)
                      )
                      Text(
                        device.address,
                        fontSize = 10.sp,
                        color = Color.Gray
                      )
                    }
                  }

                  if (isSelected) {
                    Surface(
                      color = Color(0xFF16A34A),
                      shape = RoundedCornerShape(12.dp)
                    ) {
                      Text(
                        "محددة ",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                      )
                    }
                  } else {
                    OutlinedButton(
                      onClick = { viewModel.saveBluetoothPrinter(device) },
                      shape = RoundedCornerShape(8.dp),
                      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                      modifier = Modifier.height(30.dp)
                    ) {
                      Text("اختيار", fontSize = 11.sp)
                    }
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
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("تم ")
      }
    }
  )
}
