package org.aion.wallet.crypto;

import org.aion.base.util.TypeConverter;
import org.aion.crypto.ECKey;
import org.aion.wallet.exception.ValidationException;
import org.junit.Test;

public class ED25519DerivationTest extends AbstractDerivationTest {

    private static final String DEFAULT_MNEMONIC = "empower april ill spoon grab fringe vehicle river dragon have forget today";

    private final MasterKey root = new MasterKey(getRootKey(DEFAULT_MNEMONIC));

    public ED25519DerivationTest() throws ValidationException {}

    @Test
    public void testDerivationIsCorrect() throws Exception {
        final String expectedPubKey = "0xe77ba23bc030cbca2fa5c3cee25ea4ae851e947a1d4d0e54fe9ccced9f339b19";
        final String expectedSecKey = "0xca2327b0c60dc5573825fc16ac3accdb3009d1eda6b46f78212cfad0726483dbe77ba23bc030cbca2fa5c3cee25ea4ae851e947a1d4d0e54fe9ccced9f339b19";

        final ECKey child = root.deriveHardened(FIRST_ETH_DERIVATION);
        verifyECKey(child);

        verifyArraysEqual(TypeConverter.StringHexToByteArray(expectedPubKey), child.getPubKey());
        verifyArraysEqual(TypeConverter.StringHexToByteArray(expectedSecKey), child.getPrivKeyBytes());
    }

    @Test
    public void testMnemonicSeedDerivationIsConsistent() throws Exception {
        final ECKey childOne = root.deriveHardened(new int[]{44, 60, 0, 0, 14});
        verifyECKey(childOne);

        final ECKey childTwo = root.deriveHardened(new int[]{44, 60, 0, 0, 14});
        verifyECKey(childTwo);

        verifyECKeysEqual(childOne, childTwo);
    }

    private void verifyECKeysEqual(final ECKey childOne, final ECKey childTwo) {
        verifyArraysEqual(childOne.getPrivKeyBytes(), childTwo.getPrivKeyBytes());
        verifyArraysEqual(childOne.getPubKey(), childTwo.getPubKey());
        verifyArraysEqual(childOne.getAddress(), childTwo.getAddress());
    }

}
