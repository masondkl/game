package me.mason.game

import org.joml.Matrix4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL20.*
import org.lwjgl.stb.STBImage.stbi_image_free
import org.lwjgl.stb.STBImage.stbi_load
import java.lang.System.arraycopy
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readBytes

typealias UV = FloatArray
fun uv(position: IntVector, scale: IntVector): UV {
    val min = (position.float() / SHEET_SIZE.float())
    val max = ((position.float() + scale.float()) / SHEET_SIZE.float())
    return floatArrayOf(
        max.x, max.y,
        min.x, min.y,
        max.x, min.y,
        min.x, max.y
    )
}

typealias Bounds = FloatArray
fun bounds(position: FloatVector, scale: FloatVector): Bounds {
    val radius = scale / 2f
    val min = position - radius
    val max = position + radius
    return floatArrayOf(
        max.x, min.y,
        min.x, max.y,
        max.x, max.y,
        min.x, min.y
    )
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
    fun mat4f(name: String, mat4: Matrix4f)
    fun vec2f(name: String, vec2: FloatVector)

    fun intArray(name: String, value: IntArray)
    fun float(name: String, value: Float)
    fun int(name: String, value: Int)
    fun texture(name: String, slot: Int)
}

fun shader(path: Path): Shader {
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
    val self = Bind({ glUseProgram(program) }, { glUseProgram(0) })
    return object : Shader, Bind by self {
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