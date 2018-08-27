package org.aion.wallet.hardware;

public class AionAccountDetails {
    public static final String PREFIX = "0x";
    private String publicKey;
    private String address;

    public AionAccountDetails(final String publicKey, final String address) {
        this.publicKey = publicKey.startsWith(PREFIX) ? publicKey : PREFIX + publicKey;
        this.address = address.startsWith(PREFIX) ? address : PREFIX + address;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(final String publicKey) {
        this.publicKey = publicKey;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(final String address) {
        this.address = address;
    }
}
