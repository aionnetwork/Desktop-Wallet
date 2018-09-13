package org.aion.wallet.util;

import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class FormatterTest {

    public static final int DECIMAL_PLACES = 6;

    @Test
    public void testBalanceFormatter() {
        final String stringBalance = "1016470000000000000";
        final BigInteger balanceValue = new BigInteger(stringBalance);
        final String formattedBalance = BalanceUtils.formatBalanceWithNumberOfDecimals(balanceValue, DECIMAL_PLACES);
        assertEquals("1.016470", formattedBalance);

        final String stringBalance2 = "2000000000000000000";
        final BigInteger balanceValue2 = new BigInteger(stringBalance2);
        final String formattedBalance2 = BalanceUtils.formatBalanceWithNumberOfDecimals(balanceValue2, DECIMAL_PLACES);
        assertEquals("2.000000", formattedBalance2);

        final String stringBalance3 = "2123456780000000000";
        final BigInteger balanceValue3 = new BigInteger(stringBalance3);
        final String formattedBalance3 = BalanceUtils.formatBalanceWithNumberOfDecimals(balanceValue3, DECIMAL_PLACES);
        assertEquals("2.123456", formattedBalance3);

        final String shortBalance = BalanceUtils.formatBalance(balanceValue2);
        assertEquals("2", shortBalance);
    }
}
