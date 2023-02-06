package me.mason.game

import java.lang.System.arraycopy
import java.util.*

private val MESH_VERTEX_CORNERS = arrayOf(
     1, -1,
    -1,  1,
     1,  1,
    -1, -1
)
private val MESH_UV_CORNERS = arrayOf(
    1, 1,
    0, 0,
    1, 0,
    0, 1
)

typealias ShaderData = FloatArray

interface Mesh {
    val data: ShaderData
    val quads: BitSet
    val limit: Int
    fun vertices(quad: Int, position: FloatVector, scale: FloatVector, ui: Boolean = false)
    fun uv(quad: Int, position: IntVector, scale: Int)
    fun clear(quad: Int)
    fun clear(quad: IntRange)
    operator fun set(quad: Int, mesh: Mesh)
}

fun Window.tiledMesh(limit: Int, shader: Shader, init: Mesh.() -> (Unit) = {}) =
    object : Mesh {
        private val quadLength = shader.attributesLength * 4
        override val data = ShaderData(limit * quadLength)
        override val limit = limit
        override var quads = BitSet(limit)
        override fun vertices(quad: Int, position: FloatVector, scale: FloatVector, ui: Boolean) {
            val add =
                if (ui) camera.position
                else vec(0f)
            (0 until 4).forEach {
                val index = quad * quadLength + it * shader.attributesLength
                data[index] = position.x + add.x + (scale.x / 2f) * MESH_VERTEX_CORNERS[it * 2]
                data[index + 1] = position.y + add.y + (scale.y / 2f) * MESH_VERTEX_CORNERS[it * 2 + 1]
                data[index + 2] = position.x + add.x - (scale.x / 2f)
                data[index + 3] = position.y + add.y + (scale.y / 2f)
//                data[index + 4] = tileSize
            }
            quads.set(quad)
        }
        override fun uv(quad: Int, position: IntVector, scale: Int) {
            (0 until 4).forEach {
                val index = quad * quadLength + it * shader.attributesLength
                data[index + 4] = (position.x + (position.y + scale) * SHEET_SIZE).toFloat()
//                data[index + 6] = scale / SHEET_SIZE.toFloat()
            }; quads.set(quad)
        }
        override fun set(quad: Int, mesh: Mesh) {
            arraycopy(
                mesh.data, 0,
                data, quad * quadLength,
                quadLength * mesh.quads.previousClearBit(mesh.limit)
            )
            val at = quads.previousClearBit(limit)
            var next = mesh.quads.nextSetBit(0)
            while(next != -1) {
                quads.set(at + next, mesh.quads[next])
                next = mesh.quads.nextSetBit(next)
            }
        }
        override fun clear(quad: Int) {
            data.fill(0f, quad * quadLength, quad * quadLength + quadLength)
            quads.clear(quad)
        }
        override fun clear(quad: IntRange) {
            val min = quad.first
            val max = quad.last
            data.fill(0f, quad.first * quadLength, quad.last * quadLength + quadLength)
            quads.clear(min, max)
        }
        init {
            init(this)
        }
    }