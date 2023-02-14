package me.mason.game

import org.joml.Matrix4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glGenVertexArrays
import org.lwjgl.stb.STBImage.stbi_image_free
import org.lwjgl.stb.STBImage.stbi_load
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readBytes

const val MAX_VERTICES = (Short.MAX_VALUE).toInt() * 4
val ELEMENT_ORDER = intArrayOf(2, 1, 0, 0, 1, 3)
val ELEMENTS = IntArray(MAX_VERTICES * 6) {
    ELEMENT_ORDER[it % 6] + (it / 6) * 4
}

interface Bind {
    fun attach()
    fun detach()
}
fun Bind(attach: () -> (Unit), detach: () -> (Unit)) = object : Bind {
    var attached = false
    override fun attach() { if (!attached) {
        attach(); attached = true
    } }
    override fun detach() { if (attached) {
        detach(); attached = false
    } }
}

fun texture(path: Path): Bind = glGenTextures().let { id ->
    glBindTexture(GL_TEXTURE_2D, id)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
    val width = BufferUtils.createIntBuffer(1)
    val height = BufferUtils.createIntBuffer(1)
    val channels = BufferUtils.createIntBuffer(1)
    val image = stbi_load(path.absolutePathString(), width, height, channels, 0)
    if (image != null) {
        when(channels.get(0)) {
            3 -> glTexImage2D(
                GL_TEXTURE_2D, 0, GL_RGB, width.get(0), height.get(0),
                0, GL_RGB, GL_UNSIGNED_BYTE, image
            )
            4 -> glTexImage2D(
                GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0),
                0, GL_RGBA, GL_UNSIGNED_BYTE, image
            )
            else -> error("Unknown number of channels")
        }
    } else error("Could not load image")
    stbi_image_free(image)
    return Bind({ glBindTexture(GL_TEXTURE_2D, id) }, { glBindTexture(GL_TEXTURE_2D, 0) })
}

interface Shader : Bind {
    val attributesLength: Int
    val quadLength: Int
    val attributes: Int
    val vao: Int
    val vbo: Int
    fun mat4f(name: String, mat4: Matrix4f)
    fun vec2f(name: String, vec2: FloatVector)
    fun intArray(name: String, value: IntArray)
    fun float(name: String, value: Float)
    fun int(name: String, value: Int)
    fun texture(name: String, slot: Int)
}

fun vertexBuffers(vararg attributes: Int): Pair<Int, Int> {
    val vao = glGenVertexArrays()
    val vbo = glGenBuffers()
    val ebo = glGenBuffers()
    val vertexBytes = attributes.fold(0) { acc, it -> acc + it } * Float.SIZE_BYTES

    glBindVertexArray(vao)
    glBindBuffer(GL_ARRAY_BUFFER, vbo)
    glBufferData(GL_ARRAY_BUFFER, (MAX_VERTICES * vertexBytes).toLong(), GL_DYNAMIC_DRAW)
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, BufferUtils.createIntBuffer(ELEMENTS.size).put(ELEMENTS).flip(), GL_STATIC_DRAW)

    var offset = 0
    attributes.forEachIndexed { index, attribute ->
        glVertexAttribPointer(index, attribute, GL_FLOAT, false, vertexBytes, (offset * Float.SIZE_BYTES).toLong())
        glEnableVertexAttribArray(index)
        offset += attribute
    }

//    glVertexAttribPointer(0, SHADER_POSITION_LENGTH_FLOAT, GL_FLOAT, false, VERTEX_BYTES, 0)
//    glEnableVertexAttribArray(0)
//    glVertexAttribPointer(1, SHADER_SCALE_LENGTH_FLOAT, GL_FLOAT, false, VERTEX_BYTES, (SHADER_POSITION_LENGTH_FLOAT * Float.SIZE_BYTES).toLong())
//    glEnableVertexAttribArray(1)
//    glVertexAttribPointer(2, SHADER_START_LENGTH_FLOAT, GL_FLOAT, false, VERTEX_BYTES, ((SHADER_POSITION_LENGTH_FLOAT + SHADER_SCALE_LENGTH_FLOAT) * Float.SIZE_BYTES).toLong())
//    glEnableVertexAttribArray(2)
//    glVertexAttribPointer(3, SHADER_UV_START_LENGTH_FLOAT, GL_FLOAT, false, VERTEX_BYTES, ((SHADER_POSITION_LENGTH_FLOAT + SHADER_SCALE_LENGTH_FLOAT + SHADER_START_LENGTH_FLOAT) * Float.SIZE_BYTES).toLong())
//    glEnableVertexAttribArray(3)
//    glVertexAttribPointer(4, SHADER_UV_SCALE_LENGTH_FLOAT, GL_FLOAT, false, VERTEX_BYTES, ((SHADER_POSITION_LENGTH_FLOAT + SHADER_SCALE_LENGTH_FLOAT + SHADER_START_LENGTH_FLOAT + SHADER_UV_START_LENGTH_FLOAT) * Float.SIZE_BYTES).toLong())
//    glEnableVertexAttribArray(4)
    return Pair(vao, vbo)
}

fun shader(path: Path, vararg attributes: Int): Shader {
    if (attributes.isEmpty()) error("no attributes on shader")
    val (vertex, fragment) = String(path.readBytes()).split("|")
    val vertexID = glCreateShader(GL_VERTEX_SHADER)
    glShaderSource(vertexID, vertex)
    glCompileShader(vertexID)
    var success = glGetShaderi(vertexID, GL_COMPILE_STATUS)
    if (success == GL_FALSE) {
        val len = glGetShaderi(vertexID, GL_INFO_LOG_LENGTH)
        error(glGetShaderInfoLog(vertexID, len))
    }
    val fragmentID = glCreateShader(GL_FRAGMENT_SHADER)
    glShaderSource(fragmentID, fragment)
    glCompileShader(fragmentID)
    success = glGetShaderi(fragmentID, GL_COMPILE_STATUS)
    if (success == GL_FALSE) {
        val len = glGetShaderi(fragmentID, GL_INFO_LOG_LENGTH)
        error(glGetShaderInfoLog(fragmentID, len))
    }
    val program = glCreateProgram()
    glAttachShader(program, vertexID)
    glAttachShader(program, fragmentID)
    glLinkProgram(program)
    success = glGetProgrami(program, GL_LINK_STATUS)
    if (success == GL_FALSE) {
        val len = glGetProgrami(program, GL_INFO_LOG_LENGTH)
        error(glGetProgramInfoLog(program, len))
    }
    val (vao, vbo) = vertexBuffers(*attributes)
    val self = Bind({ glUseProgram(program) }, { glUseProgram(0) })
    val attributesLength = attributes.fold(0) { acc, attribute -> acc + attribute }
    return object : Shader, Bind by self {
        override val attributesLength = attributesLength
        override val quadLength = attributesLength * 4
        override val attributes = attributes.size
        override val vao = vao
        override val vbo = vbo

        private fun location(name: String, block: (Int) -> (Unit)) {
            val location = glGetUniformLocation(program, name)
            self.attach()
            block(location)
        }
        override fun mat4f(name: String, mat4: Matrix4f) = location(name) {
            val buffer = BufferUtils.createFloatBuffer(16)
            mat4.get(buffer)
            glUniformMatrix4fv(it, false, buffer)
        }
        override fun vec2f(name: String, vec2: FloatVector) = location(name) {
            glUniform2f(it, vec2.x, vec2.y)
        }
        override fun intArray(name: String, value: IntArray) = location(name) {
            glUniform1iv(it, value)
        }
        override fun float(name: String, value: Float) = location(name) {
            glUniform1f(it, value)
        }
        override fun int(name: String, value: Int) = location(name) {
            glUniform1i(it, value)
        }
        override fun texture(name: String, slot: Int) = location(name) {
            glUniform1i(it, slot)
        }
    }
}