package me.mason.game.component

import me.mason.game.*
import java.lang.Math.random
import java.util.*
import kotlin.math.*

val GRASS_TOP_SPRITE = vec(0, 8)
val DIRT_SPRITE = vec(8, 8)
val STONE_SPRITE = vec(8, 16)
val BREAK_SPRITES = arrayOf(vec(0, 68), vec(8, 68), vec(16, 68), vec(24, 68), vec(32, 68))

const val MATERIAL_AIR = 0.toByte()
const val MATERIAL_GRASS = 1.toByte()
const val MATERIAL_DIRT = 2.toByte()
const val MATERIAL_STONE = 3.toByte()

val TILE_SCALE = 16f
val TILE_SCALE_VEC = vec(TILE_SCALE)
val TILE_UV_SCALE = 8
val TILE_UV_SCALE_VEC = vec(TILE_UV_SCALE)
val RENDER_SCALE = 86
val RENDER_RADIUS = RENDER_SCALE / 2
val WORLD_SCALE = 512
val WORLD_RADIUS = WORLD_SCALE / 2

fun materialName(material: Byte) = when (material) {
    MATERIAL_AIR -> "AIR"
    MATERIAL_GRASS -> "GRASS"
    MATERIAL_DIRT -> "DIRT"
    else -> "STONE"
}

fun materialSprite(material: Byte) = when (material) {
    MATERIAL_GRASS -> GRASS_TOP_SPRITE
    MATERIAL_DIRT -> DIRT_SPRITE
    MATERIAL_STONE -> STONE_SPRITE
    else -> DIRT_SPRITE
}

fun materialScale(material: Byte) = when (material) {
    MATERIAL_GRASS -> TILE_UV_SCALE_VEC
    MATERIAL_DIRT -> TILE_UV_SCALE_VEC
    MATERIAL_STONE -> TILE_UV_SCALE_VEC
    else -> TILE_UV_SCALE_VEC
}

interface World : Mesh {
    val tick: Tick
    operator fun set(position: FloatVector, material: Byte)
    operator fun get(position: FloatVector): Byte
    fun tileColliders(position: FloatVector, distance: Float): List<Collider>
    fun collider(position: FloatVector): Collider
    fun highestAt(x: Float): Float
    fun breakAt(position: FloatVector, strength: Float)
}

fun cellularAutomata(density: Double, iterations: Int): BitSet {
    val limit = WORLD_SCALE * WORLD_SCALE
    val grids = Array(2) { BitSet(limit) }
    var current = 0
    (0 until limit).forEach { if (random() > density) grids[current].set(it) }
    fun pass(a: BitSet, b: BitSet) {
        b.clear()
        (0 until limit).forEach { i ->
            val x = i % WORLD_SCALE
            val y = i / WORLD_SCALE
            var nearby = 0
            for (n in 0 until 9) {
                if (n == 4) continue
                val nx = x + n % 3
                val ny = y + n / 3
                val ni = nx + ny * WORLD_SCALE
                if (ni < 0 || ni > limit || a[ni]) nearby++
            }; if (nearby > 4) b.set(i)
        }
    }
    (0 until iterations).forEach { _ ->
        val next = (current + 1) % 2
        pass(grids[current], grids[next])
        current = next
    }
    return grids[current]
}

fun Window.world(shader: Shader, drops: MutableList<Drop>): World {
    val mesh = mesh(shader, RENDER_SCALE * RENDER_SCALE * 2)
    val octaves = 8
    val seed = seed1(octaves)
    val height = Array(WORLD_SCALE) { perlin1(it.toFloat(), 50f, 2f, octaves, seed) }
    val caves = cellularAutomata(0.415, 10)

    val colliders = Array(WORLD_SCALE * WORLD_SCALE) {
        val x = it % WORLD_SCALE
        val y = it / WORLD_SCALE
        collider(vec((x - WORLD_RADIUS) * TILE_SCALE, (y - WORLD_RADIUS) * TILE_SCALE), TILE_SCALE_VEC)
    }
    val materials = ByteArray(WORLD_SCALE * WORLD_SCALE) {
        val x = it % WORLD_SCALE
        val y = it / WORLD_SCALE - WORLD_RADIUS
        if (y - height[x] < -10 && caves[it]) MATERIAL_AIR
        else if (y - height[x] > 0) MATERIAL_AIR
        else if ((y - height[x]).toInt() == 0) MATERIAL_GRASS
        else if (y - height[x] > -4) MATERIAL_DIRT
        else MATERIAL_STONE
    }
    val broken = FloatArray(WORLD_SCALE * WORLD_SCALE)
    //cave grass pass
    (0 until WORLD_SCALE * WORLD_SCALE).forEach {
        val x = it % WORLD_SCALE
        val y = it / WORLD_SCALE
        val above = x + (y + 1) * WORLD_SCALE
        if (above >= WORLD_SCALE * WORLD_SCALE) return@forEach
        if (materials[above] == MATERIAL_AIR && materials[it] == MATERIAL_STONE)
            materials[it] = MATERIAL_GRASS
    }
    return object : World, Mesh by mesh {
        val self = this
        override val tick: Tick = {
            val cameraTile = (camera.position / TILE_SCALE).int() + WORLD_RADIUS
            mesh.clear(0 until RENDER_SCALE * RENDER_SCALE * 2)
            (0 until RENDER_SCALE * RENDER_SCALE).forEach {
                val tileX = it % RENDER_SCALE - RENDER_RADIUS + cameraTile.x
                val tileY = it / RENDER_SCALE - RENDER_RADIUS + cameraTile.y
                if (tileX < 0 || tileY < 0 || tileX >= WORLD_SCALE || tileY >= WORLD_SCALE) return@forEach
                val tileIndex = tileX + tileY * WORLD_SCALE
                if (materials[tileIndex] == MATERIAL_AIR) return@forEach
                val position = vec((tileX - WORLD_RADIUS) * TILE_SCALE, (tileY - WORLD_RADIUS) * TILE_SCALE)
                mesh.sprite(it * 2, materialSprite(materials[tileIndex]), vec(TILE_UV_SCALE))
                mesh.bounds(it * 2, position, TILE_SCALE_VEC)
                if (broken[tileIndex] >= 1.0f) {
                    drops.drop(shader, self, position + vec(0f, 8f), materials[tileIndex])
                    materials[tileIndex] = MATERIAL_AIR
                    broken[tileIndex] = 0f
                    mesh.clear(it * 2)
                    mesh.clear(it * 2 + 1)
                } else if (broken[tileIndex] > 0f) {
                    mesh.sprite(it * 2 + 1, BREAK_SPRITES[floor(broken[tileIndex] * BREAK_SPRITES.size).toInt()], vec(TILE_UV_SCALE))
                    mesh.bounds(
                        it * 2 + 1,
                        vec((tileX - WORLD_RADIUS) * TILE_SCALE, (tileY - WORLD_RADIUS) * TILE_SCALE),
                        TILE_SCALE_VEC
                    )
                }
            }
        }
        override fun get(position: FloatVector): Byte {
            val tilePosition = ((position + WORLD_RADIUS * TILE_SCALE + TILE_SCALE_VEC / 2f) / TILE_SCALE).int()
            val index = tilePosition.x + tilePosition.y * WORLD_SCALE
            return materials[index]
        }
        override fun set(position: FloatVector, material: Byte) {
            val tilePosition = ((position + WORLD_RADIUS * TILE_SCALE + TILE_SCALE_VEC / 2f) / TILE_SCALE).int()
            val index = tilePosition.x + tilePosition.y * WORLD_SCALE
            materials[index] = material
            broken[index] = 0f
        }
        override fun tileColliders(position: FloatVector, distance: Float): List<Collider> {
            val tileDistance = (distance / TILE_SCALE).toInt()
            val tileRadius = tileDistance / 2
            val tile = (position / TILE_SCALE).int() + WORLD_RADIUS
            return (0 until tileDistance * tileDistance).mapNotNull {
                val tileX = it % tileDistance - tileRadius + tile.x
                val tileY = it / tileDistance - tileRadius + tile.y
                if (tileX < 0 || tileY < 0 || tileX >= WORLD_SCALE || tileY >= WORLD_SCALE) return@mapNotNull null
                val tileIndex = tileX + tileY * WORLD_SCALE
                if (materials[tileIndex] == MATERIAL_AIR) return@mapNotNull null
                colliders[tileIndex]
            }
        }
        override fun collider(position: FloatVector): Collider {
            val tilePosition = ((position + WORLD_RADIUS * TILE_SCALE + TILE_SCALE_VEC / 2f) / TILE_SCALE).int()
            val index = tilePosition.x + tilePosition.y * WORLD_SCALE
            return colliders[index]
        }
        override fun highestAt(x: Float) = height[
            (x / TILE_SCALE + WORLD_RADIUS).toInt() % WORLD_SCALE
        ] * TILE_SCALE + TILE_SCALE * 4
        override fun breakAt(position: FloatVector, strength: Float) {
            val tilePosition = ((position + WORLD_RADIUS * TILE_SCALE + TILE_SCALE_VEC / 2f) / TILE_SCALE).int()
            val index = tilePosition.x + tilePosition.y * WORLD_SCALE
            if (materials[index] == MATERIAL_AIR) return
            broken[index] += strength
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