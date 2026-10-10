package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.math.AngleMode
import com.example.ui.components.CalcButton
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val expression by viewModel.expression.collectAsStateWithLifecycle()
    val livePreview by viewModel.livePreview.collectAsStateWithLifecycle()
    val evaluatedResult by viewModel.evaluatedResult.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val angleMode by viewModel.angleMode.collectAsStateWithLifecycle()
    val isInverse by viewModel.isInverse.collectAsStateWithLifecycle()
    val isPolar by viewModel.isPolarDisplay.collectAsStateWithLifecycle()
    val hasMemory by viewModel.hasMemory.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    var showAdvancedSheet by remember { mutableStateOf(false) }
    var showQuickRefSheet by remember { mutableStateOf(false) }
    val recentQuickItems by viewModel.recentQuickItems.collectAsStateWithLifecycle()
    val savedEquations by viewModel.savedEquationsList.collectAsStateWithLifecycle()

    val isDark = isDarkTheme

    // Color definitions
    val numBg = if (isDark) KeyNumDark else KeyNumLight
    val opBg = if (isDark) KeyOpDark else KeyOpLight
    val sciBg = if (isDark) KeySciDark else KeySciLight
    val actionBg = if (isDark) KeyActionDark else Color(0xFFE0E7FF)

    val textPrimary = MaterialTheme.colorScheme.onSurface
    val accentCyan = CyanNeon
    val accentAmber = AmberAccent

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Mode & Status Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // DEG / RAD badge toggle
                    FilterChip(
                        selected = angleMode == AngleMode.DEG,
                        onClick = { viewModel.toggleAngleMode() },
                        label = {
                            Text(
                                text = if (angleMode == AngleMode.DEG) "DEG" else "RAD",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentCyan.copy(alpha = 0.2f),
                            selectedLabelColor = accentCyan
                        )
                    )

                    // Complex Polar / Rect toggle
                    FilterChip(
                        selected = isPolar,
                        onClick = { viewModel.togglePolarDisplay() },
                        label = {
                            Text(
                                text = if (isPolar) "POLAR ∠" else "RECT a+bi",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PurpleNeon.copy(alpha = 0.2f),
                            selectedLabelColor = PurpleNeon
                        )
                    )

                    if (hasMemory) {
                        Surface(
                            shape = CircleShape,
                            color = AmberAccent.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                text = "M",
                                color = AmberAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Quick Ref, Theme & Advanced toggles
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { showQuickRefSheet = true },
                        modifier = Modifier.testTag("btn_quick_ref")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmarks,
                            contentDescription = "Quick Reference",
                            tint = CyanNeon
                        )
                    }

                    IconButton(
                        onClick = { showAdvancedSheet = true },
                        modifier = Modifier.testTag("btn_advanced_sheet")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Functions,
                            contentDescription = "Advanced Functions",
                            tint = PurpleNeon
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier.testTag("btn_theme_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = textPrimary
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Display Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Scrollable expression
                    val scrollState = rememberScrollState()
                    LaunchedEffect(expression) {
                        scrollState.scrollTo(scrollState.maxValue)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = if (expression.isEmpty()) "0" else expression,
                            color = if (expression.isEmpty()) textPrimary.copy(alpha = 0.4f) else textPrimary,
                            fontSize = if (expression.length > 15) 30.sp else 40.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            modifier = Modifier.testTag("calc_display_expr")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Live preview or Final Result
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = CoralNeon,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.testTag("calc_error_msg")
                        )
                    } else if (evaluatedResult != null) {
                        val formattedResult = evaluatedResult?.format(
                            decimals = 8,
                            polar = isPolar,
                            degrees = angleMode == AngleMode.DEG
                        ) ?: ""
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "= $formattedResult",
                                color = accentCyan,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.testTag("calc_result_final")
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Calculation Result", formattedResult)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Result",
                                    tint = textPrimary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else if (livePreview.isNotEmpty()) {
                        Text(
                            text = "= $livePreview",
                            color = textPrimary.copy(alpha = 0.6f),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.End,
                            modifier = Modifier.testTag("calc_live_preview")
                        )
                    }
                }
            }

            // Quick Reference Bar from Room
            if (recentQuickItems.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { showQuickRefSheet = true },
                        label = { Text("Quick Ref", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Bookmarks,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = CyanNeon
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(containerColor = CyanNeon.copy(alpha = 0.15f))
                    )

                    recentQuickItems.forEach { item ->
                        SuggestionChip(
                            onClick = {
                                viewModel.append(item.result)
                                Toast.makeText(context, "Appended ${item.result}", Toast.LENGTH_SHORT).show()
                            },
                            label = {
                                Text(
                                    text = if (item.label.isNotBlank()) "${item.label}: ${item.result}" else "${item.expression} = ${item.result}",
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Memory Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val memButtons = listOf(
                    "MC" to { viewModel.memoryClear() },
                    "MR" to { viewModel.memoryRecall() },
                    "M+" to { viewModel.memoryAdd() },
                    "M-" to { viewModel.memorySub() },
                    "MS" to { viewModel.memoryStore() },
                    "Ans" to { viewModel.append("Ans") }
                )
                memButtons.forEach { (label, action) ->
                    TextButton(
                        onClick = action,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("btn_mem_$label")
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (label == "MC" || label == "MR") accentAmber else textPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Scientific Row 1: 2nd, sin, cos, tan, ln
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalcButton(
                    text = if (isInverse) "1st" else "2nd",
                    backgroundColor = if (isInverse) PurpleNeon else sciBg,
                    contentColor = if (isInverse) Color.White else PurpleNeon,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    height = 46.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_2nd"
                ) {
                    viewModel.toggleInverse()
                }

                val sinLabel = if (isInverse) "sin⁻¹" else "sin"
                val sinFunc = if (isInverse) "asin" else "sin"
                CalcButton(
                    text = sinLabel,
                    backgroundColor = sciBg,
                    contentColor = textPrimary,
                    fontSize = 14.sp,
                    height = 46.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_sin"
                ) {
                    viewModel.appendFunction(sinFunc)
                }

                val cosLabel = if (isInverse) "cos⁻¹" else "cos"
                val cosFunc = if (isInverse) "acos" else "cos"
                CalcButton(
                    text = cosLabel,
                    backgroundColor = sciBg,
                    contentColor = textPrimary,
                    fontSize = 14.sp,
                    height = 46.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_cos"
                ) {
                    viewModel.appendFunction(cosFunc)
                }

                val tanLabel = if (isInverse) "tan⁻¹" else "tan"
                val tanFunc = if (isInverse) "atan" else "tan"
                CalcButton(
                    text = tanLabel,
                    backgroundColor = sciBg,
                    contentColor = textPrimary,
                    fontSize = 14.sp,
                    height = 46.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_tan"
                ) {
                    viewModel.appendFunction(tanFunc)
                }

                val lnLabel = if (isInverse) "eˣ" else "ln"
                CalcButton(
                    text = lnLabel,
                    backgroundColor = sciBg,
                    contentColor = textPrimary,
                    fontSize = 14.sp,
                    height = 46.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_ln"
                ) {
                    if (isInverse) viewModel.appendFunction("exp") else viewModel.appendFunction("ln")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Scientific Row 2: log, sqrt, x^y, i (complex unit), π
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val logLabel = if (isInverse) "10ˣ" else "log"
                CalcButton(
                    text = logLabel,
                    backgroundColor = sciBg,
                    contentColor = textPrimary,
                    fontSize = 14.sp,
                    height = 46.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_log"
                ) {
                    if (isInverse) viewModel.append("10^(") else viewModel.appendFunction("log")
                }

                val sqrtLabel = if (isInverse) "∛x" else "√x"
                CalcButton(
                    text = sqrtLabel,
                    backgroundColor = sciBg,
                    contentColor = textPrimary,
                    fontSize = 14.sp,
                    height = 46.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_sqrt"
                ) {
                    if (isInverse) viewModel.appendFunction("cbrt") else viewModel.appendFunction("sqrt")
                }

                val powLabel = if (isInverse) "x²" else "xʸ"
                CalcButton(
                    text = powLabel,
                    backgroundColor = sciBg,
                    contentColor = textPrimary,
                    fontSize = 14.sp,
                    height = 46.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_pow"
                ) {
                    if (isInverse) viewModel.append("^2") else viewModel.append("^")
                }

                // Complex unit 'i'
                CalcButton(
                    text = "i",
                    backgroundColor = sciBg,
                    contentColor = PurpleNeon,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    height = 46.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_complex_i"
                ) {
                    viewModel.append("i")
                }

                CalcButton(
                    text = "π",
                    backgroundColor = sciBg,
                    contentColor = EmeraldNeon,
                    fontSize = 16.sp,
                    height = 46.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_pi"
                ) {
                    viewModel.append("π")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Standard Keypad Row 1: C, DEL, (, ), ÷
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalcButton(
                    text = "AC",
                    backgroundColor = actionBg,
                    contentColor = CoralNeon,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_clear"
                ) {
                    viewModel.clear()
                }

                CalcButton(
                    text = "⌫",
                    backgroundColor = actionBg,
                    contentColor = textPrimary,
                    fontSize = 18.sp,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_del"
                ) {
                    viewModel.backspace()
                }

                CalcButton(
                    text = "(",
                    backgroundColor = opBg,
                    contentColor = textPrimary,
                    fontSize = 18.sp,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_open_paren"
                ) {
                    viewModel.append("(")
                }

                CalcButton(
                    text = ")",
                    backgroundColor = opBg,
                    contentColor = textPrimary,
                    fontSize = 18.sp,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_close_paren"
                ) {
                    viewModel.append(")")
                }

                CalcButton(
                    text = "÷",
                    backgroundColor = opBg,
                    contentColor = accentCyan,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_div"
                ) {
                    viewModel.append("÷")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Standard Keypad Row 2: 7, 8, 9, %, ×
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalcButton(
                    text = "7",
                    backgroundColor = numBg,
                    contentColor = textPrimary,
                    fontSize = 22.sp,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_7"
                ) { viewModel.append("7") }

                CalcButton(
                    text = "8",
                    backgroundColor = numBg,
                    contentColor = textPrimary,
                    fontSize = 22.sp,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_8"
                ) { viewModel.append("8") }

                CalcButton(
                    text = "9",
                    backgroundColor = numBg,
                    contentColor = textPrimary,
                    fontSize = 22.sp,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_9"
                ) { viewModel.append("9") }

                CalcButton(
                    text = "x!",
                    backgroundColor = opBg,
                    contentColor = textPrimary,
                    fontSize = 16.sp,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_fact"
                ) { viewModel.append("!") }

                CalcButton(
                    text = "×",
                    backgroundColor = opBg,
                    contentColor = accentCyan,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_mul"
                ) { viewModel.append("×") }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Standard Keypad Row 3: 4, 5, 6, %, −
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalcButton(
                    text = "4",
                    backgroundColor = numBg,
                    contentColor = textPrimary,
                    fontSize = 22.sp,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_4"
                ) { viewModel.append("4") }

                CalcButton(
                    text = "5",
                    backgroundColor = numBg,
                    contentColor = textPrimary,
                    fontSize = 22.sp,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_5"
                ) { viewModel.append("5") }

                CalcButton(
                    text = "6",
                    backgroundColor = numBg,
                    contentColor = textPrimary,
                    fontSize = 22.sp,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_6"
                ) { viewModel.append("6") }

                CalcButton(
                    text = "%",
                    backgroundColor = opBg,
                    contentColor = textPrimary,
                    fontSize = 18.sp,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_mod"
                ) { viewModel.append("%") }

                CalcButton(
                    text = "−",
                    backgroundColor = opBg,
                    contentColor = accentCyan,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_sub"
                ) { viewModel.append("−") }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Standard Keypad Row 4: 1, 2, 3, e, +
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalcButton(
                    text = "1",
                    backgroundColor = numBg,
                    contentColor = textPrimary,
                    fontSize = 22.sp,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_1"
                ) { viewModel.append("1") }

                CalcButton(
                    text = "2",
                    backgroundColor = numBg,
                    contentColor = textPrimary,
                    fontSize = 22.sp,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_2"
                ) { viewModel.append("2") }

                CalcButton(
                    text = "3",
                    backgroundColor = numBg,
                    contentColor = textPrimary,
                    fontSize = 22.sp,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_3"
                ) { viewModel.append("3") }

                CalcButton(
                    text = "e",
                    backgroundColor = opBg,
                    contentColor = EmeraldNeon,
                    fontSize = 18.sp,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_e"
                ) { viewModel.append("e") }

                CalcButton(
                    text = "+",
                    backgroundColor = opBg,
                    contentColor = accentCyan,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_add"
                ) { viewModel.append("+") }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Standard Keypad Row 5: 0, ., ±, x, =
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalcButton(
                    text = "0",
                    backgroundColor = numBg,
                    contentColor = textPrimary,
                    fontSize = 22.sp,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_0"
                ) { viewModel.append("0") }

                CalcButton(
                    text = ".",
                    backgroundColor = numBg,
                    contentColor = textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_dot"
                ) { viewModel.append(".") }

                CalcButton(
                    text = "±",
                    backgroundColor = opBg,
                    contentColor = textPrimary,
                    fontSize = 20.sp,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_neg"
                ) {
                    if (expression.startsWith("-")) {
                        // remove leading minus
                        viewModel.clear()
                        viewModel.append(expression.drop(1))
                    } else {
                        viewModel.append("(-")
                    }
                }

                CalcButton(
                    text = "x",
                    backgroundColor = opBg,
                    contentColor = PurpleNeon,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_var_x"
                ) { viewModel.append("x") }

                CalcButton(
                    text = "=",
                    backgroundColor = accentAmber,
                    contentColor = Color.Black,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    height = 54.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_equals"
                ) {
                    viewModel.evaluate()
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Advanced Functions Modal Bottom Sheet
        if (showAdvancedSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAdvancedSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = "Advanced Scientific & Complex Functions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Hyperbolic Trigonometry",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("sinh", "cosh", "tanh").forEach { fn ->
                            Button(
                                onClick = {
                                    viewModel.appendFunction(fn)
                                    showAdvancedSheet = false
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text(fn) }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Complex Number Tools",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PurpleNeon
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "abs" to "abs (Modulus |z|)",
                            "arg" to "arg (Phase ∠)",
                            "conj" to "conj (Conjugate)"
                        ).forEach { (fn, label) ->
                            FilledTonalButton(
                                onClick = {
                                    viewModel.appendFunction(fn)
                                    showAdvancedSheet = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(fn, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Combinatorics & Constants",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldNeon
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.append("nPr")
                                showAdvancedSheet = false
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("nPr") }

                        Button(
                            onClick = {
                                viewModel.append("nCr")
                                showAdvancedSheet = false
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("nCr") }

                        Button(
                            onClick = {
                                viewModel.append("phi")
                                showAdvancedSheet = false
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Golden φ") }
                    }
                }
            }
        }

        // Quick Reference Bottom Sheet (Room Database)
        if (showQuickRefSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQuickRefSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                var refTab by remember { mutableIntStateOf(0) }
                val historyList by viewModel.historyList.collectAsStateWithLifecycle()
                val historyQuery by viewModel.historySearchQuery.collectAsStateWithLifecycle()
                val equationQuery by viewModel.equationSearchQuery.collectAsStateWithLifecycle()
                var showSaveCurrentDialog by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Local Quick Reference",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon
                        )

                        if (expression.isNotBlank()) {
                            TextButton(
                                onClick = { showSaveCurrentDialog = true },
                                modifier = Modifier.testTag("btn_save_curr_expr")
                            ) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save Current", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    PrimaryTabRow(selectedTabIndex = refTab) {
                        Tab(
                            selected = refTab == 0,
                            onClick = { refTab = 0 },
                            text = { Text("Recent Calculations (${historyList.size})") }
                        )
                        Tab(
                            selected = refTab == 1,
                            onClick = { refTab = 1 },
                            text = { Text("Equations (${savedEquations.size})") }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (refTab == 0) {
                        OutlinedTextField(
                            value = historyQuery,
                            onValueChange = { viewModel.historySearchQuery.value = it },
                            placeholder = { Text("Filter past calculations...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (historyList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No calculations found in local storage",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 320.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(historyList.take(20), key = { it.id }) { item ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                if (item.label.isNotBlank()) {
                                                    Text(
                                                        text = item.label,
                                                        color = CyanNeon,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Text(
                                                    text = item.expression,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "= ${item.result}",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (item.isComplex) PurpleNeon else CyanNeon
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = { viewModel.toggleHistoryFavorite(item) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                                        contentDescription = null,
                                                        tint = if (item.isFavorite) AmberAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                FilledTonalButton(
                                                    onClick = {
                                                        viewModel.insertHistoryResult(item)
                                                        showQuickRefSheet = false
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Text("Use Result", fontSize = 11.sp)
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Button(
                                                    onClick = {
                                                        viewModel.insertHistory(item)
                                                        showQuickRefSheet = false
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Text("Load Expr", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = equationQuery,
                            onValueChange = { viewModel.equationSearchQuery.value = it },
                            placeholder = { Text("Search formulas...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(savedEquations, key = { it.id }) { eq ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = eq.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = CyanNeon.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = eq.category,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = CyanNeon,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = eq.expression,
                                                fontSize = 13.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = CyanNeon
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.insertEquationIntoCalc(eq.expression)
                                                showQuickRefSheet = false
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("Insert", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showSaveCurrentDialog) {
                    var titleInput by remember { mutableStateOf("") }
                    var descInput by remember { mutableStateOf("") }
                    var catInput by remember { mutableStateOf("Custom") }

                    AlertDialog(
                        onDismissRequest = { showSaveCurrentDialog = false },
                        title = { Text("Save Current Formula") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Expression: $expression", fontFamily = FontFamily.Monospace, color = CyanNeon)
                                OutlinedTextField(
                                    value = titleInput,
                                    onValueChange = { titleInput = it },
                                    label = { Text("Formula Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = descInput,
                                    onValueChange = { descInput = it },
                                    label = { Text("Description") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = catInput,
                                    onValueChange = { catInput = it },
                                    label = { Text("Category") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (titleInput.isNotBlank()) {
                                        viewModel.saveEquation(titleInput, expression, descInput, catInput)
                                        showSaveCurrentDialog = false
                                        Toast.makeText(context, "Saved formula to Room", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text("Save")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSaveCurrentDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}
