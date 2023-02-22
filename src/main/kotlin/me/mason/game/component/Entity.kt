package me.mason.game.component

import me.mason.game.*

typealias Tick = Window.() -> (Unit)

//interface MeshAdapter {
//    val limit: Int
//    val shader: Shader
//    val adapter: Window.(Mesh, Int) -> (Unit)
//    val createMesh: () -> (Mesh)
//}
//context(Window)
//operator fun <T: Mesh> MeshAdapter.invoke(mesh: T, index: Int): Unit =
//    adapter(this@Window, mesh, index)
//fun adapter(limit: Int, shader: Shader, createBatch: () -> (Mesh), adapter: Window.(Mesh, Int) -> (Unit)) =
//    object : MeshAdapter {
//        override val limit = limit
//        override val shader = shader
//        override val adapter = adapter
//        override val createMesh = createBatch
//    }