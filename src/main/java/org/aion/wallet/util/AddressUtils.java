package org.aion.wallet.util;

public class AddressUtils {
    public static boolean isValid(String address) {
        return address != null && !address.equalsIgnoreCase("");
    }
}
