package me.mason.game

import kotlin.math.PI
import kotlin.math.sin



private fun seed() = Math.random().toFloat() * 100000f
private fun perlin(x: Float) = (sin(2 * x) + sin(PI * x)).toFloat()

fun seed1(octaves: Int) = Array(octaves) { seed() }

fun perlin1(value: Float, stretch: Float, amplitude: Float, octaves: Int, seed: Array<Float>): Float {
    return (0 until octaves).fold(0f) { acc, index ->
        acc + perlin(value / stretch + seed[index]) * amplitude
    }
}