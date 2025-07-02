package io.bennyoe.config

enum class EntityCategory(
    val bit: Short,
) {
    ALL(0xFFFF.toShort()),
    NONE(0x0000),
    PLAYER(0x0001),
    ENEMY(0x0002),
    GROUND(0x0003),
    LIGHT(0x0004),
    SENSOR(0x0005),
}
