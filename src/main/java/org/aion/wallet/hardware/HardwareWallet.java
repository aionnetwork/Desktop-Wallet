package org.aion.wallet.hardware;

import org.aion.wallet.hardware.ledger.LedgerException;

public interface HardwareWallet {

    boolean isConnected();

    AionAccountDetails getPublicKey(final int derivationIndex) throws LedgerException;

    byte[] signMessage(final int derivationIndex, final byte[] message) throws LedgerException;
}
