package me.mason.game

import kotlin.math.PI
import kotlin.math.sin

fun seed1() = Math.random().toFloat() * Short.MAX_VALUE.toInt()
fun seed2() = vec(seed1(), seed1())
fun perlin1(value: Float, stretch: Float, amplitude: Float, seed: Float, octaves: Int) =
    (1..octaves).fold(0f) { acc, _ ->
        acc + (sin(2 * (value + seed) / stretch) + sin(PI * (value + seed) / stretch)).toFloat() * amplitude
    }
fun perlin2(value: FloatVector, stretch: Float, amplitude: Float, seed: FloatVector, octaves: Int) =
    vec(
        perlin1(value.x, stretch, amplitude, seed.x, octaves),
        perlin1(value.y, stretch, amplitude, seed.y, octaves)
    )