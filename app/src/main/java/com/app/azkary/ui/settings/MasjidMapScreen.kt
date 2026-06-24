package com.app.azkary.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.app.azkary.R
import com.app.azkary.data.model.LatLng
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlin.math.roundToInt
import com.google.android.gms.maps.model.LatLng as MapsLatLng

private val DefaultMapCenter = LatLng(21.4225, 39.8262)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasjidMapScreen(
    masjidId: String?,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val masjidPrefs by viewModel.masjidPreferences.collectAsState()
    val locationPrefs by viewModel.locationPreferences.collectAsState()
    val existingMasjid = masjidId?.let { id ->
        masjidPrefs.savedMasjids.find { it.id == id }
    }

    val initialLocation = existingMasjid?.location
        ?: locationPrefs.lastResolvedLocation
        ?: masjidPrefs.savedLocation
        ?: DefaultMapCenter

    var selectedLocation by remember(masjidId) { mutableStateOf(initialLocation) }
    var name by remember(masjidId) { mutableStateOf(existingMasjid?.name.orEmpty()) }
    var radiusMeters by remember(masjidId) {
        mutableIntStateOf(existingMasjid?.radiusMeters ?: 120)
    }
    var initializedFromPreferences by remember(masjidId) { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation.toMapsLatLng(), 17f)
    }

    LaunchedEffect(existingMasjid?.id, locationPrefs.lastResolvedLocation) {
        if (!initializedFromPreferences) {
            val updatedLocation = existingMasjid?.location
                ?: locationPrefs.lastResolvedLocation
                ?: masjidPrefs.savedLocation
                ?: DefaultMapCenter
            selectedLocation = updatedLocation
            name = existingMasjid?.name.orEmpty()
            radiusMeters = existingMasjid?.radiusMeters ?: 120
            cameraPositionState.move(
                CameraUpdateFactory.newLatLngZoom(updatedLocation.toMapsLatLng(), 17f)
            )
            initializedFromPreferences = true
        }
    }

    val primary = MaterialTheme.colorScheme.primary
    val savedStroke = MaterialTheme.colorScheme.outline
    val savedFill = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    val selectedPoint = selectedLocation.toMapsLatLng()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = { Text(stringResource(R.string.settings_masjid_map_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_content_description)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.saveMasjidSelection(
                                masjidId = masjidId,
                                name = name,
                                location = selectedLocation,
                                radiusMeters = radiusMeters,
                                onSaved = onBack
                            )
                        }
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(R.string.category_save_content_description)
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
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        compassEnabled = true,
                        mapToolbarEnabled = false,
                        myLocationButtonEnabled = false,
                        zoomControlsEnabled = false
                    ),
                    onMapClick = { point ->
                        selectedLocation = LatLng(point.latitude, point.longitude)
                    }
                ) {
                    masjidPrefs.savedMasjids
                        .filterNot { it.id == masjidId }
                        .forEach { masjid ->
                            key(masjid.id) {
                                val savedPoint = masjid.location.toMapsLatLng()
                                Circle(
                                    center = savedPoint,
                                    radius = masjid.radiusMeters.toDouble(),
                                    fillColor = savedFill,
                                    strokeColor = savedStroke,
                                    strokeWidth = 2f
                                )
                                Marker(
                                    state = rememberUpdatedMarkerState(position = savedPoint),
                                    title = masjid.name
                                )
                            }
                        }

                    Circle(
                        center = selectedPoint,
                        radius = radiusMeters.toDouble(),
                        fillColor = primary.copy(alpha = 0.18f),
                        strokeColor = primary,
                        strokeWidth = 3f
                    )
                    Marker(
                        state = rememberUpdatedMarkerState(position = selectedPoint),
                        title = name.ifBlank {
                            stringResource(R.string.settings_masjid_default_name)
                        }
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.settings_masjid_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                R.string.settings_masjid_radius,
                                radiusMeters
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = {
                                viewModel.saveMasjidSelection(
                                    masjidId = masjidId,
                                    name = name,
                                    location = selectedLocation,
                                    radiusMeters = radiusMeters,
                                    onSaved = onBack
                                )
                            }
                        ) {
                            Text(stringResource(R.string.settings_masjid_save))
                        }
                    }

                    Slider(
                        value = radiusMeters.toFloat(),
                        onValueChange = {
                            radiusMeters = it.roundToInt().coerceIn(50, 500)
                        },
                        valueRange = 50f..500f
                    )

                    Text(
                        text = selectedLocation.toReadableString(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

private fun LatLng.toMapsLatLng(): MapsLatLng = MapsLatLng(latitude, longitude)
