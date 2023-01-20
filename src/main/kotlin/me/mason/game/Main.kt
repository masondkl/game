package me.mason.game

import me.mason.game.components.*
import java.nio.file.Paths

val SHEET_SIZE = vec(128, 128)
private val TOP_LEFT = vec(-1280f/2f, 720f/2f)

fun main() = window("Game", 1280, 720) {
    val texture = shader(Paths.get("texture.glsl"))
    val sheet = texture(Paths.get("sheet128.png"))
    val world = world(mouse, 1024)
    val player = player(
        world
    )
    val inventory = inventory(9, 4)
    val gameScene: Draw = {
        player.tick()
        camera.position = player.position
        world.tick()
        inventory.tick()
        draw(
            *world.nearbyBlocks(),
            player,
            inventory
        )
        println(1/dt)
    }
    scene(texture, sheet, gameScene)
}