package org.aion.wallet;

import org.aion.wallet.ui.MainWindow;
import org.aion.wallet.util.ConfigUtils;

public class WalletApplication {
    public static void main(String args[]) {
        System.setProperty(ConfigUtils.WALLET_API_ENABLED_FLAG, "true");
        javafx.application.Application.launch(MainWindow.class, args);
    }
}
