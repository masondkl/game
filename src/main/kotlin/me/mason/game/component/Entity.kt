package me.mason.game.component

import me.mason.game.*

typealias Tick = Window.() -> (Unit)

interface Entity {
    val mesh: Mesh
    val children: List<Entity>
}

fun entity(
    shader: Shader,
    vararg children: Entity,
    limit: Int = 1
) = object : Entity {
    override val mesh = mesh(shader, limit)
    override val children: List<Entity>
        get() = TODO("Not yet implemented")

}