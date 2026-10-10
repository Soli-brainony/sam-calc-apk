package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.SciGraphTheme

enum class AppDestination(val label: String, val icon: ImageVector, val tag: String) {
    CALCULATOR("Calculator", Icons.Default.Calculate, "tab_calc"),
    GRAPHING("Graphing", Icons.Default.ShowChart, "tab_graph"),
    SOLVER("Solver", Icons.Default.AutoGraph, "tab_solver"),
    HISTORY("Quick Ref", Icons.Default.Bookmarks, "tab_history")
}

@Composable
fun MainScreen(
    viewModel: CalculatorViewModel
) {
    var currentDestination by remember { mutableStateOf(AppDestination.CALCULATOR) }
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    SciGraphTheme(darkTheme = isDarkTheme) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("main_bottom_nav"),
                    tonalElevation = 6.dp
                ) {
                    AppDestination.values().forEach { destination ->
                        val isSelected = currentDestination == destination
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentDestination = destination },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label
                                )
                            },
                            label = {
                                Text(
                                    text = destination.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = CyanNeon.copy(alpha = 0.2f),
                                selectedIconColor = CyanNeon
                            ),
                            modifier = Modifier.testTag(destination.tag)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentDestination) {
                    AppDestination.CALCULATOR -> CalculatorScreen(viewModel = viewModel)
                    AppDestination.GRAPHING -> GraphScreen(viewModel = viewModel)
                    AppDestination.SOLVER -> SolverScreen(viewModel = viewModel)
                    AppDestination.HISTORY -> HistoryScreen(
                        viewModel = viewModel,
                        onNavigateToCalculator = { currentDestination = AppDestination.CALCULATOR }
                    )
                }
            }
        }
    }
}
