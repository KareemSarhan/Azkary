package com.app.azkary.ui.qibla

import android.Manifest
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.app.azkary.R
import com.app.azkary.data.model.LatLng
import com.app.azkary.domain.QiblaCalculator
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    onBack: () -> Unit,
    viewModel: QiblaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val compassState = rememberDeviceHeading(uiState.location)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val permissionDeniedText = stringResource(R.string.error_location_permission)

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            viewModel.refreshLocation()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(permissionDeniedText)
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Text(
                        text = stringResource(R.string.qibla_title),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_content_description)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val requestLocation = {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            val location = uiState.location
            val bearingDegrees = uiState.bearingDegrees

            if (location == null || bearingDegrees == null) {
                QiblaLocationRequired(
                    isLoading = uiState.isLoadingLocation,
                    onRequestLocation = requestLocation
                )
            } else {
                QiblaCompass(
                    bearingDegrees = bearingDegrees,
                    headingDegrees = compassState.headingDegrees,
                    hasCompass = compassState.hasCompass
                )

                Spacer(Modifier.height(24.dp))

                QiblaStatus(
                    bearingDegrees = bearingDegrees,
                    headingDegrees = compassState.headingDegrees,
                    hasCompass = compassState.hasCompass
                )

                Spacer(Modifier.height(16.dp))

                QiblaInfoPanel(
                    location = location,
                    locationName = uiState.locationName,
                    bearingDegrees = bearingDegrees,
                    headingDegrees = compassState.headingDegrees
                )

                Spacer(Modifier.height(16.dp))

                OutlinedButton(
                    onClick = requestLocation,
                    enabled = !uiState.isLoadingLocation
                ) {
                    if (uiState.isLoadingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.qibla_refresh_location))
                }
            }
        }
    }
}

@Composable
private fun QiblaLocationRequired(
    isLoading: Boolean,
    onRequestLocation: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.qibla_location_required),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onRequestLocation,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.qibla_use_current_location))
            }
        }
    }
}

@Composable
private fun QiblaCompass(
    bearingDegrees: Double,
    headingDegrees: Float?,
    hasCompass: Boolean
) {
    val compassDescription = stringResource(R.string.qibla_compass_content_description)
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val ringColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val labelPaint = remember(textColor) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textAlign = Paint.Align.CENTER
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
        }
    }

    Surface(
        color = surfaceColor,
        shape = CircleShape,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Canvas(
            modifier = Modifier
                .size(300.dp)
                .padding(18.dp)
                .semantics { contentDescription = compassDescription }
        ) {
            val center = this.center
            val radius = size.minDimension / 2f
            val compassRotation = if (hasCompass && headingDegrees != null) {
                -headingDegrees
            } else {
                0f
            }
            val qiblaRotation = if (hasCompass && headingDegrees != null) {
                QiblaCalculator.relativeDirectionDegrees(
                    bearingDegrees,
                    headingDegrees.toDouble()
                ).toFloat()
            } else {
                bearingDegrees.toFloat()
            }

            drawCircle(
                color = ringColor,
                radius = radius,
                style = Stroke(width = 4f)
            )

            for (degree in 0 until 360 step 15) {
                val isMajorTick = degree % 45 == 0
                rotate(degrees = compassRotation + degree, pivot = center) {
                    drawLine(
                        color = tickColor,
                        start = Offset(center.x, center.y - radius),
                        end = Offset(
                            center.x,
                            center.y - radius + if (isMajorTick) 28f else 16f
                        ),
                        strokeWidth = if (isMajorTick) 4f else 2f,
                        cap = StrokeCap.Round
                    )
                }
            }

            drawIntoCanvas { canvas ->
                listOf(
                    0f to "N",
                    90f to "E",
                    180f to "S",
                    270f to "W"
                ).forEach { (degree, label) ->
                    val angle = Math.toRadians((degree + compassRotation - 90f).toDouble())
                    val labelRadius = radius - 58f
                    val x = center.x + cos(angle).toFloat() * labelRadius
                    val y = center.y + sin(angle).toFloat() * labelRadius -
                        ((labelPaint.descent() + labelPaint.ascent()) / 2f)

                    canvas.nativeCanvas.drawText(label, x, y, labelPaint)
                }
            }

            rotate(degrees = qiblaRotation, pivot = center) {
                drawLine(
                    color = primaryColor,
                    start = center,
                    end = Offset(center.x, center.y - radius * 0.58f),
                    strokeWidth = 12f,
                    cap = StrokeCap.Round
                )

                val arrowTip = Offset(center.x, center.y - radius * 0.78f)
                val arrowPath = Path().apply {
                    moveTo(arrowTip.x, arrowTip.y)
                    lineTo(center.x - 28f, center.y - radius * 0.48f)
                    lineTo(center.x + 28f, center.y - radius * 0.48f)
                    close()
                }
                drawPath(path = arrowPath, color = primaryColor)
            }

            drawCircle(
                color = primaryColor,
                radius = 12f,
                center = center
            )
        }
    }
}

@Composable
private fun QiblaStatus(
    bearingDegrees: Double,
    headingDegrees: Float?,
    hasCompass: Boolean
) {
    val text = when {
        !hasCompass -> stringResource(R.string.qibla_compass_unavailable)
        headingDegrees == null -> stringResource(R.string.qibla_compass_calibrating)
        else -> {
            val turn = QiblaCalculator.smallestTurnDegrees(
                bearingDegrees,
                headingDegrees.toDouble()
            )
            when {
                abs(turn) <= 3.0 -> stringResource(R.string.qibla_aligned)
                turn > 0.0 -> stringResource(R.string.qibla_turn_right, turn.roundToInt())
                else -> stringResource(R.string.qibla_turn_left, abs(turn).roundToInt())
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Explore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp
        )
    }
}

@Composable
private fun QiblaInfoPanel(
    location: LatLng,
    locationName: String?,
    bearingDegrees: Double,
    headingDegrees: Float?
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            QiblaInfoRow(
                label = stringResource(R.string.qibla_bearing_label),
                value = stringResource(R.string.qibla_degrees, bearingDegrees.roundToInt())
            )
            QiblaInfoRow(
                label = stringResource(R.string.qibla_heading_label),
                value = headingDegrees?.let {
                    stringResource(R.string.qibla_degrees, it.roundToInt())
                } ?: stringResource(R.string.qibla_unknown_heading)
            )
            QiblaInfoRow(
                label = stringResource(R.string.qibla_location_label),
                value = locationName?.takeIf { it.isNotBlank() } ?: location.toReadableString()
            )
        }
    }
}

@Composable
private fun QiblaInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f)
        )
    }
}

private data class DeviceHeadingState(
    val headingDegrees: Float?,
    val hasCompass: Boolean
)

@Composable
private fun rememberDeviceHeading(location: LatLng?): DeviceHeadingState {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    var headingDegrees by remember { mutableStateOf<Float?>(null) }
    var hasCompass by remember { mutableStateOf(true) }

    DisposableEffect(sensorManager, location) {
        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (rotationVectorSensor == null && (accelerometerSensor == null || magneticFieldSensor == null)) {
            hasCompass = false
            headingDegrees = null
            onDispose { }
        } else {
            hasCompass = true
            val declination = location?.let {
                GeomagneticField(
                    it.latitude.toFloat(),
                    it.longitude.toFloat(),
                    0f,
                    System.currentTimeMillis()
                ).declination
            } ?: 0f

            val rotationMatrix = FloatArray(9)
            val orientation = FloatArray(3)
            val gravity = FloatArray(3)
            val geomagnetic = FloatArray(3)
            var hasGravity = false
            var hasGeomagnetic = false

            fun updateHeading(matrix: FloatArray) {
                SensorManager.getOrientation(matrix, orientation)
                val magneticHeading = normalizeDegrees(
                    Math.toDegrees(orientation[0].toDouble()).toFloat()
                )
                val trueHeading = normalizeDegrees(magneticHeading + declination)
                headingDegrees = smoothHeading(headingDegrees, trueHeading)
            }

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    when (event.sensor.type) {
                        Sensor.TYPE_ROTATION_VECTOR -> {
                            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                            updateHeading(rotationMatrix)
                        }

                        Sensor.TYPE_ACCELEROMETER -> {
                            lowPass(event.values, gravity)
                            hasGravity = true
                        }

                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            lowPass(event.values, geomagnetic)
                            hasGeomagnetic = true
                        }
                    }

                    if (hasGravity && hasGeomagnetic &&
                        SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
                    ) {
                        updateHeading(rotationMatrix)
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }

            if (rotationVectorSensor != null) {
                sensorManager.registerListener(
                    listener,
                    rotationVectorSensor,
                    SensorManager.SENSOR_DELAY_UI
                )
            } else {
                sensorManager.registerListener(
                    listener,
                    accelerometerSensor,
                    SensorManager.SENSOR_DELAY_UI
                )
                sensorManager.registerListener(
                    listener,
                    magneticFieldSensor,
                    SensorManager.SENSOR_DELAY_UI
                )
            }

            onDispose {
                sensorManager.unregisterListener(listener)
            }
        }
    }

    return DeviceHeadingState(
        headingDegrees = headingDegrees,
        hasCompass = hasCompass
    )
}

private fun lowPass(input: FloatArray, output: FloatArray) {
    for (index in 0..2) {
        output[index] += 0.2f * (input[index] - output[index])
    }
}

private fun smoothHeading(previous: Float?, next: Float): Float {
    if (previous == null) return next

    val delta = ((next - previous + 540f) % 360f) - 180f
    return normalizeDegrees(previous + delta * 0.15f)
}

private fun normalizeDegrees(degrees: Float): Float {
    return ((degrees % 360f) + 360f) % 360f
}
