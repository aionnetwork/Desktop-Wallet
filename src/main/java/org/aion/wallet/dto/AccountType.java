package org.aion.wallet.dto;

public enum AccountType {
    LOCAL(0), EXTERNAL(100), LEDGER(2), TREZOR(3);

    private final String displayString;

    private final int order;

    AccountType(final int order) {
        this.order = order;
        final String s = toString();
        displayString = s.substring(0, 1) + s.substring(1).toLowerCase();
    }

    public String getDisplayString() {
        return displayString;
    }

    public int getOrder() {
        return order;
    }
}
