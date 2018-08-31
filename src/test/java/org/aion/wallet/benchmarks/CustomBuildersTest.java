package org.aion.wallet.benchmarks;

import org.aion.base.util.Hex;
import org.aion.wallet.util.CryptoUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class CustomBuildersTest {

    private static final String DEFAULT_DERIVATION_PATH = "058000002c800001a98000000080000000";
    private static final double MILLION = 1e6;
    private final int count;

    @BeforeClass
    public static void setUp() {
        CryptoUtils.preloadNatives();
        System.out.println("Begin: " + System.nanoTime() / MILLION);
    }

    public CustomBuildersTest(final int count) {
        this.count = count;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection primeNumbers() {
        return Arrays.asList(new Object[][]{
                {1},
                {100},
                {1000},
                {10000},
                {100000}
        });
    }

    @Test
    public void quickTest() {
        final long start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            final String hardenedIndex = Hex.toHexString(CryptoUtils.getHardenedNumber(i));
            final String s = DEFAULT_DERIVATION_PATH + hardenedIndex;
        }
        final long end = System.nanoTime();

        System.out.println((end - start) / MILLION);
    }

    @Test
    public void slowTest() {
        final long start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            BigInteger defaultDerivation = new BigInteger("80000000", 16);
            BigInteger pathIndex = new BigInteger(String.valueOf(i));
            final String s = DEFAULT_DERIVATION_PATH + defaultDerivation.or(pathIndex).toString(16);
        }
        final long end = System.nanoTime();

        System.out.println((end - start) / MILLION);
    }
}
