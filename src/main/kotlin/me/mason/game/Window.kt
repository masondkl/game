package me.mason.game

import me.mason.game.components.Entity
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
import java.lang.System.arraycopy
import java.util.BitSet
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

const val MAX_VERTICES = Short.MAX_VALUE
const val VERTEX_BYTES = 4 * Float.SIZE_BYTES
val OFFSETS = intArrayOf(2, 1, 0, 0, 1, 3)
val ELEMENTS = FloatArray(MAX_VERTICES * 6) {
    (OFFSETS[it % 6] + (it / 6) * 4).toFloat()
}

typealias Draw = Window.() -> (Unit)
interface Window {
    val id: Long
    val dt: Float
    val elapsed: Float
    val camera: Camera
    val keys: BitSet
    val mouse: BitSet
    fun keys(key: Int = -1, action: Int = -1, block: (Int, Int) -> (Unit))
    fun mouse(key: Int = -1, action: Int = -1, block: (Int, Int) -> (Unit))
    fun scene(textureShader: Shader, spriteSheet: Bind, block: Draw)
    fun Window.draw(vararg entities: Entity?)
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
    val mouseCallbacks = ArrayList<(Int, Int) -> (Unit)>()
    glfwSetKeyCallback(id) { _, key, _, action, _ ->
        keyCallbacks.forEach { it(key, action) }
    }
    glfwSetMouseButtonCallback(id) { _, key, action, _ ->
        mouseCallbacks.forEach { it(key, action) }
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
    glfwSwapInterval(0)
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
    val camera = camera(vec(0f, 0f))
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

    val mesh = mesh(MAX_VERTICES / 4)
    val window = object : Window {
        override val id = id
        override val dt get() = dt
        override val elapsed get() = elapsed
        override val camera get() = camera
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

        override fun keys(key: Int, action: Int, block: (Int, Int) -> (Unit)) =
            keyCallbacks.plusAssign { inKey, inAction ->
                if ((inKey != key && key != -1) || (inAction != action && action != -1))
                    return@plusAssign
                block(inKey, inAction)
            }

        override fun mouse(key: Int, action: Int, block: (Int, Int) -> (Unit)) =
            mouseCallbacks.plusAssign { inKey, inAction ->
                if ((inKey != key && key != -1) || (inAction != action && action != -1))
                    return@plusAssign
                block(inKey, inAction)
            }

        override fun scene(
            textureShader: Shader,
            spriteSheet: Bind,
            block: Draw
        ) {
            shader = textureShader
            sheet = spriteSheet
            scene = block
        }
        var previous = 0
        override fun Window.draw(vararg entities: Entity?) {
            var meshOffset = 0
            for (entity in entities.filterNotNull()) {
                entity.animations.filterNotNull().forEach {
                    val (sprite, scale, offset) = it.next(elapsed)
                    if ((meshOffset + 1) * QUAD_UV_PAIR >= vertices.size) {
                        return@forEach
                    }
//                    println(meshOffset)
//                    println()
//                    println("scale: ${it.scale.x}, ${it.scale.y}")
//                    println("offset: ${it.offset.x}, ${it.offset.y}")
//                    println("position: ${entity.position.x}, ${entity.position.y}")
//                    println("boundspos: ${entity.position.x + it.offset.x}, ${entity.position.y + it.offset.y}")
//                    if (offset.x > 0f || offset.y > 0f)
//                        println("\noffset: ${offset.x}, ${offset.y}")
                    mesh[meshOffset] = mesh(bounds(
                        entity.position + offset + (
                                if (entity.ui) camera.position
                                else vec(0f, 0f)
                        ), scale
                    ), sprite)
                    meshOffset++
                }
            }
            arraycopy(mesh.data, 0, vertices, 0, mesh.quads * QUAD_UV_PAIR)
            if (previous > mesh.quads * QUAD_UV_PAIR) {
                vertices.fill(0f, mesh.quads * QUAD_UV_PAIR, previous)
            }; previous = mesh.quads * QUAD_UV_PAIR

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
        }
        init {
            keys(key = GLFW_KEY_ESCAPE) { _, _ -> glfwSetWindowShouldClose(id, true) }
        }
    }; block(window)

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