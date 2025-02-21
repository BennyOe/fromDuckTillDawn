@file:JvmName("Lwjgl3Launcher")

package io.bennyoe.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import io.bennyoe.Duckee

/** Launches the desktop (LWJGL3) application. */
fun main() {
    // This handles macOS support and helps on Windows.
    if (StartupHelper.startNewJvmIfRequired())
      return
    Lwjgl3Application(Duckee(), Lwjgl3ApplicationConfiguration().apply {
        setTitle("FromDuckTillDawn")
        setWindowedMode(1280, 1024)
        setWindowIcon(*(arrayOf(128, 64, 32, 16).map { "libgdx$it.png" }.toTypedArray()))
    })
}
