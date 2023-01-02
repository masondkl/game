package me.mason.game.component

import me.mason.game.SHEET_SIZE
import me.mason.game.Window
import me.mason.game.uv
import me.mason.game.within
import org.joml.Vector2f
import org.joml.Vector2i
import org.lwjgl.glfw.GLFW
import java.util.*


val PLAYER_RIGHT_1 = uv(Vector2i(0, 0), Vector2i(15, 20), SHEET_SIZE)
val PLAYER_RIGHT_2 = uv(Vector2i(15, 0), Vector2i(15, 20), SHEET_SIZE)
val PLAYER_LEFT_1 = uv(Vector2i(30, 0), Vector2i(15, 20), SHEET_SIZE)
val PLAYER_LEFT_2 = uv(Vector2i(45, 0), Vector2i(15, 20), SHEET_SIZE)

fun Window.player(pressed: BitSet): Actor {
    val motion = Vector2f(0f, -2f)
    val center = Vector2f(1280f / 2, 720f / 2)
    val origin = Vector2f(1280f / 2, 5f * 720f / 8)
    val left = arrayOf(PLAYER_LEFT_1, PLAYER_LEFT_2)
    val right = arrayOf(PLAYER_RIGHT_1, PLAYER_RIGHT_2)
    var previous = origin
    var lastSpriteUpdate = elapsed
    return actor(
        origin, Vector2f(64f, 96f),
        collides = true, *left
    ) { entities ->
        if (elapsed - lastSpriteUpdate > 0.25f) {
            ++sprite; lastSpriteUpdate = elapsed
        }
        if (sprite == sprites.size) sprite = 0
        if (pressed[GLFW.GLFW_KEY_D]) {
            if (sprites !== left) sprites = left
            motion.x += 100.0f
        }
        if (pressed[GLFW.GLFW_KEY_A]) {
            if (sprites !== right) sprites = right
            motion.x -= 100.0f
        }
        val nearby = entities.within(256f, 256f).filter { it.collides }
        fun intersects(actor: Actor, change: Vector2f = Vector2f(0f, 0f)) =
            (center.y + change.y - scale.y / 2 < actor.position.y + actor.scale.y / 2 &&
                    center.y + change.y + scale.y / 2 > actor.position.y - actor.scale.y / 2 &&
                    center.x + change.x - scale.x / 2 < actor.position.x + actor.scale.x / 2 &&
                    center.x + change.x + scale.x / 2 > actor.position.x - actor.scale.x / 2)
        if (nearby.any { intersects(it) }) {
            position.x = previous.x
            position.y = previous.y
            return@actor
        }
        val change = motion
            .mul(dt, dt, Vector2f())
            .min(Vector2f(32f, 32f), Vector2f())
            .max(Vector2f(-32f, -32f), Vector2f())
        val intersectsHor = nearby.any { intersects(it, Vector2f(change.x, 0f)) }
        val intersectsVer = nearby.any { intersects(it, Vector2f(0f, change.y)) }
        if (intersectsVer) { change.y = 0f; motion.y *= 0.5f }
        else {
            if (motion.y > -8f) motion.y = -8f
            motion.y *= 1.4f
        }
        if (intersectsHor) { change.x = 0f; motion.x = 0f }
        position.add(change.min(Vector2f(change.x, 8f)).max(Vector2f(change.x, -8f)))
        if (!intersectsVer && !intersectsHor) previous = position
        motion.mul(0.8f, 1f)
    }
}