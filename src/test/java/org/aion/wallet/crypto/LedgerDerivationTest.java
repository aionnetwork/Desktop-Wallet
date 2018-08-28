package org.aion.wallet.crypto;

import org.aion.base.util.TypeConverter;
import org.aion.crypto.ECKey;
import org.aion.wallet.exception.ValidationException;
import org.junit.Test;

public class LedgerDerivationTest extends AbstractDerivationTest {

    private static final String GURDEEP_MNEMONIC = "pole museum pill kit hood museum style very mirror dash marble van " +
            "satisfy payment ring unlock require peanut able symbol toy hint frame digital";

    private static final String OFFICE_MNEMONIC = "neither buzz february rocket couch inhale rocket guitar thrive " +
            "busy tank milk ring fantasy exclude squirrel castle dose hedgehog suggest maze shy differ cute";

    private final MasterKey gurdeepRoot = new MasterKey(getRootKey(GURDEEP_MNEMONIC));
    private final MasterKey officeRoot = new MasterKey(getRootKey(OFFICE_MNEMONIC));

    public LedgerDerivationTest() throws ValidationException {}

    @Test
    public void testGurdeepDerivation() throws Exception {
        final String expectedPubKey = "0xb808763388bc601f5138e310c0b80c0db1efc9f9fb107697c209fe1e2d698e96";

        final ECKey child = gurdeepRoot.deriveHardened(FIRST_AION_DERIVATION);
        verifyECKey(child);

        verifyArraysEqual(TypeConverter.StringHexToByteArray(expectedPubKey), child.getPubKey());
    }

    @Test
    public void testOfficeDerivation() throws Exception {
        final String expectedPubKey = "0x27df1cc71c6dba7ee8c40d40a0d48c838332d131d34874920258efb37a4050a4";
        final String expectedAddress = "0xa0849f3732b70b7c1075eddb4a99d643c59b3627adeac642f589de999154cac5";

        final ECKey child = officeRoot.deriveHardened(FIRST_AION_DERIVATION);
        verifyECKey(child);

        verifyArraysEqual(TypeConverter.StringHexToByteArray(expectedPubKey), child.getPubKey());
        verifyArraysEqual(TypeConverter.StringHexToByteArray(expectedAddress), child.getAddress());
    }
}
