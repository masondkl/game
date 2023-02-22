package me.mason.game.component

import me.mason.game.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

val GRASS_TOP_SPRITE = vec(0, 8)
val DIRT_SPRITE = vec(8, 8)
val STONE_SPRITE = vec(8, 16)
val BREAK_SPRITES = arrayOf(vec(0, 68), vec(8, 68), vec(16, 68), vec(24, 68), vec(32, 68))

const val MATERIAL_AIR = 0.toByte()
const val MATERIAL_GRASS = 1.toByte()
const val MATERIAL_DIRT = 2.toByte()
const val MATERIAL_STONE = 3.toByte()

fun materialName(material: Byte) = when (material) {
    MATERIAL_AIR -> "AIR"
    MATERIAL_GRASS -> "GRASS"
    MATERIAL_DIRT -> "DIRT"
    else -> "STONE"
}

fun sprite(material: Byte) = when (material) {
    MATERIAL_GRASS -> GRASS_TOP_SPRITE
    MATERIAL_DIRT -> DIRT_SPRITE
    MATERIAL_STONE -> STONE_SPRITE
    else -> DIRT_SPRITE
}

val TILE_SCALE = 16f
val TILE_SCALE_VEC = vec(TILE_SCALE)
val TILE_UV_SCALE = 8
val RENDER_SCALE = 86
val RENDER_RADIUS = RENDER_SCALE / 2
val WORLD_SCALE = 1024
val WORLD_RADIUS = WORLD_SCALE / 2

interface World : Mesh {
    val tick: Tick
    val colliders: List<Collider>
    operator fun set(position: FloatVector, material: Byte)
    operator fun get(position: FloatVector): Byte
    fun highestAt(x: Float): Float
    fun breakAt(position: FloatVector, strength: Float)
}

fun cave(maxStretch: Float, maxAmplitude: Float): (Int) -> (Boolean) {
    val stretch = (Math.random().toFloat() * (maxStretch - 15f)) + 15f
    val amplitude = (Math.random().toFloat() * (maxAmplitude - 1f)) + 1f
    val seed = seed2()
    val perlin = Array(WORLD_SCALE * WORLD_SCALE) {
        val x = it % WORLD_SCALE
        val y = it / WORLD_SCALE
        perlin2(vec(x, y).float(), stretch, amplitude, seed, 8)
    }

    return {
        println("${abs(perlin[it].x)}, ${abs(perlin[it].y)}")
        println(maxAmplitude)
        abs(perlin[it].x) > maxAmplitude / 1.25 || abs(perlin[it].y) > maxAmplitude / 1.25
    }
}
//fun cave(maxStretch: Float, maxAmplitude: Float, height: IntRange): (Int) -> (Boolean) {
//    val stretch = Math.random().toFloat() * maxStretch + 15f
//    val amplitude = Math.random().toFloat() * maxAmplitude + 1f
//    val seed = seed1()
//    val perlinA = Array(WORLD_SCALE) { perlin1(it.toFloat(), stretch, amplitude, seed, 8) }
//    val perlinB = Array(WORLD_SCALE) { perlin1(it.toFloat(), stretch, amplitude, seed, 8) }
//    val chosen = height.random()
//    val topStretch = Math.random().toFloat() * maxStretch + 15f
//    val topAmplitude = Math.random().toFloat() * maxAmplitude + 1f
//    val topSeed = seed1()
//    val bottomSeed = seed1()
//    return {
//        val x = it % WORLD_SCALE
//        val y = it / WORLD_SCALE - WORLD_RADIUS
//        val min = minOf(y - perlinB[x], y - perlinA[x]) - (3 + perlin1(x.toFloat(), topStretch, topAmplitude, topSeed, 3))
//        val max = maxOf(y - perlinB[x], y - perlinA[x]) + (3 + perlin1(x.toFloat(), topStretch, topAmplitude, bottomSeed, 3))
//        max >= chosen && min <= chosen
////        ((y - perlinA[x] >= chosen && y - perlinB[x] <= chosen) || (y - perlinA[x] <= chosen && y - perlinB[x] >= chosen))
//    }
//}

fun Window.world(shader: Shader): World {
    val mesh = mesh(RENDER_SCALE * RENDER_SCALE * 2, shader)
    fun perlin(x: Float) = (sin(2 * x) + sin(PI * x)).toFloat()
    val seed = Math.random().toFloat() * 100000f
    val perlin = Array(WORLD_SCALE) {
        val x = seed + it
        perlin(x / 150f) * 6f + perlin(x / 50f) * 3f + perlin(x / 300f) * 6f + perlin(x / 20f) * 2f
    }
    val caves = Array(1) {
        cave(Math.random().toFloat() * 100f, 15f)
    }
    val colliders = Array(WORLD_SCALE * WORLD_SCALE) {
        val x = it % WORLD_SCALE
        val y = it / WORLD_SCALE
        collider(vec((x - WORLD_RADIUS) * TILE_SCALE, (y - WORLD_RADIUS) * TILE_SCALE), TILE_SCALE_VEC)
    }
    val materials = ByteArray(WORLD_SCALE * WORLD_SCALE) {
        val x = it % WORLD_SCALE
        val y = it / WORLD_SCALE - WORLD_RADIUS
        if (caves.any { populate -> populate(it) }) MATERIAL_AIR
        else if (y - perlin[x] > 0) MATERIAL_AIR
        else if ((y - perlin[x]).toInt() == 0) MATERIAL_GRASS
        else if (y - perlin[x] > -4) MATERIAL_DIRT
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
    var previous = 0
    val tick: Tick = {
        val cameraTile = (camera.position / TILE_SCALE).int() + WORLD_RADIUS
        mesh.clear(0 until RENDER_SCALE * RENDER_SCALE * 2)
        (0 until RENDER_SCALE * RENDER_SCALE).forEach {
            val tileX = it % RENDER_SCALE - RENDER_RADIUS + cameraTile.x
            val tileY = it / RENDER_SCALE - RENDER_RADIUS + cameraTile.y
            if (tileX < 0 || tileY < 0 || tileX >= WORLD_SCALE || tileY >= WORLD_SCALE) return@forEach
            val tileIndex = tileX + tileY * WORLD_SCALE
            if (materials[tileIndex] == MATERIAL_AIR) return@forEach

            mesh.sprite(it * 2, sprite(materials[tileIndex]), vec(TILE_UV_SCALE))
            mesh.bounds(it * 2, vec((tileX - WORLD_RADIUS) * TILE_SCALE, (tileY - WORLD_RADIUS) * TILE_SCALE), TILE_SCALE_VEC)

            if (broken[tileIndex] >= 1.0f) {
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
    return object : World, Mesh by mesh {
        override val tick = tick
        override val colliders: List<Collider> get() {
            val cameraTile = (camera.position / TILE_SCALE).int() + WORLD_RADIUS
            return (0 until RENDER_SCALE * RENDER_SCALE).mapNotNull {
                val tileX = it % RENDER_SCALE - RENDER_RADIUS + cameraTile.x
                val tileY = it / RENDER_SCALE - RENDER_RADIUS + cameraTile.y
                if (tileX < 0 || tileY < 0 || tileX >= WORLD_SCALE || tileY >= WORLD_SCALE) return@mapNotNull null
                val tileIndex = tileX + tileY * WORLD_SCALE
                if (materials[tileIndex] == MATERIAL_AIR) return@mapNotNull null
                colliders[tileIndex]
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
        override fun highestAt(x: Float): Float {
//            val cameraTile = (camera.position).int() + WORLD_RADIUS
            val tileX = x.toInt() + WORLD_RADIUS
            return 50f
        }
        override fun breakAt(position: FloatVector, strength: Float) {
            val tilePosition = ((position + WORLD_RADIUS * TILE_SCALE + TILE_SCALE_VEC / 2f) / TILE_SCALE).int()
            val index = tilePosition.x + tilePosition.y * WORLD_SCALE
            broken[index] += strength
            println(broken[index])
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