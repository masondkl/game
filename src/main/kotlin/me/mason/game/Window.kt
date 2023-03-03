package me.mason.game

import org.lwjgl.BufferUtils
import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL.createCapabilities
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.system.MemoryUtil
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

typealias Draw = Window.() -> (Unit)
interface Window {
    val id: Long
    val dt: Float
    val elapsed: Float
    val camera: Camera
    val keys: BitSet
    val mouse: BitSet
    val cursor: FloatVector
    val uiCursor: FloatVector
    fun keys(key: Int = -1, action: Int = -1, block: (Int, Int) -> (Unit))
    fun keys(vararg keys: Int, action: Int = -1, block: (Int, Int) -> (Unit))
    fun mouse(key: Int = -1, action: Int = -1, block: (Int, Int) -> (Unit))
    fun scene(spriteSheet: Bind, block: Draw = {})
    fun Window.draw(vararg adapters: Mesh)
}

fun window(title: String, originWidth: Int, originHeight: Int, block: Window.() -> (Unit)) {
    GLFWErrorCallback.createPrint(System.err).set()
    check(glfwInit()) { "Unable to initialize GLFW" }
    glfwDefaultWindowHints()
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
    val id = glfwCreateWindow(originWidth, originHeight, title, MemoryUtil.NULL, MemoryUtil.NULL)
    if (id == MemoryUtil.NULL) throw RuntimeException("Failed to create the GLFW window")

    val videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!
    glfwSetWindowPos(
        id,
        (videoMode.width() - originWidth) / 2,
        (videoMode.height() - originHeight) / 2
    )

    glfwSetInputMode(id, GLFW_CURSOR, GLFW_CURSOR_DISABLED)
    glfwMakeContextCurrent(id)
    glfwSwapInterval(0)
    glfwShowWindow(id)
    createCapabilities()
    glEnable(GL_BLEND)
    glEnable(GL_REPEAT)
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

    glfwSetWindowSize(id, originWidth, originHeight)
    glViewport(0, 0, originWidth, originHeight)
    glfwSetWindowSizeCallback(id) { _, nextWidth, nextHeight ->
        glfwSetWindowSize(id, nextWidth, nextHeight)
        glViewport(0, 0, nextWidth, nextHeight)
    }

    val keyCallbacks = ArrayList<(Int, Int) -> (Unit)>()
    val mouseCallbacks = ArrayList<(Int, Int) -> (Unit)>()
    object : Window {
        private lateinit var sheet: Bind
        private var scene: Draw = { -> }
        private val meshes = IdentityHashMap<Shader, Mesh>()
        private val offsets = IdentityHashMap<Shader, Int>()
        override var dt = 0f
        override var elapsed = 0f
        override val id = id
        override val camera = camera()
        override val keys = BitSet(256).apply {
            keys { key, action ->
                if (key !in 0 until 256) return@keys
                when(action) {
                    GLFW_PRESS -> set(key)
                    GLFW_RELEASE -> clear(key)
                }
            }
        }
        override val mouse = BitSet(2).apply {
            mouse { key, action ->
                if (key !in 0 until 2) return@mouse
                when(action) {
                    GLFW_PRESS -> set(key)
                    GLFW_RELEASE -> clear(key)
                }
            }
        }
        override val uiCursor: FloatVector
            get() {
                val x = BufferUtils.createDoubleBuffer(1)
                val y = BufferUtils.createDoubleBuffer(1)
                val width = BufferUtils.createIntBuffer(1)
                val height = BufferUtils.createIntBuffer(1)
                glfwGetCursorPos(id, x, y)
                glfwGetWindowSize(id, width, height)
                glfwSetCursorPos(
                    id,
                    min(max(x[0], 0.0), width[0].toDouble()),
                    min(max(y[0], 0.0), height[0].toDouble())
                )
                if (x[0] < 0 || x[0] > width[0] ||
                    y[0] < 0 || y[0] > height[0]
                ) glfwGetCursorPos(id, x, y)
                return (vec(x[0].toFloat(), height[0] - y[0].toFloat()) - WINDOW_SCALE / 2f)
                    .max(-WINDOW_RADIUS).min(WINDOW_RADIUS)
            }
        override val cursor: FloatVector
            get() = uiCursor + camera.position

        override fun keys(key: Int, action: Int, block: (Int, Int) -> (Unit)) =
            keyCallbacks.plusAssign { inKey, inAction ->
                if ((inKey != key && key != -1) || (inAction != action && action != -1))
                    return@plusAssign
                block(inKey, inAction)
            }

        override fun keys(vararg keys: Int, action: Int, block: (Int, Int) -> Unit) {
            keyCallbacks.plusAssign { inKey, inAction ->
                if (inKey !in keys || (inAction != action && action != -1))
                    return@plusAssign
                block(inKey, inAction)
            }
        }

        override fun mouse(key: Int, action: Int, block: (Int, Int) -> (Unit)) =
            mouseCallbacks.plusAssign { inKey, inAction ->
                if ((inKey != key && key != -1) || (inAction != action && action != -1))
                    return@plusAssign
                block(inKey, inAction)
            }
        override fun scene(
            spriteSheet: Bind,
            block: Draw
        ) {
            sheet = spriteSheet
            scene = block
        }

        override fun Window.draw(vararg adapters: Mesh) {
            offsets.clear()
            meshes.values.forEach {
                it.clear(0 until it.limit)
            }
            for (adapter in adapters) {
                val mesh = meshes.getOrPut(adapter.shader) { mesh(adapter.shader, MAX_VERTICES / adapter.shader.quadLength) }
                var index = adapter.quads.nextSetBit(0)
                while (index != -1) {
                    val offset = offsets[adapter.shader] ?: 0
                    mesh.copy(adapter.data, index, offset)
                    offsets[adapter.shader] = offset + 1
                    index = adapter.quads.nextSetBit(index + 1)
                }
            }
            meshes.forEach { (shader, mesh) ->
                glBindBuffer(GL_ARRAY_BUFFER, shader.vbo)
                glBufferSubData(GL_ARRAY_BUFFER, 0, mesh.data)

                shader.attach()
                shader.texture("TEX_SAMPLER", 0)
                glActiveTexture(GL_TEXTURE0)
                sheet.attach()
                shader.mat4f("uProjection", camera.projection)
                shader.mat4f("uView", camera.view)

                glBindVertexArray(shader.vao)
                for (i in 0 until shader.attributesCount) glEnableVertexAttribArray(i)
                glDrawElements(GL_TRIANGLES, ELEMENTS.size, GL_UNSIGNED_INT, 0)
                for (i in 0 until shader.attributesCount) glDisableVertexAttribArray(i)
                glBindVertexArray(0)

                sheet.detach()
                shader.detach()
            }
        }
        init {
            keys(key = GLFW_KEY_ESCAPE) { _, _ -> glfwSetWindowShouldClose(id, true) }
            glfwSetKeyCallback(id) { _, key, _, action, _ ->
                keyCallbacks.forEach { it(key, action) }
            }
            glfwSetMouseButtonCallback(id) { _, key, action, _ ->
                mouseCallbacks.forEach { it(key, action) }
            }
            block(this)
            val start = System.nanoTime()
            var last = -1L
            while (!glfwWindowShouldClose(id)) {
                glClearColor(139f/255f, 180f/255f, 199f/255f, 1f)
                glClear(GL_COLOR_BUFFER_BIT)
                val now = System.nanoTime()
                dt = (if (last == -1L) 0f else (now - last).toFloat()) / 1.seconds.inWholeNanoseconds.toFloat()
                elapsed = (now - start).toFloat() / 1.seconds.inWholeNanoseconds.toFloat()
                last = now
                scene()
                glfwSwapBuffers(id)
                glfwPollEvents()
            }
        }
    }

    Callbacks.glfwFreeCallbacks(id)
    glfwDestroyWindow(id)
    glfwTerminate()
    glfwSetErrorCallback(null)!!.free()
}