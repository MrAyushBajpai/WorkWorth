package com.mrayushbajpai.workworth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.House
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Group all transactions by month
    val groupedTransactions = uiState.transactions.groupBy { it.monthYear }
    
    // Get sorted list of months (descending)
    val months = groupedTransactions.keys.sortedByDescending { monthStr ->
        try {
            LocalDate.parse("01 $monthStr", DateTimeFormatter.ofPattern("dd MMMM yyyy"))
        } catch (e: Exception) {
            LocalDate.now()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("History", fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (months.isEmpty() && uiState.salary <= 0) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No history available yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // If it's the current month and there's a salary but maybe no transactions yet, 
                // we should still show the current month header if possible.
                val displayMonths = if (months.isEmpty() && uiState.salary > 0) listOf(uiState.currentMonthYear) else months

                items(displayMonths) { month ->
                    val transactions = groupedTransactions[month] ?: emptyList()
                    MonthHistoryCard(
                        month = month,
                        transactions = transactions,
                        salary = if (month == uiState.currentMonthYear) uiState.salary else 0.0,
                        daysWorked = if (month == uiState.currentMonthYear) uiState.daysWorked else 0.0,
                        onDeleteTransaction = { viewModel.confirmDeleteTransaction(it) },
                        onEditTransaction = { viewModel.startEditingTransaction(it) }
                    )
                }
            }
        }
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
}

@Composable
fun MonthHistoryCard(
    month: String,
    transactions: List<Transaction>,
    salary: Double,
    daysWorked: Double,
    onDeleteTransaction: (Transaction) -> Unit,
    onEditTransaction: (Transaction) -> Unit
) {
    Column {
        Text(
            text = month,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        val totalSpent = transactions.sumOf { it.amount }
        val remaining = if (salary > 0) salary - totalSpent else 0.0
        
        Text(
            text = "Remaining: $${"%,.0f".format(remaining)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                // Income Summary Row (if salary > 0)
                if (salary > 0) {
                    HistoryItemRow(
                        icon = Icons.Outlined.AccountBalanceWallet,
                        title = "Monthly Salary",
                        subtitle = "Days Worked: ${daysWorked.toInt()}",
                        amount = "+$${"%,.0f".format(salary)}",
                        time = "+${(daysWorked * 8).toInt()}.0 Hours",
                        isPositive = true
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }

                transactions.sortedByDescending { it.timestamp }.forEachIndexed { index, transaction ->
                    HistoryItemRow(
                        icon = getIconForTransaction(transaction),
                        title = transaction.name,
                        subtitle = "Expense",
                        amount = "-$${"%,.2f".format(transaction.amount)}",
                        time = "-${"%.1f".format(transaction.timeCost)} Hours",
                        isPositive = false,
                        onClick = { onEditTransaction(transaction) }
                    )
                    if (index < transactions.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    amount: String,
    time: String,
    isPositive: Boolean,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Right side values
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isPositive) Color(0xFF4CAF50) else Color(0xFF008080)
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun getIconForTransaction(transaction: Transaction): ImageVector {
    val name = transaction.name.lowercase()
    return when {
        name.contains("rent") -> Icons.Outlined.House
        name.contains("salary") || name.contains("income") -> Icons.Outlined.Payments
        name.contains("coffee") || name.contains("food") || name.contains("grocer") -> Icons.Outlined.Coffee
        name.contains("electric") -> Icons.Outlined.ElectricBolt
        name.contains("gas") || name.contains("fuel") -> Icons.Outlined.LocalGasStation
        name.contains("shop") -> Icons.Outlined.ShoppingCart
        else -> Icons.Outlined.Payments
    }
}
