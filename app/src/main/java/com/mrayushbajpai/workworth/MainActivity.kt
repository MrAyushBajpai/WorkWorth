package com.mrayushbajpai.workworth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mrayushbajpai.workworth.ui.theme.WorkWorthTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsManager = SettingsManager(this)

        // Month change check logic is still relevant but we now support history.
        // We might want to keep current settings but start fresh for the NEW month in the UI.
        // For now, let's keep the core logic and ensure transitions work.

        setContent {
            WorkWorthTheme {
                MainApp(settingsManager)
            }
        }
    }
}

@Composable
fun MainApp(settingsManager: SettingsManager) {
    val navController = rememberNavController()
    var showAddSheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val savedSalary by settingsManager.monthlySalaryFlow.collectAsState(initial = 0.0)
    val savedDays by settingsManager.daysWorkedFlow.collectAsState(initial = 0.0)
    val transactions by settingsManager.transactionsFlow.collectAsState(initial = emptyList())

    Scaffold(
        bottomBar = {
            BottomAppBar(
                actions = {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    IconButton(onClick = {
                        navController.navigate("home") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Home",
                            tint = if (currentDestination?.route == "home") Color(0xFF008080) else Color.Gray
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    IconButton(onClick = {
                        navController.navigate("history") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "History",
                            tint = if (currentDestination?.route == "history") Color(0xFF008080) else Color.Gray
                        )
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showAddSheet = true },
                        containerColor = Color(0xFF008080),
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(32.dp))
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                CalculationScreen(settingsManager = settingsManager)
            }
            composable("history") {
                HistoryScreen(settingsManager = settingsManager)
            }
        }
    }

    if (showAddSheet && savedSalary > 0 && savedDays > 0) {
        AddTransactionSheet(
            hourlyRate = savedSalary / (savedDays * 8),
            onDismiss = { showAddSheet = false },
            onAdd = { name, amount, timeCost ->
                coroutineScope.launch {
                    val currentMonthYear = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                    val newTransaction = Transaction(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        amount = amount,
                        timeCost = timeCost,
                        monthYear = currentMonthYear
                    )
                    settingsManager.saveTransactions(transactions + newTransaction)
                    showAddSheet = false
                }
            }
        )
    }
}
