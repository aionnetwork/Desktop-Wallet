package org.aion.wallet.hardware;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AionAccountDetails that = (AionAccountDetails) o;
        return derivationIndex == that.derivationIndex &&
                Objects.equals(publicKey, that.publicKey) &&
                Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicKey, address, derivationIndex);
    }
}
