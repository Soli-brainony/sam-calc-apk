package com.example.math

import kotlin.math.*

enum class AngleMode {
    DEG, RAD
}

sealed class MathResult {
    data class Success(val value: Complex) : MathResult()
    data class Error(val message: String) : MathResult()
}

class MathParser(private val angleMode: AngleMode = AngleMode.DEG) {

    /**
     * Evaluates a mathematical expression string, optionally substituting variable `x`.
     */
    fun evaluate(expression: String, xValue: Complex = Complex.ZERO): MathResult {
        return try {
            val tokens = tokenize(expression)
            if (tokens.isEmpty()) {
                return MathResult.Success(Complex.ZERO)
            }
            val parser = Parser(tokens, angleMode, xValue)
            val result = parser.parseExpression()
            if (parser.hasMoreTokens()) {
                MathResult.Error("Unexpected symbol '${parser.peek()}'")
            } else {
                MathResult.Success(result)
            }
        } catch (e: Exception) {
            MathResult.Error(e.message ?: "Syntax Error")
        }
    }

    /**
     * Fast real evaluation for plotting 2D functions f(x)
     */
    fun evaluateReal(expression: String, x: Double): Double {
        return try {
            val res = evaluate(expression, Complex(x, 0.0))
            if (res is MathResult.Success && res.value.isReal) {
                res.value.re
            } else {
                Double.NaN
            }
        } catch (_: Exception) {
            Double.NaN
        }
    }

    private fun tokenize(raw: String): List<String> {
        val sanitized = raw
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("–", "-")
            .replace("π", "pi")
            .replace("φ", "phi")
            .replace("√", "sqrt")
            .trim()

        val tokens = mutableListOf<String>()
        var i = 0
        val n = sanitized.length

        while (i < n) {
            val c = sanitized[i]
            when {
                c.isWhitespace() -> {
                    i++
                }
                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    while (i < n && (sanitized[i].isDigit() || sanitized[i] == '.')) {
                        sb.append(sanitized[i])
                        i++
                    }
                    tokens.add(sb.toString())
                }
                c.isLetter() -> {
                    val sb = StringBuilder()
                    while (i < n && (sanitized[i].isLetter() || sanitized[i].isDigit())) {
                        sb.append(sanitized[i])
                        i++
                    }
                    tokens.add(sb.toString().lowercase())
                }
                c in "+-*/^%!()" -> {
                    tokens.add(c.toString())
                    i++
                }
                c == ',' -> {
                    tokens.add(",")
                    i++
                }
                else -> {
                    i++
                }
            }
        }

        // Insert implicit multiplication:
        // Examples: 2x -> 2 * x, 2pi -> 2 * pi, 2(3) -> 2 * (3), (2)(3) -> (2) * (3), 3i -> 3 * i, x(x) -> x * (x)
        val expanded = mutableListOf<String>()
        for (idx in tokens.indices) {
            val current = tokens[idx]
            expanded.add(current)
            if (idx + 1 < n && idx + 1 < tokens.size) {
                val next = tokens[idx + 1]
                if (shouldInsertMultiply(current, next)) {
                    expanded.add("*")
                }
            }
        }
        return expanded
    }

    private fun shouldInsertMultiply(curr: String, next: String): Boolean {
        val isCurrNumber = curr.toDoubleOrNull() != null
        val isCurrConstantOrVar = curr in setOf("pi", "e", "phi", "i", "x")
        val isCurrCloseParen = curr == ")" || curr == "!"
        
        val isNextNumber = next.toDoubleOrNull() != null
        val isNextConstantOrVar = next in setOf("pi", "e", "phi", "i", "x")
        val isNextOpenParen = next == "("
        val isNextFunction = isKnownFunction(next)

        if ((isCurrNumber || isCurrConstantOrVar || isCurrCloseParen) &&
            (isNextConstantOrVar || isNextOpenParen || isNextFunction || (isCurrCloseParen && isNextNumber))) {
            return true
        }

        return false
    }

    private fun isKnownFunction(name: String): Boolean {
        return name in setOf(
            "sin", "cos", "tan", "asin", "acos", "atan",
            "sinh", "cosh", "tanh", "asinh", "acosh", "atanh",
            "ln", "log", "log10", "log2", "exp",
            "sqrt", "cbrt", "abs", "conj", "arg", "real", "imag", "deg", "rad"
        )
    }

    private class Parser(
        private val tokens: List<String>,
        private val angleMode: AngleMode,
        private val xVar: Complex
    ) {
        private var pos = 0

        fun hasMoreTokens(): Boolean = pos < tokens.size

        fun peek(): String = if (pos < tokens.size) tokens[pos] else ""

        private fun consume(): String {
            val token = peek()
            pos++
            return token
        }

        private fun match(token: String): Boolean {
            if (peek() == token) {
                pos++
                return true
            }
            return false
        }

        fun parseExpression(): Complex = parseAddSub()

        // + , -
        private fun parseAddSub(): Complex {
            var result = parseMulDiv()
            while (pos < tokens.size) {
                when (peek()) {
                    "+" -> {
                        consume()
                        result += parseMulDiv()
                    }
                    "-" -> {
                        consume()
                        result -= parseMulDiv()
                    }
                    else -> break
                }
            }
            return result
        }

        // *, /, %, nPr, nCr
        private fun parseMulDiv(): Complex {
            var result = parsePower()
            while (pos < tokens.size) {
                when (peek()) {
                    "*" -> {
                        consume()
                        result *= parsePower()
                    }
                    "/" -> {
                        consume()
                        val right = parsePower()
                        if (right.isZero) throw ArithmeticException("Division by zero")
                        result /= right
                    }
                    "%" -> {
                        consume()
                        val right = parsePower()
                        if (right.isZero) throw ArithmeticException("Modulo by zero")
                        result = Complex(result.re % right.re, 0.0)
                    }
                    "npr" -> {
                        consume()
                        val r = parsePower().re.roundToInt()
                        val n = result.re.roundToInt()
                        result = Complex(permutation(n, r), 0.0)
                    }
                    "ncr" -> {
                        consume()
                        val r = parsePower().re.roundToInt()
                        val n = result.re.roundToInt()
                        result = Complex(combination(n, r), 0.0)
                    }
                    else -> break
                }
            }
            return result
        }

        // ^ (power) - Right associative
        private fun parsePower(): Complex {
            var left = parsePostfix()
            if (match("^")) {
                val right = parsePower() // right-associative
                left = left.pow(right)
            }
            return left
        }

        // Postfix: ! (factorial), % (percentage as 1/100)
        private fun parsePostfix(): Complex {
            var result = parseUnary()
            while (pos < tokens.size) {
                when {
                    match("!") -> {
                        result = factorial(result)
                    }
                    else -> break
                }
            }
            return result
        }

        // Unary +, -
        private fun parseUnary(): Complex {
            if (match("+")) {
                return parseUnary()
            }
            if (match("-")) {
                return -parseUnary()
            }
            return parsePrimary()
        }

        // Primary: numbers, variables, constants, functions, parentheses
        private fun parsePrimary(): Complex {
            val token = consume()

            // Number literal
            val num = token.toDoubleOrNull()
            if (num != null) {
                return Complex(num, 0.0)
            }

            // Variables & constants
            when (token) {
                "x" -> return xVar
                "i" -> return Complex.I
                "pi" -> return Complex(Complex.PI, 0.0)
                "e" -> return Complex(Complex.E, 0.0)
                "phi" -> return Complex(Complex.PHI, 0.0)
                "(" -> {
                    val expr = parseExpression()
                    if (!match(")")) {
                        throw IllegalArgumentException("Missing closing parenthesis ')'")
                    }
                    return expr
                }
            }

            // Functions
            if (isKnownFunction(token)) {
                // Must be followed by '(' or argument
                val hasParen = match("(")
                val arg = parseExpression()
                if (hasParen && !match(")")) {
                    throw IllegalArgumentException("Missing closing parenthesis after $token")
                }
                return applyFunction(token, arg)
            }

            throw IllegalArgumentException("Unknown identifier '$token'")
        }

        private fun applyFunction(func: String, arg: Complex): Complex {
            return when (func) {
                "sin" -> {
                    if (arg.isReal && angleMode == AngleMode.DEG) {
                        Complex(sin(Math.toRadians(arg.re)), 0.0)
                    } else arg.sin()
                }
                "cos" -> {
                    if (arg.isReal && angleMode == AngleMode.DEG) {
                        Complex(cos(Math.toRadians(arg.re)), 0.0)
                    } else arg.cos()
                }
                "tan" -> {
                    if (arg.isReal && angleMode == AngleMode.DEG) {
                        Complex(tan(Math.toRadians(arg.re)), 0.0)
                    } else arg.tan()
                }
                "asin" -> {
                    val res = arg.asin()
                    if (angleMode == AngleMode.DEG && res.isReal) {
                        Complex(Math.toDegrees(res.re), 0.0)
                    } else res
                }
                "acos" -> {
                    val res = arg.acos()
                    if (angleMode == AngleMode.DEG && res.isReal) {
                        Complex(Math.toDegrees(res.re), 0.0)
                    } else res
                }
                "atan" -> {
                    val res = arg.atan()
                    if (angleMode == AngleMode.DEG && res.isReal) {
                        Complex(Math.toDegrees(res.re), 0.0)
                    } else res
                }
                "sinh" -> arg.sinh()
                "cosh" -> arg.cosh()
                "tanh" -> arg.tanh()
                "ln" -> arg.ln()
                "log", "log10" -> arg.ln() / Complex(ln(10.0), 0.0)
                "log2" -> arg.ln() / Complex(ln(2.0), 0.0)
                "exp" -> arg.exp()
                "sqrt" -> arg.sqrt()
                "cbrt" -> arg.pow(1.0 / 3.0)
                "abs" -> Complex(arg.abs(), 0.0)
                "conj" -> arg.conj()
                "arg" -> {
                    val rad = arg.arg()
                    if (angleMode == AngleMode.DEG) Complex(Math.toDegrees(rad), 0.0) else Complex(rad, 0.0)
                }
                "real" -> Complex(arg.re, 0.0)
                "imag" -> Complex(arg.im, 0.0)
                "deg" -> Complex(Math.toDegrees(arg.re), 0.0)
                "rad" -> Complex(Math.toRadians(arg.re), 0.0)
                else -> throw IllegalArgumentException("Unsupported function '$func'")
            }
        }

        private fun isKnownFunction(name: String): Boolean {
            return name in setOf(
                "sin", "cos", "tan", "asin", "acos", "atan",
                "sinh", "cosh", "tanh",
                "ln", "log", "log10", "log2", "exp",
                "sqrt", "cbrt", "abs", "conj", "arg", "real", "imag", "deg", "rad"
            )
        }

        private fun factorial(z: Complex): Complex {
            if (!z.isReal || z.re < 0 || abs(z.re - z.re.roundToInt()) > 1e-9) {
                // Approximate with Lanczos gamma function: Γ(z + 1)
                val x = z.re + 1.0
                return Complex(gammaLanczos(x), 0.0)
            }
            val n = z.re.roundToInt()
            if (n > 100) return Complex(Double.POSITIVE_INFINITY, 0.0)
            var fact = 1.0
            for (i in 2..n) fact *= i
            return Complex(fact, 0.0)
        }

        private fun gammaLanczos(x: Double): Double {
            val p = doubleArrayOf(
                676.5203681218851,
                -1259.1392167224028,
                771.32342877765313,
                -176.61502916214059,
                12.507343278686905,
                -0.138571095853205,
                9.9843695780195716e-6,
                1.5056327351493116e-7
            )
            val g = 7
            if (x < 0.5) return Complex.PI / (sin(Complex.PI * x) * gammaLanczos(1.0 - x))
            val z = x - 1.0
            var a = 0.99999999999980993
            for (i in p.indices) a += p[i] / (z + i + 1.0)
            val t = z + g + 0.5
            return sqrt(2.0 * Complex.PI) * t.pow(z + 0.5) * exp(-t) * a
        }

        private fun permutation(n: Int, r: Int): Double {
            if (r < 0 || r > n) return 0.0
            var res = 1.0
            for (i in 0 until r) res *= (n - i)
            return res
        }

        private fun combination(n: Int, r: Int): Double {
            if (r < 0 || r > n) return 0.0
            val k = min(r, n - r)
            var res = 1.0
            for (i in 1..k) {
                res = res * (n - k + i) / i
            }
            return res
        }
    }
}
