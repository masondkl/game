package me.mason.game

import me.mason.game.component.Actor
import me.mason.game.component.actor
import me.mason.game.component.drawable
import org.joml.Math.*
import org.joml.Vector2f
import org.joml.Vector2i
import org.lwjgl.glfw.GLFW.*
import java.nio.file.Paths
import java.util.*

val SHEET_SIZE = Vector2i(64, 64)
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
        if (pressed[GLFW_KEY_D]) {
            if (sprites !== left) sprites = left
            motion.x += 100.0f
        }
        if (pressed[GLFW_KEY_A]) {
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

const val SMALL = 0
const val MEDIUM = 1
const val LARGE = 2
const val HEIGHT = 256
val GRASS_SPRITE = uv(Vector2i(0, 20), Vector2i(8, 8), SHEET_SIZE)
val DIRT_SPRITE = uv(Vector2i(8, 20), Vector2i(8, 8), SHEET_SIZE)
val GOLD = uv(Vector2i(43, 23), Vector2i(1, 1), SHEET_SIZE)

fun worldX(x: Int) = x * 32f + 1280 / 2
fun worldY(y: Int) = y * 32f + 720 / 2
fun Window.world(center: Actor, size: Int = -1): List<Actor> {
    val width = when (size) {
        -1 -> 1
        SMALL -> 256
        MEDIUM -> 512
        LARGE -> 1024
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
    val tiles = List(width * (if (size == -1) 1 else HEIGHT)) {
        if (it / width == 0) tile(center, it - width / 2, 0, GRASS_SPRITE)
        else tile(center, (it % width) - width / 2, -(it / width), DIRT_SPRITE)
    }
    return tiles
}

fun List<Actor>.within(withinX: Float, withinY: Float) =
    filter {
        abs(it.position.x - 1280f / 2) < withinX && abs(it.position.y - 720f / 2) < withinY
    }

fun main() = window("Game", 1280, 720) {
    var frame = 0
    val pressed = BitSet(256).apply {
        input { key, action ->
            if (key !in 0 until 256) return@input
            when(action) {
                GLFW_PRESS -> set(key)
                GLFW_RELEASE -> clear(key)
            }
        }
    }
    val player = player(pressed)
    val tiles = world(player, size = SMALL)
    val center = Vector2f(1280f/2f, 720f/2f)
    val actors = ArrayList<Actor>()
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