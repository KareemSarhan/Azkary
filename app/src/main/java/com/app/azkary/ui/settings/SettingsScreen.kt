package com.app.azkary.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.azkary.data.prefs.LayoutDirection
import kotlinx.coroutines.launch
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val layoutDirection by viewModel.layoutDirection.collectAsState(initial = LayoutDirection.SYSTEM)
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Layout Direction", style = MaterialTheme.typography.titleMedium)

            Column(Modifier.selectableGroup()) {
                LayoutDirection.entries.forEach { direction ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (layoutDirection == direction),
                                onClick = { scope.launch { viewModel.setLayoutDirection(direction) } },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = layoutDirection == direction,
                            onClick = null // handled by Row
                        )
                        Text(direction.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Font Size", style = MaterialTheme.typography.titleMedium)
            // Slider implementation for font size can go here

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        }
    }
}
