package io.bennyoe.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import ktx.assets.disposeSafely
import ktx.graphics.color
import ktx.scene2d.Scene2DSkin
import ktx.style.checkBox
import ktx.style.label
import ktx.style.progressBar
import ktx.style.skin
import ktx.style.slider
import ktx.style.textField
import ktx.style.window

enum class Drawables(
    val atlasKey: String,
) {
    BAR_BG("bg"),
    AIR_BAR("air"),
    LIFE_BAR("life"),
    DEBUG_FRAME("debug-frame"),
}

fun createSkin() {
    val monserratGenerator = FreeTypeFontGenerator(Gdx.files.internal("fonts/Montserrat.ttf"))
    val robotoGenerator = FreeTypeFontGenerator(Gdx.files.internal("fonts/RobotoMono-Regular.ttf"))
    val mainParameter =
        FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            size = 14
            color = Color.WHITE
        }
    val smallParameter =
        FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            size = 11
            color = Color.WHITE
        }
    val mainFont = monserratGenerator.generateFont(mainParameter).apply { data.markupEnabled = true }
    val smallFont = robotoGenerator.generateFont(smallParameter).apply { data.markupEnabled = true }
    monserratGenerator.dispose()
    robotoGenerator.dispose()

    Scene2DSkin.defaultSkin =
        skin(TextureAtlas("ui/ui.atlas")) {
            add("default-font", mainFont, BitmapFont::class.java)
            add("small-font", smallFont, BitmapFont::class.java)

            window("default") {
                titleFont = this@skin.getFont("small-font")
                titleFontColor = Color.RED
            }

            slider("default-horizontal") {
                background = this@skin.getDrawable("slider-bg")
                knob = this@skin.getDrawable("slider-knob")
            }

            checkBox {
                checkboxOff = this@skin.getDrawable("checkbox-off")
                checkboxOn = this@skin.getDrawable("checkbox-on")
                this.font = this@skin.getFont("small-font")
            }

            progressBar("life-bar") {
                background = this@skin.getDrawable(Drawables.BAR_BG.atlasKey)
                knobBefore = this@skin.getDrawable(Drawables.LIFE_BAR.atlasKey)
            }

            progressBar("air-bar") {
                background = this@skin.getDrawable(Drawables.BAR_BG.atlasKey)
                knobBefore = this@skin.getDrawable(Drawables.AIR_BAR.atlasKey)
            }

            progressBar("sight-bar") {
                background = this@skin.getDrawable(Drawables.BAR_BG.atlasKey)
                knobBefore = this@skin.getDrawable(Drawables.AIR_BAR.atlasKey)
            }

            textField("default") {
                this.font = this@skin.getFont("default-font")
                fontColor = color(0f, 0f, 0f, 1f)
                background = createColorDrawable(1f, 1f, 1f, 0.8f)
            }
            label("default") {
                this.font = this@skin.getFont("default-font")
                fontColor = color(1f, 1f, 1f, 1f)
            }
            textField("btTextField") {
                this.font = this@skin.getFont("default-font")
                fontColor = color(1f, 1f, 1f, 1f)
                background = createColorDrawable(1f, 0f, 0f, 0.8f)
            }

            label("debug") {
                this.font = this@skin.getFont("small-font")
                fontColor = color(1f, 1f, 1f, 1f)
            }
        }
}

fun disposeSkin() {
    Scene2DSkin.defaultSkin.disposeSafely()
}

fun createColorDrawable(
    r: Float,
    g: Float,
    b: Float,
    a: Float,
): TextureRegionDrawable {
    val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
    pixmap.setColor(color(r, g, b, a))
    pixmap.fill()

    val texture = Texture(pixmap)
    pixmap.dispose()

    return TextureRegionDrawable(TextureRegion(texture))
}
