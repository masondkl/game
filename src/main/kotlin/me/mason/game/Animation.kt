package me.mason.game

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

//import me.mason.game.Mesh

interface Drawable {
    operator fun component1(): UV
    operator fun component2(): FloatVector
    operator fun component3(): FloatVector
}

interface State {
    val range: IntRange
    val sprites: Array<UV>
    val scale: FloatVector
    val offset: FloatVector
    val rate: Duration?
}

fun state(
    quad: Int, sprite: UV,
    scale: FloatVector, offset: FloatVector = vec(0f, 0f),
    rate: Duration? = null
) = object : State {
    override val range = quad..quad
    override val sprites = arrayOf(sprite)
    override val scale = scale
    override val offset = offset
    override val rate = rate
}

fun state(
    range: IntRange, sprites: Array<UV>,
    scale: FloatVector, offset: FloatVector = vec(0f, 0f),
    rate: Duration? = null
) = object : State {
    override val range = range
    override val sprites = sprites
    override val scale = scale
    override val offset = offset
    override val rate = rate
}

interface Animation {
    val states: Array<State>

    fun next(elapsed: Float): Drawable

    fun sprites(): Int
    fun sprite(): Int
    fun sprite(sprite: Int)

    fun state(): Int
    fun state(state: Int)
}

fun Animation.clone() = animation(*states)

fun animation(vararg states: State) = object : Animation {
    var state: State? = null
    var stateIndex = 0
    var spriteIndex = 0
    override val states = arrayOf(*states)
    override fun next(elapsed: Float): Drawable {
        if (state!!.rate != null) {
            val length = state!!.rate!!.inWholeMilliseconds / 1000f
            val step = elapsed % length
            spriteIndex = floor(step / length * sprites()).toInt()
        }; assert(spriteIndex < state!!.sprites.size)
        val sprite = state!!.sprites[min(spriteIndex, state!!.sprites.size - 1)]
        return object : Drawable {
            override fun component1() = sprite
            override fun component2() = state!!.scale
            override fun component3() = state!!.offset
        }
    }
    override fun sprites(): Int {
        return if (state == null) 0
            else state!!.range.last - state!!.range.first + 1
    }
    override fun sprite() = spriteIndex
    override fun sprite(sprite: Int) {
        if (sprite >= sprites()) {
            println("problem")
            return
        }
        spriteIndex = sprite
    }
    override fun state() = stateIndex
    override fun state(state: Int) {
        this.state = states[state]
        stateIndex = state
        spriteIndex = 0
    }
    init { state(0) }
}

fun sprite(sprite: UV, scale: FloatVector, offset: FloatVector = vec(0f, 0f), rate: Duration = 0.25.seconds) =
    animation(state(0, sprite, scale, offset, rate))

//fun mesh(bounds: Bounds, uv: UV) = object : Mesh {
//    override var quads = 1[
//    override val data = FloatArray(QUAD_UV_PAIR) {
//        val x = it % 4;
//        val y = it / 4
//        if (x < 2) bounds[it % 2 + y * 2]
//        else uv[it % 2 + y * 2]
//    }
//}