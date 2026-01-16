package com.mrayushbajpai.workworth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.mrayushbajpai.workworth.ui.theme.WorkWorthTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val settingsManager = SettingsManager(this)
        
        // Check for month change
        lifecycleScope.launch {
            val currentMonthYear = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            val savedMonthYear = settingsManager.savedMonthFlow.first()
            
            if (savedMonthYear != null && savedMonthYear != currentMonthYear) {
                settingsManager.clearSettings()
            }
        }
        
        setContent {
            WorkWorthTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CalculationScreen(
                        settingsManager = settingsManager,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
