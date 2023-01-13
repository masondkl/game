package me.mason.game

import java.lang.System.arraycopy

const val QUAD_UV_PAIR = 16

interface Mesh {
    var quads: Int
    val data: FloatArray
}

operator fun Mesh.set(index: Int, mesh: Mesh) {
    val size = QUAD_UV_PAIR * mesh.quads
    arraycopy(
        mesh.data, 0,
        data, index * QUAD_UV_PAIR,
        size
    )
}

operator fun Mesh.plusAssign(b: Mesh) {
    arraycopy(b.data, 0, data, quads * QUAD_UV_PAIR, b.quads * QUAD_UV_PAIR)
    quads += b.quads
}

fun mesh(bounds: Bounds, uv: UV) = object : Mesh {
    override var quads = 1
    override val data = FloatArray(QUAD_UV_PAIR) {
        val x = it % 4;
        val y = it / 4
        if (x < 2) bounds[it % 2 + y * 2]
        else uv[it % 2 + y * 2]
    }
}

fun mesh(quads: Int) = object : Mesh {
    override var quads = 0
    override val data = FloatArray(quads * QUAD_UV_PAIR)
}

