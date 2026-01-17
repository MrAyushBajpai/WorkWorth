package com.mrayushbajpai.workworth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mrayushbajpai.workworth.ui.theme.WorkWorthTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsManager = SettingsManager(this)
        val repository = WorkworthRepository(settingsManager)

        setContent {
            WorkWorthTheme {
                val viewModel: MainViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return MainViewModel(repository) as T
                        }
                    }
                )
                MainApp(viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    var showAddSheet by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    // Sync sheet visibility with editing state
    LaunchedEffect(uiState.editingTransaction) {
        if (uiState.editingTransaction != null) {
            showAddSheet = true
        }
    }

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
                        navController.navigate("labels") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = "Labels",
                            tint = if (currentDestination?.route == "labels") Color(0xFF008080) else Color.Gray
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
                    if (uiState.salary > 0 && uiState.daysWorked > 0) {
                        FloatingActionButton(
                            onClick = { 
                                viewModel.cancelEditingTransaction()
                                showAddSheet = true 
                            },
                            containerColor = Color(0xFF008080),
                            contentColor = Color.White,
                            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(32.dp))
                        }
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
                CalculationScreen(
                    viewModel = viewModel,
                    onSeeAll = {
                        navController.navigate("history") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable("labels") {
                LabelsScreen(viewModel = viewModel)
            }
            composable("history") {
                HistoryScreen(viewModel = viewModel)
            }
        }
    }

    if (showAddSheet) {
        AddTransactionSheet(
            uiState = uiState,
            onDismiss = { 
                showAddSheet = false
                viewModel.cancelEditingTransaction()
            },
            onAdd = { name, amount, selectedLabels ->
                viewModel.addOrUpdateTransaction(name, amount, selectedLabels)
                showAddSheet = false
            }
        )
    }
}
