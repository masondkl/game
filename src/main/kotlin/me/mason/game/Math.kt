package me.mason.game

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

interface FloatVector {
    var x: Float; var y: Float
    fun distance(b: FloatVector): Float
    operator fun plusAssign(b: FloatVector)
    operator fun plusAssign(v: Float)
    operator fun minusAssign(b: FloatVector)
    operator fun minusAssign(v: Float)
    operator fun divAssign(b: FloatVector)
    operator fun divAssign(v: Float)
    operator fun timesAssign(b: FloatVector)
    operator fun timesAssign(v: Float)
    override fun equals(other: Any?): Boolean
}

fun vec(ox: Float = 0f, oy: Float = 0f) = object : FloatVector {
    override var x = ox
    override var y = oy
    override fun distance(b: FloatVector) = sqrt((x - b.x).pow(2) + (y - b.y).pow(2))
    override fun plusAssign(b: FloatVector) { x += b.x; y += b.y }
    override fun plusAssign(v: Float) { x += v; y += v }
    override fun minusAssign(b: FloatVector) { x -= b.x; y -= b.y }
    override fun minusAssign(v: Float) { x -= v; y -= v }
    override fun divAssign(b: FloatVector) { x /= b.x; y /= b.y }
    override fun divAssign(v: Float) { x /= v; y /= v }
    override fun timesAssign(b: FloatVector) { x *= b.x; y *= b.y }
    override fun timesAssign(v: Float) { x *= v; y *= v }
    override fun equals(other: Any?): Boolean {
        if (other !is FloatVector) return false
        return x == other.x && y == other.y
    }
}

fun FloatVector.min(b: FloatVector) = vec(min(x, b.x), min(y, b.y))
fun FloatVector.max(b: FloatVector) = vec(max(x, b.x), max(y, b.y))
operator fun FloatVector.plus(b: FloatVector) = vec(x, y).also { it += b }
operator fun FloatVector.plus(v: Float) = vec(x, y).also { it += v }
operator fun FloatVector.minus(b: FloatVector) = vec(x, y).also { it -= b }
operator fun FloatVector.minus(v: Float) = vec(x, y).also { it -= v }
operator fun FloatVector.div(b: FloatVector) = vec(x, y).also { it /= b }
operator fun FloatVector.div(v: Float) = vec(x, y).also { it /= v }
operator fun FloatVector.times(b: FloatVector) = vec(x, y).also { it *= b }
operator fun FloatVector.times(v: Float) = vec(x, y).also { it *= v }

fun FloatVector.int() = vec(x.toInt(), y.toInt())
fun FloatVector.clone() = vec(x, y)

interface IntVector {
    var x: Int; var y: Int
    fun distance(b: IntVector): Int

    operator fun plusAssign(b: IntVector)
    operator fun plusAssign(v: Int)
    operator fun minusAssign(b: IntVector)
    operator fun minusAssign(v: Int)
    operator fun divAssign(b: IntVector)
    operator fun divAssign(v: Int)
    operator fun timesAssign(b: IntVector)
    operator fun timesAssign(v: Int)
    override fun equals(other: Any?): Boolean
}

fun vec(ox: Int = 0, oy: Int = 0) = object : IntVector {
    override var x = ox
    override var y = oy
    override fun distance(b: IntVector) = sqrt((x - b.x).toDouble().pow(2) + (y - b.y).toDouble().pow(2)).toInt()
    override fun plusAssign(b: IntVector) { x += b.x; y += b.y }
    override fun plusAssign(v: Int) { x += v; y += v }
    override fun minusAssign(b: IntVector) { x -= b.x; y -= b.y }
    override fun minusAssign(v: Int) { x -= v; y -= v }
    override fun divAssign(b: IntVector) { x /= b.x; y /= b.y }
    override fun divAssign(v: Int) { x /= v; y /= v }
    override fun timesAssign(b: IntVector) { x *= b.x; y *= b.y }
    override fun timesAssign(v: Int) { x *= v; y *= v }
    override fun equals(other: Any?): Boolean {
        if (other !is IntVector) return false
        return x == other.x && y == other.y
    }
}

operator fun IntVector.plus(b: IntVector) = vec(x, y).also { it += b }
operator fun IntVector.plus(v: Int) = vec(x, y).also { it += v }
operator fun IntVector.minus(b: IntVector) = vec(x, y).also { it -= b }
operator fun IntVector.minus(v: Int) = vec(x, y).also { it -= v }
operator fun IntVector.div(b: IntVector) = vec(x, y).also { it /= b }
operator fun IntVector.div(v: Int) = vec(x, y).also { it /= v }
operator fun IntVector.times(b: IntVector) = vec(x, y).also { it *= b }
operator fun IntVector.times(v: Int) = vec(x, y).also { it *= v }

fun IntVector.float() = vec(x.toFloat(), y.toFloat())
fun IntVector.clone() = vec(x, y)


