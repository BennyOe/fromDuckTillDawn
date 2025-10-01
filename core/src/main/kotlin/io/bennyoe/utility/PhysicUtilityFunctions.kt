package io.bennyoe.utility

import com.badlogic.gdx.physics.box2d.Fixture

/**
 * extension functions to retrieve body-data as [EntityBodyData]
 */
val Fixture.bodyData: EntityBodyData?
    get() = this.body.userData as? EntityBodyData

/**
 * extension functions to retrieve fixture-data as [FixtureSensorData]
 */
val Fixture.fixtureData: FixtureSensorData?
    get() = this.userData as? FixtureSensorData
