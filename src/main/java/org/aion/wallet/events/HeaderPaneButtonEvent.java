package org.aion.wallet.events;

public class HeaderPaneButtonEvent extends AbstractEvent<HeaderPaneButtonEvent.Type> {

    public static final String ID = "ui.header_button";

    public HeaderPaneButtonEvent(final Type eventType) {
        super(eventType);
    }

    public enum Type {
        OVERVIEW, SEND, RECEIVE, HISTORY, CONTRACTS, SETTINGS
    }
}
