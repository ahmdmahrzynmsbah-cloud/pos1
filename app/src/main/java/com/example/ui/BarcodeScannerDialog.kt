package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.AmberAlert

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerDialog(
  onDismissRequest: () -> Unit,
  onBarcodeScanned: (String) -> Unit,
  titleText: String = "قارئ الباركود بالكاميرا ",
  keepOpenOnScan: Boolean = false,
  lastScannedMessage: String? = null
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
      ) == PackageManager.PERMISSION_GRANTED
    )
  }

  val launcher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { granted -> hasCameraPermission = granted }
  )

  LaunchedEffect(Unit) {
    if (!hasCameraPermission) {
      launcher.launch(Manifest.permission.CAMERA)
    }
  }

  var isContinuousMode by remember { mutableStateOf(keepOpenOnScan) }
  var torchEnabled by remember { mutableStateOf(false) }
  var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
  var lastScannedCode by remember { mutableStateOf("") }
  var scanFeedbackMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(lastScannedMessage) {
    if (lastScannedMessage != null) {
      scanFeedbackMessage = lastScannedMessage
    }
  }

  Dialog(
    onDismissRequest = onDismissRequest,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .wrapContentHeight()
        .padding(12.dp),
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.size(36.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.QrCodeScanner,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
            Text(
              text = titleText,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          IconButton(
            onClick = onDismissRequest,
            modifier = Modifier.testTag("close_barcode_scanner_dialog")
          ) {
            Icon(Icons.Default.Close, contentDescription = "إغلاق")
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scan Feedback Banner
        AnimatedVisibility(
          visible = scanFeedbackMessage != null,
          enter = fadeIn(),
          exit = fadeOut()
        ) {
          scanFeedbackMessage?.let { msg ->
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
              shape = RoundedCornerShape(12.dp),
              color = EmeraldGreen.copy(alpha = 0.15f),
              border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen)
            ) {
              Text(
                text = msg,
                color = EmeraldGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
              )
            }
          }
        }

        // Camera View Container
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black),
          contentAlignment = Alignment.Center
        ) {
          if (hasCameraPermission) {
            var isProcessing by remember { mutableStateOf(false) }

            DisposableEffect(Unit) {
              onDispose {
                try {
                  val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                  cameraProvider.unbindAll()
                } catch (e: Exception) {
                  Log.e("BarcodeScanner", "Failed to unbind camera", e)
                }
              }
            }

            AndroidView(
              factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                  scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                  val cameraProvider = cameraProviderFuture.get()
                  val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                  }

                  val barcodeScanner = BarcodeScanning.getClient()

                  val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                  imageAnalysis.setAnalyzer(
                    ContextCompat.getMainExecutor(ctx)
                  ) { imageProxy ->
                    @androidx.camera.core.ExperimentalGetImage
                    val mediaImage = imageProxy.image
                    if (mediaImage != null && !isProcessing) {
                      val image = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                      )
                      isProcessing = true
                      barcodeScanner.process(image)
                        .addOnSuccessListener { barcodes ->
                          for (barcode in barcodes) {
                            val rawValue = barcode.rawValue
                            if (!rawValue.isNullOrEmpty() && rawValue != lastScannedCode) {
                              lastScannedCode = rawValue
                              onBarcodeScanned(rawValue)
                              if (!isContinuousMode) {
                                onDismissRequest()
                              } else {
                                scanFeedbackMessage = "تم المسح: $rawValue "
                              }
                              break
                            }
                          }
                        }
                        .addOnCompleteListener {
                          imageProxy.close()
                          android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            isProcessing = false
                          }, 1000)
                        }
                    } else {
                      imageProxy.close()
                    }
                  }

                  val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                  try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                      lifecycleOwner,
                      cameraSelector,
                      preview,
                      imageAnalysis
                    )
                    cameraControl = camera.cameraControl
                  } catch (exc: Exception) {
                    Log.e("BarcodeScanner", "Use case binding failed", exc)
                  }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
              },
              modifier = Modifier.fillMaxSize()
            )

            // Visual Scan Overlay Frame
            Box(
              modifier = Modifier
                .size(240.dp, 130.dp)
                .border(
                  width = 2.dp,
                  color = if (lastScannedCode.isNotEmpty()) EmeraldGreen else MaterialTheme.colorScheme.primary,
                  shape = RoundedCornerShape(16.dp)
                )
                .background(
                  color = (if (lastScannedCode.isNotEmpty()) EmeraldGreen else MaterialTheme.colorScheme.primary).copy(
                    alpha = 0.08f
                  )
                )
            )

            // Torch Control
            IconButton(
              onClick = {
                torchEnabled = !torchEnabled
                cameraControl?.enableTorch(torchEnabled)
              },
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
              Icon(
                imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = "كشاف الكاميرا",
                tint = if (torchEnabled) AmberAlert else Color.White
              )
            }

            // Instructions
            Text(
              text = "ضع الباركود داخل الإطار للمسح الضوئي",
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
            )
          } else {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.padding(16.dp)
            ) {
              Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.Gray
              )
              Text(
                text = "تتطلب هذه الميزة الإذن بالوصول للكاميرا لمسح باركود المنتجات تلقائياً",
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = Color.White
              )
              Button(
                onClick = { launcher.launch(Manifest.permission.CAMERA) },
                shape = RoundedCornerShape(12.dp)
              ) {
                Text("السماح بالكاميرا ", fontSize = 12.sp)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Mode Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          FilterChip(
            selected = isContinuousMode,
            onClick = { isContinuousMode = !isContinuousMode },
            label = {
              Text(
                text = if (isContinuousMode) "وضع المسح المتتابع (مفعل) " else "المسح المتتابع ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            },
            leadingIcon = {
              Icon(
                Icons.Default.Autorenew,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = Color.White
            )
          )

          Button(
            onClick = onDismissRequest,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("تم الإغلاق", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
          }
        }
      }
    }
  }
}
