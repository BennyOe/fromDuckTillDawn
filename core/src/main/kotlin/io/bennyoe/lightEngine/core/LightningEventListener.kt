package io.bennyoe.lightEngine.core

/**
 * Manages consumers and broadcasts lightning events.
 */
object LightningEventListener {
    private val lightningConsumerList: MutableList<Consumer> = mutableListOf()

    /**
     * Adds a consumer to the listener.
     *
     * @param consumer the consumer to add
     */
    fun subscribe(consumer: Consumer) {
        lightningConsumerList.add(consumer)
        println("Consumer: ${consumer.javaClass.simpleName} successfully added to LightningEventListener")
    }

    /**
     * Removes a consumer from the listener.
     *
     * @param consumer the consumer to remove
     */
    fun unsubscribe(consumer: Consumer) {
        lightningConsumerList.remove(consumer)
        println("Consumer: ${consumer.javaClass.simpleName} successfully removed from LightningEventListener")
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
interface Consumer {
    /**
     * Called when a lightning event is emitted.
     */
    fun onLightning()
}
