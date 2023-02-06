package me.mason.game.component

import me.mason.game.*
import java.nio.file.Paths

const val BLOCK_UV_SCALE = 8
val BLOCK_UV_SCALE_VEC = vec(BLOCK_UV_SCALE)
const val BLOCK_SCALE = 32f
val BLOCK_SCALE_VEC = vec(BLOCK_SCALE)

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

interface Chunk {
    val groups: Array<Group?>
    val ids: Array<Int?>
    val scale: Int
    val blocks: Int
}

fun chunk(types: Array<Byte>, scale: Int) = object : Chunk {
    override val scale = scale
    override val blocks = scale * scale
    override val groups = Array<Group?>(blocks) { null }
    override val ids = Array<Int?>(blocks) { null }
    init { groups(scale, types, groups, ids) }
}

fun Chunk.draw(mesh: Mesh, index: Int) = groups.forEachIndexed { groupIndex, it ->
    if (it == null || it.type == MATERIAL_AIR) return@forEachIndexed
    val startX = it.start % scale
    val x = (startX + (it.width / 2f) - (scale / 2)) * BLOCK_SCALE
    val y = (it.start / scale - (scale / 2)) * BLOCK_SCALE
    mesh.uv(index + groupIndex, sprite(it.type), BLOCK_UV_SCALE)
    mesh.vertices(index + groupIndex, vec(x, y), vec(it.width * BLOCK_SCALE, BLOCK_SCALE))
}

const val CHUNK_SIZE = 16

fun Window.world(): MeshAdapter {
    val shader = shader(Paths.get("tiled.glsl"), 2, 2, 1)
    val createBatch = { tiledMesh(MAX_VERTICES / shader.quadLength, shader) }
    val scale = 16
    val radius = scale / 2
    val blocks = scale * scale
    val types = Array(scale * scale) {
        val y = (it / scale) - radius
        if (y > 0) MATERIAL_AIR
        else if (y == 0) MATERIAL_GRASS
        else if (y > -4) MATERIAL_DIRT
        else MATERIAL_STONE
    }
    val chunk = chunk(types, CHUNK_SIZE)
    return adapter(shader, scale * scale, createBatch) { mesh, index ->
        chunk.draw(mesh, index)
    }
}

//Greedy meshing

interface Group {
    val type: Byte
    val start: Int
    var width: Int
}

fun group(type: Byte, start: Int) = object : Group {
    override val type = type
    override val start = start
    override var width = 1
}

fun groups(scale: Int, types: Array<Byte>, groups: Array<Group?>, ids: Array<Int?>) {
    val blocks = scale * scale
    var id = 0
    (0 until blocks).forEach { current ->
        val previous = current - 1
        val y = current / scale
        val previousY = previous / scale
        val lastType = if (current in 0 until blocks) types[current] else null
        val previousType = if (previous in 0 until blocks) types[previous] else null
        if (previousType != null && ids[previous] != null && lastType == previousType && y == previousY) {
            val groupId = ids[previous]!!
            val group = groups[groupId]!!
            ids[current] = groupId
            group.width++
            return@forEach
        }
        id++.let {
            groups[it] = group(types[current], current)
            ids[current] = it
        }
    }
}