package com.example.math

import kotlin.math.*

object EquationSolvers {

    data class QuadraticResult(
        val a: Double,
        val b: Double,
        val c: Double,
        val discriminant: Double,
        val root1: Complex,
        val root2: Complex,
        val vertexX: Double,
        val vertexY: Double,
        val description: String
    )

    fun solveQuadratic(a: Double, b: Double, c: Double): QuadraticResult {
        if (abs(a) < 1e-12) {
            // Linear: bx + c = 0 -> x = -c/b
            if (abs(b) < 1e-12) {
                val desc = if (abs(c) < 1e-12) "Infinite solutions (0 = 0)" else "No solution"
                return QuadraticResult(a, b, c, 0.0, Complex.NaN, Complex.NaN, 0.0, 0.0, desc)
            }
            val root = Complex(-c / b, 0.0)
            return QuadraticResult(a, b, c, 0.0, root, root, 0.0, 0.0, "Degenerate linear equation: x = ${root.format()}")
        }

        val delta = b * b - 4 * a * c
        val vertexX = -b / (2 * a)
        val vertexY = c - (b * b) / (4 * a)

        val root1: Complex
        val root2: Complex
        val desc: String

        if (delta > 1e-12) {
            val sqrtDelta = sqrt(delta)
            root1 = Complex((-b + sqrtDelta) / (2 * a), 0.0)
            root2 = Complex((-b - sqrtDelta) / (2 * a), 0.0)
            desc = "Two distinct real roots (Δ = ${Complex.formatDouble(delta)})"
        } else if (abs(delta) <= 1e-12) {
            root1 = Complex(-b / (2 * a), 0.0)
            root2 = root1
            desc = "One repeated real root (Δ = 0)"
        } else {
            val sqrtAbsDelta = sqrt(-delta)
            root1 = Complex(-b / (2 * a), sqrtAbsDelta / (2 * a))
            root2 = Complex(-b / (2 * a), -sqrtAbsDelta / (2 * a))
            desc = "Two complex conjugate roots (Δ < 0)"
        }

        return QuadraticResult(a, b, c, delta, root1, root2, vertexX, vertexY, desc)
    }

    data class CubicResult(
        val a: Double,
        val b: Double,
        val c: Double,
        val d: Double,
        val roots: List<Complex>,
        val description: String
    )

    fun solveCubic(a: Double, b: Double, c: Double, d: Double): CubicResult {
        if (abs(a) < 1e-12) {
            val quad = solveQuadratic(b, c, d)
            return CubicResult(a, b, c, d, listOf(quad.root1, quad.root2), "Reduced to quadratic: " + quad.description)
        }

        // Depressed cubic: t^3 + pt + q = 0 via x = t - b/(3a)
        val an = b / a
        val bn = c / a
        val cn = d / a

        val p = bn - (an * an) / 3.0
        val q = (2.0 * an * an * an) / 27.0 - (an * bn) / 3.0 + cn

        val delta = (q * q) / 4.0 + (p * p * p) / 27.0
        val shift = an / 3.0
        val roots = mutableListOf<Complex>()

        if (delta > 1e-12) {
            // One real root and two complex conjugate roots
            val sqrtDelta = sqrt(delta)
            val u = cbrtClean(-q / 2.0 + sqrtDelta)
            val v = cbrtClean(-q / 2.0 - sqrtDelta)

            val t1 = u + v
            roots.add(Complex(t1 - shift, 0.0))

            val realPart = -(u + v) / 2.0 - shift
            val imagPart = (u - v) * sqrt(3.0) / 2.0
            roots.add(Complex(realPart, imagPart))
            roots.add(Complex(realPart, -imagPart))
            return CubicResult(a, b, c, d, roots, "1 Real root and 2 Complex conjugate roots")
        } else if (abs(delta) <= 1e-12) {
            // All roots real, at least two are equal
            val u = cbrtClean(-q / 2.0)
            roots.add(Complex(2 * u - shift, 0.0))
            roots.add(Complex(-u - shift, 0.0))
            roots.add(Complex(-u - shift, 0.0))
            return CubicResult(a, b, c, d, roots, "3 Real roots (multiple root)")
        } else {
            // All 3 roots real and distinct (Casus irreducibilis)
            val r = sqrt(-((p * p * p) / 27.0))
            val phi = acos(-q / (2.0 * r))
            val m = 2.0 * cbrtClean(r)

            val t1 = m * cos(phi / 3.0)
            val t2 = m * cos((phi + 2 * PI) / 3.0)
            val t3 = m * cos((phi + 4 * PI) / 3.0)

            roots.add(Complex(t1 - shift, 0.0))
            roots.add(Complex(t2 - shift, 0.0))
            roots.add(Complex(t3 - shift, 0.0))
            return CubicResult(a, b, c, d, roots, "3 Distinct real roots")
        }
    }

    private fun cbrtClean(v: Double): Double =
        if (v < 0) -(-v).pow(1.0 / 3.0) else v.pow(1.0 / 3.0)

    data class Linear2x2Result(
        val x: Double,
        val y: Double,
        val determinant: Double,
        val status: String
    )

    fun solve2x2Linear(
        a1: Double, b1: Double, c1: Double,
        a2: Double, b2: Double, c2: Double
    ): Linear2x2Result {
        // a1 x + b1 y = c1
        // a2 x + b2 y = c2
        val det = a1 * b2 - a2 * b1
        val detX = c1 * b2 - c2 * b1
        val detY = a1 * c2 - a2 * c1

        return if (abs(det) > 1e-12) {
            val x = detX / det
            val y = detY / det
            Linear2x2Result(x, y, det, "Unique solution found")
        } else {
            if (abs(detX) < 1e-12 && abs(detY) < 1e-12) {
                Linear2x2Result(Double.NaN, Double.NaN, 0.0, "Infinite solutions (dependent equations)")
            } else {
                Linear2x2Result(Double.NaN, Double.NaN, 0.0, "No solution (parallel inconsistent lines)")
            }
        }
    }

    /**
     * Numerical root finder for f(x) = 0 using Bisection + Secant hybrid method
     */
    fun findRoot(
        parser: MathParser,
        expression: String,
        xMin: Double = -10.0,
        xMax: Double = 10.0,
        maxIter: Int = 100
    ): List<Double> {
        val foundRoots = mutableListOf<Double>()
        val samples = 200
        val dx = (xMax - xMin) / samples

        var prevX = xMin
        var prevY = parser.evaluateReal(expression, prevX)

        for (i in 1..samples) {
            val currX = xMin + i * dx
            val currY = parser.evaluateReal(expression, currX)

            if (!prevY.isNaN() && !currY.isNaN()) {
                if (abs(prevY) < 1e-8) {
                    if (foundRoots.none { abs(it - prevX) < 1e-4 }) foundRoots.add(prevX)
                } else if (abs(currY) < 1e-8) {
                    if (foundRoots.none { abs(it - currX) < 1e-4 }) foundRoots.add(currX)
                } else if (prevY * currY < 0) {
                    // Sign change: refine using bisection
                    val root = refineRoot(parser, expression, prevX, currX, maxIter)
                    if (!root.isNaN() && foundRoots.none { abs(it - root) < 1e-4 }) {
                        foundRoots.add(root)
                    }
                }
            }
            prevX = currX
            prevY = currY
        }
        return foundRoots
    }

    private fun refineRoot(
        parser: MathParser,
        expression: String,
        low: Double,
        high: Double,
        maxIter: Int
    ): Double {
        var a = low
        var b = high
        var fa = parser.evaluateReal(expression, a)
        var fb = parser.evaluateReal(expression, b)

        for (i in 0 until maxIter) {
            if (abs(b - a) < 1e-9) return (a + b) / 2.0
            val mid = (a + b) / 2.0
            val fmid = parser.evaluateReal(expression, mid)

            if (fmid.isNaN()) return Double.NaN
            if (abs(fmid) < 1e-10) return mid

            if (fa * fmid < 0) {
                b = mid
                fb = fmid
            } else {
                a = mid
                fa = fmid
            }
        }
        return (a + b) / 2.0
    }
}
