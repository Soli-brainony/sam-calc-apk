package com.example.ui

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.math.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.abs

data class GraphFunctionUiModel(
    val id: Long = 0,
    val expression: String,
    val color: Color,
    val isVisible: Boolean = true,
    val name: String = ""
)

data class TableRowData(
    val x: Double,
    val values: List<Double> // Value for each visible function
)

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val calculationRepository = CalculationRepository(db.calculationDao())
    private val equationRepository = EquationRepository(db.savedEquationDao())
    private val graphDao = db.graphDao()

    // Calculator State
    private val _expression = MutableStateFlow("")
    val expression: StateFlow<String> = _expression.asStateFlow()

    private val _livePreview = MutableStateFlow("")
    val livePreview: StateFlow<String> = _livePreview.asStateFlow()

    private val _evaluatedResult = MutableStateFlow<Complex?>(null)
    val evaluatedResult: StateFlow<Complex?> = _evaluatedResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _angleMode = MutableStateFlow(AngleMode.DEG)
    val angleMode: StateFlow<AngleMode> = _angleMode.asStateFlow()

    private val _isInverse = MutableStateFlow(false)
    val isInverse: StateFlow<Boolean> = _isInverse.asStateFlow()

    private val _isPolarDisplay = MutableStateFlow(false)
    val isPolarDisplay: StateFlow<Boolean> = _isPolarDisplay.asStateFlow()

    private val _memory = MutableStateFlow(Complex.ZERO)
    val memory: StateFlow<Complex> = _memory.asStateFlow()

    private val _hasMemory = MutableStateFlow(false)
    val hasMemory: StateFlow<Boolean> = _hasMemory.asStateFlow()

    private val _lastAnswer = MutableStateFlow(Complex.ZERO)
    val lastAnswer: StateFlow<Complex> = _lastAnswer.asStateFlow()

    // Theme Mode
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // History Flow and Filters
    val historySearchQuery = MutableStateFlow("")
    val historyFilterOnlyFavorites = MutableStateFlow(false)

    val historyList: StateFlow<List<CalculationEntity>> = combine(
        calculationRepository.allHistory,
        historySearchQuery,
        historyFilterOnlyFavorites
    ) { all, query, onlyFavs ->
        all.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.expression.contains(query, ignoreCase = true) ||
                item.result.contains(query, ignoreCase = true) ||
                item.label.contains(query, ignoreCase = true)
            val matchesFav = !onlyFavs || item.isFavorite
            matchesQuery && matchesFav
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Quick Reference Equations & Formulas
    val equationCategoryFilter = MutableStateFlow("All")
    val equationSearchQuery = MutableStateFlow("")

    val savedEquationsList: StateFlow<List<SavedEquationEntity>> = combine(
        equationRepository.allEquations,
        equationCategoryFilter,
        equationSearchQuery
    ) { all, cat, query ->
        all.filter { item ->
            val matchesCat = cat == "All" || item.category.equals(cat, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                item.title.contains(query, ignoreCase = true) ||
                item.expression.contains(query, ignoreCase = true) ||
                item.description.contains(query, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recent items for quick reference row in calculator display
    val recentQuickItems: StateFlow<List<CalculationEntity>> = calculationRepository.allHistory
        .map { it.take(8) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Graphing State
    val defaultColors = listOf(
        Color(0xFF38BDF8), // Cyan
        Color(0xFFF59E0B), // Amber
        Color(0xFF10B981), // Emerald
        Color(0xFFF43F5E), // Coral
        Color(0xFFA855F7), // Purple
        Color(0xFF06B6D4)  // Teal
    )

    private val _graphFunctions = MutableStateFlow(
        listOf(
            GraphFunctionUiModel(1, "sin(x)", defaultColors[0], true, "f₁(x)"),
            GraphFunctionUiModel(2, "0.2x^2 - 3", defaultColors[1], true, "f₂(x)")
        )
    )
    val graphFunctions: StateFlow<List<GraphFunctionUiModel>> = _graphFunctions.asStateFlow()

    // Viewport bounds
    private val _xMin = MutableStateFlow(-10.0)
    val xMin: StateFlow<Double> = _xMin.asStateFlow()

    private val _xMax = MutableStateFlow(10.0)
    val xMax: StateFlow<Double> = _xMax.asStateFlow()

    private val _yMin = MutableStateFlow(-8.0)
    val yMin: StateFlow<Double> = _yMin.asStateFlow()

    private val _yMax = MutableStateFlow(8.0)
    val yMax: StateFlow<Double> = _yMax.asStateFlow()

    private val _traceX = MutableStateFlow<Double?>(null)
    val traceX: StateFlow<Double?> = _traceX.asStateFlow()

    private val _tableData = MutableStateFlow<List<TableRowData>>(emptyList())
    val tableData: StateFlow<List<TableRowData>> = _tableData.asStateFlow()

    // Solver states
    val quadA = MutableStateFlow("1")
    val quadB = MutableStateFlow("-3")
    val quadC = MutableStateFlow("2")
    private val _quadResult = MutableStateFlow<EquationSolvers.QuadraticResult?>(null)
    val quadResult: StateFlow<EquationSolvers.QuadraticResult?> = _quadResult.asStateFlow()

    val cubicA = MutableStateFlow("1")
    val cubicB = MutableStateFlow("0")
    val cubicC = MutableStateFlow("-4")
    val cubicD = MutableStateFlow("0")
    private val _cubicResult = MutableStateFlow<EquationSolvers.CubicResult?>(null)
    val cubicResult: StateFlow<EquationSolvers.CubicResult?> = _cubicResult.asStateFlow()

    val linA1 = MutableStateFlow("2")
    val linB1 = MutableStateFlow("1")
    val linC1 = MutableStateFlow("8")
    val linA2 = MutableStateFlow("1")
    val linB2 = MutableStateFlow("-1")
    val linC2 = MutableStateFlow("1")
    private val _linearResult = MutableStateFlow<EquationSolvers.Linear2x2Result?>(null)
    val linearResult: StateFlow<EquationSolvers.Linear2x2Result?> = _linearResult.asStateFlow()

    val rootExpr = MutableStateFlow("x^3 - x - 2")
    val rootIntervalMin = MutableStateFlow("-5")
    val rootIntervalMax = MutableStateFlow("5")
    private val _foundRoots = MutableStateFlow<List<Double>>(emptyList())
    val foundRoots: StateFlow<List<Double>> = _foundRoots.asStateFlow()

    init {
        // Ensure default equations exist
        viewModelScope.launch(Dispatchers.IO) {
            val existing = equationRepository.allEquations.first()
            if (existing.isEmpty()) {
                val defaults = listOf(
                    SavedEquationEntity(
                        title = "Quadratic Formula",
                        expression = "(-b + sqrt(b^2 - 4*a*c)) / (2*a)",
                        description = "Solutions to ax² + bx + c = 0",
                        category = "Algebra",
                        isBuiltIn = true
                    ),
                    SavedEquationEntity(
                        title = "Pythagorean Theorem",
                        expression = "sqrt(a^2 + b^2)",
                        description = "Hypotenuse c = √(a² + b²)",
                        category = "Geometry",
                        isBuiltIn = true
                    ),
                    SavedEquationEntity(
                        title = "Euler's Identity",
                        expression = "exp(i * pi) + 1",
                        description = "e^(iπ) + 1 = 0",
                        category = "Complex",
                        isBuiltIn = true
                    ),
                    SavedEquationEntity(
                        title = "Kinetic Energy",
                        expression = "0.5 * m * v^2",
                        description = "Ek = ½mv²",
                        category = "Physics",
                        isBuiltIn = true
                    ),
                    SavedEquationEntity(
                        title = "Gravitational Energy",
                        expression = "m * 9.80665 * h",
                        description = "Ep = mgh",
                        category = "Physics",
                        isBuiltIn = true
                    ),
                    SavedEquationEntity(
                        title = "Ohm's Law (Voltage)",
                        expression = "I * R",
                        description = "V = I × R",
                        category = "Physics",
                        isBuiltIn = true
                    ),
                    SavedEquationEntity(
                        title = "Area of Circle",
                        expression = "pi * r^2",
                        description = "A = πr²",
                        category = "Geometry",
                        isBuiltIn = true
                    ),
                    SavedEquationEntity(
                        title = "Compound Interest",
                        expression = "P * (1 + r / n)^(n * t)",
                        description = "Future value with compounding",
                        category = "Finance",
                        isBuiltIn = true
                    )
                )
                equationRepository.populateDefaults(defaults)
            }
        }

        solveQuadratic()
        solveCubic()
        solveLinear()
        findRoots()
        generateTable()
    }

    // --- Calculator Methods ---

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun toggleAngleMode() {
        _angleMode.value = if (_angleMode.value == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG
        updateLivePreview(_expression.value)
    }

    fun toggleInverse() {
        _isInverse.value = !_isInverse.value
    }

    fun togglePolarDisplay() {
        _isPolarDisplay.value = !_isPolarDisplay.value
    }

    fun append(token: String) {
        _errorMessage.value = null
        val current = _expression.value
        val next = current + token
        _expression.value = next
        updateLivePreview(next)
    }

    fun appendFunction(func: String) {
        _errorMessage.value = null
        val current = _expression.value
        val next = "$current$func("
        _expression.value = next
        updateLivePreview(next)
    }

    fun backspace() {
        _errorMessage.value = null
        val current = _expression.value
        if (current.isNotEmpty()) {
            val next = current.dropLast(1)
            _expression.value = next
            updateLivePreview(next)
        }
    }

    fun clear() {
        _expression.value = ""
        _livePreview.value = ""
        _errorMessage.value = null
        _evaluatedResult.value = null
    }

    private fun updateLivePreview(expr: String) {
        if (expr.isBlank()) {
            _livePreview.value = ""
            return
        }
        val parser = MathParser(_angleMode.value)
        when (val res = parser.evaluate(expr, _lastAnswer.value)) {
            is MathResult.Success -> {
                _livePreview.value = res.value.format(
                    decimals = 6,
                    polar = _isPolarDisplay.value,
                    degrees = _angleMode.value == AngleMode.DEG
                )
            }
            is MathResult.Error -> {
                _livePreview.value = ""
            }
        }
    }

    fun evaluate() {
        val expr = _expression.value.trim()
        if (expr.isEmpty()) return

        val parser = MathParser(_angleMode.value)
        when (val res = parser.evaluate(expr, _lastAnswer.value)) {
            is MathResult.Success -> {
                val value = res.value
                _evaluatedResult.value = value
                _lastAnswer.value = value
                val formatted = value.format(
                    decimals = 8,
                    polar = _isPolarDisplay.value,
                    degrees = _angleMode.value == AngleMode.DEG
                )
                _livePreview.value = ""
                _errorMessage.value = null

                // Save to Room database via repository
                viewModelScope.launch(Dispatchers.IO) {
                    calculationRepository.insert(
                        CalculationEntity(
                            expression = expr,
                            result = formatted,
                            isComplex = !value.isReal,
                            angleMode = _angleMode.value.name
                        )
                    )
                }
            }
            is MathResult.Error -> {
                _errorMessage.value = res.message
            }
        }
    }

    // Memory operations
    fun memoryClear() {
        _memory.value = Complex.ZERO
        _hasMemory.value = false
    }

    fun memoryRecall() {
        val mem = _memory.value
        append(mem.format(decimals = 6))
    }

    fun memoryStore() {
        val currentVal = _evaluatedResult.value ?: run {
            val parser = MathParser(_angleMode.value)
            val res = parser.evaluate(_expression.value, _lastAnswer.value)
            if (res is MathResult.Success) res.value else Complex.ZERO
        }
        _memory.value = currentVal
        _hasMemory.value = !currentVal.isZero
    }

    fun memoryAdd() {
        val currentVal = _evaluatedResult.value ?: run {
            val parser = MathParser(_angleMode.value)
            val res = parser.evaluate(_expression.value, _lastAnswer.value)
            if (res is MathResult.Success) res.value else Complex.ZERO
        }
        _memory.value = _memory.value + currentVal
        _hasMemory.value = true
    }

    fun memorySub() {
        val currentVal = _evaluatedResult.value ?: run {
            val parser = MathParser(_angleMode.value)
            val res = parser.evaluate(_expression.value, _lastAnswer.value)
            if (res is MathResult.Success) res.value else Complex.ZERO
        }
        _memory.value = _memory.value - currentVal
        _hasMemory.value = true
    }

    // --- Room Local Storage & Retrieval Actions ---

    fun insertHistory(entity: CalculationEntity) {
        _expression.value = entity.expression
        updateLivePreview(entity.expression)
    }

    fun insertHistoryResult(entity: CalculationEntity) {
        append(entity.result)
    }

    fun insertEquationIntoCalc(equationExpression: String) {
        _expression.value = equationExpression
        updateLivePreview(equationExpression)
    }

    fun appendEquationIntoCalc(equationExpression: String) {
        append(equationExpression)
    }

    fun toggleHistoryFavorite(entity: CalculationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            calculationRepository.toggleFavorite(entity.id, !entity.isFavorite)
        }
    }

    fun updateHistoryLabel(entity: CalculationEntity, label: String) {
        viewModelScope.launch(Dispatchers.IO) {
            calculationRepository.update(entity.copy(label = label))
        }
    }

    fun deleteHistoryItem(entity: CalculationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            calculationRepository.delete(entity)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            calculationRepository.clearAll()
        }
    }

    // Saved Equations CRUD
    fun saveEquation(title: String, expr: String, description: String, category: String) {
        if (title.isBlank() || expr.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            equationRepository.insert(
                SavedEquationEntity(
                    title = title.trim(),
                    expression = expr.trim(),
                    description = description.trim(),
                    category = category.trim().ifBlank { "Custom" }
                )
            )
        }
    }

    fun updateEquation(equation: SavedEquationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            equationRepository.update(equation)
        }
    }

    fun toggleEquationFavorite(equation: SavedEquationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            equationRepository.toggleFavorite(equation.id, !equation.isFavorite)
        }
    }

    fun deleteEquation(equation: SavedEquationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            equationRepository.delete(equation)
        }
    }

    // --- Graphing Methods ---

    fun addGraphFunction(expr: String = "") {
        val list = _graphFunctions.value.toMutableList()
        val nextIdx = list.size + 1
        val color = defaultColors[(nextIdx - 1) % defaultColors.size]
        list.add(
            GraphFunctionUiModel(
                id = System.currentTimeMillis(),
                expression = expr.ifBlank { "x" },
                color = color,
                isVisible = true,
                name = "f$nextIdx(x)"
            )
        )
        _graphFunctions.value = list
        generateTable()
    }

    fun updateGraphFunction(index: Int, expr: String, isVisible: Boolean = true) {
        val list = _graphFunctions.value.toMutableList()
        if (index in list.indices) {
            val current = list[index]
            list[index] = current.copy(expression = expr, isVisible = isVisible)
            _graphFunctions.value = list
            generateTable()
        }
    }

    fun toggleFunctionVisibility(index: Int) {
        val list = _graphFunctions.value.toMutableList()
        if (index in list.indices) {
            val current = list[index]
            list[index] = current.copy(isVisible = !current.isVisible)
            _graphFunctions.value = list
            generateTable()
        }
    }

    fun removeGraphFunction(index: Int) {
        val list = _graphFunctions.value.toMutableList()
        if (index in list.indices && list.size > 1) {
            list.removeAt(index)
            _graphFunctions.value = list
            generateTable()
        }
    }

    fun resetGraphViewport() {
        _xMin.value = -10.0
        _xMax.value = 10.0
        _yMin.value = -8.0
        _yMax.value = 8.0
        _traceX.value = null
    }

    fun zoomGraph(zoomFactor: Float) {
        val cx = (_xMin.value + _xMax.value) / 2.0
        val cy = (_yMin.value + _yMax.value) / 2.0
        val halfW = ((_xMax.value - _xMin.value) / 2.0) / zoomFactor
        val halfH = ((_yMax.value - _yMin.value) / 2.0) / zoomFactor

        if (halfW > 0.001 && halfW < 10000.0) {
            _xMin.value = cx - halfW
            _xMax.value = cx + halfW
            _yMin.value = cy - halfH
            _yMax.value = cy + halfH
        }
    }

    fun panGraph(deltaXRatio: Float, deltaYRatio: Float) {
        val w = _xMax.value - _xMin.value
        val h = _yMax.value - _yMin.value
        val dx = -deltaXRatio * w
        val dy = deltaYRatio * h

        _xMin.value += dx
        _xMax.value += dx
        _yMin.value += dy
        _yMax.value += dy
    }

    fun setTraceX(x: Double?) {
        _traceX.value = x
    }

    fun setViewport(newXMin: Double, newXMax: Double, newYMin: Double, newYMax: Double) {
        if (newXMax > newXMin && newYMax > newYMin) {
            _xMin.value = newXMin
            _xMax.value = newXMax
            _yMin.value = newYMin
            _yMax.value = newYMax
        }
    }

    fun autoFitY() {
        val parser = MathParser(_angleMode.value)
        val visible = _graphFunctions.value.filter { it.isVisible && it.expression.isNotBlank() }
        if (visible.isEmpty()) return

        var minVal = Double.POSITIVE_INFINITY
        var maxVal = Double.NEGATIVE_INFINITY
        val steps = 100
        val dx = (_xMax.value - _xMin.value) / steps

        for (i in 0..steps) {
            val x = _xMin.value + i * dx
            visible.forEach { fn ->
                val y = parser.evaluateReal(fn.expression, x)
                if (!y.isNaN() && !y.isInfinite() && abs(y) < 100000.0) {
                    if (y < minVal) minVal = y
                    if (y > maxVal) maxVal = y
                }
            }
        }

        if (!minVal.isInfinite() && !maxVal.isInfinite() && maxVal >= minVal) {
            val span = maxVal - minVal
            val padding = if (span < 1e-4) 2.0 else span * 0.15
            _yMin.value = minVal - padding
            _yMax.value = maxVal + padding
        }
    }

    fun generateTable(start: Double = -5.0, end: Double = 5.0, step: Double = 1.0) {
        val parser = MathParser(_angleMode.value)
        val funcs = _graphFunctions.value.filter { it.isVisible }
        val rows = mutableListOf<TableRowData>()

        var x = start
        val safeStep = if (step <= 0.0) 1.0 else step
        while (x <= end + 1e-6) {
            val vals = funcs.map { fn ->
                parser.evaluateReal(fn.expression, x)
            }
            rows.add(TableRowData(x, vals))
            x += safeStep
        }
        _tableData.value = rows
    }

    // --- Solvers Methods ---

    fun solveQuadratic() {
        val a = quadA.value.toDoubleOrNull() ?: 1.0
        val b = quadB.value.toDoubleOrNull() ?: 0.0
        val c = quadC.value.toDoubleOrNull() ?: 0.0
        _quadResult.value = EquationSolvers.solveQuadratic(a, b, c)
    }

    fun solveCubic() {
        val a = cubicA.value.toDoubleOrNull() ?: 1.0
        val b = cubicB.value.toDoubleOrNull() ?: 0.0
        val c = cubicC.value.toDoubleOrNull() ?: 0.0
        val d = cubicD.value.toDoubleOrNull() ?: 0.0
        _cubicResult.value = EquationSolvers.solveCubic(a, b, c, d)
    }

    fun solveLinear() {
        val a1 = linA1.value.toDoubleOrNull() ?: 1.0
        val b1 = linB1.value.toDoubleOrNull() ?: 0.0
        val c1 = linC1.value.toDoubleOrNull() ?: 0.0
        val a2 = linA2.value.toDoubleOrNull() ?: 0.0
        val b2 = linB2.value.toDoubleOrNull() ?: 1.0
        val c2 = linC2.value.toDoubleOrNull() ?: 0.0
        _linearResult.value = EquationSolvers.solve2x2Linear(a1, b1, c1, a2, b2, c2)
    }

    fun findRoots() {
        val expr = rootExpr.value.ifBlank { "x" }
        val min = rootIntervalMin.value.toDoubleOrNull() ?: -10.0
        val max = rootIntervalMax.value.toDoubleOrNull() ?: 10.0
        val parser = MathParser(_angleMode.value)
        _foundRoots.value = EquationSolvers.findRoot(parser, expr, min, max)
    }
}
