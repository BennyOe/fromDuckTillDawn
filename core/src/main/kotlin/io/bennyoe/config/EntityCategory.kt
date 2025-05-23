package io.bennyoe.config

enum class EntityCategory(
    val bit: Short,
) {
    PLAYER(0x0001),
    ENEMY(0x0002),
    WALL(0x0003),
}
