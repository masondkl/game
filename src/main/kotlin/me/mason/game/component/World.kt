package me.mason.game.component

import me.mason.game.SHEET_SIZE
import me.mason.game.UV
import me.mason.game.Window
import me.mason.game.uv
import org.joml.Vector2f
import org.joml.Vector2i


const val WORLD_SMALL = 0
const val WORLD_MEDIUM = 1
const val WORLD_LARGE = 2
const val WORLD_HEIGHT = 256
val GRASS_SPRITE = uv(Vector2i(0, 20), Vector2i(8, 8), SHEET_SIZE)
val DIRT_SPRITE = uv(Vector2i(8, 20), Vector2i(8, 8), SHEET_SIZE)
val GOLD = uv(Vector2i(43, 23), Vector2i(1, 1), SHEET_SIZE)

fun worldX(x: Int) = x * 32f + 1280 / 2
fun worldY(y: Int) = y * 32f + 720 / 2
fun Window.world(center: Actor, size: Int = -1): List<Actor> {
    val width = when (size) {
        -1 -> 1
        WORLD_SMALL -> 256
        WORLD_MEDIUM -> 512
        WORLD_LARGE -> 1024
        else -> 0
    }
    fun tile(center: Actor, x: Int, y: Int, sprite: UV): Actor {
        val worldX = worldX(x)
        val worldY = worldY(y)
        return actor(
            Vector2f(worldX, worldY), Vector2f(32f, 32f),
            collides = true, sprite
        ) {
            position.x = worldX - (center.position.x - 1280f / 2)
            position.y = worldY - (center.position.y - 3f * 720f / 8)
        }
    }
    val tiles = List(width * (if (size == -1) 1 else WORLD_HEIGHT)) {
        if (it / width == 0) tile(center, it - width / 2, 0, GRASS_SPRITE)
        else tile(center, (it % width) - width / 2, -(it / width), DIRT_SPRITE)
    }
    return tiles
}