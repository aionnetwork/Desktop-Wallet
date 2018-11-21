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

    private static final String OFFICE_MNEMONIC = "ritual deer guilt mountain raw zone host notable silk garden casual fun balance sad enemy error pass notice world mule happy hotel erase provide";
    private static final String OFFICE_PUB = "0x38810118c4cab7df9dba0d90dba652cf00162ac9e29a52ef6641fc8985db9195";
    private static final String OFFICE_ADDRESS = "0xa0badae73c15e1e118049e520c0b9f2e8e11eb0585f9c3d869f59352320e43ff";

    private static final String OFFICE_PUB_1 = "0x6619dea9a4770d33e29407c5ac274ddc5af0288ce7a30a36220318e9d0ebb50c";
    private static final String OFFICE_ADDRESS_1 = "0xa0f90129a9f5787574268be44895298992063d74f332a907ac2c531f9b24bcbe";

    private static final String OFFICE_PUB_2 = "0x10f449a784830cb78e57b681986de1171f8cb05ea3430e32ece74fddbb9b4766";
    private static final String OFFICE_ADDRESS_2 = "0xa0b48366e08e94f6a350d9e60e41e14e3b8bc0922037d8837b38f70f3e3c166f";

    private final MasterKey root;
    private final String expectedPubKey;
    private final String expectedAddress;
    private final int derivationIndex;


    public LedgerDerivationTest(final String name, final String mnemonic, final String expectedPubKey, final String expectedAddress, final int derivationIndex) throws ValidationException {
        this.root = new MasterKey(getRootKey(mnemonic));
        this.expectedPubKey = expectedPubKey;
        this.expectedAddress = expectedAddress;
        this.derivationIndex = derivationIndex;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection data() {
        return Arrays.asList(new Object[][]{
//                {"gurdeep", GURDEEP_MNEMONIC, GURDEEP_PUB, GURDEEP_ADDRESS, 0},
                {"office", OFFICE_MNEMONIC, OFFICE_PUB, OFFICE_ADDRESS, 0},
                {"office1", OFFICE_MNEMONIC, OFFICE_PUB_1, OFFICE_ADDRESS_1, 1},
                {"office2", OFFICE_MNEMONIC, OFFICE_PUB_2, OFFICE_ADDRESS_2, 2}
        });
    }

    @Test
    public void testDerivation() throws Exception {

        final ECKey child = root.deriveHardened(new int[]{44, 425, 0, 0, derivationIndex});
        verifyECKey(child);

        verifyArraysEqual(TypeConverter.StringHexToByteArray(expectedPubKey), child.getPubKey());
        verifyArraysEqual(TypeConverter.StringHexToByteArray(expectedAddress), child.getAddress());

        final HardwareWallet hardwareWallet = HardwareWalletFactory.getHardwareWallet(AccountType.LEDGER);
        if (hardwareWallet.isConnected()) {
            final AionAccountDetails accountDetails = hardwareWallet.getAccountDetails(derivationIndex);
            assertEquals(expectedPubKey, accountDetails.getPublicKey());
            assertEquals(expectedAddress, accountDetails.getAddress());
        }
    }
}
