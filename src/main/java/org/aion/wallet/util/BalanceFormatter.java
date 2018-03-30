package org.aion.wallet.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * @author Cristian Ilca, Centrys Inc.
 */
public class BalanceFormatter {
    public static final BigDecimal WEI_MULTIPLIER = BigDecimal.valueOf(1000000000000000000L);
    private static final BigInteger BILLION = BigInteger.valueOf(1000000000);

    public static String formatBalance(BigInteger balance) {
        BigDecimal bigDecimalBalance = new BigDecimal(balance);
        BigDecimal decimalPlaces = new BigDecimal(BILLION.multiply(BILLION));

        return String.valueOf(bigDecimalBalance.divide(decimalPlaces, 10, RoundingMode.HALF_EVEN));
    }

    public static BigInteger extractBalance(String formattedBalance) {
        return new BigDecimal(formattedBalance).multiply(WEI_MULTIPLIER).toBigInteger();
    }
}
