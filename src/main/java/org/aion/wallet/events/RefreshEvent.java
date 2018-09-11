package org.aion.wallet.events;

import java.util.Optional;

public class RefreshEvent extends AbstractEvent<RefreshEvent.Type> {

    public static final String ID = "ui.data_refresh";

    private final Boolean payload;

    public RefreshEvent(final Type eventType, final Boolean payload) {
        super(eventType);
        this.payload = payload;
    }

    private Optional<Boolean> getPayload() {
        return Optional.ofNullable(payload);
    }

    public enum Type {
        CONNECTED,
        DISCONNECTED,
        TRANSACTION_FINISHED,
        TIMER
    }
}
