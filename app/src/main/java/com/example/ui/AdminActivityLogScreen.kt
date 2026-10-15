package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminActivityLogScreen(viewModel: AppViewModel, onBackClick: () -> Unit) {
  val auditLogs by viewModel.allAuditLogs.collectAsState(initial = emptyList())
  val userAccount by viewModel.currentUserAccount.collectAsState()

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    CenterAlignedTopAppBar(
      title = { Text("سجل الأنشطة (Activity Log)", fontWeight = FontWeight.Bold) },
      navigationIcon = {
        IconButton(onClick = onBackClick) {
          Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "رجوع")
        }
      },
      colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )

    var selectedFilter by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("الكل") }
    val filters = listOf("الكل", "المخزون", "المبيعات", "أخرى")

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
      androidx.compose.material3.ScrollableTabRow(
        selectedTabIndex = filters.indexOf(selectedFilter),
        edgePadding = 0.dp,
        containerColor = Color.Transparent,
        divider = {}
      ) {
        filters.forEachIndexed { index, title ->
          Tab(
            selected = selectedFilter == title,
            onClick = { selectedFilter = title },
            text = { Text(title, fontWeight = FontWeight.Bold) }
          )
        }
      }
    }

    if (auditLogs.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("لا توجد سجلات أنشطة حالياً.", color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    } else {
      val filteredLogs = auditLogs.filter { log ->
        when (selectedFilter) {
          "المخزون" -> log.action.contains("منتج") || log.action.contains("تصنيف") || log.action.contains("جرد")
          "المبيعات" -> log.action.contains("بيع") || log.action.contains("فاتورة")
          "أخرى" -> !log.action.contains("منتج") && !log.action.contains("تصنيف") && !log.action.contains("بيع") && !log.action.contains("فاتورة")
          else -> true
        }
      }
      LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        items(filteredLogs.sortedByDescending { it.timestamp }) { log ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              }

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = log.action,
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )
                if (log.details.isNotEmpty()) {
                  Text(
                    text = log.details,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                Text(
                  text = "بواسطة: ${log.employeeName} - ${dateFormat.format(Date(log.timestamp))}",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
          }
        }
      }
    }
  }
}
