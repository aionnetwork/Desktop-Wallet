package org.aion.wallet.hardware.ledger;

import org.aion.wallet.hardware.HardwareWallet;

public class LedgerWallet implements HardwareWallet {
    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public byte[] getPublicKey(final int derivationIndex) {
        return new byte[0];
    }

    @Override
    public byte[] signMessage(final byte[] message) {
        return new byte[0];
    }
}
