@file:JvmName("Lwjgl3Launcher")

package io.bennyoe.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import io.bennyoe.Duckee
import io.bennyoe.config.GameConstants.GAME_HEIGHT
import io.bennyoe.config.GameConstants.GAME_WIDTH

/** Launches the desktop (LWJGL3) application. */
fun main() {
    // This handles macOS support and helps on Windows.
    if (StartupHelper.startNewJvmIfRequired()) {
        return
    }
    Lwjgl3Application(
        Duckee(),
        Lwjgl3ApplicationConfiguration().apply {
            setTitle("FromDuckTillDawn")
            setWindowedMode(GAME_WIDTH.toInt(), GAME_HEIGHT.toInt())
            setWindowIcon(*(arrayOf(128, 64, 32, 16).map { "libgdx$it.png" }.toTypedArray()))

            // only needed when not drawing in an FBO but on the standard framebuffer
//            setBackBufferConfig(8, 8, 8, 8, 8, 1, 0)
            disableAudio(true)
        },
    )
}
