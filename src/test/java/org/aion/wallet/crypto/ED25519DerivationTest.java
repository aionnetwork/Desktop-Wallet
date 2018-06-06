package org.aion.wallet.crypto;

import io.github.novacrypto.bip39.SeedCalculator;
import org.aion.base.util.TypeConverter;
import org.aion.crypto.ECKey;
import org.junit.Assert;
import org.junit.Test;

public class ED25519DerivationTest {
    private static final String DEFAULT_MNEMONIC = "blade gauge short pledge view sort onion anger network two lunar leopard";
    private static final String GENERATED_PUBLIC_ROOT = "0xa0f6b716e560cadc7fbcb29d3d964e74a01a05acd358d0d17f22297c817c7499";
    private static final String DEFAULT_MNEMONIC_SALT = "";

    private ECKey getRootKey() {
        final byte[] seed = new SeedCalculator().calculateSeed(DEFAULT_MNEMONIC, DEFAULT_MNEMONIC_SALT);
        return new SeededECKeyEd25519(seed);
    }


    @Test
    public void testMnemonicSeedDerivation() throws Exception {
        ECKey ecKey = getRootKey();
        Assert.assertEquals(GENERATED_PUBLIC_ROOT, TypeConverter.toJsonHex(ecKey.computeAddress(ecKey.getPubKey())));
        ExtendedKey root = new ExtendedKey(ecKey);

        ExtendedKey childOne = root.deriveHardened(new int[]{44, 60, 0, 0});
        ExtendedKey childTwo = root.deriveHardened(new int[]{44, 60, 0, 0});

        Assert.assertEquals(TypeConverter.toJsonHex(childOne.getEcKey().computeAddress(childOne.getEcKey().getPubKey())),
                TypeConverter.toJsonHex(childTwo.getEcKey().computeAddress(childTwo.getEcKey().getPubKey())));
        Assert.assertEquals(TypeConverter.toJsonHex(childOne.getEcKey().getPrivKeyBytes()),
                TypeConverter.toJsonHex(childTwo.getEcKey().getPrivKeyBytes()));
    }
}
