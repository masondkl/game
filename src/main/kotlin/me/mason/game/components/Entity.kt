package me.mason.game.components

import me.mason.game.*
import me.mason.game.Animation
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

typealias Tick<T> = context(Window) T.() -> (Unit)

interface Entity {
    val tick: Set<Tick<Entity>>
    val ui: Boolean
    val position: FloatVector
    val motion: FloatVector
    val scale: FloatVector
    var animations: Array<Animation?>
}

context(Window)
fun Entity.tick() {
    this.tick.forEach { it(this@Window, this) }
}

fun entity(
    position: FloatVector = vec(0f, 0f),
    scale: FloatVector = vec(0f, 0f),
    vararg animations: Animation?,
    ui: Boolean = false,
    tick: Tick<Entity>? = null
): Entity = object : Entity {
    override val tick = HashSet<Tick<Entity>>()
    override val ui = ui
    override val position = vec(position.x, position.y)
    override val motion = vec(0f, 0f)
    override val scale = scale
    override var animations = arrayOf(*animations)
    init { tick?.also { this.tick += it } }
}

fun Entity.clone() = object : Entity {
    override val tick = this@clone.tick.toSet()
    override val position = this@clone.position
    override val motion = this@clone.motion
    override val scale = this@clone.scale
    override val ui = this@clone.ui
    override var animations = this@clone.animations.clone()
}

fun group(
    vararg entities: Entity,
    scale: FloatVector = vec(0f, 0f),
    ui: Boolean = false,
    groupTick: Tick<Entity>? = null
) = object : Entity {
    override val tick = HashSet<Tick<Entity>>()
    override val motion = vec(0f, 0f)
    fun change(to: Float, get: (Entity) -> (Float)): Float {
        val min = entities.minOf(get)
        val max = entities.maxOf(get)
        val center = min + (max - min) / 2f
        return to - center
    }
    override val position = object : FloatVector by vec(0f, 0f) {
        override var x: Float
            get() {
                val min = entities.minOf { it.position.x }
                val max = entities.maxOf { it.position.x }
                return min + (max - min) / 2f
            }
            set(value) {
                val change = change(value) { it.position.x }
                entities.forEach { it.position.x += change }
            }
        override var y: Float
            get() {
                val min = entities.minOf { it.position.y }
                val max = entities.maxOf { it.position.y }
                return min + (max - min) / 2f
            }
            set(value) {
                val change = change(value) { it.position.y }
                entities.forEach { it.position.y += change }
            }
    }
    override val scale = scale
    override val ui = ui
    override var animations: Array<Animation?>
        get() {
            var animIndex = 0
            val animations = entities
                .fold(ArrayList<Animation?>()) { acc, entity ->
                    acc += entity.animations.map { animation ->
                        val result =
                            if (animation == null) null
                            else {
                                animation(*Array(animation.states.size) { index ->
                                    val state = animation.states[index]
                                    val offset = state.offset + vec(
                                        change(entity.position.x) { it.position.x },
                                        change(entity.position.y) { it.position.y }
                                    )
//                                    vec(
//                                        change(entity.position.x) { it.position.x },
//                                        change(entity.position.y) { it.position.y }
//                                    )
                                    state(state.range, state.sprites, state.scale, offset, state.rate)
                                })
                            }
                        animIndex++; result
                    }; acc
                }.toTypedArray()
            return animations
        }
        set(value) { }
    init {
        tick += { entities.forEach { it.tick() } }
        groupTick?.also { tick += it }
    }
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

fun Entity.move(change: FloatVector, collision: List<Entity>) = collision.apply {
    val intersectsHor = find { collides(it, change = vec(change.x, 0f)) }
    val intersectsDia = find { collides(it, change = vec(change.x, change.y)) }
    val intersectVer = find { collides(it, change = vec(0f, change.y)) }
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
fun Entity.motion(collision: List<Entity>) {
    motion.x *= 0.8f
    if (motion.y > 0f && collision.any { collides(it, vec(0f, 1f)) }) {
        motion.y = 0f
    }
    val change = (motion * dt)
        .min(vec(32f, 32f)).max(vec(-32f, -32f)) // stop motion from putting you into blocks
    move(change, collision)
    position += change
}

context(Window)
fun Entity.gravity() {
    motion.y = max(motion.y - 1200f * dt, -600f)
}