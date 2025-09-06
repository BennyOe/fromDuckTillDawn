package io.bennyoe.systems

import com.badlogic.gdx.Gdx
import com.github.quillraven.fleks.IntervalSystem
import io.bennyoe.components.GameStateComponent
import io.bennyoe.components.TimeScaleComponent
import io.bennyoe.config.GameConstants.DYNAMIC_TIME_OF_DAY
import io.bennyoe.config.GameConstants.HIT_STOP_DURATION
import io.bennyoe.config.GameConstants.TIME_OF_DAY_SPEED

private const val HOURS_IN_DAY = 24f
private const val FAST_FORWARD_DURATION_HOURS = 3f

class TimeSystem : IntervalSystem() {
    private val gameStateCmp by lazy { world.family { all(GameStateComponent) }.first()[GameStateComponent] }
    private val timeScaleCmp by lazy { world.family { all(TimeScaleComponent) }.first()[TimeScaleComponent] }
    private var isFastForwarding = false
    private var fastForwardStartTime = 0f
    private var fastForwardEndTime = 0f

    override fun onTick() {
        handleHitStop()
        handleFastForwardTrigger(gameStateCmp)
        updateTime()
    }

    fun startHitStop(
        scale: Float = 0f,
        duration: Float = HIT_STOP_DURATION,
    ) {
        if (timeScaleCmp.hitStopTimer <= 0f) {
            timeScaleCmp.current = scale
            timeScaleCmp.hitStopTimer = duration
        }
    }

    private fun handleHitStop() {
        if (timeScaleCmp.hitStopTimer > 0f) {
            timeScaleCmp.hitStopTimer -= Gdx.graphics.deltaTime
            if (timeScaleCmp.hitStopTimer <= 0f) {
                timeScaleCmp.current = 1f
            }
        }
    }

    private fun updateTime() {
        if (isFastForwarding || DYNAMIC_TIME_OF_DAY) {
            val timeToAdd = TIME_OF_DAY_SPEED * deltaTime * timeScaleCmp.current
            gameStateCmp.timeOfDay = (gameStateCmp.timeOfDay + timeToAdd) % HOURS_IN_DAY
        }
    }

    private fun handleFastForwardTrigger(gameStateCmp: GameStateComponent) {
        if (gameStateCmp.isTriggerTimeOfDayJustPressed) {
            isFastForwarding = true
            fastForwardStartTime = gameStateCmp.timeOfDay
            fastForwardEndTime = (fastForwardStartTime + FAST_FORWARD_DURATION_HOURS) % HOURS_IN_DAY
            gameStateCmp.isTriggerTimeOfDayJustPressed = false
        }

        if (isFastForwarding && isTimeProgressionComplete(fastForwardStartTime, fastForwardEndTime, gameStateCmp.timeOfDay)) {
            isFastForwarding = false
        }
    }

    private fun isTimeProgressionComplete(
        start: Float,
        end: Float,
        current: Float,
    ): Boolean {
        // This logic correctly handles the wrap-around at 24h
        val distance = (end - start + HOURS_IN_DAY) % HOURS_IN_DAY
        val progress = (current - start + HOURS_IN_DAY) % HOURS_IN_DAY
        return progress >= distance
    }
}
