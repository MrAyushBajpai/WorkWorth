package com.mrayushbajpai.workworth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
    val currentMonthYear = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    
    val currentMonthTransactions = transactions.filter { it.monthYear == currentMonthYear }
    val previousTransactions = transactions.filter { it.monthYear != currentMonthYear }
    
    val totalIncome by settingsManager.monthlySalaryFlow.collectAsState(initial = 0.0)
    val daysWorked by settingsManager.daysWorkedFlow.collectAsState(initial = 0.0)
    val totalLifeHours = currentMonthTransactions.sumOf { it.timeCost }

    Column(modifier = modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("History", fontWeight = FontWeight.Bold) }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Current Month Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                SummaryCard(
                    income = totalIncome,
                    days = daysWorked,
                    hours = totalLifeHours
                )
            }

            if (previousTransactions.isNotEmpty()) {
                item {
                    Text(
                        text = "Previous Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                val groupedTransactions = previousTransactions.groupBy { it.monthYear }
                groupedTransactions.forEach { (month, monthTransactions) ->
                    item {
                        Text(
                            text = month,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(monthTransactions) { transaction ->
                        TransactionCard(transaction = transaction, onDelete = {})
                    }
                }
            } else if (currentMonthTransactions.isNotEmpty()) {
                 item {
                    Text(
                        text = "This Month's Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(currentMonthTransactions) { transaction ->
                     TransactionCard(transaction = transaction, onDelete = {})
                }
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
                    HistoryInfoItem(label = "Total Income", value = "$${"%,.2f".format(income)}")
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
