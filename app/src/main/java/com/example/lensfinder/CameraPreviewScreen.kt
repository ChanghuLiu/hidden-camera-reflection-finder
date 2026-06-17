package com.hidden.camera.reflection.finder

import android.widget.FrameLayout
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraPreviewScreen(
    onBack: () -> Unit,
    onExit: () -> Unit
) {
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val detector = remember { BrightSpotDetector() }
    val previewViewState = remember { mutableStateOf<PreviewView?>(null) }
    val overlayViewState = remember { mutableStateOf<DetectionOverlayView?>(null) }
    val cameraState = remember { mutableStateOf<Camera?>(null) }
    var sensitivity by remember { mutableFloatStateOf(0.72f) }
    var torchOn by remember { mutableStateOf(true) }
    var hasDetection by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            cameraState.value?.cameraControl?.enableTorch(false)
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(torchOn) {
        cameraState.value?.cameraControl?.enableTorch(torchOn)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                val previewView = PreviewView(viewContext).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                val overlayView = DetectionOverlayView(viewContext)
                previewViewState.value = previewView
                overlayViewState.value = overlayView
                FrameLayout(viewContext).apply {
                    addView(
                        previewView,
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    )
                    addView(
                        overlayView,
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    )
                }
            }
        )

        CameraBinder(
            previewViewState = previewViewState,
            overlayViewState = overlayViewState,
            cameraState = cameraState,
            cameraExecutor = cameraExecutor,
            detector = detector,
            sensitivityProvider = { sensitivity },
            onDetectionChanged = { hasDetection = it }
        )

        Button(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.62f))
        ) {
            Text("Back")
        }

        Button(
            onClick = {
                cameraState.value?.cameraControl?.enableTorch(false)
                onExit()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.62f))
        ) {
            Text("Exit")
        }

        if (hasDetection) {
            Text(
                text = "Suspicious reflection detected. Check manually.",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp, start = 16.dp, end = 16.dp)
                    .background(Color.Red.copy(alpha = 0.78f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Brightness sensitivity",
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = sensitivity,
                onValueChange = { sensitivity = it },
                valueRange = 0f..1f
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { torchOn = !torchOn },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (torchOn) "Flashlight OFF" else "Flashlight ON")
                }
            }
            Text(
                text = "This tool only helps find lens reflections. It cannot guarantee detection of all hidden cameras.",
                color = Color.White.copy(alpha = 0.82f)
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun CameraBinder(
    previewViewState: MutableState<PreviewView?>,
    overlayViewState: MutableState<DetectionOverlayView?>,
    cameraState: MutableState<Camera?>,
    cameraExecutor: ExecutorService,
    detector: BrightSpotDetector,
    sensitivityProvider: () -> Float,
    onDetectionChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = previewViewState.value
    val overlayView = overlayViewState.value

    DisposableEffect(previewView, overlayView) {
        if (previewView == null || overlayView == null) {
            onDispose { }
        } else {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            val listener = Runnable {
                val cameraProvider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { imageAnalysis ->
                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            val spots = detector.detect(imageProxy, sensitivityProvider())
                            overlayView.updateDetections(
                                newSpots = spots,
                                imageWidth = imageProxy.width,
                                imageHeight = imageProxy.height,
                                imageRotationDegrees = imageProxy.imageInfo.rotationDegrees
                            )
                            ContextCompat.getMainExecutor(context).execute {
                                onDetectionChanged(spots.isNotEmpty())
                            }
                            imageProxy.close()
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                    camera.cameraControl.enableTorch(true)
                    cameraState.value = camera
                } catch (error: Exception) {
                    Toast.makeText(context, "Camera failed to start: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
            providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))

            onDispose {
                runCatching { cameraState.value?.cameraControl?.enableTorch(false) }
                runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
            }
        }
    }
}
