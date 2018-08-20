package org.aion.wallet.hardware.ledger;

public class LedgerAccount {

    private byte[] publicKey;
    private byte[] publicAddress;

    public LedgerAccount(byte[] publicKey, byte[] publicAddress) {
        this.publicKey = publicKey;
        this.publicAddress = publicAddress;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public byte[] getPublicAddress() {
        return publicAddress;
    }

    public void setPublicAddress(byte[] publicAddress) {
        this.publicAddress = publicAddress;
    }
}
