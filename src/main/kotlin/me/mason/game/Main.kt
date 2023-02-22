package me.mason.game

import me.mason.game.component.*
import org.lwjgl.BufferUtils.createDoubleBuffer
import org.lwjgl.BufferUtils.createIntBuffer
import org.lwjgl.glfw.GLFW.*
import java.nio.file.Paths
import kotlin.math.floor

const val SHEET_SIZE = 512
val SHEET_SIZE_VEC = vec(SHEET_SIZE)
val POINTER_SPRITE = vec(13, 159)
val POINTER_UV_SCALE = 14
val POINTER_UV_SCALE_VEC = vec(POINTER_UV_SCALE)
val POINTER_SCALE = 14f
val POINTER_SCALE_VEC = vec(POINTER_SCALE)

fun main() = window("Game", 1280, 720) {
    val texture = shader(Paths.get("texture.glsl"), 2, 1)
    val sheet = texture(Paths.get("sheet512.png"))
    val world = world(texture)
    val player = player(world, texture)
    val inventory = inventory(texture)
    val cursor = mesh(1, texture)
    scene(sheet) {
        val cameraTile = (camera.position / TILE_SCALE).int() + WORLD_RADIUS

//        world[world.colliders.minBy {
//            val width = createIntBuffer(1)
//            val height = createIntBuffer(1)
//            glfwGetWindowSize(id, width, height)
//            val xBuffer = createDoubleBuffer(1)
//            val yBuffer = createDoubleBuffer(1)
//            glfwGetCursorPos(id, xBuffer, yBuffer)
//            val x = xBuffer[0].toFloat() - width[0] / 2f + camera.position.x
//            val y = (height[0] - yBuffer[0].toFloat()) - height[0] / 2f + camera.position.y
//            it.position.distance(vec(x, y))
//        }.position] = MATERIAL_GRASS

        val xBuffer = createDoubleBuffer(1)
        val yBuffer = createDoubleBuffer(1)
        glfwGetCursorPos(id, xBuffer, yBuffer)
        if (xBuffer[0] < 0 || xBuffer[0] > 1280 || yBuffer[0] < 0 || yBuffer[0] > 720) {
            glfwSetCursorPos(id, minOf(maxOf(xBuffer[0], 0.0), 1280.0), minOf(maxOf(yBuffer[0], 0.0), 720.0))
            glfwGetCursorPos(id, xBuffer, yBuffer)
        }
        val x = xBuffer[0].toFloat() - 1280f / 2f
        val y = (720f - yBuffer[0].toFloat()) - 720f / 2f
        cursor.sprite(0, POINTER_SPRITE, POINTER_UV_SCALE_VEC)
        cursor.bounds(0, vec(
            x,
//                    + POINTER_SCALE / 2,
            y
//                    - POINTER_SCALE / 2
        ), POINTER_SCALE_VEC, true)

        if (mouse[GLFW_MOUSE_BUTTON_LEFT]) {
            world.breakAt(camera.position + vec(x, y), dt)
//            world[] = MATERIAL_GRASS
        }
        camera.position.set(player.position.int().float())
        camera.look()
        world.tick(this)
        player.tick(this)
        inventory.tick(this)
        draw(world, player, inventory, cursor)
//        println(1/dt)
    }
}