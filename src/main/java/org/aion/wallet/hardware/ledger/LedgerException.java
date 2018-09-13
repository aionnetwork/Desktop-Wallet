package org.aion.wallet.hardware.ledger;

import org.aion.wallet.hardware.HardwareWalletException;

public class LedgerException extends HardwareWalletException {

    public LedgerException(final Exception cause) {
        super(cause);
    }

    public LedgerException(String message) {
        super(message);
    }
}
