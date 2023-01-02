package me.mason.game.component

import me.mason.game.Drawable
import me.mason.game.UV
import me.mason.game.Window
import me.mason.game.bounds
import org.joml.Vector2f

typealias Tick = context(Actor, Window) (List<Actor>) -> (Unit)
interface Actor {
    val collides: Boolean
    val tick: MutableList<Tick>
    val position: Vector2f
    val scale: Vector2f
    var sprites: Array<UV>
    var sprite: Int
    fun tick(actors: List<Actor>)
    operator fun plusAssign(tick: Tick)
    operator fun minusAssign(tick: Tick)
}

context(Window)
fun Actor.clone() = actor(position, scale, collides, *sprites) { others -> tick.forEach { it(this, this@Window, others) } }

fun Actor.drawable(lock: Vector2f? = null): Drawable =
    bounds(lock ?: position, scale) to sprites[sprite]

fun Window.actor(
    position: Vector2f,
    scale: Vector2f,
    collides: Boolean,
    vararg sprites: UV,
    tick: Tick = {}
): Actor {
    return object : Actor {
        override val collides = collides
        override val tick = ArrayList<Tick>()
        override val position = position
        override val scale = scale
        override var sprites = sprites.toList().toTypedArray()
        override var sprite = 0
        override fun tick(actors: List<Actor>) = tick(this, this@actor, actors)
        override fun plusAssign(tick: Tick) = this.tick.plusAssign(tick)
        override fun minusAssign(tick: Tick) = this.tick.minusAssign(tick)
    }
}