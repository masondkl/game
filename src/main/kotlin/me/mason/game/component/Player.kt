package me.mason.game.component

import me.mason.game.*
import org.lwjgl.glfw.GLFW.*
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource.Monotonic.markNow

private val UV_SCALE = vec(16, 20)
private val SCALE = vec(32f, 40f)

val RIGHT =       arrayOf(vec(0, 118), vec(15, 118), vec(30, 118), vec(45, 118))
val LEFT =      arrayOf(vec(60, 118), vec(75, 118), vec(90, 118), vec(105, 118))
val WALK_LEFT =       arrayOf(vec(0, 138), vec(15, 138), vec(30, 138), vec(45, 138))
val WALK_RIGHT =      arrayOf(vec(60, 138), vec(75, 138), vec(90, 138), vec(105, 138))

interface Player : Mesh {
    val tick: Tick
    val position: FloatVector
}

val MAX_MOTION = 256f

fun Window.player(world: World, shader: Shader): Player {
    val mesh = mesh(1, shader)
    val position = vec(0f, world.highestAt(0f) + 2.5f)
    val motion = vec(0f)
    val collider = collider(position, vec(31f, 39f))
    var animation = LEFT
    keys(GLFW_KEY_SPACE, GLFW_PRESS) { _, _ ->
        if (world.colliders.none { collider.collides(it, vec(0f, -1f)) })
            return@keys
        motion.y = 288f
    }
    var last = markNow()
    val tick: Tick = {
        mesh.sprite(0, animation[frame(0.5.seconds, animation.size)], UV_SCALE)
        mesh.bounds(0, (position * 32f).int().float() / 32f, SCALE)
        if (keys[GLFW_KEY_A]) {
            motion.x = max(min(motion.x - dt * MAX_MOTION, MAX_MOTION * dt), -MAX_MOTION * dt)
            animation = WALK_LEFT
        }
        if (keys[GLFW_KEY_D]) {
            motion.x = max(min(motion.x + dt * MAX_MOTION, MAX_MOTION * dt), -MAX_MOTION * dt)
            animation = WALK_RIGHT
        }
        if (abs(motion.x) < 0.0005f && animation.contentEquals(WALK_LEFT)) animation = RIGHT
        if (abs(motion.x) < 0.0005f && animation.contentEquals(WALK_RIGHT)) animation = LEFT
        motion.x *= 0.9f
        motion.y -= 1500 * dt
        if (motion.y < 0f) motion.y = 0f
        position += collider.move(
            vec(0f, max(-256f * dt, -16f)) + vec(motion.x, min(motion.y, 512f * dt)),
            world.colliders
        )
        if (last.elapsedNow() > 3.seconds) {
//            println("${position.x}, ${position.y}")
            last = markNow()
        }
    }
    return object : Player, Mesh by mesh {
        override val tick = tick
        override val position = position
    }
}

fun Collider.move(motion: FloatVector, collisions: List<Collider>): FloatVector {
    val change = vec(motion.x, motion.y)
    val intersectsHor = collisions.find { collides(it, change = vec(change.x, 0f)) }
    val intersectsDia = collisions.find { collides(it, change = vec(change.x, change.y)) }
    val intersectVer = collisions.find { collides(it, change = vec(0f, change.y)) }
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
    }; return change
}


//fun Window.player(position: FloatVector): MeshAdapter {
//    var animation = LEFT
//    var sprite = 0
//    return { mesh, index ->
//        mesh.uv(index, animation[sprite], UV_SCALE)
//        mesh.vertices(index, position, SCALE)
//        QUADS
//    }
//}


//private val LEFT_1_SPRITE = uv(vec(0, 0), vec(15, 20))
//private val LEFT_2_SPRITE = uv(vec(15, 0), vec(15, 20))
//private val LEFT_3_SPRITE = uv(vec(30, 0), vec(15, 20))
//private val LEFT_4_SPRITE = uv(vec(45, 0), vec(15, 20))
//private val LEFT_SPRITES = arrayOf(LEFT_1_SPRITE, LEFT_2_SPRITE, LEFT_3_SPRITE, LEFT_4_SPRITE)
//
//private val RIGHT_1_SPRITE = uv(vec(60, 0), vec(15, 20))
//private val RIGHT_2_SPRITE = uv(vec(75, 0), vec(15, 20))
//private val RIGHT_3_SPRITE = uv(vec(90, 0), vec(15, 20))
//private val RIGHT_4_SPRITE = uv(vec(105, 0), vec(15, 20))
//private val RIGHT_SPRITES = arrayOf(RIGHT_1_SPRITE, RIGHT_2_SPRITE, RIGHT_3_SPRITE, RIGHT_4_SPRITE)
//
//private val WALK_LEFT_1_SPRITE = uv(vec(0, 20), vec(15, 20))
//private val WALK_LEFT_2_SPRITE = uv(vec(15, 20), vec(15, 20))
//private val WALK_LEFT_3_SPRITE = uv(vec(30, 20), vec(15, 20))
//private val WALK_LEFT_4_SPRITE = uv(vec(45, 20), vec(15, 20))
//private val WALK_LEFT_SPRITES = arrayOf(WALK_LEFT_1_SPRITE, WALK_LEFT_2_SPRITE, WALK_LEFT_3_SPRITE, WALK_LEFT_4_SPRITE)
//
//private val WALK_RIGHT_1_SPRITE = uv(vec(60, 20), vec(15, 20))
//private val WALK_RIGHT_2_SPRITE = uv(vec(75, 20), vec(15, 20))
//private val WALK_RIGHT_3_SPRITE = uv(vec(90, 20), vec(15, 20))
//private val WALK_RIGHT_4_SPRITE = uv(vec(105, 20), vec(15, 20))
//private val WALK_RIGHT_SPRITES = arrayOf(WALK_RIGHT_1_SPRITE, WALK_RIGHT_2_SPRITE, WALK_RIGHT_3_SPRITE, WALK_RIGHT_4_SPRITE)
