package me.mason.game

import me.mason.game.components.*
import org.lwjgl.glfw.GLFW.*
import java.nio.file.Paths
import java.util.*
import kotlin.math.abs


val SHEET_SIZE = vec(128, 128)

val PLAYER_LEFT_1 = uv(vec(0, 0), vec(15, 20))
val PLAYER_LEFT_2 = uv(vec(15, 0), vec(15, 20))
val PLAYER_LEFT_3 = uv(vec(30, 0), vec(15, 20))
val PLAYER_LEFT_4 = uv(vec(45, 0), vec(15, 20))

val PLAYER_RIGHT_1 = uv(vec(60, 0), vec(15, 20))
val PLAYER_RIGHT_2 = uv(vec(75, 0), vec(15, 20))
val PLAYER_RIGHT_3 = uv(vec(90, 0), vec(15, 20))
val PLAYER_RIGHT_4 = uv(vec(105, 0), vec(15, 20))

val PLAYER_WALK_LEFT_1 = uv(vec(0, 20), vec(15, 20))
val PLAYER_WALK_LEFT_2 = uv(vec(15, 20), vec(15, 20))
val PLAYER_WALK_LEFT_3 = uv(vec(30, 20), vec(15, 20))
val PLAYER_WALK_LEFT_4 = uv(vec(45, 20), vec(15, 20))

val PLAYER_WALK_RIGHT_1 = uv(vec(60, 20), vec(15, 20))
val PLAYER_WALK_RIGHT_2 = uv(vec(75, 20), vec(15, 20))
val PLAYER_WALK_RIGHT_3 = uv(vec(90, 20), vec(15, 20))
val PLAYER_WALK_RIGHT_4 = uv(vec(105, 20), vec(15, 20))

fun main() = window("Game", 1280, 720) {
    val keys = BitSet(256).apply {
        keys { key, action ->
            if (key !in 0 until 256) return@keys
            when(action) {
                GLFW_PRESS -> set(key)
                GLFW_RELEASE -> clear(key)
            }
        }
    }
//    val mouse = BitSet(2).apply {
//        mouse { key, action ->
//            if (key !in 0 until 2) return@mouse
//            when(action) {
//                GLFW_PRESS -> set(key)
//                GLFW_RELEASE -> clear(key)
//            }
//        }
//    }
    val textureShader = shader(Paths.get("texture.glsl"))
    val sheet = texture(Paths.get("sheet128.png"))
    val world = world(2048)
    var jumped = false
    keys(GLFW_KEY_SPACE, GLFW_PRESS) { _, _ -> jumped = true }
    val player = entity(sprite = PLAYER_LEFT_1, scale = vec(48f, 64f)) {
        val collisions = world.nearby()
            .filter { it.mesh.quads > 0 }
            .toSet()
        motion.x = if (keys[GLFW_KEY_D]) 500f else 0f +
                if (keys[GLFW_KEY_A]) -500f else 0f
        if (jumped && collisions.any { collides(it, vec(0f, -1f)) }) {
            motion.y = 450f
        }
        if (abs(motion.x) > 1f / 32f || abs(motion.y) > 1f / 32f)
            motion(collisions)
        gravity()
        jumped = false
        mesh[0] = mesh(bounds(position, scale), PLAYER_LEFT_1)
    }.apply { position.y = world.highest(0f) + scale.y / 2 + 32f }
    val inventoryView = sliced(
        vec(0f, 0f), vec(192f, 96f), 32f,
        vec(4, 91), 24,
        camera = camera
    )
    val gameScene: Draw = {
        world.tick()
        player.tick()
        camera.position = player.position
        inventoryView.forEach { it.tick() }
        draw(
            player,
            world,
            *inventoryView
        )
        println(1/dt)
    }
    scene(textureShader, sheet, gameScene)
}