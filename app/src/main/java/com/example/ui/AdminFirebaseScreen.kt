package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFirebaseScreen(viewModel: AppViewModel, onBackClick: () -> Unit) {
  val savedProjectId by viewModel.firebaseProjectId.collectAsState()
  val savedWebApiKey by viewModel.firebaseWebApiKey.collectAsState()
  val savedDatabaseUrl by viewModel.firebaseDatabaseUrl.collectAsState()
  val isSyncing by viewModel.isCloudSyncing.collectAsState()
  val lastSync by viewModel.lastSyncTime.collectAsState()

  var projectId by remember(savedProjectId) { mutableStateOf(savedProjectId) }
  var webApiKey by remember(savedWebApiKey) { mutableStateOf(savedWebApiKey) }
  var databaseUrl by remember(savedDatabaseUrl) { mutableStateOf(savedDatabaseUrl) }
  
  val userAccount by viewModel.currentUserAccount.collectAsState()

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    CenterAlignedTopAppBar(
      title = { Text("إعدادات مزامنة Firebase ", fontWeight = FontWeight.Bold) },
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
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
          Text(
            "استخدم هذه الإعدادات لربط التطبيق بقاعدة بيانات Firebase لضمان الحفظ السحابي الآمن لبيانات المشتركين وتكوينات النظام وربط الكاشير بالمسؤول.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            lineHeight = 20.sp
          )
        }
      }

      // Sync Status Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            "حالة الاتصال والمزامنة السحابية",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
          )
          
          val isConfigured = !com.example.data.FirebaseSyncManager.isPlaceholderConfig(savedProjectId, savedWebApiKey)
          
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isConfigured) EmeraldGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
          ) {
            Text(
              text = if (isConfigured) "متصل وسحابي نشط" else "غير متصل بالسحابة (يتطلب إدخال بيانات Firebase)",
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (isConfigured) EmeraldGreen else MaterialTheme.colorScheme.error,
              textAlign = TextAlign.Center
            )
          }
          
          Spacer(modifier = Modifier.height(4.dp))
          
          Text(
            text = "آخر مزامنة ناجحة: $lastSync",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )

          if (isConfigured) {
            Spacer(modifier = Modifier.height(4.dp))
            if (isSyncing) {
              CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
              )
              Text("جاري رفع ومزامنة البيانات وتحديث الحسابات...", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            } else {
              Button(
                onClick = { viewModel.syncWithCloud(isManual = true) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
              ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("مزامنة الآن ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Text("بيانات الاتصال (Connection Credentials):", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
          
          OutlinedTextField(
            value = projectId,
            onValueChange = { projectId = it },
            label = { Text("Project ID (مُعرّف المشروع)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
          
          OutlinedTextField(
            value = webApiKey,
            onValueChange = { webApiKey = it },
            label = { Text("Web API Key (مفتاح الواجهة البرمجية)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
          
          OutlinedTextField(
            value = databaseUrl,
            onValueChange = { databaseUrl = it },
            label = { Text("Database URL (رابط قاعدة البيانات)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )

          Button(
            onClick = {
              viewModel.saveFirebaseConfig(projectId, webApiKey, databaseUrl)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isSyncing
          ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("حفظ وتفعيل المزامنة", fontWeight = FontWeight.Bold)
          }
        }
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
          Text(
            "تنبيه: تأكد من صحة مفاتيح الاتصال لربط الكاشير بلوحة الإدارة والتحكم الكامل للأدمن سحابياً.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.error
          )
        }
      }
    }
  }
}
