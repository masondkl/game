package me.mason.game

import me.mason.game.component.Camera
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.ARBVertexArrayObject
import org.lwjgl.opengl.GL.createCapabilities
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import kotlin.time.Duration.Companion.seconds

const val MAX_VERTICES = Short.MAX_VALUE * 2
const val VERTEX_BYTES = 4 * Float.SIZE_BYTES
const val MESH_SIZE = 16
val OFFSETS = intArrayOf(2, 1, 0, 0, 1, 3)
val ELEMENTS = FloatArray(MAX_VERTICES * 6) {
    (OFFSETS[it % 6] + (it / 6) * 4).toFloat()
}

typealias Draw = Window.() -> (Unit)
interface Window {
    val dt: Float
    val elapsed: Float
    val camera: Camera
//    fun zoom(value: Float)
    fun input(key: Int = -1, action: Int = -1, block: (Int, Int) -> (Unit))
    fun scene(textureShader: Shader, camera: Camera, spriteSheet: Bind, block: Draw)
    fun Window.draw(vararg entries: Drawable)
}

fun window(title: String, originWidth: Int, originHeight: Int, block: Window.() -> (Unit)) {
    GLFWErrorCallback.createPrint(System.err).set()
    check(glfwInit()) { "Unable to initialize GLFW" }
    glfwDefaultWindowHints()
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
    val id = glfwCreateWindow(originWidth, originHeight, title, MemoryUtil.NULL, MemoryUtil.NULL)
    if (id == MemoryUtil.NULL) throw RuntimeException("Failed to create the GLFW window")

    val keyCallbacks = ArrayList<(Int, Int) -> (Unit)>()
    glfwSetKeyCallback(id) { _, key, scancode, action, mods ->
        keyCallbacks.forEach { it(key, action) }
    }

    MemoryStack.stackPush().use { stack ->
        val videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!
        val width = stack.mallocInt(1)
        val height = stack.mallocInt(1)
        glfwGetWindowSize(id, width, height)
        glfwSetWindowPos(
            id,
            (videoMode.width() - width[0]) / 2,
            (videoMode.height() - height[0]) / 2
        )
    }

    glfwMakeContextCurrent(id)
    glfwSwapInterval(1)
    glfwShowWindow(id)
    createCapabilities()
    glEnable(GL_BLEND)
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

    val offsets = intArrayOf(2, 1, 0, 0, 1, 3)
    val elements = IntArray(MAX_VERTICES * 6) {
        offsets[it % 6] + (it / 6) * 4
    }

    val vao = ARBVertexArrayObject.glGenVertexArrays()
    ARBVertexArrayObject.glBindVertexArray(vao)

    val vbo = glGenBuffers()
    glBindBuffer(GL_ARRAY_BUFFER, vbo)
    glBufferData(GL_ARRAY_BUFFER, (VERTEX_BYTES * MAX_VERTICES).toLong(), GL_DYNAMIC_DRAW)

    val elementBuffer = BufferUtils.createIntBuffer(elements.size)
    elementBuffer.put(elements).flip()

    val ebo = glGenBuffers()
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementBuffer, GL_STATIC_DRAW)

    val positionsSize = 2
    val uvSize = 2

    glVertexAttribPointer(0, positionsSize, GL_FLOAT, false, VERTEX_BYTES, 0)
    glEnableVertexAttribArray(0)

    glVertexAttribPointer(
        2,
        uvSize,
        GL_FLOAT,
        false,
        VERTEX_BYTES,
        (positionsSize * Float.SIZE_BYTES).toLong()
    )
    glEnableVertexAttribArray(2)

    var scene: Draw = { -> }
    lateinit var _camera: Camera
    lateinit var shader: Shader
    lateinit var sheet: Bind
    var dt = 0f
    var elapsed = 0f
    val vertices = FloatArray(MAX_VERTICES * 4)

    glfwSetWindowSize(id, originWidth, originHeight)
    glViewport(0, 0, originWidth, originHeight)
    glfwSetWindowSizeCallback(id) { _, width, height ->
        glfwSetWindowSize(id, width, height)
        glViewport(0, 0, width, height)
    }

    val window = object : Window {
        override val dt get() = dt
        override val elapsed get() = elapsed
        override val camera get() = _camera

        override fun input(key: Int, action: Int, block: (Int, Int) -> (Unit)) =
            keyCallbacks.plusAssign { inKey, inAction ->
                if ((inKey != key && key != -1) || (inAction != action && action != -1))
                    return@plusAssign
                block(inKey, inAction)
            }

        override fun scene(
            textureShader: Shader,
            camera: Camera,
            spriteSheet: Bind,
            block: Draw
        ) {
            shader = textureShader
            _camera = camera
            sheet = spriteSheet
            _camera.projection.apply {
                identity()
                ortho(0.0f, originWidth.toFloat(), 0.0f, originHeight.toFloat(), 0.0f, 100.0f)
            }
            scene = block
        }

        override fun Window.draw(vararg entries: Drawable) {
            entries.forEachIndexed { entryIndex, (bounds, uv) ->
                val offset = entryIndex * MESH_SIZE
                val mesh = mesh(bounds, uv)
                mesh.forEachIndexed { index, value ->
                    vertices[offset + index] = value
                }
            }

            glBindBuffer(GL_ARRAY_BUFFER, vbo)
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices)

            shader.attach()
            shader.texture("TEX_SAMPLER", 0)
            glActiveTexture(GL_TEXTURE0)
            sheet.attach()
            shader.mat4f("uProjection", camera.projection)
            shader.mat4f("uView", camera.view)

            glBindVertexArray(vao)

            glEnableVertexAttribArray(0)
            glEnableVertexAttribArray(1)

            glDrawElements(GL_TRIANGLES, ELEMENTS.size, GL_UNSIGNED_INT, 0)

            glDisableVertexAttribArray(0)
            glDisableVertexAttribArray(1)
            glBindVertexArray(0)
            shader.detach()

            for (index in vertices.indices) vertices[index] = 0f
        }
        init {
            input(key = GLFW_KEY_ESCAPE) { _, _ -> glfwSetWindowShouldClose(id, true) }
        }
    }; block(window)
//    window.zoom(2.0f)

    val start = System.nanoTime()
    var last = -1L
    while (!glfwWindowShouldClose(id)) {
        glClearColor(139f/255f, 180f/255f, 199f/255f, 1f)
        glClear(GL_COLOR_BUFFER_BIT)
        val now = System.nanoTime()
        dt = (if (last == -1L) 0f else (now - last).toFloat()) / 1.seconds.inWholeNanoseconds.toFloat()
        elapsed = (now - start).toFloat() / 1.seconds.inWholeNanoseconds.toFloat()
        last = now
        window.scene()
        glfwSwapBuffers(id)
        glfwPollEvents()
    }

    Callbacks.glfwFreeCallbacks(id)
    glfwDestroyWindow(id)
    glfwTerminate()
    glfwSetErrorCallback(null)!!.free()
}