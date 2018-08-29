package org.aion.wallet.crypto;

import org.aion.base.util.TypeConverter;
import org.aion.crypto.ECKey;
import org.aion.wallet.dto.AccountType;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.hardware.AionAccountDetails;
import org.aion.wallet.hardware.HardwareWallet;
import org.aion.wallet.hardware.HardwareWalletFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class LedgerDerivationTest extends AbstractDerivationTest {

    private static final String GURDEEP_MNEMONIC = "pole museum pill kit hood museum style very mirror dash marble van " +
            "satisfy payment ring unlock require peanut able symbol toy hint frame digital";
    private static final String GURDEEP_PUB = "0xb808763388bc601f5138e310c0b80c0db1efc9f9fb107697c209fe1e2d698e96";
    private static final String GURDEEP_ADDRESS = "0xa0208c72fad2b444b7d7195bd0452c0f6c2f34cfa94a3691c1637da46430d196";

    private static final String OFFICE_MNEMONIC = "neither buzz february rocket couch inhale rocket guitar thrive " +
            "busy tank milk ring fantasy exclude squirrel castle dose hedgehog suggest maze shy differ cute";
    private static final String OFFICE_PUB = "0x27df1cc71c6dba7ee8c40d40a0d48c838332d131d34874920258efb37a4050a4";
    private static final String OFFICE_ADDRESS = "0xa0849f3732b70b7c1075eddb4a99d643c59b3627adeac642f589de999154cac5";

    private final MasterKey root;
    private final String expectedPubKey;
    private final String expectedAddress;


    public LedgerDerivationTest(final String name, final String mnemonic, final String expectedPubKey, final String expectedAddress) throws ValidationException {
        this.root = new MasterKey(getRootKey(mnemonic));
        this.expectedPubKey = expectedPubKey;
        this.expectedAddress = expectedAddress;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection data() {
        return Arrays.asList(new Object[][]{
                {"gurdeep", GURDEEP_MNEMONIC, GURDEEP_PUB, GURDEEP_ADDRESS},
                {"office", OFFICE_MNEMONIC, OFFICE_PUB, OFFICE_ADDRESS},
        });
    }

    @Test
    public void testDerivation() throws Exception {

        final ECKey child = root.deriveHardened(FIRST_AION_DERIVATION);
        verifyECKey(child);

        verifyArraysEqual(TypeConverter.StringHexToByteArray(expectedPubKey), child.getPubKey());
        verifyArraysEqual(TypeConverter.StringHexToByteArray(expectedAddress), child.getAddress());

        final HardwareWallet hardwareWallet = HardwareWalletFactory.getHardwareWallet(AccountType.LEDGER);
        if (hardwareWallet.isConnected()) {
            final AionAccountDetails accountDetails = hardwareWallet.getAccountDetails(0);
            assertEquals(expectedPubKey, accountDetails.getPublicKey());
            assertEquals(expectedAddress, accountDetails.getAddress());
        }
    }
}
