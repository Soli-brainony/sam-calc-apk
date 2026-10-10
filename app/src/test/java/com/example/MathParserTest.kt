package com.example

import com.example.math.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class MathParserTest {

    @Test
    fun testBasicArithmetic() {
        val parser = MathParser(AngleMode.DEG)
        val res = parser.evaluate("2 + 3 * 4")
        assertTrue(res is MathResult.Success)
        assertEquals(14.0, (res as MathResult.Success).value.re, 1e-6)
    }

    @Test
    fun testImplicitMultiplication() {
        val parser = MathParser(AngleMode.DEG)
        val res1 = parser.evaluate("2pi")
        assertTrue(res1 is MathResult.Success)
        assertEquals(2.0 * Math.PI, (res1 as MathResult.Success).value.re, 1e-6)

        val res2 = parser.evaluate("2(3 + 4)")
        assertTrue(res2 is MathResult.Success)
        assertEquals(14.0, (res2 as MathResult.Success).value.re, 1e-6)

        val res3 = parser.evaluate("(2 + 1)(3 + 1)")
        assertTrue(res3 is MathResult.Success)
        assertEquals(12.0, (res3 as MathResult.Success).value.re, 1e-6)
    }

    @Test
    fun testComplexArithmetic() {
        val parser = MathParser(AngleMode.DEG)

        // i^2 = -1
        val res1 = parser.evaluate("i * i")
        assertTrue(res1 is MathResult.Success)
        val v1 = (res1 as MathResult.Success).value
        assertEquals(-1.0, v1.re, 1e-6)
        assertEquals(0.0, v1.im, 1e-6)

        // (2 + 3i) * (1 - 2i) = 2 - 4i + 3i - 6i^2 = 8 - i
        val res2 = parser.evaluate("(2 + 3i) * (1 - 2i)")
        assertTrue(res2 is MathResult.Success)
        val v2 = (res2 as MathResult.Success).value
        assertEquals(8.0, v2.re, 1e-6)
        assertEquals(-1.0, v2.im, 1e-6)

        // sqrt(-4) = 2i
        val res3 = parser.evaluate("sqrt(-4)")
        assertTrue(res3 is MathResult.Success)
        val v3 = (res3 as MathResult.Success).value
        assertEquals(0.0, v3.re, 1e-6)
        assertEquals(2.0, v3.im, 1e-6)
    }

    @Test
    fun testTrigDegAndRad() {
        val parserDeg = MathParser(AngleMode.DEG)
        val resDeg = parserDeg.evaluate("sin(30)")
        assertTrue(resDeg is MathResult.Success)
        assertEquals(0.5, (resDeg as MathResult.Success).value.re, 1e-6)

        val parserRad = MathParser(AngleMode.RAD)
        val resRad = parserRad.evaluate("sin(pi / 6)")
        assertTrue(resRad is MathResult.Success)
        assertEquals(0.5, (resRad as MathResult.Success).value.re, 1e-6)
    }

    @Test
    fun testQuadraticSolver() {
        // Real roots: x^2 - 5x + 6 = 0 -> x = 2, 3
        val q1 = EquationSolvers.solveQuadratic(1.0, -5.0, 6.0)
        assertEquals(1.0, q1.discriminant, 1e-6)
        val roots1 = setOf(q1.root1.re.toInt(), q1.root2.re.toInt())
        assertTrue(roots1.contains(2) && roots1.contains(3))

        // Complex roots: x^2 + 1 = 0 -> x = i, -i
        val q2 = EquationSolvers.solveQuadratic(1.0, 0.0, 1.0)
        assertEquals(-4.0, q2.discriminant, 1e-6)
        assertEquals(0.0, q2.root1.re, 1e-6)
        assertEquals(1.0, abs(q2.root1.im), 1e-6)
    }

    @Test
    fun testLinear2x2Solver() {
        // 2x + y = 5
        // x - y = 1
        // -> x = 2, y = 1
        val lin = EquationSolvers.solve2x2Linear(2.0, 1.0, 5.0, 1.0, -1.0, 1.0)
        assertEquals(2.0, lin.x, 1e-6)
        assertEquals(1.0, lin.y, 1e-6)
    }

    @Test
    fun testSavedFormulaEvaluation() {
        val parser = MathParser(AngleMode.DEG)

        // Pythagorean: sqrt(3^2 + 4^2) = 5
        val pythagorean = parser.evaluate("sqrt(3^2 + 4^2)")
        assertTrue(pythagorean is MathResult.Success)
        assertEquals(5.0, (pythagorean as MathResult.Success).value.re, 1e-6)

        // Euler's Identity: exp(i * pi) + 1 = 0
        val euler = parser.evaluate("exp(i * pi) + 1")
        assertTrue(euler is MathResult.Success)
        val eulerVal = (euler as MathResult.Success).value
        assertEquals(0.0, eulerVal.re, 1e-6)
        assertEquals(0.0, eulerVal.im, 1e-6)

        // Kinetic Energy: 0.5 * 10 * 4^2 = 80
        val ke = parser.evaluate("0.5 * 10 * 4^2")
        assertTrue(ke is MathResult.Success)
        assertEquals(80.0, (ke as MathResult.Success).value.re, 1e-6)
    }
}
