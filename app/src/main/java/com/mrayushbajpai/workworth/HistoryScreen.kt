package com.mrayushbajpai.workworth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(settingsManager: SettingsManager, modifier: Modifier = Modifier) {
    val transactions by settingsManager.transactionsFlow.collectAsState(initial = emptyList())
    val monthlySummaries by settingsManager.monthlySummariesFlow.collectAsState(initial = emptyMap())
    val allLabels by settingsManager.labelsFlow.collectAsState(initial = emptyList())
    
    val currentMonthYear = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    val currentSalary by settingsManager.monthlySalaryFlow.collectAsState(initial = 0.0)
    val currentDays by settingsManager.daysWorkedFlow.collectAsState(initial = 0.0)
    
    // Group all transactions by month
    val groupedTransactions = transactions.groupBy { it.monthYear }

    val totalLifeHours = transactions.filter { it.monthYear == currentMonthYear }.sumOf { it.timeCost }

    Column(modifier = modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("History", fontWeight = FontWeight.Bold) }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current Month Summary Card
            item {
                Text(
                    text = "Current Month Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                SummaryCard(
                    income = currentSalary,
                    days = currentDays,
                    hours = totalLifeHours
                )
            }

            if (transactions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                        Text("No transaction history found.", color = Color.Gray)
                    }
                }
            } else {
                item {
                    Text(
                        text = "Transactions by Month",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Iterate through all months that have transactions
                groupedTransactions.forEach { (month, monthTransactions) ->
                    item {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = month,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            
                            val summary = monthlySummaries[month] ?: if (month == currentMonthYear) {
                                MonthlySummary(currentSalary, currentDays)
                            } else null

                            if (summary != null && summary.salary > 0) {
                                Text(
                                    text = "₹${"%,.0f".format(summary.salary)} • ${"%.1f".format(summary.daysWorked)} days",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    items(monthTransactions.reversed()) { transaction ->
                        TransactionHistoryCard(transaction = transaction, allLabels = allLabels)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionHistoryCard(transaction: Transaction, allLabels: List<Label>) {
    val labelsToDisplay = transaction.labelIds.mapNotNull { id -> 
        allLabels.find { it.id == id } 
    }

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
                    text = "₹${"%,.2f".format(transaction.amount)}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun SummaryCard(income: Double, days: Double, hours: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF2C3E50), Color(0xFF4CA1AF))
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    HistoryInfoItem(label = "Total Income", value = "₹${"%,.2f".format(income)}")
                    HistoryInfoItem(label = "Days Worked", value = "${"%.1f".format(days)}")
                }
                Spacer(modifier = Modifier.height(16.dp))
                HistoryInfoItem(label = "Total Life Hours Spent", value = "${"%.1f".format(hours)} hrs")
            }
        }
    }
}

@Composable
fun HistoryInfoItem(label: String, value: String) {
    Column {
        Text(text = label, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
        Text(text = value, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}
