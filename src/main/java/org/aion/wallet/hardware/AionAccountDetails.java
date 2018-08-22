package org.aion.wallet.hardware;

public class AionAccountDetails {
    private String publicKey;
    private String address;

    public AionAccountDetails(String publicKey, String address) {
        this.publicKey = publicKey;
        this.address = address;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
