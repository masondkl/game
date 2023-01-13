package me.mason.game.components

import me.mason.game.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

interface Block : Entity {
    var broken: Double
}

interface World : Entity {
    val chunks: Array<Chunk>
    fun highest(x: Float): Float
    fun nearby(): Set<Block>
}

interface Chunk : Entity {
    val blocks: Array<Block>
    val chunkPosition: IntVector
    val worldPosition: FloatVector
}

val GRASS_TOP = uv(vec(72, 90), vec(8, 8))
val GRASS_MIDDLE = uv(vec(80, 90), vec(8, 8))
val GRASS_BOTTOM = uv(vec(88, 90), vec(8, 8))
val BREAK_0 = uv(vec(0, 68), vec(8, 8))
val BREAK_1 = uv(vec(8, 68), vec(8, 8))
val BREAK_2 = uv(vec(16, 68), vec(8, 8))
val BREAK_3 = uv(vec(24, 68), vec(8, 8))
val BREAK_4 = uv(vec(32, 68), vec(8, 8))
val BREAK = arrayOf(BREAK_0, BREAK_1, BREAK_2, BREAK_3, BREAK_4)

fun Window.world(diameter: Int, chunkBlockCount: Int = 32, seed: Int = (0..10000).random()): World {
    val chunkCount = diameter / chunkBlockCount
    val chunkDiameter = diameter / chunkCount
    val radius = diameter / 2
    val heightmap = Array(diameter) {
        radius + ((sin(2.0 * (it + seed) / 48.0) + sin(PI * (it + seed) / 48.0)).toFloat() * 5f).toInt()
    }
    val chunks = Array<Chunk>(chunkCount * chunkCount) { chunkIndex ->
        val chunkPosition = vec(chunkIndex % chunkCount, chunkIndex / chunkCount)
        val chunkWorldPosition = (chunkPosition * chunkDiameter - radius).float() * 32f + 16f
        val blocks = Array<Block>(chunkDiameter * chunkDiameter) { blockIndex ->
            val blockPosition = vec(blockIndex % chunkDiameter, blockIndex / chunkDiameter)
            val sprite =
                if (blockPosition.y + chunkPosition.y * chunkBlockCount > heightmap[blockPosition.x + chunkPosition.x * chunkBlockCount])
                    null
                else if (blockPosition.y + chunkPosition.y * chunkBlockCount == heightmap[blockPosition.x + chunkPosition.x * chunkBlockCount]) GRASS_TOP
                else GRASS_MIDDLE
            val position = chunkWorldPosition + vec(blockPosition.x, blockPosition.y).float() * 32f
            object : Block, Entity by entity(sprite = null, vec(32f, 32f), position, quads = 2) {
                override var broken = 0.0
                    set(value) {
                        field = value
                        val breakIndex = (field * (BREAK.size - 1)).toInt()
                        if (breakIndex < BREAK.size && mesh.quads == 1)
                            mesh += mesh(bounds(position, scale), BREAK[breakIndex])
                    }

                init {
                    sprite?.let { mesh += mesh(bounds(position, scale), it) }
                }
            }
        }
        val chunkEntity = entity(quads = blocks.size * 2) {
            var meshOffset = 0
            blocks.forEach { block ->
                mesh[meshOffset] = block.mesh
                meshOffset += block.mesh.quads
            }; mesh.quads = meshOffset
        }
        blocks
            .filter { it.mesh.quads > 0 }
            .forEach { block ->
                chunkEntity.mesh += block.mesh
            }
        object : Chunk, Entity by chunkEntity {
            override val blocks = blocks
            override val chunkPosition = chunkPosition
            override val worldPosition = chunkWorldPosition
        }
    }
    fun nearby() = chunks.filter {
        abs(it.worldPosition.x - camera.position.x) < 640f + chunkBlockCount * 32f &&
                abs(it.worldPosition.y - camera.position.y) < 360f + chunkBlockCount * 32f
    }
    val worldEntity = entity(quads = 8 * chunkBlockCount * chunkBlockCount) {
        var meshOffset = 0
        nearby().forEach {
            mesh[meshOffset] = it.mesh
            meshOffset += it.mesh.quads
        }; mesh.quads = meshOffset
    }
    nearby().forEach { worldEntity.mesh += it.mesh }
    return object : World, Entity by worldEntity {
        override val chunks = chunks
        override fun highest(x: Float): Float {
            if (x / 32f < -radius || x / 32f > radius)
                return 0f
            val blockX = (x / 32f + radius).toInt()
            return (heightmap[blockX] - radius) * 32f + 32f
        }
        override fun nearby() = nearby().fold(HashSet<Block>()) { acc, it ->
            acc += it.blocks; acc
        }
    }
}