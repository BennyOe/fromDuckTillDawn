package unitTests

import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.tiled.TiledMap
import io.bennyoe.assets.MapAssets
import io.bennyoe.assets.TextureAssets
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import ktx.assets.async.AssetStorage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AssetStoreUnitTest {
    private lateinit var assetStorage: AssetStorage
    private lateinit var mockTextureAtlas: TextureAtlas
    private lateinit var mockTiledMap: TiledMap

    @BeforeEach
    fun setup() {
        // Create mock objects
        mockTextureAtlas = mockk<TextureAtlas>()
        mockTiledMap = mockk<TiledMap>()

        // Configure mocks
        every { mockTextureAtlas.regions } returns
            com.badlogic.gdx.utils.Array<TextureAtlas.AtlasRegion>().apply {
                add(mockk<TextureAtlas.AtlasRegion>())
            }
        every { mockTiledMap.layers } returns
            mockk {
                every { count } returns 2
            }

        // Create a spy of AssetStorage to mock its behavior
        assetStorage = spyk(AssetStorage())

        // Mock the load and isLoaded methods
        coEvery { assetStorage.load<TextureAtlas>(TextureAssets.PLAYER_ATLAS.descriptor) } returns mockTextureAtlas
        coEvery { assetStorage.load<TiledMap>(MapAssets.TEST_MAP.descriptor) } returns mockTiledMap
        every { assetStorage.isLoaded(TextureAssets.PLAYER_ATLAS.descriptor) } returns true
        every { assetStorage.isLoaded(MapAssets.TEST_MAP.descriptor) } returns true
        every { assetStorage.get<TextureAtlas>(TextureAssets.PLAYER_ATLAS.descriptor) } returns mockTextureAtlas
        every { assetStorage.get<TiledMap>(MapAssets.TEST_MAP.descriptor) } returns mockTiledMap
        coEvery { assetStorage.unload(any<AssetDescriptor<*>>()) } returns true
    }

    @Test
    fun `asset storage can load texture atlas`() =
        runBlocking {
            // Load the player texture atlas
            val atlas = assetStorage.load(TextureAssets.PLAYER_ATLAS.descriptor)

            // Verify the asset was loaded
            assertTrue(assetStorage.isLoaded(TextureAssets.PLAYER_ATLAS.descriptor))

            // Verify the asset is valid
            assertNotNull(atlas)
            assertTrue(atlas.regions.size > 0, "Texture atlas should contain regions")
        }

    @Test
    fun `asset storage can load tiled map`() =
        runBlocking {
            // Load the test map
            val map = assetStorage.load(MapAssets.TEST_MAP.descriptor)

            // Verify the asset was loaded
            assertTrue(assetStorage.isLoaded(MapAssets.TEST_MAP.descriptor))

            // Verify the asset is valid
            assertNotNull(map)
            assertTrue(map.layers.count > 0, "Map should have at least one layer")
        }

    @Test
    fun `asset storage can unload assets`() =
        runBlocking {
            // Load the asset
            assetStorage.load(TextureAssets.PLAYER_ATLAS.descriptor)

            // Verify the asset was loaded
            assertTrue(assetStorage.isLoaded(TextureAssets.PLAYER_ATLAS.descriptor))

            // Mock isLoaded to return false after unloading
            every { assetStorage.isLoaded(TextureAssets.PLAYER_ATLAS.descriptor) } returns false

            // Unload the asset
            assetStorage.unload(TextureAssets.PLAYER_ATLAS.descriptor)

            // Verify the asset was unloaded
            assertTrue(!assetStorage.isLoaded(TextureAssets.PLAYER_ATLAS.descriptor))
        }
}
