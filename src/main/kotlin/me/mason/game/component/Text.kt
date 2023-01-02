package me.mason.game

import me.mason.game.component.Actor
import me.mason.game.component.actor
import me.mason.game.component.drawable
import org.joml.Vector2f
import org.joml.Vector2i

fun Window.alphabet() = Array(36) {
    val x = it % 12
    val y = it / 12
    actor(
        Vector2f(0f, 0f),
        Vector2f(25f, 25f),
        false,
        uv(Vector2i(x * 5, 49 + y * 5), Vector2i(5, 5), SHEET_SIZE)
    )
}

fun Array<Actor>.actorOf(char: Char): Actor = this[
        if (char.uppercaseChar() in 65.toChar()..90.toChar()) char.uppercaseChar() - 65.toChar()
        else char.uppercaseChar() - 48.toChar() + 26
]

fun Array<Actor>.text(position: Vector2f, text: String): Array<Drawable> {
    return Array(text.length) {
        if (text[it] == ' ') return@Array null
        val character = actorOf(text[it])
        character.drawable(Vector2f(30f * it, position.y))
    }.filterNotNull().toTypedArray()
}
