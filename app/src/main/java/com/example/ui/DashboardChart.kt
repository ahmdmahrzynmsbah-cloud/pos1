package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.CoralError
import com.example.ui.theme.AmberAlert

@Composable
fun UsersChart(active: Int, expired: Int, pending: Int) {
  val total = active + expired + pending
  val activeAngle = if (total == 0) 0f else (active.toFloat() / total) * 360f
  val expiredAngle = if (total == 0) 0f else (expired.toFloat() / total) * 360f
  val pendingAngle = if (total == 0) 0f else (pending.toFloat() / total) * 360f

  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(24.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 24f
        if (total == 0) {
          drawArc(
            color = Color.LightGray,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
          )
        } else {
          var currentAngle = -90f
          if (active > 0) {
            drawArc(
              color = EmeraldGreen,
              startAngle = currentAngle,
              sweepAngle = activeAngle,
              useCenter = false,
              style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            currentAngle += activeAngle
          }
          if (expired > 0) {
            drawArc(
              color = CoralError,
              startAngle = currentAngle,
              sweepAngle = expiredAngle,
              useCenter = false,
              style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            currentAngle += expiredAngle
          }
          if (pending > 0) {
            drawArc(
              color = AmberAlert,
              startAngle = currentAngle,
              sweepAngle = pendingAngle,
              useCenter = false,
              style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            currentAngle += pendingAngle
          }
        }
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$total", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(text = "إجمالي", fontSize = 12.sp, color = Color.Gray)
      }
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      ChartLegendItem(color = EmeraldGreen, label = "نشط", value = active)
      ChartLegendItem(color = CoralError, label = "منتهي الاشتراك", value = expired)
      ChartLegendItem(color = AmberAlert, label = "بانتظار الموافقة", value = pending)
    }
  }
}

@Composable
fun ChartLegendItem(color: Color, label: String, value: Int) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
    Text(text = label, fontSize = 12.sp, color = Color.Gray)
    Spacer(modifier = Modifier.weight(1f))
    Text(text = "$value", fontSize = 14.sp, fontWeight = FontWeight.Bold)
  }
}
