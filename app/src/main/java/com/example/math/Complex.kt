package com.example.math

import kotlin.math.*

/**
 * Immutable Complex Number representation z = a + bi
 * Supports full arithmetic, powers, exponential, logs, and trigonometric functions.
 */
data class Complex(val re: Double, val im: Double = 0.0) {

    val isReal: Boolean
        get() = abs(im) < EPSILON

    val isZero: Boolean
        get() = abs(re) < EPSILON && abs(im) < EPSILON

    val isNaN: Boolean
        get() = re.isNaN() || im.isNaN()

    val isInfinite: Boolean
        get() = re.isInfinite() || im.isInfinite()

    operator fun plus(other: Complex): Complex =
        Complex(re + other.re, im + other.im)

    operator fun plus(other: Double): Complex =
        Complex(re + other, im)

    operator fun minus(other: Complex): Complex =
        Complex(re - other.re, im - other.im)

    operator fun minus(other: Double): Complex =
        Complex(re - other, im)

    operator fun times(other: Complex): Complex =
        Complex(re * other.re - im * other.im, re * other.im + im * other.re)

    operator fun times(other: Double): Complex =
        Complex(re * other, im * other)

    operator fun div(other: Complex): Complex {
        val denom = other.re * other.re + other.im * other.im
        if (denom == 0.0) return NaN
        return Complex(
            (re * other.re + im * other.im) / denom,
            (im * other.re - re * other.im) / denom
        )
    }

    operator fun div(other: Double): Complex {
        if (other == 0.0) return NaN
        return Complex(re / other, im / other)
    }

    operator fun unaryMinus(): Complex = Complex(-re, -im)

    /** Absolute value / Modulus: |z| = sqrt(re^2 + im^2) */
    fun abs(): Double = hypot(re, im)

    /** Argument / Phase angle in radians: arg(z) in (-pi, pi] */
    fun arg(): Double = atan2(im, re)

    /** Complex conjugate: z* = re - im * i */
    fun conj(): Complex = Complex(re, -im)

    /** Natural logarithm: ln(z) = ln|z| + i*arg(z) */
    fun ln(): Complex {
        val r = abs()
        if (r == 0.0) return Complex(Double.NEGATIVE_INFINITY, 0.0)
        return Complex(kotlin.math.ln(r), arg())
    }

    /** Exponential: e^z = e^re * (cos(im) + i*sin(im)) */
    fun exp(): Complex {
        val expReal = kotlin.math.exp(re)
        return Complex(expReal * cos(im), expReal * sin(im))
    }

    /** Complex power: z^p = exp(p * ln(z)) */
    fun pow(p: Complex): Complex {
        if (isZero) {
            return if (p.re > 0) ZERO else if (p.re == 0.0 && p.im == 0.0) ONE else NaN
        }
        return (p * this.ln()).exp()
    }

    fun pow(p: Double): Complex = pow(Complex(p, 0.0))

    /** Square root: sqrt(z) */
    fun sqrt(): Complex {
        if (isReal && re >= 0.0) {
            return Complex(kotlin.math.sqrt(re), 0.0)
        }
        if (isReal && re < 0.0) {
            return Complex(0.0, kotlin.math.sqrt(-re))
        }
        val r = abs()
        val u = kotlin.math.sqrt((r + re) / 2.0)
        val v = (if (im >= 0) 1.0 else -1.0) * kotlin.math.sqrt((r - re) / 2.0)
        return Complex(u, v)
    }

    // Trigonometric functions
    fun sin(): Complex {
        // sin(a + bi) = sin(a)cosh(b) + i cos(a)sinh(b)
        return Complex(kotlin.math.sin(re) * cosh(im), kotlin.math.cos(re) * sinh(im))
    }

    fun cos(): Complex {
        // cos(a + bi) = cos(a)cosh(b) - i sin(a)sinh(b)
        return Complex(kotlin.math.cos(re) * cosh(im), -kotlin.math.sin(re) * sinh(im))
    }

    fun tan(): Complex {
        val c = cos()
        if (c.isZero) return NaN
        return sin() / c
    }

    // Inverse trig
    fun asin(): Complex {
        // asin(z) = -i * ln(i*z + sqrt(1 - z^2))
        val iz = I * this
        val sqrtTerm = (ONE - (this * this)).sqrt()
        return -I * (iz + sqrtTerm).ln()
    }

    fun acos(): Complex {
        // acos(z) = pi/2 - asin(z)
        return Complex(PI / 2.0, 0.0) - asin()
    }

    fun atan(): Complex {
        // atan(z) = (i/2) * ln((1 - i*z) / (1 + i*z))
        val iz = I * this
        val num = ONE - iz
        val den = ONE + iz
        return (Complex(0.0, 0.5)) * (num / den).ln()
    }

    // Hyperbolic functions
    fun sinh(): Complex = (exp() - (-this).exp()) / 2.0
    fun cosh(): Complex = (exp() + (-this).exp()) / 2.0
    fun tanh(): Complex = sinh() / cosh()

    /** Formats the complex number nicely into a string */
    fun format(decimals: Int = 8, polar: Boolean = false, degrees: Boolean = false): String {
        if (isNaN) return "Undefined"
        if (isInfinite) return if (re < 0 || im < 0) "-Infinity" else "Infinity"

        if (polar) {
            val r = abs()
            var theta = arg()
            val unit = if (degrees) {
                theta = Math.toDegrees(theta)
                "°"
            } else " rad"
            return "${formatDouble(r, decimals)} ∠ ${formatDouble(theta, decimals)}$unit"
        }

        val roundedRe = roundClean(re, decimals)
        val roundedIm = roundClean(im, decimals)

        val realZero = abs(roundedRe) < EPSILON
        val imagZero = abs(roundedIm) < EPSILON

        return when {
            realZero && imagZero -> "0"
            imagZero -> formatDouble(roundedRe, decimals)
            realZero -> {
                when {
                    roundedIm == 1.0 -> "i"
                    roundedIm == -1.0 -> "-i"
                    else -> "${formatDouble(roundedIm, decimals)}i"
                }
            }
            else -> {
                val sign = if (roundedIm >= 0) " + " else " - "
                val absIm = abs(roundedIm)
                val imPart = when (absIm) {
                    1.0 -> "i"
                    else -> "${formatDouble(absIm, decimals)}i"
                }
                "${formatDouble(roundedRe, decimals)}$sign$imPart"
            }
        }
    }

    override fun toString(): String = format()

    companion object {
        const val EPSILON = 1e-12

        val ZERO = Complex(0.0, 0.0)
        val ONE = Complex(1.0, 0.0)
        val I = Complex(0.0, 1.0)
        val PI = kotlin.math.PI
        val E = kotlin.math.E
        val PHI = (1.0 + kotlin.math.sqrt(5.0)) / 2.0
        val NaN = Complex(Double.NaN, Double.NaN)

        fun fromPolar(r: Double, thetaRad: Double): Complex =
            Complex(r * cos(thetaRad), r * sin(thetaRad))

        fun fromPolarDeg(r: Double, thetaDeg: Double): Complex =
            fromPolar(r, Math.toRadians(thetaDeg))

        private fun roundClean(value: Double, decimals: Int): Double {
            if (value.isNaN() || value.isInfinite()) return value
            val factor = 10.0.pow(decimals.toDouble())
            val rounded = (value * factor).roundToLong() / factor
            return if (abs(rounded) < EPSILON) 0.0 else rounded
        }

        fun formatDouble(value: Double, maxDecimals: Int = 8): String {
            if (value.isNaN()) return "NaN"
            if (value.isInfinite()) return if (value < 0) "-∞" else "∞"
            val rounded = roundClean(value, maxDecimals)

            // Exponential notation for very small or very large non-zero numbers
            val absVal = abs(rounded)
            if (absVal != 0.0 && (absVal >= 1e12 || absVal < 1e-6)) {
                return String.format(java.util.Locale.US, "%.5e", rounded)
                    .replace("e+", "×10^")
                    .replace("e-", "×10^-")
            }

            // Check if integer
            if (abs(rounded - rounded.toLong()) < EPSILON) {
                return rounded.toLong().toString()
            }

            var s = String.format(java.util.Locale.US, "%.${maxDecimals}f", rounded)
            while (s.contains(".") && (s.endsWith("0") || s.endsWith("."))) {
                if (s.endsWith(".")) {
                    s = s.substring(0, s.length - 1)
                    break
                }
                s = s.substring(0, s.length - 1)
            }
            return s
        }
    }
}
