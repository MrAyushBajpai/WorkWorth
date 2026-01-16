package com.mrayushbajpai.workworth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CalculationScreen(settingsManager: SettingsManager, modifier: Modifier = Modifier) {
    val savedSalary by settingsManager.monthlySalaryFlow.collectAsState(initial = 0.0)
    val savedDays by settingsManager.daysWorkedFlow.collectAsState(initial = 0.0)
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
            days = savedDays,
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

@Composable
fun MainScreen(salary: Double, days: Double, onReset: () -> Unit, modifier: Modifier = Modifier) {
    var expenseAmount by remember { mutableStateOf("") }
    val expense = expenseAmount.toDoubleOrNull() ?: 0.0

    val hoursOfLife = if (salary > 0 && days > 0) {
        val hourlyRate = salary / (days * 8)
        expense / hourlyRate
    } else {
        0.0
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "WorkWorth Calculator",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Settings for ${LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = expenseAmount,
            onValueChange = { expenseAmount = it.filter { char -> char.isDigit() || char == '.' } },
            label = { Text("Expense Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            singleLine = true
        )

        if (expense > 0) {
            Text(
                text = "This expense will cost you ${"%.1f".format(hoursOfLife)} hours of your life.",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 48.dp)
            )
        } else {
            Text(
                text = "Enter an amount to see its cost in time.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 48.dp)
            )
        }

        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset Profile")
        }
    }
}
