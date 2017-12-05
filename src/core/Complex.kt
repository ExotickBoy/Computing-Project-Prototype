package core

import java.lang.Math.hypot

data class Complex(val re: Double = 0.0, val im: Double = 0.0) {

    operator fun plus(b: Complex) = Complex(this.re + b.re, this.im + b.im)
    operator fun plus(b: Double) = Complex(this.re + b, this.im + b)
    operator fun plus(b: Number) = this.plus(b.toDouble())

    operator fun minus(b: Complex) = Complex(this.re - b.re, this.im - b.im)
    operator fun minus(b: Double) = Complex(this.re - b, this.im - b)
    operator fun minus(b: Number) = this.minus(b.toDouble())

    operator fun times(b: Complex) = Complex(this.re * b.re - this.im * b.im, this.re * b.im + this.im * b.re)
    operator fun times(b: Double) = Complex(this.re * b, this.im * b)
    operator fun times(b: Number) = this.times(b.toDouble())

    operator fun div(b: Double) = Complex(this.re / b, this.im / b)
    operator fun div(b: Number) = this.div(b.toDouble())

    fun magnitude() = hypot(this.re, this.im)
    fun conjugate() = Complex(this.re, -this.im)


}