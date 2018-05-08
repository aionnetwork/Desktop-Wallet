package org.aion.wallet.util;

public class ConfigUtils {

    public static final String WALLET_API_ENABLED_FLAG = "wallet.api.enabled";

    private static final int THREAD_SEARCH_COUNT = 4;

    public static boolean isEmbedded() {
        return !Boolean.valueOf(System.getProperty(WALLET_API_ENABLED_FLAG));
    }

    public static int getTransactionSearchThreadCount() {
        return THREAD_SEARCH_COUNT;
    }
}
