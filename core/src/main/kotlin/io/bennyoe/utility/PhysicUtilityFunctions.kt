package io.bennyoe.utility

import com.badlogic.gdx.physics.box2d.Fixture

/**
 * extension functions to retrieve body-data as [BodyData]
 */
val Fixture.bodyData: BodyData?
    get() = this.body.userData as? BodyData

/**
 * extension functions to retrieve fixture-data as [FixtureData]
 */
val Fixture.fixtureData: FixtureData?
    get() = this.userData as? FixtureData
