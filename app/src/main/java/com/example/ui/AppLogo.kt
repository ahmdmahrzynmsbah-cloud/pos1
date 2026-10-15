package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AppLogo(modifier: Modifier = Modifier, size: Dp = 48.dp, logoUrl: String = "") {
  val presetIcon: Pair<ImageVector, List<Color>>? = when (logoUrl) {
    "preset:store" -> Pair(
      Icons.Default.Storefront,
      listOf(Color(0xFF2196F3), Color(0xFF1565C0))
    )
    "preset:account" -> Pair(
      Icons.Default.AccountBalance,
      listOf(Color(0xFF009688), Color(0xFF004D40))
    )
    "preset:calc" -> Pair(
      Icons.Default.Calculate,
      listOf(Color(0xFF673AB7), Color(0xFF311B92))
    )
    "preset:building" -> Pair(
      Icons.Default.Business,
      listOf(Color(0xFF607D8B), Color(0xFF263238))
    )
    "preset:crown" -> Pair(
      Icons.Default.Stars,
      listOf(Color(0xFFFFD700), Color(0xFFFF8C00))
    )
    "preset:diamond", "" -> Pair(
      Icons.Default.Diamond,
      listOf(Color(0xFFF9D423), Color(0xFFE65C00))
    )
    else -> null
  }

  if (presetIcon != null) {
    Box(
      modifier = modifier
        .size(size)
        .clip(RoundedCornerShape(size * 0.28f))
        .background(
          brush = Brush.linearGradient(
            colors = presetIcon.second
          )
        ),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = presetIcon.first,
        contentDescription = "شعار التطبيق",
        tint = Color.White,
        modifier = Modifier.size(size * 0.58f)
      )
    }
  } else {
    coil.compose.AsyncImage(
      model = logoUrl,
      contentDescription = "شعار التطبيق Custom Logo",
      modifier = modifier
        .size(size)
        .clip(RoundedCornerShape(size * 0.28f))
        .background(Color.White),
      contentScale = androidx.compose.ui.layout.ContentScale.Crop
    )
  }
}
