package com.mrayushbajpai.workworth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CalculationScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Confirmation Dialog
    uiState.transactionToDelete?.let { transaction ->
        WorkworthConfirmationDialog(
            title = "Delete Transaction?",
            message = "This will permanently remove this expense and update your remaining time and money.",
            onConfirm = { viewModel.deleteTransaction(transaction.id) },
            onDismiss = { viewModel.dismissDeleteTransaction() }
        )
    }

    if (uiState.salary <= 0.0 || uiState.daysWorked <= 0.0) {
        SetupScreen(
            onSave = { salary, days ->
                viewModel.updateSettings(salary, days)
            },
            modifier = modifier
        )
    } else {
        HomeScreen(
            uiState = uiState,
            onDeleteTransaction = { viewModel.confirmDeleteTransaction(it) },
            onEditTransaction = { viewModel.startEditingTransaction(it) },
            onReset = { viewModel.resetAll() },
            modifier = modifier
        )
    }
}

@Composable
fun SetupScreen(onSave: (Double, Double) -> Unit, modifier: Modifier = Modifier) {
    var salaryInput by remember { mutableStateOf("") }
    var daysInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Setup Your Profile",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Enter details for ${LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = salaryInput,
            onValueChange = { salaryInput = it.filter { char -> char.isDigit() || char == '.' } },
            label = { Text("Monthly Salary") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = daysInput,
            onValueChange = { daysInput = it.filter { char -> char.isDigit() || char == '.' } },
            label = { Text("Days Worked per Month") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            singleLine = true
        )

        Button(
            onClick = {
                val s = salaryInput.toDoubleOrNull() ?: 0.0
                val d = daysInput.toDoubleOrNull() ?: 0.0
                if (s > 0 && d > 0) {
                    onSave(s, d)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Profile")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: WorkWorthUiState,
    onDeleteTransaction: (Transaction) -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        CenterAlignedTopAppBar(
            title = { Text("WorkWorth", fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(onClick = onReset) {
                    Icon(Icons.Default.Settings, contentDescription = "Reset Settings")
                }
            }
        )

        // Hero Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF008080), Color(0xFF00B2B2))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "${"%.1f".format(uiState.moneyDaysLeft)} Days Remaining",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    text = "Balance: $${"%,.2f".format(uiState.remainingMoney)}",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoItem(label = "Monthly Salary", value = "$${"%,.0f".format(uiState.salary)}")
                    InfoItem(label = "Calendar Left", value = "${uiState.calendarDaysLeft} Days")
                }
            }
        }

        Text(
            text = "Recent Transactions",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            fontWeight = FontWeight.Bold
        )

        if (uiState.currentMonthTransactions.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No transactions yet. Tap + to add one!", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.currentMonthTransactions.sortedByDescending { it.timestamp }, key = { it.id }) { transaction ->
                    TransactionCard(
                        transaction = transaction,
                        allLabels = uiState.labels,
                        onDelete = { onDeleteTransaction(transaction) },
                        onEdit = { onEditTransaction(transaction) }
                    )
                }
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column {
        Text(text = label, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
        Text(text = value, color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionCard(
    transaction: Transaction,
    allLabels: List<Label>,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val labelsToDisplay = transaction.labelIds.mapNotNull { id -> 
        allLabels.find { it.id == id } 
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { },
                onLongClick = onEdit
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE0F2F2)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "💸", fontSize = 24.sp)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = transaction.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "-${"%.1f".format(transaction.timeCost)} hours",
                    color = Color(0xFF008080),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                if (labelsToDisplay.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        labelsToDisplay.forEach { label ->
                            val labelColor = Color(label.color)
                            AssistChip(
                                onClick = { },
                                label = { Text(label.name, fontSize = 10.sp) },
                                modifier = Modifier.height(24.dp),
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = labelColor
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = labelColor.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${"%.2f".format(transaction.amount)}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionSheet(
    uiState: WorkWorthUiState,
    onDismiss: () -> Unit,
    onAdd: (String, Double, List<String>) -> Unit
) {
    val editing = uiState.editingTransaction
    var name by remember(editing) { mutableStateOf(editing?.name ?: "") }
    var amountInput by remember(editing) { mutableStateOf(editing?.amount?.toString() ?: "") }
    var selectedLabelIds by remember(editing) { mutableStateOf(editing?.labelIds?.toSet() ?: setOf<String>()) }
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 48.dp)
        ) {
            Text(
                text = if (editing != null) "Edit Transaction" else "Add Transaction",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Expense Name") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp)
            )
            
            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it.filter { char -> char.isDigit() || char == '.' } },
                label = { Text("Amount ($)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp)
            )

            if (uiState.labels.isNotEmpty()) {
                Text(
                    text = "Select Labels",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.labels.forEach { label ->
                        FilterChip(
                            selected = selectedLabelIds.contains(label.id),
                            onClick = {
                                selectedLabelIds = if (selectedLabelIds.contains(label.id)) {
                                    selectedLabelIds - label.id
                                } else {
                                    selectedLabelIds + label.id
                                }
                            },
                            label = { Text(label.name) }
                        )
                    }
                }
            }
            
            val amount = amountInput.toDoubleOrNull() ?: 0.0
            if (amount > 0 && uiState.salary > 0 && uiState.daysWorked > 0) {
                val timeCost = FinancialEngine.calculateTimeCost(amount, uiState.salary, uiState.daysWorked)
                Text(
                    text = "Cost in time: ${"%.1f".format(timeCost)} hours",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF008080),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            } else {
                 Spacer(modifier = Modifier.height(24.dp))
            }

            Button(
                onClick = {
                    val amountVal = amountInput.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && amountVal > 0) {
                        onAdd(name, amountVal, selectedLabelIds.toList())
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008080))
            ) {
                Text(
                    text = if (editing != null) "Update Transaction" else "Add Transaction",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
