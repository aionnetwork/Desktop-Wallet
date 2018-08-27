package org.aion.wallet.hardware.ledger;

public class LedgerException extends Exception {

    public LedgerException(final Exception cause) {
        super(cause);
    }

    public LedgerException(String message) {
        super(message);
    }
}
