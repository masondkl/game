package me.mason.game.component

import me.mason.game.SHEET_SIZE
import me.mason.game.Window
import me.mason.game.uv
import me.mason.game.within
import org.joml.Vector2f
import org.joml.Vector2i
import org.lwjgl.glfw.GLFW.*
import java.lang.Float.max
import java.lang.Float.min
import java.util.*


val PLAYER_RIGHT_1 = uv(Vector2i(0, 0), Vector2i(15, 20), SHEET_SIZE)
val PLAYER_RIGHT_2 = uv(Vector2i(15, 0), Vector2i(15, 20), SHEET_SIZE)
val PLAYER_LEFT_1 = uv(Vector2i(30, 0), Vector2i(15, 20), SHEET_SIZE)
val PLAYER_LEFT_2 = uv(Vector2i(45, 0), Vector2i(15, 20), SHEET_SIZE)

fun Window.player(pressed: BitSet, offset: Vector2f): Actor {
    val center = Vector2f(1280f / 2, 720f / 2)
    val left = arrayOf(PLAYER_LEFT_1, PLAYER_LEFT_2)
    val right = arrayOf(PLAYER_RIGHT_1, PLAYER_RIGHT_2)
    var lastSpriteUpdate = elapsed
    var canJump = false
    var jumped = false
    input(GLFW_KEY_SPACE) { _, action ->
        if (action != GLFW_PRESS || !canJump) return@input
        jumped = true
    }
    return actor(
        center, Vector2f(60f, 90f),
        collides = true, *left
    ) { entities ->
        if (elapsed - lastSpriteUpdate > 0.25f) {
            ++sprite; lastSpriteUpdate = elapsed
        }
        if (sprite == sprites.size) sprite = 0
        if (pressed[GLFW_KEY_D]) {
            if (sprites !== left) sprites = left
            motion.x += 100.0f
        }
        if (pressed[GLFW_KEY_A]) {
            if (sprites !== right) sprites = right
            motion.x -= 100.0f
        }
        if (jumped) { motion.y += 400f; jumped = false }
        val nearby = entities.within(256f, 256f).filter { it.collides }
        val change = motion
            .mul(dt, dt, Vector2f())
            .min(Vector2f(32f, 32f))
            .max(Vector2f(-32f, -32f))
        canJump = nearby.any { intersects(it, expand = Vector2f(0f, 8f), change = Vector2f(0f, change.y)) }
        move(change, nearby)
        offset.add(change)
        motion.mul(0.8f, 1f)
        motion.y -= 1000f * dt
        motion.x = max(min(motion.x, 400f), -400f)
        motion.y = max(min(motion.y, 400f), -400f)
    }
}