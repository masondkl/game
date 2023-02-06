package me.mason.game.component

import me.mason.game.*


interface MeshAdapter {
    val limit: Int
    val shader: Shader
    val adapter: Window.(Mesh, Int) -> (Unit)
    val createMesh: () -> (Mesh)
}
context(Window)
operator fun MeshAdapter.invoke(mesh: Mesh, index: Int): Unit =
    adapter(this@Window, mesh, index)

fun adapter(shader: Shader, limit: Int, createBatch: () -> (Mesh), adapter: Window.(Mesh, Int) -> (Unit)) =
    object : MeshAdapter {
        override val limit = limit
        override val shader = shader
        override val adapter = adapter
        override val createMesh = createBatch
    }

//fun Window.tiledEntity(shader: Shader, createBatch: () -> (Mesh), block: context(Window, Entity) () -> (Unit)): MeshAdapter {
//    return adapter(shader, createBatch) { mesh, index ->
//        var quads = 0
//        block(this@Window, object : Entity {
//            override fun vertices(quad: Int, position: FloatVector, scale: FloatVector, ui: Boolean) {
//                mesh.vertices(quad + index, position, scale, ui)
//                quads = max(quad + 1, quads)
//            }
//            override fun uv(quad: Int, position: IntVector, scale: Int) {
//                mesh.uv(quad + index, position, scale)
//                quads = max(quad + 1, quads)
//            }
//        })
//        quads
//    }
//}