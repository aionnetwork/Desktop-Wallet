package org.aion.wallet.hardware;

import org.aion.wallet.hardware.ledger.LedgerException;

import java.io.IOException;

public interface HardwareWallet {

    boolean isConnected();

    AionAccountDetails getAccountDetails(final int derivationIndex) throws LedgerException, IOException;

    String signMessage(final int derivationIndex, final byte[] message) throws LedgerException, IOException;
}
