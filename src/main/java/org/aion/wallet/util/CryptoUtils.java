package org.aion.wallet.util;

import io.github.novacrypto.bip39.SeedCalculator;
import org.aion.crypto.ECKey;
import org.aion.crypto.ed25519.ECKeyEd25519;
import org.aion.wallet.exception.ValidationException;
import org.libsodium.jni.Sodium;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.params.KeyParameter;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public final class CryptoUtils {

    private static final int PBKDF2_ITERATIONS = 4096;

    private static final byte[] SALT_KEY = "salt".getBytes();
    private static final byte[] ED25519_KEY = "ed25519 seed".getBytes();
    private static final String HMAC_SHA512_ALGORITHM = "HmacSHA512";
    private static final String DEFAULT_MNEMONIC_PASSPHRASE = "";
    private static final int HARDENED_KEY_MULTIPLIER = 0x80000000;
    private static final ECKeyEd25519 EC_KEY_FACTORY = new ECKeyEd25519();

    private static final int MACBYTES = Sodium.crypto_secretbox_macbytes();
    private static final int NONCEBYTES = Sodium.crypto_secretbox_noncebytes();
    private static final int KEYBYTES = Sodium.crypto_secretbox_keybytes();
    private static final byte[] NONCE = new byte[NONCEBYTES];

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
        Arrays.fill(NONCE, (byte) 0);
    }

    public static String getEncryptedText(final String mnemonic, final String password) throws ValidationException {
        final byte[] kdfPassword = getKDFPassword(password);
        final byte[] mnemonicBytes = mnemonic.getBytes(StandardCharsets.UTF_8);
        final byte[] encrypted = new byte[MACBYTES + mnemonicBytes.length];
        Sodium.crypto_secretbox_easy(encrypted, mnemonicBytes, mnemonicBytes.length, NONCE, kdfPassword);
        return new String(encrypted, StandardCharsets.ISO_8859_1);
    }

    public static String decryptMnemonic(final byte[] encryptedMnemonicBytes, final String password) throws ValidationException {
        final byte[] kdfPassword = getKDFPassword(password);
        final byte[] decrypted = new byte[encryptedMnemonicBytes.length - MACBYTES];
        final int result = Sodium.crypto_secretbox_open_easy(
                decrypted,
                encryptedMnemonicBytes,
                encryptedMnemonicBytes.length,
                NONCE,
                kdfPassword
        );
        if (result == 0) {
            return new String(decrypted, StandardCharsets.UTF_8);
        } else {
            throw new IllegalArgumentException("Unable to decrypt mnemonic");
        }
    }

    private static byte[] getKDFPassword(final String password) throws ValidationException {
        final PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA256Digest());
        final byte[] salt = getSha512(SALT_KEY, password.getBytes());
        gen.init(password.getBytes(StandardCharsets.UTF_8), salt, PBKDF2_ITERATIONS);
        return ((KeyParameter) gen.generateDerivedParameters(KEYBYTES * 8)).getKey();
    }
}
