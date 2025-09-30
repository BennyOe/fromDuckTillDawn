package io.bennyoe.widgets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import ktx.assets.disposeSafely
import ktx.graphics.color
import ktx.scene2d.Scene2DSkin
import ktx.style.label
import ktx.style.progressBar
import ktx.style.skin
import ktx.style.textField

enum class Drawables(
    val atlasKey: String,
) {
    BAR_BG("bg"),
    AIR_BAR("air"),
    LIFE_BAR("life"),
}

operator fun Skin.get(drawable: Drawables): Drawable = this.getDrawable(drawable.atlasKey)

fun createSkin() {
    val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/Montserrat.ttf"))
    val parameter =
        FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            size = 14
            color = Color.WHITE
        }
    val mainFont = generator.generateFont(parameter)
    generator.dispose()

    Scene2DSkin.defaultSkin =
        skin(TextureAtlas("ui/ui.atlas")) {
            add("default-font", mainFont, BitmapFont::class.java) // Add the generated font to the skin for reuse

            progressBar("life-bar") {
                background = this@skin.getDrawable(Drawables.BAR_BG.atlasKey)
                knobBefore = this@skin.getDrawable(Drawables.LIFE_BAR.atlasKey)
            }

            progressBar("air-bar") {
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

            label("ui") {
                this.font = this@skin.getFont("default-font")
                fontColor = color(1f, 1f, 1f, 1f)
                background = createColorDrawable(1f, 0f, 0f, 1f)
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
