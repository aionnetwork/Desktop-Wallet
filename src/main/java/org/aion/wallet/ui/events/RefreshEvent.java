package org.aion.wallet.ui.events;

public class RefreshEvent extends AbstractUIEvent{

    public RefreshEvent(Enum eventType) {
        super(eventType);
    }

    public enum Type {
        TIMER, OPERATION_FINISHED
    }
}
