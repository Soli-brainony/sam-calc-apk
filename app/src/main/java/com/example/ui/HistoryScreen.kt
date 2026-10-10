package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CalculationEntity
import com.example.data.SavedEquationEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: CalculatorViewModel,
    onNavigateToCalculator: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: History, 1: Equations & Formulas
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()
    val savedEquations by viewModel.savedEquationsList.collectAsStateWithLifecycle()

    val historyQuery by viewModel.historySearchQuery.collectAsStateWithLifecycle()
    val onlyFavorites by viewModel.historyFilterOnlyFavorites.collectAsStateWithLifecycle()

    val equationCategory by viewModel.equationCategoryFilter.collectAsStateWithLifecycle()
    val equationQuery by viewModel.equationSearchQuery.collectAsStateWithLifecycle()

    var showClearDialog by remember { mutableStateOf(false) }
    var showAddEquationDialog by remember { mutableStateOf(false) }
    var itemToLabel by remember { mutableStateOf<CalculationEntity?>(null) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (selectedTab == 0) "Calculation History" else "Saved Equations & Reference",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    actions = {
                        if (selectedTab == 0 && historyList.isNotEmpty()) {
                            IconButton(
                                onClick = { showClearDialog = true },
                                modifier = Modifier.testTag("btn_clear_history")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear History",
                                    tint = CoralNeon
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Tab Switcher: Calculations vs Saved Equations
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("History (${historyList.size})", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Bookmarks, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Equations (${savedEquations.size})", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                ExtendedFloatingActionButton(
                    onClick = { showAddEquationDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Save Equation") },
                    containerColor = CyanNeon,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_equation")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedTab == 0) {
                // --- HISTORY VIEW ---
                // Search & Filter Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = historyQuery,
                        onValueChange = { viewModel.historySearchQuery.value = it },
                        placeholder = { Text("Search calculations...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (historyQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.historySearchQuery.value = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Favorite Filter Toggle
                    FilterChip(
                        selected = onlyFavorites,
                        onClick = { viewModel.historyFilterOnlyFavorites.value = !onlyFavorites },
                        label = { Text("★ Starred") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberAccent.copy(alpha = 0.2f),
                            selectedLabelColor = AmberAccent
                        ),
                        modifier = Modifier.height(48.dp)
                    )
                }

                if (historyList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = if (historyQuery.isNotEmpty() || onlyFavorites) "No matching calculations" else "No calculations saved yet",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Every calculation you evaluate is automatically stored in Room database for quick reference",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(historyList, key = { it.id }) { item ->
                            HistoryItemCard(
                                item = item,
                                dateFormat = dateFormat,
                                onExpressionClick = {
                                    viewModel.insertHistory(item)
                                    onNavigateToCalculator()
                                },
                                onResultClick = {
                                    viewModel.insertHistoryResult(item)
                                    onNavigateToCalculator()
                                },
                                onToggleFavorite = {
                                    viewModel.toggleHistoryFavorite(item)
                                },
                                onEditLabel = {
                                    itemToLabel = item
                                },
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Result", item.result)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied result: ${item.result}", Toast.LENGTH_SHORT).show()
                                },
                                onDelete = {
                                    viewModel.deleteHistoryItem(item)
                                }
                            )
                        }
                    }
                }
            } else {
                // --- SAVED EQUATIONS VIEW ---
                // Search Bar
                OutlinedTextField(
                    value = equationQuery,
                    onValueChange = { viewModel.equationSearchQuery.value = it },
                    placeholder = { Text("Search equations or formulas...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (equationQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.equationSearchQuery.value = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                // Category Chips
                val categories = listOf("All", "Algebra", "Geometry", "Physics", "Complex", "Finance", "Custom")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = equationCategory == cat,
                            onClick = { viewModel.equationCategoryFilter.value = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }

                if (savedEquations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmarks,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "No equations found",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(savedEquations, key = { it.id }) { eq ->
                            SavedEquationCard(
                                equation = eq,
                                onUseInCalc = {
                                    viewModel.insertEquationIntoCalc(eq.expression)
                                    onNavigateToCalculator()
                                },
                                onAppendToCalc = {
                                    viewModel.appendEquationIntoCalc(eq.expression)
                                    onNavigateToCalculator()
                                },
                                onToggleFavorite = {
                                    viewModel.toggleEquationFavorite(eq)
                                },
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Equation", eq.expression)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied: ${eq.expression}", Toast.LENGTH_SHORT).show()
                                },
                                onDelete = if (!eq.isBuiltIn) {
                                    { viewModel.deleteEquation(eq) }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        // Clear History Dialog
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Clear All History") },
                text = { Text("Are you sure you want to delete all saved calculations from local storage?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAllHistory()
                            showClearDialog = false
                        }
                    ) {
                        Text("Clear All", color = CoralNeon, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Add Custom Equation Dialog
        if (showAddEquationDialog) {
            var newTitle by remember { mutableStateOf("") }
            var newExpr by remember { mutableStateOf("") }
            var newDesc by remember { mutableStateOf("") }
            var newCat by remember { mutableStateOf("Custom") }

            AlertDialog(
                onDismissRequest = { showAddEquationDialog = false },
                title = { Text("Save Equation for Reference") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Equation Title*") },
                            placeholder = { Text("e.g. Voltage Divider") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newExpr,
                            onValueChange = { newExpr = it },
                            label = { Text("Formula / Expression*") },
                            placeholder = { Text("e.g. Vin * R2 / (R1 + R2)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newDesc,
                            onValueChange = { newDesc = it },
                            label = { Text("Description / Notes") },
                            placeholder = { Text("e.g. Calculate Vout across R2") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newCat,
                            onValueChange = { newCat = it },
                            label = { Text("Category") },
                            placeholder = { Text("Algebra, Physics, Custom...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newTitle.isNotBlank() && newExpr.isNotBlank()) {
                                viewModel.saveEquation(newTitle, newExpr, newDesc, newCat)
                                showAddEquationDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                    ) {
                        Text("Save to Room", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddEquationDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Edit Label Dialog
        itemToLabel?.let { item ->
            var labelText by remember { mutableStateOf(item.label) }
            AlertDialog(
                onDismissRequest = { itemToLabel = null },
                title = { Text("Add Label / Note") },
                text = {
                    OutlinedTextField(
                        value = labelText,
                        onValueChange = { labelText = it },
                        label = { Text("Label (e.g. Hypotenuse, Project Cost)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.updateHistoryLabel(item, labelText)
                            itemToLabel = null
                        }
                    ) {
                        Text("Save Label", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToLabel = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun HistoryItemCard(
    item: CalculationEntity,
    dateFormat: SimpleDateFormat,
    onExpressionClick: () -> Unit,
    onResultClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditLabel: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = dateFormat.format(Date(item.timestamp)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SuggestionChip(
                        onClick = {},
                        label = { Text(item.angleMode, fontSize = 9.sp) },
                        modifier = Modifier.height(22.dp)
                    )
                    if (item.isComplex) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PurpleNeon.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "CMPLX",
                                color = PurpleNeon,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (item.label.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CyanNeon.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = item.label,
                                color = CyanNeon,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Favorite toggle
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Star calculation",
                            tint = if (item.isFavorite) AmberAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Add / edit label
                    IconButton(onClick = onEditLabel, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Label,
                            contentDescription = "Add label",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Copy
                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Result",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Delete
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Item",
                            tint = CoralNeon.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Expression - tap to load into calculator
            Text(
                text = item.expression,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onExpressionClick)
                    .padding(vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Result - tap to insert into calculator
            Text(
                text = "= ${item.result}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (item.isComplex) PurpleNeon else CyanNeon,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onResultClick)
                    .padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
fun SavedEquationCard(
    equation: SavedEquationEntity,
    onUseInCalc: () -> Unit,
    onAppendToCalc: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCopy: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = equation.category,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (equation.isBuiltIn) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Standard",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (equation.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Star Equation",
                            tint = if (equation.isFavorite) AmberAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Expression",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Equation",
                                tint = CoralNeon.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = equation.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (equation.description.isNotBlank()) {
                Text(
                    text = equation.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = equation.expression,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = CyanNeon,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onAppendToCalc,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("+ Append", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onUseInCalc,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Use in Calc",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
