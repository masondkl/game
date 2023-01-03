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
    val motion: Vector2f
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
        override val motion = Vector2f()
        override val scale = scale
        override var sprites = sprites.toList().toTypedArray()
        override var sprite = 0
        override fun tick(actors: List<Actor>) = tick(this, this@actor, actors)
        override fun plusAssign(tick: Tick) = this.tick.plusAssign(tick)
        override fun minusAssign(tick: Tick) = this.tick.minusAssign(tick)
    }
}

fun Actor.intersects(
    with: Actor,
    expand: Vector2f = Vector2f(),
    change: Vector2f = Vector2f()
) = position.y + change.y - (scale.y + expand.y) / 2 < with.position.y + with.scale.y / 2 &&
        position.y + change.y + (scale.y + expand.y) / 2 > with.position.y - with.scale.y / 2 &&
        position.x + change.x - (scale.x + expand.x) / 2 < with.position.x + with.scale.x / 2 &&
        position.x + change.x + (scale.x + expand.x) / 2 > with.position.x - with.scale.x / 2

fun Actor.move(change: Vector2f, nearby: List<Actor>) {
    nearby.run {
        val vertical = Vector2f(0f, change.y)
        val intersectsHor = any { intersects(it, change = Vector2f(change.x, 0f)) }
        val intersectsDia = any { intersects(it, change = Vector2f(change.x, change.y)) }
        val intersectsVer = any { intersects(it, change = vertical) }
        if (intersectsVer) { change.y = 0f; motion.y = 0f }
        else if (motion.y < 0) motion.y *= 1.4f
        if (intersectsHor || (!intersectsVer && intersectsDia)) { change.x = 0f; motion.x = 0f }
    }
}