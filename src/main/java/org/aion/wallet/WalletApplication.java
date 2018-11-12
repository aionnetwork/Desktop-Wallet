package org.aion.wallet;

import org.aion.wallet.ui.MainWindow;
import org.aion.wallet.util.ConfigUtils;

public class WalletApplication {
    public static void main(String args[]) {
        // Freebie to get better anti-aliased fonts
        // see: https://stackoverflow.com/questions/24254000/how-to-force-anti-aliasing-in-javafx-fonts
        System.setProperty("prism.lcdtext", "false");
        System.setProperty(ConfigUtils.WALLET_API_ENABLED_FLAG, "true");
        javafx.application.Application.launch(MainWindow.class, args);
    }
}
