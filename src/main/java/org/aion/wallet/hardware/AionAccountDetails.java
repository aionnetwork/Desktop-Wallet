package org.aion.wallet.hardware;

public class AionAccountDetails {
    private byte[] publicKey;
    private byte[] address;

    public AionAccountDetails(byte[] publicKey, byte[] address) {
        this.publicKey = publicKey;
        this.address = address;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }
}
