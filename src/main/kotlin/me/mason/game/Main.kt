package me.mason.game

import me.mason.game.component.*
import org.lwjgl.glfw.GLFW.*
import java.nio.file.Paths
import kotlin.math.floor

const val SHEET_SIZE = 512
val SHEET_SIZE_VEC = vec(SHEET_SIZE)

fun textureBatch(limit: Int, shader: Shader) = mesh(limit, shader)

fun main() = window("Game", 1280, 720) {
    val texture = shader(Paths.get("texture.glsl"), 2, 1)
    val sheet = texture(Paths.get("sheet512.png"))
    val world = world(texture)
    val player = player(world, texture)
    val inventory = sliced(texture, vec(-14.5f, 8f), vec(8.0f, 5.0f), 0.6666f, vec(4, 91), 12, true)
    scene(sheet) {
        camera.position.set((player.position * 32f).int().float() / 32f)
        camera.look()
        draw(world, player, inventory)
        println(1/dt)
    }
}