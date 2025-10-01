package io.bennyoe.ui.widgets

import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import ktx.scene2d.KTable

class CharacterInfo(
    skin: Skin,
) : Table(skin),
    KTable {
    val lifeBar: ProgressBar = ProgressBar(0f, 1f, 0.1f, false, skin, "life-bar")
    val airBar: ProgressBar = ProgressBar(0f, 1f, 0.1f, false, skin, "air-bar")

    init {
        add(lifeBar).center()
        row()
        add(airBar).center().padTop(10f).padBottom(10f)
        lifeBar.value = 0.8f
        lifeBar.setAnimateDuration(0.5f)
        airBar.value = 0.8f
        airBar.setAnimateDuration(0.5f)
    }
}
