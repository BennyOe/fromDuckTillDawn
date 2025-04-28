package io.bennyoe.widgets

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import ktx.graphics.color
import ktx.scene2d.Scene2DSkin
import ktx.style.label
import ktx.style.skin
import ktx.style.textField

fun createSkin() {
    val font = BitmapFont()
    Scene2DSkin.defaultSkin =
        skin {
            label("default") {
                this.font = font
            }

            textField("default") {
                this.font = font
                fontColor = color(0f, 0f, 0f, 1f)
                background = createColorDrawable(1f, 1f, 1f, 0.8f)
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
