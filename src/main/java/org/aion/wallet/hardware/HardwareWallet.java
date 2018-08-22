package org.aion.wallet.hardware;

import org.aion.wallet.hardware.ledger.LedgerException;

public interface HardwareWallet {

    boolean isConnected();

    AionAccountDetails getAccountDetails(final int derivationIndex) throws LedgerException;

    String signMessage(final int derivationIndex, final byte[] message) throws LedgerException;
}
