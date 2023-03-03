package me.mason.game.component

import me.mason.game.*
import org.lwjgl.glfw.GLFW.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

private val UV_SCALE = vec(16, 20)
val PLAYER_SCALE = vec(32f, 40f)

val RIGHT = arrayOf(vec(0, 118), vec(15, 118), vec(30, 118), vec(45, 118))
val LEFT = arrayOf(vec(60, 118), vec(75, 118), vec(90, 118), vec(105, 118))
val WALK_LEFT = arrayOf(vec(0, 138), vec(15, 138), vec(30, 138), vec(45, 138))
val WALK_RIGHT = arrayOf(vec(60, 138), vec(75, 138), vec(90, 138), vec(105, 138))

interface Player : Mesh {
    val tick: Tick
    val collider: Collider
    val position: FloatVector
}

val MAX_MOTION = 256f

fun Window.player(shader: Shader, world: World, inventory: Inventory, drops: MutableList<Drop>): Player {
    val mesh = mesh(shader, 1)
    val position = vec(0f, world.highestAt(0f) + 2.5f)
    val motion = vec(0f)
    val collider = collider(position, vec(31f, 39f))
    var animation = LEFT
    var fall = 0f
    keys(GLFW_KEY_SPACE, GLFW_PRESS) { _, _ ->
        if (world.tileColliders(position, TILE_SCALE * 12f).none { collider.collides(it, vec(0f, -1f)) })
            return@keys
        motion.y = 300f
    }
    return object : Player, Mesh by mesh {
        val self = this
        override val tick: Tick = {
            mesh.sprite(0, animation[frame(0.5.seconds, animation.size)], UV_SCALE)
            mesh.bounds(0, (position * 32f).int().float() / 32f, PLAYER_SCALE)
            if (keys[GLFW_KEY_A]) {
                motion.x = max(min(motion.x - dt * MAX_MOTION, MAX_MOTION * dt), -MAX_MOTION * dt)
                animation = WALK_LEFT
            }
            if (keys[GLFW_KEY_D]) {
                motion.x = max(min(motion.x + dt * MAX_MOTION, MAX_MOTION * dt), -MAX_MOTION * dt)
                animation = WALK_RIGHT
            }
            val colliders = world.tileColliders(position, TILE_SCALE * 12f)
            if (colliders.none { collider.collides(it, vec(0f, -1f)) }) {
                fall += dt
            } else if (fall != 0f) {
                println("${fall}")
                fall = 0f
            }
            if (abs(motion.x) < 0.0005f && animation.contentEquals(WALK_LEFT)) animation = RIGHT
            if (abs(motion.x) < 0.0005f && animation.contentEquals(WALK_RIGHT)) animation = LEFT
            motion.x *= 0.9f
            motion.y -= 2000f * dt
            if (motion.y < 0f) motion.y = 0f
            position += collider.move(
                vec(0f, max(-512f * dt, -16f)) + vec(motion.x, max(min(motion.y, 1024f * dt), -1024f * dt)),
                colliders
            )
            drops.removeIf {
                val nearby = it.collider.position.distance(position) < 32f
                if (nearby) inventory.add(materialItem(it.material)); nearby
            }
        }
        override val collider = collider
        override val position = position
        init { inventory.apply {
            mouse(GLFW_MOUSE_BUTTON_LEFT, GLFW_PRESS) { _, _ ->
                items[selected].left(this@player, self, world, inventory, items[selected])
            }
            mouse(GLFW_MOUSE_BUTTON_RIGHT, GLFW_PRESS) { _, _ ->
                items[selected].right(this@player, self, world, inventory, items[selected])
            }
        } }
    }
}
