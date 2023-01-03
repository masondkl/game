package me.mason.game

import me.mason.game.component.*
import org.joml.Math.*
import org.joml.Vector2f
import org.joml.Vector2i
import org.lwjgl.glfw.GLFW.*
import java.nio.file.Paths
import java.util.*

val SHEET_SIZE = Vector2i(64, 64)

fun List<Actor>.within(withinX: Float, withinY: Float) =
    filter {
        abs(it.position.x - 1280f / 2) < withinX && abs(it.position.y - 720f / 2) < withinY
    }

fun main() = window("Game", 1280, 720) {
    val pressed = BitSet(256).apply {
        input { key, action ->
            if (key !in 0 until 256) return@input
            when(action) {
                GLFW_PRESS -> set(key)
                GLFW_RELEASE -> clear(key)
            }
        }
    }
    val offset = Vector2f(0f, 0f)
    val tiles = world(offset, size = WORLD_LARGE)
    val player = player(pressed, offset)
    val center = Vector2f(1280f/2f, 720f/2f)
    val actors = ArrayList<Actor>()
    offset.add(center.x, tiles[0].position.y + center.y)
    val gameScene: Draw = {
        val renderTiles = tiles.within(21f * 32f, 21f * 32f)
        tiles.forEach { it.tick(actors) }
        player.tick(tiles)
//        val tileBoundsCorners = renderTiles.fold(ArrayList<Drawable>()) { acc, it ->
//            acc += listOf(
//                bounds(
//                    Vector2f(it.position.x - it.scale.x / 2, it.position.y - it.scale.y / 2),
//                    Vector2f(8f, 8f)
//                ) to GOLD,
//                bounds(
//                    Vector2f(it.position.x - it.scale.x / 2, it.position.y + it.scale.y / 2),
//                    Vector2f(8f, 8f)
//                ) to GOLD,
//                bounds(
//                    Vector2f(it.position.x + it.scale.x / 2, it.position.y - it.scale.y / 2),
//                    Vector2f(8f, 8f)
//                ) to GOLD,
//                bounds(
//                    Vector2f(it.position.x + it.scale.x / 2, it.position.y + it.scale.y / 2),
//                    Vector2f(8f, 8f)
//                ) to GOLD
//            ); acc
//        }.toTypedArray()
//        val playerBoundsCorners = arrayOf(
//            bounds(
//                Vector2f(1280f / 2f - player.scale.x / 2, 720f / 2f - player.scale.y / 2),
//                Vector2f(8f, 8f)
//            ) to GOLD,
//            bounds(
//                Vector2f(1280f / 2f - player.scale.x / 2, 720f / 2f + player.scale.y / 2),
//                Vector2f(8f, 8f)
//            ) to GOLD,
//            bounds(
//                Vector2f(1280f / 2f + player.scale.x / 2, 720f / 2f - player.scale.y / 2),
//                Vector2f(8f, 8f)
//            ) to GOLD,
//            bounds(
//                Vector2f(1280f / 2f + player.scale.x / 2, 720f / 2f + player.scale.y / 2),
//                Vector2f(8f, 8f)
//            ) to GOLD
//        )
        draw(
            *renderTiles.map { it.drawable() }.toTypedArray(),
//            *tileBoundsCorners,
            player.drawable(center),
//            *playerBoundsCorners
        )
    }

    val textureShader = shader(Paths.get("texture.glsl"))
    val camera = camera(Vector2f(0f, 0f))
    val sheet = texture(Paths.get("sheet.png"))

    scene(textureShader, camera, sheet, gameScene)
}