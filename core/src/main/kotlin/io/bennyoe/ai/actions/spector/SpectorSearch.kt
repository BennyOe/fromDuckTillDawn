package io.bennyoe.ai.actions.spector

import com.badlogic.gdx.Gdx
import io.bennyoe.ai.blackboards.SpectorContext
import io.bennyoe.ai.core.AbstractAction

class SpectorSearch : AbstractAction<SpectorContext>() {
    private var timer = 0f

    override fun enter() {
        timer = 0f
        // MODIFIED: Ensure the FSM knows we are currently searching
        ctx.searchIsFinished = false
    }

    override fun onExecute(): Status {
        // MODIFIED: simple 10 second timer logic
        timer += Gdx.graphics.deltaTime

        return if (timer >= 10f) {
            // MODIFIED: Important for your HasAwareness logic to transition back to CALM
            ctx.searchIsFinished = true
            Status.SUCCEEDED
        } else {
            // Optional: add search-specific movement or animations here
            ctx.stopMovement()
            Status.RUNNING
        }
    }
}
