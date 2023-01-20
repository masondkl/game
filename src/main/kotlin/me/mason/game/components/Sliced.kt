package me.mason.game.components

import me.mason.game.*

fun Window.sliced(
    worldPosition: FloatVector, worldScale: FloatVector,
    inset: Float,
    uvPosition: IntVector, uvScale: Int,
    ui: Boolean = false,
    slicedTick: Tick<Entity> = {}
): Entity {
    val slices = Array(9) { index ->
        val x = index % 3
        val y = index / 3
        val stretch = worldScale - 2 * inset
        val sprite = uv(
            uvPosition + vec(
                uvScale / 3f * x,
                uvScale / 3f * (2 - y)
            ).int(),
            vec((uvScale / 3f).toInt(), (uvScale / 3f).toInt())
        )
        val slicePosition = vec(
            when (x) { 2 -> inset / 2; 1 -> worldScale.x / 2; else -> worldScale.x - inset / 2 },
            when (y) { 2 -> inset / 2 ; 1 -> worldScale.y / 2; else -> worldScale.y - inset / 2 }
        ) + worldPosition - worldScale / 2f
        val sliceScale = vec(
            if (x == 1) stretch.x else inset,
            if (y == 1) stretch.y else inset
        )
        entity(worldPosition, worldScale, animation(state(index, sprite, sliceScale, worldPosition - slicePosition)))
    }
    val entity = group(*slices, scale = worldScale, ui = ui) {
        slicedTick(this@sliced, this)
    }
    return entity
}
