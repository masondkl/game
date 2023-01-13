package me.mason.game.components

import me.mason.game.*
import me.mason.game.plusAssign
import kotlin.math.abs
import kotlin.math.max

typealias Tick<T> = context(Window) T.() -> (Unit)

interface Entity {
    val tick: Set<Tick<Entity>>
    val mesh: Mesh
    val position: FloatVector
    val motion: FloatVector
    val scale: FloatVector
    var sprite: UV?
}

context(Window)
fun Entity.tick() {
    this.tick.forEach { it(this@Window, this) }
}

fun entity(
    sprite: UV? = null,
    scale: FloatVector = vec(0f, 0f),
    position: FloatVector = vec(0f, 0f),
    quads: Int = 1,
    tick: Tick<Entity>? = null
): Entity = object : Entity {
    override val mesh = mesh(quads)
    override val position = position
    override val motion = vec(0f, 0f)
    override val scale = scale
    override var sprite: UV? = sprite
    override val tick = HashSet<Tick<Entity>>()
    init {
        sprite?.also {
            mesh += mesh(bounds(position, scale), it)
        }
        tick?.also {
            this.tick += it
        }
    }
}

context(Window)
fun Entity.clone() = object : Entity {
    override val tick = this@clone.tick.toSet()
    override val mesh = mesh(this@clone.mesh.quads).also { it += this@clone.mesh }
    override val position = this@clone.position
    override val motion = this@clone.motion
    override val scale = this@clone.scale
    override var sprite = this@clone.sprite
}
//
//fun Entity.mesh(): Mesh? =
//    if (sprite != null) mesh(bounds(position, scale) to sprite!!)
//    else null

fun Entity.collides(b: Entity, change: FloatVector = vec(0f, 0f), expand: FloatVector = vec(0f, 0f)) =
    position.y + change.y - (scale.y + expand.y) / 2 < b.position.y + b.scale.y / 2 &&
        position.y + change.y + (scale.y + expand.y) / 2 > b.position.y - b.scale.y / 2 &&
        position.x + change.x - (scale.x + expand.x) / 2 < b.position.x + b.scale.x / 2 &&
        position.x + change.x + (scale.x + expand.x) / 2 > b.position.x - b.scale.x / 2

fun Entity.move(change: FloatVector, collision: Set<Entity>) = collision.apply {
    val vertical = vec(0f, change.y)
    val intersectsHor = find { collides(it, change = vec(change.x, 0f)) }
    val intersectsDia = find { collides(it, change = vec(change.x, change.y)) }
    val intersectVer = find { collides(it, change = vertical) }
    if (intersectVer != null) {
        val max = intersectVer.position.y + intersectVer.scale.y / 2
        val min = position.y + change.y - scale.y / 2
        if (max - min < abs(change.y)) change.y += (max - min) * 1.05f
        else change.y = 0f
    }
    if (intersectsHor != null || intersectVer == null && intersectsDia != null) {
        val with = intersectsHor ?: intersectsDia!!
        val withMax = with.position.x + with.scale.x / 2
        val withMin = with.position.x - with.scale.x / 2
        val max = position.x + change.x + scale.x / 2
        val min = position.x + change.x - scale.x / 2
        if (withMax - min > change.x && withMax - min < with.scale.x) {
            change.x += (withMax - min) * 1.05f
        } else if (max - withMin < change.x && max - withMin < with.scale.x) {
            change.x -= (max - withMin) * 1.05f
        } else change.x = 0f
    }
}

context(Window)
fun Entity.motion(collision: Set<Entity>) {
    motion.x *= 0.8f
    val change = (motion * dt)
        .min(vec(32f, 32f)).max(vec(-32f, -32f)) // stop motion from putting you into blocks
    move(change, collision)
    position += change
}

context(Window)
fun Entity.gravity() {
    motion.y = max(motion.y - 1200f * dt, -600f)
}