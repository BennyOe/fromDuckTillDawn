package io.bennyoe.systems.render

import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import io.bennyoe.components.ImageComponent
import io.bennyoe.lightEngine.core.Scene2dLightEngine

/**
 * Minimal helper: one place for region drawing (with flip + color).
 */
object DrawUtils {
    fun drawRegion(
        engine: Scene2dLightEngine,
        imageCmp: ImageComponent,
    ) {
        val texture = imageCmp.image.drawable as? TextureRegionDrawable ?: return
        val region = texture.region
        val oldColor = engine.batch.color.cpy()
        engine.batch.color = imageCmp.image.color

        if (imageCmp.flipImage) {
            engine.batch.draw(
                region,
                imageCmp.image.x + imageCmp.image.width,
                imageCmp.image.y,
                imageCmp.image.originX,
                imageCmp.image.originY,
                -imageCmp.image.width,
                imageCmp.image.height,
                imageCmp.image.scaleX,
                imageCmp.image.scaleY,
                imageCmp.image.rotation,
            )
        } else {
            engine.batch.draw(
                region,
                imageCmp.image.x,
                imageCmp.image.y,
                imageCmp.image.originX,
                imageCmp.image.originY,
                imageCmp.image.width,
                imageCmp.image.height,
                imageCmp.image.scaleX,
                imageCmp.image.scaleY,
                imageCmp.image.rotation,
            )
        }
        engine.batch.color = oldColor
    }
}
