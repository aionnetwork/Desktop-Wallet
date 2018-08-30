package org.aion.wallet.hardware;

public abstract class HardwareWalletException extends Exception {

    protected HardwareWalletException(final Exception cause) {
        super(cause);
    }

    protected HardwareWalletException(final String message) {
        super(message);
    }
}
