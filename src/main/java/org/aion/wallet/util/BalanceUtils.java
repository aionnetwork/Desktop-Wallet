package org.aion.wallet.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * @author Cristian Ilca, Centrys Inc.
 */
public class BalanceUtils {
    private static final BigDecimal WEI_MULTIPLIER = BigDecimal.valueOf(1000000000000000000L);

    public static String formatBalance(final BigInteger balance) {
        if (BigInteger.ZERO.equals(balance)) {
            return String.valueOf(0);
        }
        BigDecimal bigDecimalBalance = new BigDecimal(balance);
        return String.valueOf(bigDecimalBalance.divide(WEI_MULTIPLIER, 10, RoundingMode.HALF_EVEN));
    }

    public static BigInteger extractBalance(final String formattedBalance) {
        return new BigDecimal(formattedBalance).multiply(WEI_MULTIPLIER).toBigInteger();
    }

    //TODO will be done in future story
    public static String convertBalance() {
        return null;
    }
}
