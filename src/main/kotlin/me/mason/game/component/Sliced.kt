package me.mason.game.component

import me.mason.game.*

//private const val SLICED_LIMIT = 9
//
//fun Window.sliced(
//    shader: Shader,
//    position: FloatVector, scale: FloatVector,
//    uvStart: IntVector, uvScale: Int
//) {
//
//    if (uvScale % 3 != 0) error("Cannot be turned into sliced sprite")
//    val slice = uvScale / 3
//    val ratio = vec(scale.y / scale.x, scale.x / scale.y)
//    adapter(SLICED_LIMIT, shader, { textureBatch(SLICED_LIMIT, shader) }) { mesh, index ->
//
//    }
//}

interface Sliced : Mesh {
    val tick: Tick
    val worldPosition: FloatVector
    var sprite: IntVector
}

fun Window.sliced(
    shader: Shader,
    origin: FloatVector, worldScale: FloatVector, inset: Float,
    sprite: IntVector, spriteScale: Int,
    ui: Boolean = false
): Sliced {
    val mesh = mesh(shader, 9)
    return object : Sliced, Mesh by mesh {
        val self = this
        override val worldPosition = origin.clone()
        override var sprite = sprite.clone()
        override val tick: Tick = {
            (0 until 9).forEach {
                val x = it % 3
                val y = it / 3
                val stretch = worldScale - 2 * inset
                val position = vec(
                    when (x) { 2 -> worldScale.x - inset / 2; 1 -> worldScale.x / 2; else -> inset / 2 },
                    when (y) { 2 -> inset / 2; 1 -> worldScale.y / 2; else -> worldScale.y - inset / 2 }
                ) + worldPosition - worldScale / 2f
                val scale = vec(
                    if (x == 1) stretch.x else inset,
                    if (y == 1) stretch.y else inset
                )
                val uv = self.sprite + vec(
                    spriteScale / 3f * x,
                    spriteScale / 3f * y
                ).int()
                mesh.sprite(it, uv, vec((spriteScale / 3f).toInt(), (spriteScale / 3f).toInt()))
                mesh.bounds(it, position + (if (ui) camera.position else vec(0f)), scale)
            }
        }
    }
}