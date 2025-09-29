// main/kotlin/io/bennyoe/widgets/Skin.kt

package io.bennyoe.widgets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import ktx.graphics.color
import ktx.scene2d.Scene2DSkin
import ktx.style.label
import ktx.style.skin
import ktx.style.textField

fun createSkin() {
    val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/Montserrat.ttf"))
    val parameter =
        FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            size = 14 // Set the desired font size in pixels
            color = Color.WHITE
        }
    val mainFont = generator.generateFont(parameter)
    generator.dispose() // dispose the generator after creating the font

    Scene2DSkin.defaultSkin =
        skin {
            add("default-font", mainFont, BitmapFont::class.java) // Add the generated font to the skin for reuse

            textField("default") {
                this.font = this@skin.getFont("default-font") // Use the generated font
                fontColor = color(0f, 0f, 0f, 1f)
                background = createColorDrawable(1f, 1f, 1f, 0.8f)
            }
            label("default") {
                this.font = this@skin.getFont("default-font") // Use the generated font
                fontColor = color(1f, 1f, 1f, 1f)
            }
            textField("btTextField") {
                this.font = this@skin.getFont("default-font") // Use the generated font
                fontColor = color(1f, 1f, 1f, 1f)
                background = createColorDrawable(1f, 0f, 0f, 0.8f)
            }
        }
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
