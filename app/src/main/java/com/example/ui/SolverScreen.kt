package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.math.Complex
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolverScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    var selectedSolverIndex by remember { mutableIntStateOf(0) }
    val solverTabs = listOf("Quadratic", "Cubic", "Linear 2x2", "Root Finder", "Complex Tool")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab row for solvers
        ScrollableTabRow(
            selectedTabIndex = selectedSolverIndex,
            edgePadding = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            solverTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSolverIndex == index,
                    onClick = { selectedSolverIndex = index },
                    text = { Text(title, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedSolverIndex) {
                0 -> item { QuadraticSolverSection(viewModel) }
                1 -> item { CubicSolverSection(viewModel) }
                2 -> item { LinearSolverSection(viewModel) }
                3 -> item { RootFinderSection(viewModel) }
                4 -> item { ComplexToolSection() }
            }
        }
    }
}

@Composable
fun QuadraticSolverSection(viewModel: CalculatorViewModel) {
    val a by viewModel.quadA.collectAsStateWithLifecycle()
    val b by viewModel.quadB.collectAsStateWithLifecycle()
    val c by viewModel.quadC.collectAsStateWithLifecycle()
    val result by viewModel.quadResult.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quadratic Equation Solver",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CyanNeon
            )
            Text(
                text = "ax² + bx + c = 0",
                fontSize = 15.sp,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = a,
                    onValueChange = {
                        viewModel.quadA.value = it
                        viewModel.solveQuadratic()
                    },
                    label = { Text("a") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = b,
                    onValueChange = {
                        viewModel.quadB.value = it
                        viewModel.solveQuadratic()
                    },
                    label = { Text("b") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = c,
                    onValueChange = {
                        viewModel.quadC.value = it
                        viewModel.solveQuadratic()
                    },
                    label = { Text("c") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            result?.let { res ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = res.description,
                            color = AmberAccent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Discriminant (Δ = b² - 4ac): ${Complex.formatDouble(res.discriminant, 4)}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )

                        Text(
                            text = "Root x₁:  ${res.root1.format(6)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (res.root1.isReal) CyanNeon else PurpleNeon
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Root x₂:  ${res.root2.format(6)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (res.root2.isReal) CyanNeon else PurpleNeon
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Parabola Vertex: (h, k) = (${Complex.formatDouble(res.vertexX, 4)}, ${Complex.formatDouble(res.vertexY, 4)})",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CubicSolverSection(viewModel: CalculatorViewModel) {
    val a by viewModel.cubicA.collectAsStateWithLifecycle()
    val b by viewModel.cubicB.collectAsStateWithLifecycle()
    val c by viewModel.cubicC.collectAsStateWithLifecycle()
    val d by viewModel.cubicD.collectAsStateWithLifecycle()
    val result by viewModel.cubicResult.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Cubic Equation Solver",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CyanNeon
            )
            Text(
                text = "ax³ + bx² + cx + d = 0 (Cardano Formula)",
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = a,
                    onValueChange = {
                        viewModel.cubicA.value = it
                        viewModel.solveCubic()
                    },
                    label = { Text("a") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = b,
                    onValueChange = {
                        viewModel.cubicB.value = it
                        viewModel.solveCubic()
                    },
                    label = { Text("b") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = c,
                    onValueChange = {
                        viewModel.cubicC.value = it
                        viewModel.solveCubic()
                    },
                    label = { Text("c") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = d,
                    onValueChange = {
                        viewModel.cubicD.value = it
                        viewModel.solveCubic()
                    },
                    label = { Text("d") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            result?.let { res ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = res.description,
                            color = AmberAccent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        res.roots.forEachIndexed { i, root ->
                            Text(
                                text = "Root x${i + 1}:  ${root.format(6)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (root.isReal) CyanNeon else PurpleNeon,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LinearSolverSection(viewModel: CalculatorViewModel) {
    val a1 by viewModel.linA1.collectAsStateWithLifecycle()
    val b1 by viewModel.linB1.collectAsStateWithLifecycle()
    val c1 by viewModel.linC1.collectAsStateWithLifecycle()
    val a2 by viewModel.linA2.collectAsStateWithLifecycle()
    val b2 by viewModel.linB2.collectAsStateWithLifecycle()
    val c2 by viewModel.linC2.collectAsStateWithLifecycle()
    val result by viewModel.linearResult.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "System of 2 Linear Equations",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CyanNeon
            )
            Text(
                text = "a₁x + b₁y = c₁\na₂x + b₂y = c₂",
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text("Equation 1:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = a1,
                    onValueChange = { viewModel.linA1.value = it; viewModel.solveLinear() },
                    label = { Text("a₁") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = b1,
                    onValueChange = { viewModel.linB1.value = it; viewModel.solveLinear() },
                    label = { Text("b₁") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = c1,
                    onValueChange = { viewModel.linC1.value = it; viewModel.solveLinear() },
                    label = { Text("c₁") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Equation 2:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = a2,
                    onValueChange = { viewModel.linA2.value = it; viewModel.solveLinear() },
                    label = { Text("a₂") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = b2,
                    onValueChange = { viewModel.linB2.value = it; viewModel.solveLinear() },
                    label = { Text("b₂") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = c2,
                    onValueChange = { viewModel.linC2.value = it; viewModel.solveLinear() },
                    label = { Text("c₂") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            result?.let { res ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = res.status,
                            color = AmberAccent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Determinant (D = a₁b₂ - a₂b₁): ${Complex.formatDouble(res.determinant, 4)}",
                            fontSize = 13.sp
                        )

                        if (!res.x.isNaN() && !res.y.isNaN()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "x = ${Complex.formatDouble(res.x, 6)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanNeon
                            )
                            Text(
                                text = "y = ${Complex.formatDouble(res.y, 6)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldNeon
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RootFinderSection(viewModel: CalculatorViewModel) {
    val expr by viewModel.rootExpr.collectAsStateWithLifecycle()
    val min by viewModel.rootIntervalMin.collectAsStateWithLifecycle()
    val max by viewModel.rootIntervalMax.collectAsStateWithLifecycle()
    val roots by viewModel.foundRoots.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Numerical Root Finder",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CyanNeon
            )
            Text(
                text = "Finds all real solutions for f(x) = 0 in range [a, b]",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = expr,
                onValueChange = { viewModel.rootExpr.value = it },
                label = { Text("f(x)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = min,
                    onValueChange = { viewModel.rootIntervalMin.value = it },
                    label = { Text("Search Min (a)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = max,
                    onValueChange = { viewModel.rootIntervalMax.value = it },
                    label = { Text("Search Max (b)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.findRoots() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Find Roots", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (roots.isEmpty()) "No roots found in this interval" else "Found ${roots.size} root(s):",
                        fontWeight = FontWeight.SemiBold,
                        color = if (roots.isEmpty()) CoralNeon else AmberAccent
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    roots.forEachIndexed { i, root ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 3.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldNeon, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "x${i + 1} ≈ ${Complex.formatDouble(root, 6)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanNeon
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComplexToolSection() {
    var realInput by remember { mutableStateOf("3") }
    var imagInput by remember { mutableStateOf("4") }
    var powerNInput by remember { mutableStateOf("2") }

    val re = realInput.toDoubleOrNull() ?: 0.0
    val im = imagInput.toDoubleOrNull() ?: 0.0
    val n = powerNInput.toDoubleOrNull() ?: 2.0
    val z = Complex(re, im)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Complex Number Analysis",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PurpleNeon
            )
            Text(
                text = "Convert Rectangular (a + bi) ↔ Polar (r ∠ θ) & operations",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = realInput,
                    onValueChange = { realInput = it },
                    label = { Text("Real (a)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = imagInput,
                    onValueChange = { imagInput = it },
                    label = { Text("Imaginary (b)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Rectangular: ${z.format()}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CyanNeon)
                    Text("Polar (Degrees): ${z.format(polar = true, degrees = true)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AmberAccent)
                    Text("Polar (Radians): ${z.format(polar = true, degrees = false)}", fontSize = 14.sp)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Text("Modulus |z|: ${Complex.formatDouble(z.abs(), 6)}", fontSize = 14.sp)
                    Text("Conjugate z*: ${z.conj().format()}", fontSize = 14.sp)
                    Text("Reciprocal 1/z: ${(Complex.ONE / z).format()}", fontSize = 14.sp)
                    Text("Square Root √z: ${z.sqrt().format()}", fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = powerNInput,
                        onValueChange = { powerNInput = it },
                        label = { Text("Power (n)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("z^n = ${z.pow(n).format()}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = EmeraldNeon)
                }
            }
        }
    }
}
