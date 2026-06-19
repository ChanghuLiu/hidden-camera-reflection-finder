package com.hidden.camera.reflection.finder

import android.Manifest
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
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
import kotlin.math.sqrt

private enum class Screen {
    Home,
    HowToUse,
    PrivacyPolicy,
    Camera,
    MagneticDetector,
    InspectionChecklist
}

private data class LanguageChoice(
    val tag: String,
    val labelResId: Int
)

private data class ChecklistItem(
    val titleResId: Int,
    val tipResId: Int,
    val riskResId: Int
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

private val checklistItems = listOf(
    ChecklistItem(R.string.check_smoke_detector, R.string.tip_smoke_detector, R.string.risk_high),
    ChecklistItem(R.string.check_wall_clock, R.string.tip_wall_clock, R.string.risk_medium),
    ChecklistItem(R.string.check_power_outlet, R.string.tip_power_outlet, R.string.risk_high),
    ChecklistItem(R.string.check_usb_charger, R.string.tip_usb_charger, R.string.risk_high),
    ChecklistItem(R.string.check_router, R.string.tip_router, R.string.risk_medium),
    ChecklistItem(R.string.check_tv_area, R.string.tip_tv_area, R.string.risk_medium),
    ChecklistItem(R.string.check_lamp, R.string.tip_lamp, R.string.risk_medium),
    ChecklistItem(R.string.check_mirror, R.string.tip_mirror, R.string.risk_high),
    ChecklistItem(R.string.check_air_vent, R.string.tip_air_vent, R.string.risk_medium),
    ChecklistItem(R.string.check_picture_frame, R.string.tip_picture_frame, R.string.risk_medium),
    ChecklistItem(R.string.check_bookshelf, R.string.tip_bookshelf, R.string.risk_low),
    ChecklistItem(R.string.check_bathroom_ceiling, R.string.tip_bathroom_ceiling, R.string.risk_high),
    ChecklistItem(R.string.check_bedside_table, R.string.tip_bedside_table, R.string.risk_medium)
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
            onMagneticDetector = { screen = Screen.MagneticDetector },
            onChecklist = { screen = Screen.InspectionChecklist },
            onHowToUse = { screen = Screen.HowToUse },
            onPrivacyPolicy = { screen = Screen.PrivacyPolicy },
            onLanguage = { showLanguageDialog = true },
            onExit = onExit,
            permissionDenied = permissionDenied
        )
        Screen.HowToUse -> HowToUseScreen(onBack = { screen = Screen.Home })
        Screen.PrivacyPolicy -> PrivacyPolicyScreen(onBack = { screen = Screen.Home })
        Screen.MagneticDetector -> MagneticDetectorScreen(onBack = { screen = Screen.Home })
        Screen.InspectionChecklist -> InspectionChecklistScreen(onBack = { screen = Screen.Home })
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
    onMagneticDetector: () -> Unit,
    onChecklist: () -> Unit,
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
                modifier = Modifier
                    .padding(26.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    Text(stringResource(R.string.start_camera_inspection))
                }
                OutlinedButton(
                    onClick = onMagneticDetector,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.magnetic_field_detector))
                }
                OutlinedButton(
                    onClick = onChecklist,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.room_inspection_checklist))
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
private fun MagneticDetectorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val magneticSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) }
    var magneticStrength by remember { mutableFloatStateOf(0f) }

    DisposableEffect(magneticSensor) {
        if (magneticSensor == null) {
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    magneticStrength = sqrt(x * x + y * y + z * z)
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, magneticSensor, SensorManager.SENSOR_DELAY_UI)
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }

    val statusResId = when {
        magneticSensor == null -> R.string.sensor_not_available
        magneticStrength >= 90f -> R.string.status_high
        magneticStrength >= 60f -> R.string.status_medium
        else -> R.string.status_normal
    }
    val statusColor = when (statusResId) {
        R.string.status_high -> Color(0xFF9E2A2B)
        R.string.status_medium -> Color(0xFF9A6700)
        else -> Color(0xFF1F6F43)
    }

    ToolScreen(
        title = stringResource(R.string.magnetic_field_detector),
        onBack = onBack
    ) {
        Text(
            text = stringResource(R.string.magnetic_strength_format, magneticStrength),
            color = Color(0xFF101820),
            fontSize = 34.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = stringResource(R.string.status_format, stringResource(statusResId)),
            color = statusColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (magneticSensor == null) {
                stringResource(R.string.magnetic_sensor_unavailable)
            } else {
                stringResource(R.string.magnetic_detector_explanation)
            },
            color = Color(0xFF243241),
            fontSize = 16.sp,
            lineHeight = 23.sp
        )
        Text(
            text = stringResource(R.string.detection_limitation_short),
            color = Color(0xFF5A3921),
            fontSize = 15.sp,
            lineHeight = 21.sp
        )
    }
}

@Composable
private fun InspectionChecklistScreen(onBack: () -> Unit) {
    val checkedItems = remember { mutableStateListOf<Boolean>().apply { repeat(checklistItems.size) { add(false) } } }
    ToolScreen(
        title = stringResource(R.string.room_inspection_checklist),
        onBack = onBack
    ) {
        Text(
            text = stringResource(R.string.checklist_intro),
            color = Color(0xFF243241),
            fontSize = 16.sp,
            lineHeight = 23.sp
        )
        checklistItems.forEachIndexed { index, item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Checkbox(
                        checked = checkedItems[index],
                        onCheckedChange = { checkedItems[index] = it }
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(item.titleResId),
                            color = Color(0xFF101820),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = stringResource(R.string.risk_format, stringResource(item.riskResId)),
                            color = Color(0xFF5A3921),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(item.tipResId),
                            color = Color(0xFF344454),
                            fontSize = 15.sp,
                            lineHeight = 21.sp
                        )
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.checklist_limitation),
            color = Color(0xFF5A3921),
            fontSize = 15.sp,
            lineHeight = 21.sp
        )
    }
}

@Composable
private fun ToolScreen(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
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
                content()
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
    ToolScreen(title = title, onBack = onBack) {
        Text(
            text = body,
            color = Color(0xFF243241),
            fontSize = 17.sp,
            lineHeight = 25.sp
        )
    }
}
