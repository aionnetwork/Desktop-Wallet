package org.aion.wallet.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * @author Cristian Ilca, Centrys Inc.
 */
public class BalanceFormatter {

    private static final BigInteger BILLION = BigInteger.valueOf(1000000000);

    public static String formatBalance(BigInteger balance) {
        BigDecimal bigDecimalBalance = new BigDecimal(balance);
        BigDecimal decimalPlaces = new BigDecimal(BILLION.multiply(BILLION));

        return String.valueOf(bigDecimalBalance.divide(decimalPlaces, 10, RoundingMode.HALF_EVEN));
    }
}
