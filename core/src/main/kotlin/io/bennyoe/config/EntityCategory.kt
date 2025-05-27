package io.bennyoe.config

enum class EntityCategory(
    val bit: Short,
) {
    NONE(0x0000),
    PLAYER(0x0001),
    ENEMY(0x0002),
    GROUND(0x0003),
}
