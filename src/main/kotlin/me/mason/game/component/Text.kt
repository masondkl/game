package me.mason.game.component

import me.mason.game.*

val CHARACTER_UV_SCALE = 5
val CHARACTER_UV_SCALE_VEC = vec(CHARACTER_UV_SCALE)

interface Text: Mesh {
    val tick: Tick
    val position: FloatVector
    var value: String
}

fun alphabet() = Array(36) {
    val x = it % 12
    val y = it / 12
    vec(x * 5, 76 + y * 5)
}

fun Array<IntVector>.actorSprite(char: Char): IntVector = this[
    if (char.uppercaseChar() in 65.toChar()..90.toChar()) char.uppercaseChar() - 65.toChar()
    else char.uppercaseChar() - 48.toChar() + 26
]


const val TEXT_PADDING = 2f
fun Array<IntVector>.text(
    shader: Shader,
    position: FloatVector,
    characterScale: FloatVector,
    text: String,
    limit: Int = 128,
    ui: Boolean = true
): Text = object : Text, Mesh by mesh(shader, limit) {
    val self = this
    override var value = text
    override val position = position.clone()
    override val tick: Tick = {
        val scaleX = characterScale.x * value.length + value.length * TEXT_PADDING
        clear(0 until limit)
        (value.indices).forEach {
            if (value[it] == ' ') return@forEach
            val characterPosition = vec((self.position.x - scaleX / 2) + it * characterScale.x, self.position.y)
            sprite(it, actorSprite(value[it]), CHARACTER_UV_SCALE_VEC)
            bounds(it, characterPosition + vec(TEXT_PADDING, 0f) * it.toFloat(), characterScale, ui)
        }
    }
}