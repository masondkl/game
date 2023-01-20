package me.mason.game.components

import me.mason.game.*
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.*
import java.nio.DoubleBuffer
import java.util.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin


const val AIR = 0
const val GRASS = 1
const val DIRT = 2
const val STONE = 3

typealias Material = Int
interface Block : Entity {
    var type: Material
    var broken: Double
}

interface World {
    val chunks: Array<Chunk>
    fun highest(x: Float): Float
    fun nearbyChunks(): List<Chunk>
    fun nearbyBlocks(): Array<Block?>
    fun tick()
}

interface Chunk {
    val blocks: Array<Block>
    val chunkPosition: IntVector
    val worldPosition: FloatVector
}


val GRASS_TOP_SPRITE = uv(vec(72, 90), vec(8, 8))
val DIRT_TOP_SPRITE = uv(vec(72, 98), vec(8, 8))
val DIRT_SPRITE = uv(vec(80, 98), vec(8, 8))
val STONE_SPRITE = uv(vec(80, 106), vec(8, 8))
val BREAK_0_SPRITE = uv(vec(0, 68), vec(8, 8))
val BREAK_1_SPRITE = uv(vec(8, 68), vec(8, 8))
val BREAK_2_SPRITE = uv(vec(16, 68), vec(8, 8))
val BREAK_3_SPRITE = uv(vec(24, 68), vec(8, 8))
val BREAK_4_SPRITE = uv(vec(32, 68), vec(8, 8))
val BREAK_SPRITES = arrayOf(BREAK_0_SPRITE, BREAK_1_SPRITE, BREAK_2_SPRITE, BREAK_3_SPRITE, BREAK_4_SPRITE)

val GRASS_ANIMATION = animation(
    state(0, GRASS_TOP_SPRITE, vec(32f, 32f), vec(0f, 0f))
)
val DIRT_ANIMATION = animation(
    state(0, DIRT_SPRITE, vec(32f, 32f), vec(0f, 0f)),
    state(1, DIRT_TOP_SPRITE, vec(32f, 32f), vec(0f, 0f))
)
val STONE_ANIMATION = animation(
    state(0, STONE_SPRITE, vec(32f, 32f), vec(0f, 0f))
)
val BREAK_ANIMATION = animation(
    state(0 until 5, BREAK_SPRITES, vec(32f, 32f))
)

fun Window.world(mouse: BitSet, diameter: Int, chunkBlockCount: Int = 32, seed: Int = (0..10000).random()): World {
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
            val position = chunkWorldPosition + vec(blockPosition.x, blockPosition.y).float() * 32f
            val y = blockPosition.y + chunkPosition.y * chunkBlockCount
            val highest = heightmap[blockPosition.x + chunkPosition.x * chunkBlockCount]
            var _type =
                if (y > highest) AIR
                else if (y == highest) GRASS
                else if (abs(y - highest) < 5) DIRT
                else STONE
            fun animationOf(type: Int) = when (type) {
                GRASS -> GRASS_ANIMATION
                DIRT -> DIRT_ANIMATION
                STONE -> STONE_ANIMATION
                else -> null
            }?.clone()

            val blockEntity = entity(position, vec(32f, 32f), animationOf(_type), null)
            object : Block, Entity by blockEntity {
                override var type: Int
                    get() = _type
                    set(value) {
                        _type = value
                        if (value != AIR) animations[0] = animationOf(value)
                    }
                override var broken = 0.0
                    set(value) {
                        if (animations[0] == null) return
                        if (animations[1] == null) animations[1] = BREAK_ANIMATION.clone()
                        val breakIndex = floor(value * (BREAK_SPRITES.size)).toInt()
                        if (breakIndex >= BREAK_SPRITES.size) {
                            animations[0] = null; animations[1] = null
                            return
                        }
                        val animation = animations[1]!!
                        if (animation.state() != breakIndex) animation.sprite(breakIndex)
                        field = value
                    }
            }
        }
        object : Chunk {
            override val blocks = blocks
            override val chunkPosition = chunkPosition
            override val worldPosition = chunkWorldPosition
        }
    }
    return object : World {
        override val chunks = chunks
        private val blocks = Array<Block?>(8 * chunkBlockCount * chunkBlockCount) { null }
        override fun highest(x: Float): Float {
            if (x / 32f < -radius || x / 32f > radius)
                return 0f
            val blockX = (x / 32f + radius).toInt()
            return (heightmap[blockX] - radius) * 32f + 32f
        }
        override fun nearbyChunks() = chunks.filter {
            abs(it.worldPosition.x - camera.position.x) < 640f + chunkBlockCount * 32f &&
                    abs(it.worldPosition.y - camera.position.y) < 360f + chunkBlockCount * 32f
        }
        override fun nearbyBlocks(): Array<Block?> {
            val nearby = nearbyChunks()
            var blockIndex = 0
            blocks.fill(null, 0, blocks.size)
            nearby.forEach { chunk ->
                chunk.blocks.forEach { block ->
                    if (block.animations.any { it != null }) blocks[blockIndex++] = block
                }
            }
            return blocks
        }
        override fun tick() {
            val width = BufferUtils.createIntBuffer(1)
            val height = BufferUtils.createIntBuffer(1)
            glfwGetWindowSize(id, width, height)
            val x = BufferUtils.createDoubleBuffer(1)
            val y = BufferUtils.createDoubleBuffer(1)
            glfwGetCursorPos(id, x, y)
            val cursorPosition = vec(x[0].toFloat(), (height[0] - y[0]).toFloat()) + vec(-width[0]/2f, -height[0]/2f) + camera.position
            if (mouse[GLFW_MOUSE_BUTTON_LEFT]) {
                val block = nearbyBlocks().filterNotNull().minByOrNull {
                    cursorPosition.distance(it.position)
                } ?: return
                if (
                    camera.position.distance(block.position) < 128f &&
                    abs(cursorPosition.x - block.position.x) < block.scale.x / 2f &&
                    abs(cursorPosition.y - block.position.y) < block.scale.y / 2f
                ) {
                    block.broken += dt
                }
            }
        }
    }
}