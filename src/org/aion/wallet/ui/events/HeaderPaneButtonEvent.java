package org.aion.wallet.ui.events;

public class HeaderPaneButtonEvent extends AbstractUIEvent<HeaderPaneButtonEvent.Type> {

    public static final String ID = "ui.header_button";

    public HeaderPaneButtonEvent(final Type eventType) {
        super(eventType);
    }

    public enum Type {
        HOME, SEND, RECEIVE, HISTORY, CONTRACTS, SETTINGS
    }
}
