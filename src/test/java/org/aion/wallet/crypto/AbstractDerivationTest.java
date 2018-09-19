package org.aion.wallet.crypto;

import org.aion.base.util.TypeConverter;
import org.aion.crypto.ECKey;
import org.aion.crypto.ISignature;
import org.aion.crypto.ed25519.ECKeyEd25519;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.util.CryptoUtils;
import org.junit.Assert;

import static org.junit.Assert.assertEquals;

public class AbstractDerivationTest {

    protected static final int[] FIRST_AION_DERIVATION = {44, 425, 0, 0, 0};
    protected static final int[] FIRST_ETH_DERIVATION = {44, 60, 0, 0, 0};

    private static final byte[] MESSAGE = "dadakaaakaksadfasdfaasd8123".getBytes();

    protected final byte[] getRootKey(final String mnemonic) throws ValidationException {
        return CryptoUtils.getBip39ECKey(mnemonic).getPrivKeyBytes();
    }

    protected final void verifyECKey(final ECKey ecKey) {
        final ISignature sig = ecKey.sign(MESSAGE);
        Assert.assertTrue(ECKeyEd25519.verify(MESSAGE, sig.getSignature(), sig.getPubkey(null)));
    }

    protected final void verifyArraysEqual(final byte[] expected, final byte[] actual) {
        assertEquals(TypeConverter.toJsonHex(expected), TypeConverter.toJsonHex(actual));
    }
}
