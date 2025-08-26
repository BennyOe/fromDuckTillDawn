package io.bennyoe.systems.render

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import io.bennyoe.components.ImageComponent
import io.bennyoe.lightEngine.core.Scene2dLightEngine

/**
 * Minimal helper: one place for region drawing (with flip + color).
 */
object DrawUtils {
    // for non lighting
    // TODO remove after deleting the non lighting
    fun drawRegion(
        batch: Batch,
        imageCmp: ImageComponent,
    ) {
        val texture = imageCmp.image.drawable as? TextureRegionDrawable ?: return
        val region = texture.region
        val oldColor = batch.color.cpy()
        batch.color = imageCmp.image.color

        if (imageCmp.flipImage) {
            batch.draw(
                region,
                imageCmp.image.x + imageCmp.image.width,
                imageCmp.image.y,
                -imageCmp.image.width,
                imageCmp.image.height,
            )
        } else {
            batch.draw(
                region,
                imageCmp.image.x,
                imageCmp.image.y,
                imageCmp.image.width,
                imageCmp.image.height,
            )
        }
        batch.color = oldColor
    }

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
                -imageCmp.image.width,
                imageCmp.image.height,
            )
        } else {
            engine.batch.draw(
                region,
                imageCmp.image.x,
                imageCmp.image.y,
                imageCmp.image.width,
                imageCmp.image.height,
            )
        }
        engine.batch.color = oldColor
    }
}
