package org.aion.wallet.crypto;

import org.aion.crypto.ECKey;
import org.aion.crypto.ISignature;
import org.aion.crypto.ed25519.ECKeyEd25519;
import org.libsodium.jni.Sodium;

import java.math.BigInteger;

public class SeededECKeyEd25519 extends ECKeyEd25519 {

    private final byte[] publicKey;
    private final byte[] secretKey;
    private final byte[] address;

    public SeededECKeyEd25519(final byte[] seed) {
        publicKey = new byte[PUBKEY_BYTES];
        secretKey = new byte[SECKEY_BYTES];
        final int expectedSeedLength = Sodium.crypto_sign_ed25519_seedbytes();
        if (seed.length != expectedSeedLength) {
            throw new IllegalArgumentException(String.format("Seed has %s length and should be %s", seed.length, expectedSeedLength));
        }
        Sodium.crypto_sign_ed25519_seed_keypair(publicKey, secretKey, seed);
        address = computeAddress(publicKey);
    }

    @Override
    public byte[] getAddress() {
        return address;
    }

    @Override
    public byte[] getPubKey() {
        return publicKey;
    }

    @Override
    public byte[] getPrivKeyBytes() {
        return secretKey;
    }

    @Override
    public ECKey fromPrivate(final BigInteger privateKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ECKey fromPrivate(final byte[] bs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] computeAddress(final byte[] pubBytes) {
        return super.computeAddress(pubBytes);
    }

    @Override
    public ISignature sign(final byte[] msg) {
        return super.sign(msg);
    }
}
