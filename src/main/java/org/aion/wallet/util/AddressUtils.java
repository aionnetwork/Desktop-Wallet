package org.aion.wallet.util;

import org.aion.base.util.TypeConverter;

public final class AddressUtils {

    private AddressUtils() {}

    public static boolean isValid(final String address) {
        return address != null && !address.isEmpty() && isAionAddress(address);
    }

    public static boolean equals(final String addrOne, final String addrTwo) {
        return TypeConverter.toJsonHex(addrOne).equals(TypeConverter.toJsonHex(addrTwo));
    }

    private static boolean isAionAddress(final String address) {
        final boolean isFull = address.startsWith("0xa") && address.length() == 66;
        final boolean isStripped = address.startsWith("a") && address.length() == 64;
        final String strippedAddress = isFull ? address.substring(2) : (isStripped ? address : "");
        return strippedAddress.matches("[0-9a-fA-F]+");
    }
}
