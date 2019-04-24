package org.aion.wallet.config;

import java.util.Optional;

public class Env {

    public static final String KERNEL_URI = getOrDefault("KERNEL_URI", "tcp://127.0.0.1:8547");


    @SuppressWarnings("SameParameterValue")
    private static String getOrDefault(final String envName, final String defaultVal){
        return Optional.ofNullable(System.getenv(envName)).orElse(defaultVal);
    }
}
