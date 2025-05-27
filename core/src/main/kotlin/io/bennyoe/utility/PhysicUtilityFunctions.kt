package io.bennyoe.utility

import com.badlogic.gdx.physics.box2d.Fixture

// extension functions to retrieve fixture-data more convenient
val Fixture.bodyData: BodyData?
    get() = this.body.userData as? BodyData

val Fixture.fixtureData: FixtureData?
    get() = this.userData as? FixtureData
