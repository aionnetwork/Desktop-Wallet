package org.aion.wallet.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * @author Cristian Ilca, Centrys Inc.
 */
public final class BalanceUtils {

    public static final String CCY_SEPARATOR = " ";

    private static final int PRECISION = 18;

    private static final BigDecimal AION_MULTIPLIER = BigDecimal.valueOf(AionConstants.AION.longValue());

    private BalanceUtils() {}

    public static String formatBalance(final BigInteger balance) {
        return formatBalance(balance, true);
    }

    public static BigInteger extractBalance(final String formattedBalance) {
        return new BigDecimal(formattedBalance).multiply(AION_MULTIPLIER).toBigInteger();
    }

    public static String formatBalanceWithNumberOfDecimals(final BigInteger balance, final int decimalPlaces) {
        String formattedBalance = formatBalance(balance, false);
        if(formattedBalance.indexOf(".") > 0) {
            return formattedBalance.substring(0, formattedBalance.indexOf(".") + 1 + decimalPlaces);
        }
        return formattedBalance;
    }

    private static String formatBalance(final BigInteger balance, final boolean skipTrailingZeros) {
        if (BigInteger.ZERO.equals(balance) || balance == null) {
            return String.valueOf(0);
        }
        BigDecimal bigDecimalBalance = new BigDecimal(balance);
        BigDecimal decimal = bigDecimalBalance.divide(AION_MULTIPLIER, PRECISION, RoundingMode.HALF_EVEN);
        if (skipTrailingZeros) {
            decimal = decimal.stripTrailingZeros();
        }
        return decimal.toPlainString();
    }
}
