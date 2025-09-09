package io.bennyoe.utility

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.GdxRuntimeException

fun setupShader(name: String): ShaderProgram {
    val vertShader: FileHandle = Gdx.files.internal("shader/$name.vert")
    val fragShader: FileHandle = Gdx.files.internal("shader/$name.frag")
    ShaderProgram.pedantic = false
    val shader = ShaderProgram(vertShader, fragShader)

    if (!shader.isCompiled) {
        throw GdxRuntimeException("Could not compile shader: ${shader.log}")
    }

    shader.bind()
    shader.setUniformi("u_texture", 0)

    return shader
}
