package org.aion.wallet.hardware;

public interface HardwareWallet {

    boolean isConnected();

    byte[] getPublicKey(final int derivationIndex);

    byte[] signMessage(final byte[] message);
}
