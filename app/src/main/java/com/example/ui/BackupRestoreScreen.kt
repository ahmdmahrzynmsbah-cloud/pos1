package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
  viewModel: AppViewModel,
  onBackClick: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var isLoading by remember { mutableStateOf(false) }
  var loadingMessage by remember { mutableStateOf("") }
  var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
  var showRestoreConfirmDialog by remember { mutableStateOf(false) }

  // Launcher for exporting / saving backup file
  val createDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json")
  ) { uri: Uri? ->
    if (uri != null) {
      scope.launch {
        isLoading = true
        loadingMessage = "جاري إنشاء وتصدير النسخة الاحتياطية..."
        try {
          val isSuccess = viewModel.exportFullBackup(context, uri)
          if (isSuccess) {
            viewModel.showToast("تم إنشاء النسخة الاحتياطية وحفظها بنجاح!")
          } else {
            viewModel.showToast("فشل في إنشاء ملف النسخة الاحتياطية")
          }
        } catch (e: Exception) {
          viewModel.showToast("خطأ أثناء النسخ الاحتياطي: ${e.localizedMessage}")
        } finally {
          isLoading = false
          loadingMessage = ""
        }
      }
    }
  }

  // Launcher for choosing backup file to restore
  val openDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      pendingRestoreUri = uri
      showRestoreConfirmDialog = true
    }
  }

  // Restore Confirmation Dialog
  if (showRestoreConfirmDialog && pendingRestoreUri != null) {
    AlertDialog(
      onDismissRequest = {
        showRestoreConfirmDialog = false
        pendingRestoreUri = null
      },
      icon = {
        Icon(
          Icons.Default.WarningAmber,
          contentDescription = null,
          tint = Color(0xFFD97706),
          modifier = Modifier.size(36.dp)
        )
      },
      title = {
        Text(
          text = "تأكيد استعادة النسخة الاحتياطية",
          fontWeight = FontWeight.Bold,
          fontSize = 17.sp,
          textAlign = TextAlign.Center
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "هل أنت متأكد من استعادة هذه النسخة الاحتياطية؟",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "• سيتم استبدال بيانات النظام (المنتجات، الفواتير، الحسابات) بالبيانات الواردة في الملف المختار.\n• سيتم إعادة تشغيل التطبيق فور اكتمال الاستعادة لتحديث قواعد البيانات.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val uri = pendingRestoreUri
            showRestoreConfirmDialog = false
            pendingRestoreUri = null
            if (uri != null) {
              scope.launch {
                isLoading = true
                loadingMessage = "جاري استعادة البيانات وإعادة ضبط النظام..."
                try {
                  val restored = viewModel.restoreFullBackup(context, uri)
                  if (restored) {
                    viewModel.showToast("تم استعادة النسخة الاحتياطية بنجاح! جاري إعادة التشغيل...")
                    kotlinx.coroutines.delay(1000)
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    if (intent != null) {
                      val restartIntent = android.content.Intent.makeRestartActivityTask(intent.component)
                      context.startActivity(restartIntent)
                      (context as? android.app.Activity)?.finish()
                      android.os.Process.killProcess(android.os.Process.myPid())
                    }
                  } else {
                    viewModel.showToast("فشل استعادة ملف النسخة الاحتياطية! تأكد من صحة الملف")
                  }
                } catch (e: Exception) {
                  viewModel.showToast("خطأ أثناء الاستعادة: ${e.localizedMessage}")
                } finally {
                  isLoading = false
                  loadingMessage = ""
                }
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("تأكيد الاستعادة الآن", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = {
            showRestoreConfirmDialog = false
            pendingRestoreUri = null
          },
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("إلغاء")
        }
      }
    )
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // 1. Clean Modern Top Bar
    Surface(
      modifier = Modifier.fillMaxWidth(),
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 2.dp
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(40.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
              )
            }
          }
          Column {
            Text(
              text = "النسخ الاحتياطي والاستعادة",
              fontWeight = FontWeight.Bold,
              fontSize = 17.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "حفظ واسترجاع قاعدة بيانات النظام بأمان",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Back Button
        FilledTonalButton(
          onClick = onBackClick,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
          )
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "رجوع",
              modifier = Modifier.size(18.dp)
            )
            Text("رجوع", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)

    // 2. Scrollable Body Content
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

      // Loading Indicator Overlay
      AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              color = MaterialTheme.colorScheme.primary,
              strokeWidth = 2.5.dp
            )
            Text(
              text = loadingMessage.ifBlank { "جاري معالجة البيانات..." },
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }
      }

      // Hero Information Card
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        shadowElevation = 1.dp
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = Color(0xFF10B981).copy(alpha = 0.12f),
            modifier = Modifier.size(42.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(24.dp)
              )
            }
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "قاعدة البيانات مشفرة وجاهزة",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "تشمل النسخة ملفات المنتجات، الفواتير، العملاء، الموردين، القيود المحاسبية، وحركات المخزن في ملف JSON قياسي.",
              fontSize = 11.5.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 16.sp
            )
          }
        }
      }

      // -------------------------------------------------------------
      // CARD 1: Export Backup (حفظ نسخة احتياطية)
      // -------------------------------------------------------------
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, Color(0xFF3B82F6).copy(alpha = 0.3f)),
        shadowElevation = 2.dp
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                  Brush.linearGradient(
                    colors = listOf(Color(0xFF2563EB), Color(0xFF0284C7))
                  )
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
              )
            }

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "إنشاء نسخة احتياطية للبيانات",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "تصدير وحفظ على الهاتف أو Google Drive",
                fontSize = 12.sp,
                color = Color(0xFF2563EB),
                fontWeight = FontWeight.Medium
              )
            }
          }

          Text(
            text = "احفظ نسخة آمنة من كافة بياناتك واسترجعها في أي وقت على أي جهاز آخر دون فقدان أي فاتورة أو حركة مخزنية.",
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
          )

          // Bullet Features
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFFF1F5F9))
              .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            BackupFeatureRow(text = "سجل المبيعات، المشتريات، والفواتير الضريبية")
            BackupFeatureRow(text = "دليل المنتجات، الأصناف، والأسعار وباركود المخزن")
            BackupFeatureRow(text = "أرصدة وحسابات العملاء والموردين ودفتر الأستاذ")
          }

          Button(
            onClick = {
              val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US)
              val fileName = "POS_Backup_${sdf.format(Date())}.json"
              createDocumentLauncher.launch(fileName)
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            enabled = !isLoading
          ) {
            Icon(
              imageVector = Icons.Default.SaveAlt,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
              tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "حفظ نسخة احتياطية جديدة",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = Color.White
            )
          }
        }
      }

      // -------------------------------------------------------------
      // CARD 2: Import / Restore Backup (استعادة نسخة احتياطية)
      // -------------------------------------------------------------
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
        shadowElevation = 2.dp
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                  Brush.linearGradient(
                    colors = listOf(Color(0xFFD97706), Color(0xFFF59E0B))
                  )
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.CloudDownload,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
              )
            }

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "استعادة نسخة احتياطية سابقة",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "استيراد ملف JSON واسترجاع البيانات",
                fontSize = 12.sp,
                color = Color(0xFFD97706),
                fontWeight = FontWeight.Medium
              )
            }
          }

          // Safety Notice Box
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFFEF3C7),
            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.Top,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFFB45309),
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = "تنبيه: استعادة نسخة احتياطية سيقوم بتحديث واستبدال السجلات الحالية بالبيانات الموجودة داخل ملف النسخة مع إعادة تشغيل التطبيق تلقائياً.",
                fontSize = 11.5.sp,
                color = Color(0xFF92400E),
                lineHeight = 16.sp
              )
            }
          }

          Button(
            onClick = {
              openDocumentLauncher.launch(arrayOf("*/*", "application/json"))
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
            enabled = !isLoading
          ) {
            Icon(
              imageVector = Icons.Default.FolderOpen,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
              tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "اختيار ملف النسخة واستعادتها",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = Color.White
            )
          }
        }
      }

      // -------------------------------------------------------------
      // CARD 3: Tips & Cloud Integration Info
      // -------------------------------------------------------------
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              Icons.Default.TipsAndUpdates,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = "نصيحة أمان لحفظ البيانات",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
          Text(
            text = "يمكنك مشاركة ملف النسخة الاحتياطية مباشرة إلى Google Drive أو إرساله لنفسك عبر الواتساب أو البريد الإلكتروني بعد حفظه لضمان وجود نسخة سحابية آمنة دائماً في حال تغيير الجهاز.",
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
          )
        }
      }

      // Extra bottom spacing so nothing is ever hidden by the bottom navigation bar
      Spacer(modifier = Modifier.height(100.dp))
    }
  }
}

@Composable
private fun BackupFeatureRow(text: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Icon(
      imageVector = Icons.Default.Check,
      contentDescription = null,
      tint = Color(0xFF16A34A),
      modifier = Modifier.size(14.dp)
    )
    Text(
      text = text,
      fontSize = 11.5.sp,
      color = Color(0xFF334155),
      fontWeight = FontWeight.Medium
    )
  }
}
