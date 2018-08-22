package org.aion.wallet.util;

import java.util.function.Consumer;

public final class OSUtils {
    private static String OS = System.getProperty("os.name").toLowerCase();
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

    public static boolean isWindows() {

        return (OS.contains("win"));

    }

    public static boolean isMac() {

        return (OS.contains("mac"));

    }

    public static boolean isUnix() {

        return (OS.contains("nix") || OS.contains("nux") || OS.indexOf("aix") > 0 );

    }
}
