package org.aion.wallet.hardware;

import java.util.List;

public interface HardwareWallet {

    boolean isConnected();

    AionAccountDetails getAccountDetails(final int derivationIndex) throws HardwareWalletException;

    List<AionAccountDetails> getMultipleAccountDetails(final int derivationIndexStart, final int derivationIndexEnd) throws HardwareWalletException;

    byte[] signMessage(final int derivationIndex, final byte[] message) throws HardwareWalletException;
}
