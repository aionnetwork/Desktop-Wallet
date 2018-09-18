package org.aion.wallet.util;

import io.github.novacrypto.bip39.SeedCalculator;
import org.aion.base.util.Hex;
import org.aion.base.util.TypeConverter;
import org.aion.crypto.ECKey;
import org.aion.crypto.ed25519.ECKeyEd25519;
import org.aion.wallet.exception.ValidationException;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.params.KeyParameter;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

public final class CryptoUtils {

    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA265";

    private static final int KEY_SIZE = 256;
    private static final int PBKDF2_ITERATIONS = 4096;

    private static final byte[] SALT_KEY = "salt".getBytes();
    private static final byte[] ED25519_KEY = "ed25519 seed".getBytes();
    private static final String HMAC_SHA512_ALGORITHM = "HmacSHA512";
    private static final String DEFAULT_MNEMONIC_PASSPHRASE = "";
    private static final int HARDENED_KEY_MULTIPLIER = 0x80000000;
    private static final ECKeyEd25519 EC_KEY_FACTORY = new ECKeyEd25519();

    private CryptoUtils() {}

    public static ECKey getBip39ECKey(final String mnemonic) throws ValidationException {
        final byte[] seed = new SeedCalculator().calculateSeed(mnemonic, DEFAULT_MNEMONIC_PASSPHRASE);
        return getECKey(getSha512(ED25519_KEY, seed));

    }

    public static byte[] getSha512(final byte[] secret, final byte[] hashData) throws ValidationException {
        final byte[] bytes;
        try {
            final Mac mac = Mac.getInstance(HMAC_SHA512_ALGORITHM);
            final SecretKey key = new SecretKeySpec(secret, HMAC_SHA512_ALGORITHM);
            mac.init(key);
            bytes = mac.doFinal(hashData);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new ValidationException(e);
        }
        return bytes;
    }

    public static byte[] getHardenedNumber(final int number) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putInt(number | HARDENED_KEY_MULTIPLIER);
        return byteBuffer.array();
    }

    public static ECKey getECKey(final byte[] privateKey) {
        return EC_KEY_FACTORY.fromPrivate(privateKey);
    }

    public static void preloadNatives() {
        //no need to do anything here, we just want the EC_KEY_FACTORY to be initialized
    }

    public static String getKDFPassword(final String password) throws ValidationException {
        final PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA256Digest());
        final byte[] salt = getSha512(SALT_KEY, password.getBytes());
        gen.init(password.getBytes(StandardCharsets.UTF_8), salt, PBKDF2_ITERATIONS);
        final byte[] derivedKey = ((KeyParameter) gen.generateDerivedParameters(KEY_SIZE)).getKey();
        return Hex.toHexString(derivedKey);
    }

    public static String getEncryptedText(final String mnemonic, final String password) {
        return null;
    }
}
