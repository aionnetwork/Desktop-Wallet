package org.aion.wallet.dto;

public enum AccountType {
    LOCAL, IMPORTED, LEDGER, TREZOR;

    private final String displayString;

    AccountType() {
        final String s = toString();
        displayString = s.substring(0, 1) + s.substring(1).toLowerCase();
    }

    public String getDisplayString() {
        return displayString;
    }
}
