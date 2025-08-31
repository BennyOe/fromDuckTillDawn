package io.bennyoe.lightEngine.core

/**
 * Manages consumers and broadcasts lightning events.
 */
object LightningEventListener {
    private val lightningConsumerList: MutableList<LightningEventConsumer> = mutableListOf()

    /**
     * Adds a consumer to the listener.
     *
     * @param lightningEventConsumer the consumer to add
     */
    fun subscribe(lightningEventConsumer: LightningEventConsumer) {
        lightningConsumerList.add(lightningEventConsumer)
        println("Consumer: ${lightningEventConsumer.javaClass.simpleName} successfully added to LightningEventListener")
    }

    /**
     * Removes a consumer from the listener.
     *
     * @param lightningEventConsumer the consumer to remove
     */
    fun unsubscribe(lightningEventConsumer: LightningEventConsumer) {
        lightningConsumerList.remove(lightningEventConsumer)
        println("Consumer: ${lightningEventConsumer.javaClass.simpleName} successfully removed from LightningEventListener")
    }

    /**
     * Triggers the [onLightning] callback on all registered consumers.
     */
    fun emitLightningEvent() {
        lightningConsumerList.forEach { consumer -> consumer.onLightning() }
    }
}

/**
 * An observer that reacts to lightning events.
 */
interface LightningEventConsumer {
    /**
     * Called when a lightning event is emitted.
     */
    fun onLightning()
}
