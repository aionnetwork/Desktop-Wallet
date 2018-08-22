package org.aion.wallet.hardware;

public class AionAccountDetails {
    private static final String PREFIX = "0x";

    private final String publicKey;
    private final String address;
    private final int derivationIndex;

    public AionAccountDetails(final String publicKey, final String address, final int derivationIndex) {
        this.publicKey = publicKey.startsWith(PREFIX) ? publicKey : PREFIX + publicKey;
        this.address = address.startsWith(PREFIX) ? address : PREFIX + address;
        this.derivationIndex = derivationIndex;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getAddress() {
        return address;
    }


    public int getDerivationIndex() {
        return derivationIndex;
    }
}
