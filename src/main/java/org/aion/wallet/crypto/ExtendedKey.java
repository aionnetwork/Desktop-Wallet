package org.aion.wallet.crypto;

import org.aion.crypto.ECKey;
import org.aion.crypto.ed25519.ECKeyEd25519;
import org.aion.wallet.exception.ValidationException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class ExtendedKey {
    private static final String HmacSHA512_ALGORITHM = "HmacSHA512";
    private static final int HARDENED_KEY_MULTIPLIER = 0x80000000;

    private final ECKey ecKey;
    private final ECKeyEd25519 ed25519KeyFactory = new ECKeyEd25519();


    public ExtendedKey(ECKey ecKey) {
        this.ecKey = ecKey;
    }

    public ECKey getEcKey() {
        return ecKey;
    }

    public ExtendedKey deriveHardened(int[] derivationPath) throws ValidationException {
        if (derivationPath.length == 0) {
            throw new ValidationException("Derivation path is incorrect");
        }
        try {
            ExtendedKey key = this.getChild(derivationPath[0]);
            for (int i = 1; i < derivationPath.length; i++) {
                key = key.getChild(derivationPath[i]);
            }
            return key;
        } catch (Exception e) {
            throw new ValidationException(e.getMessage());
        }

    }

    private ExtendedKey getChild(int i) throws Exception {
        byte[] keyHash = ecKey.getPrivKeyBytes();

        byte[] parentPrivateKey = Arrays.copyOfRange(keyHash, 0, 32);
        byte[] parentChainCode = Arrays.copyOfRange(keyHash, 32, 64);

        // ed25519 supports ONLY hardened keys
        i |= HARDENED_KEY_MULTIPLIER;

        ByteBuffer byteBuffer = ByteBuffer.allocate(32);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putInt(i);

        byte[] parentPaddedKey = org.spongycastle.util.Arrays.concatenate(new byte[]{0}, parentPrivateKey, byteBuffer.array());

        Mac mac = Mac.getInstance(HmacSHA512_ALGORITHM);
        SecretKey key = new SecretKeySpec(parentChainCode, HmacSHA512_ALGORITHM);
        mac.init(key);
        return new ExtendedKey(ed25519KeyFactory.fromPrivate(mac.doFinal(parentPaddedKey)));
    }

}
