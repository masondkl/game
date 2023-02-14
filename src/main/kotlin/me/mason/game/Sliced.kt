package me.mason.game

import me.mason.game.component.MeshAdapter
import me.mason.game.component.adapter

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

fun Window.sliced(
    shader: Shader,
    worldPosition: FloatVector, worldScale: FloatVector, inset: Float,
    uvPosition: IntVector, uvScale: Int,
    ui: Boolean = false,
//    slicedTick: Tick<Entity> = {}
): MeshAdapter {
//    val slices = Array(9) { index ->
//        val x = index % 3
//        val y = index / 3
//        val stretch = worldScale - 2 * inset
//        val sprite = uv(
//            uvPosition + vec(
//                uvScale / 3f * x,
//                uvScale / 3f * (2 - y)
//            ).int(),
//            vec((uvScale / 3f).toInt(), (uvScale / 3f).toInt())
//        )
//        val slicePosition = vec(
//            when (x) { 2 -> inset / 2; 1 -> worldScale.x / 2; else -> worldScale.x - inset / 2 },
//            when (y) { 2 -> inset / 2 ; 1 -> worldScale.y / 2; else -> worldScale.y - inset / 2 }
//        ) + worldPosition - worldScale / 2f
//        val sliceScale = vec(
//            if (x == 1) stretch.x else inset,
//            if (y == 1) stretch.y else inset
//        )
//        entity(worldPosition, worldScale, animation(state(index, sprite, sliceScale, worldPosition - slicePosition)))
//    }
//    val entity = group(*slices, scale = worldScale, ui = ui) {
//        slicedTick(this@sliced, this)
//    }
    val limit = 9
    return adapter(limit, shader, { textureBatch(limit, shader) }) { mesh, index ->
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
            val uv = uvPosition + vec(
                uvScale / 3f * x,
                uvScale / 3f * y
            ).int()
            mesh.sprite(index + it, uv, vec((uvScale / 3f).toInt(), (uvScale / 3f).toInt()))
            mesh.bounds(index + it, position + (if (ui) camera.position else vec(0f)), scale)
        }
    }
}