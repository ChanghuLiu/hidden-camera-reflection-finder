package com.hidden.camera.reflection.finder

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hidden.camera.reflection.finder.ui.theme.LensFinderTheme

private const val CAMERA_PERMISSION_MESSAGE =
    "Camera permission is required to use the reflection checking feature."

private enum class Screen {
    Home,
    HowToUse,
    PrivacyPolicy,
    Camera
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContent {
            LensFinderTheme(dynamicColor = false) {
                LensFinderApp(onExit = ::exitApp)
            }
        }
    }

    private fun exitApp() {
        finish()
    }
}

@Composable
private fun LensFinderApp(onExit: () -> Unit) {
    val context = LocalContext.current
    var screen by remember { mutableStateOf(Screen.Home) }
    var permissionDenied by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        permissionDenied = !granted
        if (granted) {
            screen = Screen.Camera
        } else {
            Toast.makeText(context, CAMERA_PERMISSION_MESSAGE, Toast.LENGTH_LONG).show()
        }
    }

    when (screen) {
        Screen.Home -> HomeScreen(
            onStartCamera = {
                if (hasCameraPermission) {
                    screen = Screen.Camera
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onHowToUse = { screen = Screen.HowToUse },
            onPrivacyPolicy = { screen = Screen.PrivacyPolicy },
            onExit = onExit,
            permissionDenied = permissionDenied
        )
        Screen.HowToUse -> HowToUseScreen(onBack = { screen = Screen.Home })
        Screen.PrivacyPolicy -> PrivacyPolicyScreen(onBack = { screen = Screen.Home })
        Screen.Camera -> CameraPreviewScreen(
            onBack = { screen = Screen.Home },
            onExit = onExit
        )
    }
}

@Composable
private fun HomeScreen(
    onStartCamera: () -> Unit,
    onHowToUse: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onExit: () -> Unit,
    permissionDenied: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF08111F), Color(0xFF172A3A), Color(0xFF0C0D10))
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier.padding(26.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "Hidden Camera Reflection Finder",
                    color = Color(0xFF101820),
                    fontSize = 34.sp,
                    lineHeight = 39.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Use your phone camera to visually check possible lens reflections.",
                    color = Color(0xFF293645),
                    fontSize = 18.sp,
                    lineHeight = 25.sp
                )
                if (permissionDenied) {
                    Text(
                        text = CAMERA_PERMISSION_MESSAGE,
                        color = Color(0xFF8A2E24),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onStartCamera,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Camera")
                }
                OutlinedButton(
                    onClick = onHowToUse,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("How to Use")
                }
                OutlinedButton(
                    onClick = onPrivacyPolicy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Privacy Policy")
                }
                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Exit")
                }
            }
        }
    }
}

@Composable
private fun HowToUseScreen(onBack: () -> Unit) {
    InfoScreen(
        title = "How to Use",
        body = """1. Slowly scan the room using your phone camera.
2. Look for small bright reflection points.
3. Check suspicious objects from different angles.
4. Use this app only as a visual checking tool.

Important:
This app helps you find possible lens reflections. It does not guarantee detection of all hidden cameras.""",
        onBack = onBack
    )
}

@Composable
private fun PrivacyPolicyScreen(onBack: () -> Unit) {
    InfoScreen(
        title = "Privacy Policy",
        body = """Hidden Camera Reflection Finder uses the device camera to help users visually check possible lens reflections.

Camera Permission:
The camera is used only for real-time preview and visual checking on the device.

Data Collection:
We do not collect, upload, sell, or share personal data.

Photos and Videos:
The app does not upload photos or videos to any server.

Account:
No account or login is required.

Backend:
The app does not use a backend server.

Limitation:
This app helps users find possible lens reflections. It does not guarantee detection of all hidden cameras.

Privacy Policy URL:
https://changhuliu.github.io/hidden-camera-reflection-finder/privacy-policy.html""",
        onBack = onBack
    )
}

@Composable
private fun InfoScreen(
    title: String,
    body: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101820))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF4EFE5)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF101820)
                )
                Text(
                    text = body,
                    color = Color(0xFF243241),
                    fontSize = 17.sp,
                    lineHeight = 25.sp
                )
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back")
                }
            }
        }
    }
}
