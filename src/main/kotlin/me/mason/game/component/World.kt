package me.mason.game.component

import me.mason.game.*
import java.nio.file.Paths
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.sin

val GRASS_TOP_SPRITE = vec(0, 8)
val DIRT_SPRITE = vec(8, 8)
val STONE_SPRITE = vec(8, 16)
val BREAK_SPRITES = arrayOf(vec(0, 68), vec(8, 68), vec(16, 68), vec(24, 68), vec(32, 68))

const val MATERIAL_AIR = 0.toByte()
const val MATERIAL_GRASS = 1.toByte()
const val MATERIAL_DIRT = 2.toByte()
const val MATERIAL_STONE = 3.toByte()

fun sprite(material: Byte) = when (material) {
    MATERIAL_GRASS -> GRASS_TOP_SPRITE
    MATERIAL_DIRT -> DIRT_SPRITE
    MATERIAL_STONE -> STONE_SPRITE
    else -> DIRT_SPRITE
}

val TILE_SCALE = 1.0f
val TILE_SCALE_VEC = vec(TILE_SCALE)
val TILE_UV_SCALE = 8
val RENDER_SCALE = 43
val RENDER_RADIUS = RENDER_SCALE / 2
val WORLD_SCALE = 128
val WORLD_RADIUS = WORLD_SCALE / 2

interface World : MeshAdapter {
    val colliders: List<Collider>
    fun highestAt(x: Float): Float
}

fun Window.world(shader: Shader): World {
    val createBatch = { mesh(MAX_VERTICES / shader.quadLength, shader) }
    fun perlin(x: Float) = (sin(2 * x) + sin(PI * x)).toFloat()
    val perlin = Array(WORLD_SCALE) {
        perlin(it / 150f) * 6f + perlin(it / 50f) * 3f + perlin(it / 300f) * 6f + perlin(it / 20f) * 2f
    }
    val colliders = Array(WORLD_SCALE * WORLD_SCALE) {
        val x = it % WORLD_SCALE
        val y = it / WORLD_SCALE
        collider(vec(x - WORLD_RADIUS, y - WORLD_RADIUS).float(), TILE_SCALE_VEC)
    }
    val tiles = ByteArray(WORLD_SCALE * WORLD_SCALE) {
        val x = it % WORLD_SCALE
        val y = (it / WORLD_SCALE) - WORLD_RADIUS
        if (y - perlin[x] > 0) MATERIAL_AIR
        else if ((y - perlin[x]).toInt() == 0) MATERIAL_GRASS
        else if (y - perlin[x] > -4) MATERIAL_DIRT
        else MATERIAL_STONE
    }
    val delegate = adapter(RENDER_SCALE * RENDER_SCALE, shader, createBatch) { mesh, index ->
        val cameraTile = camera.position.int() + WORLD_RADIUS
        (0 until RENDER_SCALE * RENDER_SCALE).forEach {
            val tileX = it % RENDER_SCALE - RENDER_RADIUS + cameraTile.x
            val tileY = it / RENDER_SCALE - RENDER_RADIUS + cameraTile.y
            if (tileX < 0 || tileY < 0 || tileX >= WORLD_SCALE || tileY >= WORLD_SCALE) return@forEach
            val tileIndex = tileX + tileY * WORLD_SCALE
            if (tiles[tileIndex] == MATERIAL_AIR) return@forEach
            mesh.sprite(index + it, sprite(tiles[tileIndex]), vec(TILE_UV_SCALE))
            mesh.bounds(index + it, vec(tileX - WORLD_RADIUS, tileY - WORLD_RADIUS).float(), TILE_SCALE_VEC)
        }
    }
    return object : World, MeshAdapter by delegate {
        override val colliders: List<Collider> get() {
            val cameraTile = camera.position.int() + WORLD_RADIUS
            return (0 until RENDER_SCALE * RENDER_SCALE).mapNotNull {
                val tileX = it % RENDER_SCALE - RENDER_RADIUS + cameraTile.x
                val tileY = it / RENDER_SCALE - RENDER_RADIUS + cameraTile.y
                if (tileX < 0 || tileY < 0 || tileX >= WORLD_SCALE || tileY >= WORLD_SCALE) return@mapNotNull null
                val tileIndex = tileX + tileY * WORLD_SCALE
                if (tiles[tileIndex] == MATERIAL_AIR) return@mapNotNull null
                colliders[tileIndex]
            }
        }
        override fun highestAt(x: Float): Float {
//            val cameraTile = (camera.position).int() + WORLD_RADIUS
            val tileX = x.toInt() + WORLD_RADIUS
            return ceil(perlin[tileX]) + TILE_SCALE / 2
        }
    }
}



////Greedy meshing
//
//interface Group {
//    val type: Byte
//    val start: Int
//    var width: Int
//}
//
//fun group(type: Byte, start: Int) = object : Group {
//    override val type = type
//    override val start = start
//    override var width = 1
//}
//
//fun Chunk.groups(types: Array<Byte>) {
//    var id = 0
//    (0 until CHUNK_SIZE * CHUNK_SIZE).forEach { current ->
//        val previous = current - 1
//        val y = current / CHUNK_SIZE
//        val previousY = previous / CHUNK_SIZE
//        val lastType = if (current in 0 until (CHUNK_SIZE * CHUNK_SIZE)) types[current] else null
//        val previousType = if (previous in 0 until (CHUNK_SIZE * CHUNK_SIZE)) types[previous] else null
//        if (previousType != null && ids[previous] != null && lastType == previousType && y == previousY) {
//            val groupId = ids[previous]!!
//            val group = groups[groupId]!!
//            ids[current] = groupId
//            group.width++
//            return@forEach
//        }
//        id++.let {
//            groups[it] = group(types[current], current)
//            ids[current] = it
//        }
//    }
//}