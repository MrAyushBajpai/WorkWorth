package com.mrayushbajpai.workworth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
fun CalculationScreen(settingsManager: SettingsManager, modifier: Modifier = Modifier) {
    val savedSalary by settingsManager.monthlySalaryFlow.collectAsState(initial = 0.0)
    val savedDays by settingsManager.daysWorkedFlow.collectAsState(initial = 0.0)
    val savedTransactions by settingsManager.transactionsFlow.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    if (savedSalary <= 0.0 || savedDays <= 0.0) {
        SetupScreen(
            onSave = { salary, days ->
                coroutineScope.launch {
                    val currentMonthYear = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                    settingsManager.saveSettings(salary, days, currentMonthYear)
                }
            },
            modifier = modifier
        )
    } else {
        MainScreen(
            salary = savedSalary,
            daysWorked = savedDays,
            transactions = savedTransactions,
            onSaveTransactions = { updatedList ->
                coroutineScope.launch {
                    settingsManager.saveTransactions(updatedList)
                }
            },
            onReset = {
                coroutineScope.launch {
                    settingsManager.clearSettings()
                }
            },
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
fun MainScreen(
    salary: Double,
    daysWorked: Double,
    transactions: List<Transaction>,
    onSaveTransactions: (List<Transaction>) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val totalSpent = transactions.sumOf { it.amount }
    val remainingMoney = salary - totalSpent
    
    // Calculate days remaining based on money
    val moneyDaysLeft = if (salary > 0) (remainingMoney / salary) * daysWorked else 0.0
    
    // Calendar days remaining (for info only)
    val today = LocalDate.now()
    val lastDayOfMonth = YearMonth.from(today).atEndOfMonth()
    val calendarDaysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, lastDayOfMonth).toInt()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF008080),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("WorkWorth", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onReset) {
                        Icon(Icons.Default.Settings, contentDescription = "Reset Settings")
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
            // Hero Section (Teal Gradient Box)
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
                        text = "${"%.1f".format(moneyDaysLeft)} Days Remaining",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Text(
                        text = "Balance: $${"%,.2f".format(remainingMoney)}",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoItem(label = "Monthly Salary", value = "$${"%,.0f".format(salary)}")
                        InfoItem(label = "Calendar Left", value = "$calendarDaysLeft Days")
                    }
                }
            }

            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                fontWeight = FontWeight.Bold
            )

            if (transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions yet. Tap + to add one!", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(transactions.reversed(), key = { it.id }) { transaction ->
                        TransactionCard(
                            transaction = transaction,
                            onDelete = {
                                val newList = transactions.filter { it.id != transaction.id }
                                onSaveTransactions(newList)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            hourlyRate = salary / (daysWorked * 8),
            onDismiss = { showAddDialog = false },
            onAdd = { name, amount, timeCost ->
                val newTransaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    amount = amount,
                    timeCost = timeCost
                )
                onSaveTransactions(transactions + newTransaction)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column {
        Text(text = label, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
        Text(text = value, color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TransactionCard(transaction: Transaction, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            // Icon Placeholder
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
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${"%.2f".format(transaction.amount)}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun AddTransactionDialog(
    hourlyRate: Double,
    onDismiss: () -> Unit,
    onAdd: (String, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Transaction") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Expense Name") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                val amount = amountInput.toDoubleOrNull() ?: 0.0
                if (amount > 0) {
                    val timeCost = amount / hourlyRate
                    Text(
                        text = "Cost in time: ${"%.1f".format(timeCost)} hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF008080),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountInput.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && amount > 0) {
                        onAdd(name, amount, amount / hourlyRate)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008080))
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
