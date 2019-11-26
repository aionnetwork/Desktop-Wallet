package org.aion.wallet.util;

import org.junit.Test;

public class URLManagerTest {
    @Test
    public void testAddressUrlFormatting() {
        // empty
        String addr = "";
        assert URLManager.checkAddress(addr) == false;

        // null
        addr = null;
        assert URLManager.checkAddress(addr) == false;

        // incorrect length
        addr = "0xa01d2fb2c000fcd6f934495b7c9c2b94eedbfb0f5edbfcd0f0d4938abd87da83";
        assert URLManager.checkAddress(addr) == false;

        // ambiguous character (there is a k in there)
        addr = "a01d2fb2c000fcd6f934495b7c9c2b94eedkfb0f5edbfcd0f0d4938abd87da83";
        assert URLManager.checkAddress(addr) == false;

        // doesnt begin with a0
        addr = "b01d2fb2c000fcd6f934495b7c9c2b94eedbfb0f5edbfcd0f0d4938abd87da83";
        assert URLManager.checkAddress(addr) == false;

        // happy path
        addr = "a01d2fb2c000fcd6f934495b7c9c2b94eedbfb0f5edbfcd0f0d4938abd87da83";
        assert URLManager.checkAddress(addr) == true;
    }
}
