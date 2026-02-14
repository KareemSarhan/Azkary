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
                    text = "Add Custom Zikr",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextField(
                    value = arabicText,
                    onValueChange = { arabicText = it },
                    label = { Text("Arabic Text") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    isError = showError != null && arabicText.isEmpty()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                TextField(
                    value = transliteration,
                    onValueChange = { transliteration = it },
                    label = { Text("Transliteration") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                TextField(
                    value = translation,
                    onValueChange = { translation = it },
                    label = { Text("Translation (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                TextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Reference (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Infinite Counter",
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
                        text = "Repeat Count",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { if (requiredRepeats > 1) requiredRepeats-- },
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(50.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null)
                        }
                        
                        Text(
                            text = requiredRepeats.toString(),
                            style = MaterialTheme.typography.headlineSmall
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
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (!isValid) {
                                showError = "Please fill in at least Arabic text or transliteration"
                                return@Button
                            }
                            if (!isInfinite && requiredRepeats < 1) {
                                showError = "Repeat count must be at least 1"
                                return@Button
                            }
                            onSave(arabicText, transliteration, translation, reference, requiredRepeats, isInfinite)
                        }
                    ) {
                        Text("Save Zikr")
                    }
                }
            }
        }
    }
}