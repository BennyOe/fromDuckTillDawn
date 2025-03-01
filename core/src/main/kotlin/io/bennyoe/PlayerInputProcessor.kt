package io.bennyoe

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import ktx.app.KtxInputAdapter
import ktx.log.logger

class PlayerInputProcessor : KtxInputAdapter {
    init {
        Gdx.input.inputProcessor = this

    }

    override fun keyDown(keycode: Int): Boolean {
        when (keycode) {
            Keys.W -> LOG.debug { "W Pressed" }
            Keys.S -> LOG.debug { "S Pressed" }
            Keys.A -> LOG.debug { "A Pressed" }
            Keys.D -> LOG.debug { "D Pressed" }
            else -> return false
        }
        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        when (keycode) {
            Keys.W -> LOG.debug { "W released" }
            Keys.S -> LOG.debug { "S released" }
            Keys.A -> LOG.debug { "A released" }
            Keys.D -> LOG.debug { "D released" }
            else -> return false
        }
        return true
    }

    companion object {
        val LOG = logger<PlayerInputProcessor>()
    }
}
