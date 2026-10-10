package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.math.Complex
import com.example.math.MathParser
import com.example.ui.components.FunctionGraphCanvas
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val funcs by viewModel.graphFunctions.collectAsStateWithLifecycle()
    val xMin by viewModel.xMin.collectAsStateWithLifecycle()
    val xMax by viewModel.xMax.collectAsStateWithLifecycle()
    val yMin by viewModel.yMin.collectAsStateWithLifecycle()
    val yMax by viewModel.yMax.collectAsStateWithLifecycle()
    val traceX by viewModel.traceX.collectAsStateWithLifecycle()
    val tableData by viewModel.tableData.collectAsStateWithLifecycle()
    val angleMode by viewModel.angleMode.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Graph, 1: Table
    var showFunctionEditor by remember { mutableStateOf(true) }
    var showTangent by remember { mutableStateOf(false) }
    var showAreaShading by remember { mutableStateOf(false) }

    // Table settings
    var tableStartInput by remember { mutableStateOf("-5") }
    var tableEndInput by remember { mutableStateOf("5") }
    var tableStepInput by remember { mutableStateOf("1") }

    val parser = remember(angleMode) { MathParser(angleMode) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab Row & Top Graph Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .width(220.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Graph", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        viewModel.generateTable(
                            tableStartInput.toDoubleOrNull() ?: -5.0,
                            tableEndInput.toDoubleOrNull() ?: 5.0,
                            tableStepInput.toDoubleOrNull() ?: 1.0
                        )
                        selectedTab = 1
                    },
                    text = { Text("Table", fontWeight = FontWeight.SemiBold) }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Auto Fit Y
                IconButton(
                    onClick = { viewModel.autoFitY() },
                    modifier = Modifier.testTag("btn_autofit_y")
                ) {
                    Icon(
                        imageVector = Icons.Default.Height,
                        contentDescription = "Auto Fit Y",
                        tint = CyanNeon
                    )
                }

                // Reset Viewport
                IconButton(
                    onClick = { viewModel.resetGraphViewport() },
                    modifier = Modifier.testTag("btn_reset_viewport")
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Reset Viewport",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Toggle Function Editor Panel
                IconButton(
                    onClick = { showFunctionEditor = !showFunctionEditor },
                    modifier = Modifier.testTag("btn_toggle_func_panel")
                ) {
                    Icon(
                        imageVector = if (showFunctionEditor) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Function Panel",
                        tint = PurpleNeon
                    )
                }
            }
        }

        if (selectedTab == 0) {
            // GRAPH VIEW
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF070B12))
            ) {
                // Real-time Canvas Graph Component
                FunctionGraphCanvas(
                    functions = funcs,
                    mathParser = parser,
                    xMin = xMin,
                    xMax = xMax,
                    yMin = yMin,
                    yMax = yMax,
                    traceX = traceX,
                    showTangent = showTangent,
                    showAreaShading = showAreaShading,
                    onViewportChange = { nXMin, nXMax, nYMin, nYMax ->
                        viewModel.setViewport(nXMin, nXMax, nYMin, nYMax)
                    },
                    onTraceChange = { tx ->
                        viewModel.setTraceX(tx)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Viewport Controls Overlay (Zoom In, Zoom Out)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledIconButton(
                        onClick = { viewModel.zoomGraph(1.35f) },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xCC1E293B)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White)
                    }

                    FilledIconButton(
                        onClick = { viewModel.zoomGraph(0.74f) },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xCC1E293B)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White)
                    }
                }

                // Quick Viewport Range presets & Tools overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = showTangent,
                        onClick = { showTangent = !showTangent },
                        label = { Text("Tangent", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberAccent.copy(alpha = 0.2f),
                            selectedLabelColor = AmberAccent
                        ),
                        modifier = Modifier.height(28.dp)
                    )

                    FilterChip(
                        selected = showAreaShading,
                        onClick = { showAreaShading = !showAreaShading },
                        label = { Text("Area ∫", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldNeon.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldNeon
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }

                // Interactive Trace Pill (Shows (x, f(x)) coordinates & derivative slope)
                traceX?.let { tx ->
                    val primaryFunc = funcs.firstOrNull { it.isVisible && it.expression.isNotBlank() }
                    val ty = if (primaryFunc != null) parser.evaluateReal(primaryFunc.expression, tx) else Double.NaN

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xDD0F172A),
                        border = ButtonDefaults.outlinedButtonBorder,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "X: ${Complex.formatDouble(tx, 3)}",
                                color = CyanNeon,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            if (!ty.isNaN() && !ty.isInfinite()) {
                                Text(
                                    text = "Y: ${Complex.formatDouble(ty, 3)}",
                                    color = AmberAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                if (showTangent && primaryFunc != null) {
                                    val h = 1e-5
                                    val slope = (parser.evaluateReal(primaryFunc.expression, tx + h) -
                                        parser.evaluateReal(primaryFunc.expression, tx - h)) / (2.0 * h)
                                    if (!slope.isNaN() && !slope.isInfinite()) {
                                        Text(
                                            text = "f'(x): ${Complex.formatDouble(slope, 2)}",
                                            color = EmeraldNeon,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = { viewModel.setTraceX(null) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear trace", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // Real-time Function Input & Presets Section
            AnimatedVisibility(visible = showFunctionEditor) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    // Quick Function Presets Bar
                    val presets = listOf(
                        "sin(x)",
                        "cos(x)",
                        "tan(x)",
                        "x^2 - 4",
                        "x^3 - 3x",
                        "exp(-x^2)",
                        "exp(-0.2x)*cos(3x)",
                        "1/x",
                        "ln(x)",
                        "abs(x) - 3",
                        "sqrt(16 - x^2)"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { preset ->
                            SuggestionChip(
                                onClick = { viewModel.addGraphFunction(preset) },
                                label = { Text(preset, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Real-time Function List with Color Indicators & Validation
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 165.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(funcs) { index, fn ->
                            val isValid = remember(fn.expression) {
                                if (fn.expression.isBlank()) false
                                else {
                                    val testVal = parser.evaluateReal(fn.expression, 1.0)
                                    !testVal.isNaN() && !testVal.isInfinite()
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Color dot indicator
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(fn.color)
                                )

                                Text(
                                    text = "y = ",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )

                                OutlinedTextField(
                                    value = fn.expression,
                                    onValueChange = { newExpr ->
                                        viewModel.updateGraphFunction(index, newExpr, fn.isVisible)
                                    },
                                    placeholder = { Text("e.g. sin(x)") },
                                    singleLine = true,
                                    trailingIcon = {
                                        if (fn.expression.isNotBlank()) {
                                            Icon(
                                                imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Info,
                                                contentDescription = if (isValid) "Valid formula" else "Evaluating...",
                                                tint = if (isValid) EmeraldNeon else AmberAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = fn.color,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )

                                // Visibility eye toggle
                                IconButton(
                                    onClick = { viewModel.toggleFunctionVisibility(index) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (fn.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Visibility",
                                        tint = if (fn.isVisible) fn.color else MaterialTheme.colorScheme.outline
                                    )
                                }

                                // Delete function
                                if (funcs.size > 1) {
                                    IconButton(
                                        onClick = { viewModel.removeGraphFunction(index) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Function",
                                            tint = CoralNeon
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Button(
                                onClick = { viewModel.addGraphFunction("x") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Another Function", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        } else {
            // TABLE OF VALUES VIEW
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Table of Values Configuration",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = tableStartInput,
                                onValueChange = { tableStartInput = it },
                                label = { Text("Start X") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = tableEndInput,
                                onValueChange = { tableEndInput = it },
                                label = { Text("End X") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = tableStepInput,
                                onValueChange = { tableStepInput = it },
                                label = { Text("Step Δx") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                viewModel.generateTable(
                                    tableStartInput.toDoubleOrNull() ?: -5.0,
                                    tableEndInput.toDoubleOrNull() ?: 5.0,
                                    tableStepInput.toDoubleOrNull() ?: 1.0
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Compute Table Values")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Table Header
                val visibleFuncs = funcs.filter { it.isVisible && it.expression.isNotBlank() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("X", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    visibleFuncs.forEach { fn ->
                        Text(
                            text = "y = ${fn.expression}",
                            color = fn.color,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                }

                // Table Rows
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(tableData) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = Complex.formatDouble(row.x, 3),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            row.values.forEach { v ->
                                Text(
                                    text = if (v.isNaN()) "Undefined" else Complex.formatDouble(v, 4),
                                    color = if (v.isNaN()) CoralNeon else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1.5f)
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }
}
