package com.mrayushbajpai.workworth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelsScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()
    val editing = uiState.editingLabel
    
    var newLabelName by remember(editing) { mutableStateOf(editing?.name ?: "") }
    var selectedColor by remember(editing) { 
        mutableStateOf(if (editing != null) Color(editing.color) else Color(0xFF008080)) 
    }

    val colors = listOf(
        Color(0xFF008080), Color(0xFFE91E63), Color(0xFF9C27B0),
        Color(0xFF673AB7), Color(0xFF3F51B5), Color(0xFF2196F3),
        Color(0xFF4CAF50), Color(0xFFFFC107), Color(0xFFFF5722)
    )

    // Confirmation Dialog
    uiState.labelToDelete?.let { label ->
        WorkworthConfirmationDialog(
            title = "Delete Label?",
            message = "Deleting this label will remove it from all associated transactions. This cannot be undone.",
            onConfirm = { viewModel.deleteLabel(label.id) },
            onDismiss = { viewModel.dismissDeleteLabel() }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("Manage Labels", fontWeight = FontWeight.Bold) }
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newLabelName,
                    onValueChange = { newLabelName = it },
                    label = { Text("Label Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    trailingIcon = {
                        if (editing != null) {
                            IconButton(onClick = { viewModel.cancelEditingLabel() }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel Edit")
                            }
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text("Select Color", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { selectedColor = color }
                            .then(
                                if (selectedColor == color) 
                                    Modifier.background(color.copy(alpha = 0.5f), CircleShape) 
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedColor == color) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.White))
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (newLabelName.isNotBlank()) {
                        viewModel.addOrUpdateLabel(newLabelName, selectedColor.toArgb())
                        newLabelName = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = newLabelName.isNotBlank()
            ) {
                Icon(if (editing != null) Icons.Default.Check else Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (editing != null) "Update Label" else "Add Label")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Existing Labels",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.labels.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No labels created yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.labels, key = { it.id }) { label ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (editing?.id == label.id) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(label.color))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Text(
                                    text = label.name, 
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Row {
                                    IconButton(onClick = { viewModel.startEditingLabel(label) }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { viewModel.confirmDeleteLabel(label) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
