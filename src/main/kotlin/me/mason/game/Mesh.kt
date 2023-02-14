package me.mason.game

import java.lang.System.arraycopy
import java.util.*

private val MESH_VERTEX_CORNERS = arrayOf(
    1f, 0f,
    0f, 1f,
    1f, 1f,
    0f, 0f
)
private val MESH_UV_CORNERS = arrayOf(
    1, 1,
    0, 0,
    1, 0,
    0, 1
)

typealias ShaderData = FloatArray

interface Mesh {
    val limit: Int
    val shader: Shader
    val data: ShaderData
    val quads: BitSet
    fun clear(quad: Int)
    fun clear(quad: IntRange)
}

fun mesh(limit: Int, shader: Shader, init: Mesh.() -> (Unit) = {}): Mesh =
    object : Mesh {
        override val limit = limit
        override val shader = shader
        override val data = ShaderData(limit * shader.quadLength)
        override var quads = BitSet(limit)
//        override fun set(quad: Int, mesh: Mesh) {
//            arraycopy(
//                mesh.data, 0,
//                data, quad * shader.quadLength,
//                shader.quadLength * mesh.quads.previousClearBit(mesh.limit)
//            )
//            val at = quads.previousClearBit(limit)
//            var next = mesh.quads.nextSetBit(0)
//            while(next != -1) {
//                quads.set(at + next, mesh.quads[next])
//                next = mesh.quads.nextSetBit(next)
//            }
//        }
        override fun clear(quad: Int) {
            data.fill(0f, quad * shader.quadLength, quad * shader.quadLength + shader.quadLength)
            quads.clear(quad)
        }
        override fun clear(quad: IntRange) {
            val min = quad.first
            val max = quad.last
            data.fill(0f, quad.first * shader.quadLength, quad.last * shader.quadLength + shader.quadLength)
            quads.clear(min, max)
        }
        init {
            init(this)
        }
    }

context(Window)
fun Mesh.tiledBounds(quad: Int, position: FloatVector, scale: FloatVector, ui: Boolean = false) {
    val add =
        if (ui) camera.position
        else vec(0f)
    (0 until 4).forEach {
        val index = quad * shader.quadLength + it * shader.attributesLength
        data[index] = position.x + add.x + (scale.x / 2f) * MESH_VERTEX_CORNERS[it * 2]
        data[index + 1] = position.y + add.y + (scale.y / 2f) * MESH_VERTEX_CORNERS[it * 2 + 1]
        data[index + 2] = position.x + add.x - (scale.x / 2f)
        data[index + 3] = position.y + add.y + (scale.y / 2f)
//                data[index + 4] = tileSize
    }; quads.set(quad)
}
fun Mesh.tiledSprite(quad: Int, position: IntVector, scale: Int) {
    (0 until 4).forEach {
        val index = quad * shader.quadLength + it * shader.attributesLength
        data[index + 4] = (position.x + (position.y + scale) * SHEET_SIZE).toFloat()
//                data[index + 6] = scale / SHEET_SIZE.toFloat()
    }; quads.set(quad)
}

context(Window)
fun Mesh.bounds(quad: Int, position: FloatVector, scale: FloatVector, ui: Boolean = false) {
    val add =
        if (ui) camera.position
        else vec(0f)
    (0 until 4).forEach {
        val index = quad * shader.quadLength + it * shader.attributesLength
        val topLeft = position + add - (scale / 2f)
        data[index] = topLeft.x + scale.x * MESH_VERTEX_CORNERS[it * 2]
        data[index + 1] = topLeft.y + scale.y * MESH_VERTEX_CORNERS[it * 2 + 1]
//                data[index + 4] = tileSize
    }; quads.set(quad)
}
fun Mesh.sprite(quad: Int, position: IntVector, scale: IntVector) {
    (0 until 4).forEach {
        val index = quad * shader.quadLength + it * shader.attributesLength
        val x = position.x + scale.x * MESH_UV_CORNERS[it * 2]
        val y = position.y + scale.y * MESH_UV_CORNERS[it * 2 + 1]
        data[index + 2] = (x + y * SHEET_SIZE).toFloat()
//                data[index + 6] = scale / SHEET_SIZE.toFloat()
    }; quads.set(quad)
}