package me.mason.game

import org.joml.*
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL20.*
import org.lwjgl.stb.STBImage.stbi_image_free
import org.lwjgl.stb.STBImage.stbi_load
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readBytes

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

typealias Drawable = Pair<Bounds, UV>

typealias UV = FloatArray
fun uv(position: Vector2i, scale: Vector2i, sheet: Vector2i): UV {
    val minX = position.x.toFloat() / sheet.x.toFloat() + 0.001f
    val minY = position.y.toFloat() / sheet.y.toFloat() + 0.001f
    val maxX = (position.x + scale.x).toFloat() / sheet.x.toFloat() - 0.001f
    val maxY = (position.y + scale.y).toFloat() / sheet.y.toFloat() - 0.001f
    return floatArrayOf(
        maxX, maxY,
        minX, minY,
        maxX, minY,
        minX, maxY
    )
}

typealias Bounds = FloatArray
fun bounds(position: Vector2f, scale: Vector2f): Bounds {
    val maxX = position.x + scale.x / 2
    val minX = position.x - scale.x / 2
    val maxY = position.y + scale.y / 2
    val minY = position.y - scale.y / 2
    return floatArrayOf(
        maxX, minY,
        minX, maxY,
        maxX, maxY,
        minX, minY
    )
}

fun mesh(bounds: Bounds, uv: UV) =
    floatArrayOf(
        bounds[0], bounds[1], uv[0], uv[1],
        bounds[2], bounds[3], uv[2], uv[3],
        bounds[4], bounds[5], uv[4], uv[5],
        bounds[6], bounds[7], uv[6], uv[7]
    )

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
    fun mat3f(name: String, mat3: Matrix3f)

    fun vec4f(name: String, vec4: Vector4f)
    fun vec3f(name: String, vec3: Vector3f)
    fun vec2f(name: String, vec2: Vector2f)

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
        override fun mat3f(name: String, mat3: Matrix3f) = location(name) {
            val buffer = BufferUtils.createFloatBuffer(9)
            mat3.get(buffer)
            glUniformMatrix3fv(it, false, buffer)
        }
        override fun vec4f(name: String, vec4: Vector4f) = location(name) {
            glUniform4f(it, vec4.x, vec4.y, vec4.z, vec4.w)
        }

        override fun vec3f(name: String, vec3: Vector3f) = location(name) {
            glUniform3f(it, vec3.x, vec3.y, vec3.z)
        }

        override fun vec2f(name: String, vec2: Vector2f) = location(name) {
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