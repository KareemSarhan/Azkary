package com.app.azkary.ui.category

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import com.app.azkary.R
import com.app.azkary.data.model.CategoryItemConfig
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomZikrDialog(
    onDismiss: () -> Unit,
    onSave: (arabicText: String, transliteration: String, translation: String, reference: String, requiredRepeats: Int, isInfinite: Boolean) -> Unit
) {
    var arabicText by remember { mutableStateOf("") }
    var transliteration by remember { mutableStateOf("") }
    var translation by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var requiredRepeats by remember { mutableStateOf(3) }
    var isInfinite by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf<String?>(null) }

    val errorFillText = stringResource(R.string.custom_zikr_error_fill_text)
    val errorMinRepeat = stringResource(R.string.custom_zikr_error_min_repeat)

    val isValid = if (isInfinite) {
        arabicText.isNotEmpty() || transliteration.isNotEmpty()
    } else {
        requiredRepeats > 0 && (arabicText.isNotEmpty() || transliteration.isNotEmpty())
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.custom_zikr_dialog_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextField(
                    value = arabicText,
                    onValueChange = { arabicText = it },
                    label = { Text(stringResource(R.string.custom_zikr_arabic_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    isError = showError != null && arabicText.isEmpty()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                TextField(
                    value = transliteration,
                    onValueChange = { transliteration = it },
                    label = { Text(stringResource(R.string.custom_zikr_transliteration_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                TextField(
                    value = translation,
                    onValueChange = { translation = it },
                    label = { Text(stringResource(R.string.custom_zikr_translation_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                TextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text(stringResource(R.string.custom_zikr_reference_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.custom_zikr_infinite_counter_label),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = isInfinite,
                        onCheckedChange = { isInfinite = it }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (!isInfinite) {
                    Text(
                        text = stringResource(R.string.custom_zikr_repeat_count_label),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    var countText by remember(requiredRepeats) { mutableStateOf(requiredRepeats.toString()) }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { if (requiredRepeats > 1) requiredRepeats-- },
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(50.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null)
                        }

                        OutlinedTextField(
                            value = countText,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                    countText = newValue
                                    val newCount = newValue.toIntOrNull() ?: 0
                                    if (newCount > 0) {
                                        requiredRepeats = newCount
                                    }
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            textStyle = MaterialTheme.typography.headlineSmall.copy(
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        )

                        Button(
                            onClick = { requiredRepeats++ },
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(50.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                showError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.custom_zikr_button_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (!isValid) {
                                showError = errorFillText
                                return@Button
                            }
                            if (!isInfinite && requiredRepeats < 1) {
                                showError = errorMinRepeat
                                return@Button
                            }
                            onSave(arabicText, transliteration, translation, reference, requiredRepeats, isInfinite)
                        }
                    ) {
                        Text(stringResource(R.string.custom_zikr_button_save))
                    }
                }
            }
        }
    }
}