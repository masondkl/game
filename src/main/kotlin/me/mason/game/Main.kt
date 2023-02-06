package me.mason.game

import me.mason.game.component.*
import java.nio.file.Paths

const val SHEET_SIZE = 512
val SHEET_SIZE_VEC = vec(SHEET_SIZE)

fun main() = window("Game", 1280, 720) {
    val sheet = texture(Paths.get("sheet128.png"))
    val world = world()
    val gameScene: Draw = {
        draw(world)
        println(1/dt)
    }
    scene(sheet, gameScene)
}