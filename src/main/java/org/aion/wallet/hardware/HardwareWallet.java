package org.aion.wallet.hardware;

import org.aion.wallet.hardware.ledger.LedgerException;

import java.util.List;

public interface HardwareWallet {

    boolean isConnected();

    AionAccountDetails getAccountDetails(final int derivationIndex) throws LedgerException;

    List<AionAccountDetails> getMultipleAccountDetails(final int derivationIndexStart, final int derivationIndexEnd) throws LedgerException;

    String signMessage(final int derivationIndex, final byte[] message) throws LedgerException;
}
