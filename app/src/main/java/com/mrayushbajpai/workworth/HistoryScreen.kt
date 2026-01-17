package com.mrayushbajpai.workworth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    }.toMutableList()

    // Ensure current month is represented even if no transactions
    if (!months.contains(uiState.currentMonthYear) && uiState.salary > 0) {
        months.add(0, uiState.currentMonthYear)
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
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (months.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No history available yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                months.forEach { month ->
                    val transactions = groupedTransactions[month] ?: emptyList()
                    val summary = uiState.monthlySummaries[month]
                    val salary = summary?.salary ?: (if (month == uiState.currentMonthYear) uiState.salary else 0.0)
                    val daysWorked = summary?.daysWorked ?: (if (month == uiState.currentMonthYear) uiState.daysWorked else 0.0)
                    val totalSpent = transactions.sumOf { it.amount }

                    stickyHeader {
                        MonthHeader(
                            month = month,
                            salary = salary,
                            daysWorked = daysWorked,
                            totalSpent = totalSpent
                        )
                    }

                    items(transactions.sortedByDescending { it.timestamp }, key = { it.id }) { transaction ->
                        val labels = transaction.labelIds.mapNotNull { id -> 
                            uiState.labels.find { it.id == id } 
                        }
                        
                        HistoryItemRow(
                            icon = getIconForTransaction(transaction),
                            title = transaction.name,
                            labels = labels,
                            amount = "-$${"%,.2f".format(transaction.amount)}",
                            time = "-${"%.1f".format(transaction.timeCost)} Hours",
                            isPositive = false,
                            onClick = { viewModel.startEditingTransaction(transaction) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                    
                    // Add some space after each month's transactions
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
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
fun MonthHeader(
    month: String,
    salary: Double,
    daysWorked: Double,
    totalSpent: Double
) {
    val remaining = if (salary > 0) salary - totalSpent else 0.0
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 2.dp // Slight elevation to distinguish it
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = month,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (salary > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Days: ${daysWorked.toInt()}",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Income: $${"%,.0f".format(salary)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Remaining: $${"%,.0f".format(remaining)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (remaining >= 0) Color(0xFF008080) else Color.Red
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryItemRow(
    icon: ImageVector,
    title: String,
    labels: List<Label>,
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
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
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
                
                if (labels.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        labels.forEach { label ->
                            val labelColor = Color(label.color)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, labelColor.copy(alpha = 0.5f)),
                                color = labelColor.copy(alpha = 0.1f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = label.name,
                                    fontSize = 10.sp,
                                    color = labelColor,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Expense",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
