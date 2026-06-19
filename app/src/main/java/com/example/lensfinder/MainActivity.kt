package com.hidden.camera.reflection.finder

import android.Manifest
import android.content.Context
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hidden.camera.reflection.finder.ui.theme.LensFinderTheme

private enum class Screen {
    Home,
    HowToUse,
    PrivacyPolicy,
    Camera
}

private data class LanguageChoice(
    val tag: String,
    val labelResId: Int
)

private val languageChoices = listOf(
    LanguageChoice(LocaleHelper.LANGUAGE_SYSTEM, R.string.language_system_default),
    LanguageChoice(LocaleHelper.LANGUAGE_ENGLISH, R.string.language_english),
    LanguageChoice(LocaleHelper.LANGUAGE_FRENCH, R.string.language_french),
    LanguageChoice(LocaleHelper.LANGUAGE_HINDI, R.string.language_hindi),
    LanguageChoice(LocaleHelper.LANGUAGE_PORTUGUESE_BRAZIL, R.string.language_portuguese_brazil),
    LanguageChoice(LocaleHelper.LANGUAGE_INDONESIAN, R.string.language_indonesian),
    LanguageChoice(LocaleHelper.LANGUAGE_SPANISH, R.string.language_spanish),
    LanguageChoice(LocaleHelper.LANGUAGE_TURKISH, R.string.language_turkish),
    LanguageChoice(LocaleHelper.LANGUAGE_GERMAN, R.string.language_german),
    LanguageChoice(LocaleHelper.LANGUAGE_JAPANESE, R.string.language_japanese),
    LanguageChoice(LocaleHelper.LANGUAGE_CHINESE_SIMPLIFIED, R.string.language_chinese_simplified),
    LanguageChoice(LocaleHelper.LANGUAGE_CHINESE_TRADITIONAL, R.string.language_chinese_traditional)
)

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        LocaleHelper.applyPersistedLanguage(this)
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContent {
            LensFinderTheme(dynamicColor = false) {
                LensFinderApp(
                    onExit = ::exitApp,
                    onLanguageChanged = {
                        LocaleHelper.applyPersistedLanguage(this)
                        recreate()
                    }
                )
            }
        }
    }

    private fun exitApp() {
        finish()
    }
}

@Composable
private fun LensFinderApp(
    onExit: () -> Unit,
    onLanguageChanged: () -> Unit
) {
    val context = LocalContext.current
    var screen by remember { mutableStateOf(Screen.Home) }
    var permissionDenied by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf(LocaleHelper.getSavedLanguage(context)) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionMessage = stringResource(R.string.camera_permission_required)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        permissionDenied = !granted
        if (granted) {
            screen = Screen.Camera
        } else {
            Toast.makeText(context, permissionMessage, Toast.LENGTH_LONG).show()
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
            onLanguage = { showLanguageDialog = true },
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

    if (showLanguageDialog) {
        LanguageDialog(
            selectedLanguage = selectedLanguage,
            onLanguageSelected = { languageTag ->
                selectedLanguage = languageTag
                LocaleHelper.saveLanguage(context, languageTag)
                showLanguageDialog = false
                onLanguageChanged()
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
private fun HomeScreen(
    onStartCamera: () -> Unit,
    onHowToUse: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onLanguage: () -> Unit,
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
                    text = stringResource(R.string.app_name),
                    color = Color(0xFF101820),
                    fontSize = 34.sp,
                    lineHeight = 39.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = stringResource(R.string.home_description),
                    color = Color(0xFF293645),
                    fontSize = 18.sp,
                    lineHeight = 25.sp
                )
                if (permissionDenied) {
                    Text(
                        text = stringResource(R.string.camera_permission_required),
                        color = Color(0xFF8A2E24),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onStartCamera,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.start_camera))
                }
                OutlinedButton(
                    onClick = onHowToUse,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.how_to_use))
                }
                OutlinedButton(
                    onClick = onPrivacyPolicy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.privacy_policy))
                }
                OutlinedButton(
                    onClick = onLanguage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.language))
                }
                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.exit))
                }
            }
        }
    }
}

@Composable
private fun LanguageDialog(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_language)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.language_scroll_hint),
                    color = Color(0xFF4C5967),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .background(
                            color = Color(0xFFF4EFFA),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(vertical = 6.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    languageChoices.forEach { choice ->
                        TextButton(
                            onClick = { onLanguageSelected(choice.tag) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedLanguage == choice.tag,
                                onClick = { onLanguageSelected(choice.tag) }
                            )
                            Text(
                                text = stringResource(choice.labelResId),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.restart_app_language_message),
                        color = Color(0xFF4C5967),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun HowToUseScreen(onBack: () -> Unit) {
    InfoScreen(
        title = stringResource(R.string.how_to_use),
        body = stringResource(R.string.how_to_use_body),
        onBack = onBack
    )
}

@Composable
private fun PrivacyPolicyScreen(onBack: () -> Unit) {
    InfoScreen(
        title = stringResource(R.string.privacy_policy),
        body = stringResource(R.string.privacy_policy_body),
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
                    Text(stringResource(R.string.back))
                }
            }
        }
    }
}
