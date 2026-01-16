package unitTests

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import io.bennyoe.assets.TextureAtlases
import io.bennyoe.components.AttackComponent
import io.bennyoe.components.HealthComponent
import io.bennyoe.components.ImageComponent
import io.bennyoe.components.TransformComponent
import io.bennyoe.components.animation.AnimationComponent
import io.bennyoe.components.animation.NoAnimationKey
import io.bennyoe.components.animation.PlayerAnimation
import io.bennyoe.systems.AnimationSystem
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import ktx.collections.gdxArrayOf
import ktx.math.vec2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.badlogic.gdx.utils.Array as GdxArray

class AnimationSystemUnitTest {
    private lateinit var world: World
    private lateinit var aniCmp: AnimationComponent
    private lateinit var imgCmp: ImageComponent
    private lateinit var imageMock: Image

    @BeforeEach
    fun setUp() {
        Gdx.app = mockk<Application>(relaxed = true)

        imageMock = mockk(relaxed = true)

        val stageMock = mockk<Stage>(relaxed = true)
        every { stageMock.actors.contains(any(), any()) } returns false

        val atlasMock = mockk<TextureAtlas>(relaxed = true)
        val regionMock = mockk<TextureAtlas.AtlasRegion>(relaxed = true)

        every { atlasMock.findRegions(any()) } returns gdxArrayOf(regionMock)

        world =
            configureWorld {
                injectables {
                    add("dawnAtlases", TextureAtlases(atlasMock, atlasMock, atlasMock))
                    add("mushroomAtlases", TextureAtlases(atlasMock, atlasMock, atlasMock))
                    add("crowAtlases", TextureAtlases(atlasMock, atlasMock, atlasMock))
                    add("minotaurAtlases", TextureAtlases(atlasMock, atlasMock, atlasMock))
                    add("spectorAtlases", TextureAtlases(atlasMock, atlasMock, atlasMock))
                    add("stage", stageMock)
                }
                systems { add(AnimationSystem()) }
            }

        aniCmp =
            AnimationComponent().apply {
                val frameArray = GdxArray<TextureRegionDrawable>()
                frameArray.add(mockk(relaxed = true))
                animation = Animation(0.1f, frameArray)
            }

        imgCmp =
            ImageComponent(stageMock).also {
                it.image = imageMock
            }

        world.entity {
            it += AttackComponent()
            it += HealthComponent()
            it += aniCmp
            it += imgCmp
            it += TransformComponent(vec2(0f, 0f), 0f, 0f)
        }
    }

    @Test
    fun `stateTime increases by world delta`() {
        val delta = 0.2f
        val before = aniCmp.stateTime

        world.update(delta)

        assertEquals(
            before + delta,
            aniCmp.stateTime,
            1e-6f,
            "stateTime should be incremented by the world's delta time",
        )
    }

    @Test
    fun `image drawable is updated each tick`() {
        every { imageMock.drawable = any() } just Runs

        world.update(0.016f)

        verify(exactly = 1) { imageMock.drawable = any() }
    }

    @Test
    fun `nextAnimation triggers applyNextAnimation and resets flags`() {
        aniCmp.nextAnimation(
            PlayerAnimation.WALK,
        )

        world.update(0f)

        // after update the "next" flags should be cleared
        assertEquals(
            NoAnimationKey,
            aniCmp.nextAnimationType,
            "nextAnimationType should be cleared after applyNextAnimation()",
        )
    }
}
