package org.aion.wallet.events;

public class RefreshEvent extends AbstractEvent<RefreshEvent.Type> {

    public static final String ID = "ui.data_refresh";

    public RefreshEvent(final Type eventType) {
        super(eventType);
    }

    public enum Type {
        TIMER, OPERATION_FINISHED
    }
}
