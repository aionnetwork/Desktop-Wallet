package org.aion.wallet.events;

public class UiMessageEvent extends AbstractEvent<UiMessageEvent.Type> {

    public static final String ID = "ui.message";

    private final String mnemonic;

    public UiMessageEvent(final Type eventType, final String mnemonic) {
        super(eventType);
        this.mnemonic = mnemonic;
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public enum Type {
        MNEMONIC_CREATED
    }
}
