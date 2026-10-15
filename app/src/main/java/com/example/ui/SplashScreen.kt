package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(viewModel: AppViewModel, onSplashFinished: () -> Unit) {
  val storeLogoUrl by viewModel.storeLogoUrl.collectAsState()
  val storeName by viewModel.storeName.collectAsState()
  var startAnimation by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    startAnimation = true
    android.util.Log.d("SplashScreen", "Splash screen started animation")
    delay(600) // Fast snappy transition
    android.util.Log.d("SplashScreen", "Splash screen delay finished, calling onSplashFinished")
    onSplashFinished()
  }

  // Logo entrance physics-based bouncy scale
  val logoScale by animateFloatAsState(
    targetValue = if (startAnimation) 1f else 0.1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
    ),
    label = "logoScale"
  )

  // Logo professional smooth luxury tilt entrance (-15 to 0 degrees)
  val logoRotation by animateFloatAsState(
    targetValue = if (startAnimation) 0f else -15f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessLow
    ),
    label = "logoRotation"
  )

  // Logo smooth alpha fade
  val logoAlpha by animateFloatAsState(
    targetValue = if (startAnimation) 1f else 0f,
    animationSpec = tween(
      durationMillis = 600,
      easing = EaseInOutCubic
    ),
    label = "logoAlpha"
  )

  // Staggered title animation
  val titleAlpha by animateFloatAsState(
    targetValue = if (startAnimation) 1f else 0f,
    animationSpec = tween(
      durationMillis = 600,
      delayMillis = 200,
      easing = EaseOutCubic
    ),
    label = "titleAlpha"
  )
  val titleOffsetY by animateDpAsState(
    targetValue = if (startAnimation) 0.dp else 16.dp,
    animationSpec = tween(
      durationMillis = 600,
      delayMillis = 200,
      easing = EaseOutCubic
    ),
    label = "titleOffsetY"
  )

  // Staggered subtitle animation
  val subtitleAlpha by animateFloatAsState(
    targetValue = if (startAnimation) 0.8f else 0f,
    animationSpec = tween(
      durationMillis = 600,
      delayMillis = 400,
      easing = EaseOutCubic
    ),
    label = "subtitleAlpha"
  )
  val subtitleOffsetY by animateDpAsState(
    targetValue = if (startAnimation) 0.dp else 12.dp,
    animationSpec = tween(
      durationMillis = 600,
      delayMillis = 400,
      easing = EaseOutCubic
    ),
    label = "subtitleOffsetY"
  )

  // Infinite pulsing aura around the logo
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = 1.6f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2000, easing = EaseOutQuad),
      repeatMode = RepeatMode.Restart
    ),
    label = "pulseScale"
  )
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 0.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2000, easing = EaseOutQuad),
      repeatMode = RepeatMode.Restart
    ),
    label = "pulseAlpha"
  )

  // Particles Drift Animation (Subtle glowing gold and blue particles)
  val particleTime by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 8000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "particleTime"
  )

  // Smooth system-ready loader progress
  val loaderProgress by animateFloatAsState(
    targetValue = if (startAnimation) 1.0f else 0.0f,
    animationSpec = tween(
      durationMillis = 1500,
      easing = EaseInOutSine
    ),
    label = "loaderProgress"
  )

  // Dynamic professional status message
  val statusText = when {
    loaderProgress < 0.25f -> "جاري تهيئة قواعد البيانات المحلية..."
    loaderProgress < 0.55f -> "تحميل إعدادات الفروع والربط السحابي..."
    loaderProgress < 0.85f -> "تأمين الاتصال بخوادم الكنز المشفرة..."
    loaderProgress < 0.98f -> "أهلاً بك! جاري تحميل الواجهة..."
    else -> "جاهز للبدء "
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .clickable { onSplashFinished() }
      .background(
        brush = Brush.radialGradient(
          colors = listOf(
            Color(0xFFFFFFFF), // Pure bright white center
            Color(0xFFF1F5F9), // Luxurious pristine light slate/porcelain perimeter
          ),
          center = Offset.Unspecified,
          radius = Float.POSITIVE_INFINITY
        )
      ),
    contentAlignment = Alignment.Center
  ) {
    // 1. Drifting Luxury Sparkles Background (extremely subtle gold & indigo)
    Canvas(modifier = Modifier.fillMaxSize()) {
      val width = size.width
      val height = size.height
      if (width <= 10f || height <= 10f) return@Canvas

      // 15 deterministic drifting points
      val particleConfigs = listOf(
        Pair(0.15f, 0.35f), Pair(0.85f, 0.15f), Pair(0.50f, 0.75f),
        Pair(0.30f, 0.85f), Pair(0.70f, 0.45f), Pair(0.22f, 0.60f),
        Pair(0.78f, 0.90f), Pair(0.40f, 0.20f), Pair(0.90f, 0.65f),
        Pair(0.10f, 0.95f), Pair(0.60f, 0.10f), Pair(0.05f, 0.50f),
        Pair(0.95f, 0.40f), Pair(0.35f, 0.55f), Pair(0.65f, 0.80f)
      )

      particleConfigs.forEachIndexed { index, (relX, relY) ->
        val initialY = relY * height
        val currentY = (initialY - (particleTime * height)) % height
        val finalY = if (currentY < 0) currentY + height else currentY

        val xWobble = (kotlin.math.sin(particleTime * (2 * Math.PI) + index) * 10.dp.toPx()).toFloat()
        val finalX = ((relX * width + xWobble) % width)

        val radius = (3.dp + (index % 3).dp).toPx()
        val alpha = (0.08f + 0.15f * (1.0f - (finalY / height))) * (1.0f - pulseAlpha * 0.3f)

        drawCircle(
          color = if (index % 2 == 0) Color(0xFF4F46E5) else Color(0xFFF59E0B), // Royal Indigo or Warm Gold
          radius = radius,
          center = Offset(finalX, finalY),
          alpha = alpha
        )
      }
    }

    // 2. Central Layout containing Logo, Title, Slogan and Loader
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
    ) {
      // Logo & Pulsating Glow
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(170.dp)
      ) {
        // Glow Halo Ring 1 (faint primary blue)
        Box(
          modifier = Modifier
            .size(110.dp)
            .scale(pulseScale)
            .alpha(pulseAlpha)
            .background(
              brush = Brush.radialGradient(
                colors = listOf(
                  Color(0xFF4F46E5).copy(alpha = 0.2f),
                  Color(0xFF4F46E5).copy(alpha = 0.0f)
                )
              ),
              shape = CircleShape
            )
        )

        // Glow Halo Ring 2 (faint gold highlight)
        Box(
          modifier = Modifier
            .size(100.dp)
            .scale(1.0f + (pulseScale - 1.0f) * 0.6f)
            .alpha(pulseAlpha * 1.1f)
            .background(
              brush = Brush.radialGradient(
                colors = listOf(
                  Color(0xFFF59E0B).copy(alpha = 0.15f),
                  Color(0xFFF59E0B).copy(alpha = 0.0f)
                )
              ),
              shape = CircleShape
            )
        )

        // The Premium Animated Al-Kanz Logo with elevation shadow
        Box(
          modifier = Modifier
            .size(110.dp)
            .graphicsLayer(
              scaleX = logoScale,
              scaleY = logoScale,
              rotationZ = logoRotation,
              alpha = logoAlpha
            ),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .shadow(8.dp, RoundedCornerShape(28.dp), clip = false)
              .background(Color.White, RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
          ) {
            AppLogo(size = 110.dp, logoUrl = storeLogoUrl)
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Main Title text (crisp dark contrast)
      Text(
        text = storeName,
        color = Color(0xFF0F172A), // Premium slate-900 (sharp contrast)
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .offset(y = titleOffsetY)
          .graphicsLayer(alpha = titleAlpha)
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Slogan/Subtitle text
      Text(
        text = "نظام الفوترة الذكي ومزامنة البيانات الآمنة",
        color = Color(0xFF475569), // Slate-600
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .offset(y = subtitleOffsetY)
          .graphicsLayer(alpha = subtitleAlpha)
      )

      Spacer(modifier = Modifier.height(50.dp))

      // 3. Elegant Bottom Progress & Status Details
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .width(260.dp)
          .graphicsLayer(alpha = titleAlpha)
      ) {
        // Continuous elegant loading progress track
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(CircleShape)
            .background(Color(0xFFE2E8F0)) // Clean slate-200 track
        ) {
          Box(
            modifier = Modifier
              .fillMaxHeight()
              .fillMaxWidth(fraction = loaderProgress)
              .clip(CircleShape)
              .background(
                brush = Brush.linearGradient(
                  colors = listOf(
                    Color(0xFF4F46E5), // Indigo
                    Color(0xFF10B981) // Success Green
                  )
                )
              )
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Actionable loading message
        Text(
          text = statusText,
          color = Color(0xFF64748B), // Slate-500
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          textAlign = TextAlign.Center
        )
      }
    }
  }
}
