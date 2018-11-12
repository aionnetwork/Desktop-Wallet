package org.aion.wallet.events;

import javafx.scene.input.InputEvent;

public class UiMessageEvent extends AbstractEvent<UiMessageEvent.Type> {

    public static final String ID = "ui.message";

    private final String message;

    private final InputEvent eventSource;

    public UiMessageEvent(final Type eventType, final String message) {
        super(eventType);
        this.message = message;
        eventSource = null;
    }

    public UiMessageEvent(final Type eventType, final InputEvent eventSource) {
        super(eventType);
        this.eventSource = eventSource;
        this.message = null;
    }

    public String getMessage() {
        return message;
    }

    public InputEvent getEventSource() {
        return eventSource;
    }

    public enum Type {
        MNEMONIC_CREATED,
        LEDGER_CONNECTED,
        LEDGER_ACCOUNT_SELECTED,
        TOKEN_BALANCES_SHOW,
        TOKEN_ADDED
    }
}
