package org.aion.wallet.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class OSUtils {

    private static final String WIN = "win";
    private static final String MAC = "mac";
    private static final String NIX = "nix";
    private static final String NUX = "nux";
    private static final String AIX = "aix";

    private static String OS = System.getProperty("os.name").toLowerCase();

    private OSUtils() {}

    public static <T> void executeForOs(
            final Consumer<T> windowsConsumer,
            final Consumer<T> macConsumer,
            final Consumer<T> linuxConsumer,
            final T parameter
    ) {
        if (isWindows()) {
            windowsConsumer.accept(parameter);
        } else if (isMac()) {
            macConsumer.accept(parameter);
        } else if (isUnix()) {
            linuxConsumer.accept(parameter);
        }
    }

    public static <T, R> R getForOs(
            final Function<T, R> windowsFunction,
            final Function<T, R> macFunction,
            final Function<T, R> linuxFunction,
            final T parameter
    ) {
        if (isWindows()) {
            return windowsFunction.apply(parameter);
        } else if (isMac()) {
            return macFunction.apply(parameter);
        } else if (isUnix()) {
            return linuxFunction.apply(parameter);
        }
        throw new IllegalStateException("Unknown OS: " + OS);
    }

    public static <R> R getForOs(
            final Supplier<R> windowsSupplier,
            final Supplier<R> macSupplier,
            final Supplier<R> linuxSupplier
    ) {
        if (isWindows()) {
            return windowsSupplier.get();
        } else if (isMac()) {
            return macSupplier.get();
        } else if (isUnix()) {
            return linuxSupplier.get();
        }
        throw new IllegalStateException("Unknown OS: " + OS);
    }

    public static boolean isWindows() {
        return (OS.contains(WIN));
    }

    public static boolean isMac() {
        return (OS.contains(MAC));
    }

    public static boolean isUnix() {
        return (OS.contains(NIX) || OS.contains(NUX) || OS.indexOf(AIX) > 0);
    }
}
