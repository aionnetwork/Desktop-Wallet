package org.aion.wallet.util;

public final class ConfigUtils {

    public static final String WALLET_API_ENABLED_FLAG = "wallet.api.enabled";

    private ConfigUtils() {}

    public static boolean isEmbedded() {
        return !Boolean.valueOf(System.getProperty(WALLET_API_ENABLED_FLAG));
    }
}
