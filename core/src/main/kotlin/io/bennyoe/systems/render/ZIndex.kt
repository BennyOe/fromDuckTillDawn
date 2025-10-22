package io.bennyoe.systems.render

/**
 * Enum representing common zIndex values used for rendering order.
 * Note: Many zIndex values are calculated dynamically (layerZIndex + offset).
 * This enum lists significant fixed values and default base values for layers.
 */
enum class ZIndex(
    val value: Int,
) {
    MIN(0),
    MAX(Int.MAX_VALUE),
    ENEMY_OFFSET(100), // Default offset for enemies relative to their layer
    PLAYER_OFFSET(200), // Default offset for the player relative to their layer

    SKY(1000),
    CLOUDS(2500),

    PARALLAX_BG_0(2000),
    PARALLAX_BG_1(3000),
    PARALLAX_BG_2(4000),
    PARALLAX_BG_3(5000),
    PARALLAX_BG_4(6000),

    TILES_BG(7000),
    DOORS(7100),
    BG_WITH_NORMALS(7200),
    TILES(7500),

    BG_OBJECTS(8000),

    CHARACTERS(9000),

    WATER(10_000),
    TILES_BEFORE_WATER(11_000),

    FADING_FOREGROUND(12_000),

    // Specific Particle Effects Z-Indices
    PARTICLES(14_000),
}
