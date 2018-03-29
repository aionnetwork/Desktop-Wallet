package org.aion.wallet.ui.events;

public class AbstractUIEvent<T extends Enum> {

    private final T eventType;

    protected AbstractUIEvent(T eventType) {
        this.eventType = eventType;
    }

    public T getType() {
        return eventType;
    }
}
