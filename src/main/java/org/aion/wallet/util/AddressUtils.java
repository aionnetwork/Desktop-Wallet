package org.aion.wallet.util;

import org.aion.base.util.TypeConverter;

public class AddressUtils {

    public static boolean isValid(String address) {
        return address != null && !address.isEmpty();
    }

    public static boolean equals(String addrOne, String addrTwo) {
        return TypeConverter.toJsonHex(addrOne).equals(TypeConverter.toJsonHex(addrTwo));
    }
}
