package me.mason.game.components

import me.mason.game.*

//private val EMPTY_UV = uv(vec(0, 0), vec(0, 0), vec(0, 0))
//private val EMPTY_BOUNDS = bounds(vec(0f, 0f), vec(0f, 0f))

fun Window.sliced(
    worldPosition: FloatVector, worldScale: FloatVector, inset: Float,
    uvPosition: IntVector, uvScale: Int,
    camera: Camera? = null,
): Array<Entity> {
    return Array(9) {
        val x = it % 3; val y = it / 3
        val stretch = worldScale - 2 * inset
        val sprite = uv(
            uvPosition + vec(
                uvScale / 3f * x,
                uvScale / 3f * (2 - y)
            ).int(),
            vec((uvScale / 3f).toInt(), (uvScale / 3f).toInt())
        )
        val scale = vec(
            if (x == 1) stretch.x else inset,
            if (y == 1) stretch.y else inset
        )
        val position = vec(
            if (x == 2) worldScale.x - inset / 2 else if (x == 1) worldScale.x / 2 else inset / 2,
            if (y == 2) worldScale.y - inset / 2 else if (y == 1) worldScale.y / 2 else inset / 2
        ) + worldPosition - worldScale / 2f
        entity(sprite, scale, position) {
            mesh[0] = mesh(bounds(vec(
                if (x == 2) worldScale.x - inset / 2 else if (x == 1) worldScale.x / 2 else inset / 2,
                if (y == 2) worldScale.y - inset / 2 else if (y == 1) worldScale.y / 2 else inset / 2
            ) + worldPosition - worldScale / 2f + (camera?.position ?: vec(0f, 0f)), scale), sprite)
        }
    }
}
