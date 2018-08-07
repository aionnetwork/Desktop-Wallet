package org.aion.wallet.util;

import java.util.function.Consumer;

public final class OSUtils {
    private OSUtils() {}

    public static <T> void executeForOs(final Consumer<T> windowsConsumer, final Consumer<T> linuxConsumer, final Consumer<T> macConsumer, final T parameter) {
        final String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            windowsConsumer.accept(parameter);
        } else if (os.contains("nix") || os.contains("nux") || os.indexOf("aix") > 0) {
            linuxConsumer.accept(parameter);
        } else if (os.contains("Mac")) {
            macConsumer.accept(parameter);
        }
    }
}
