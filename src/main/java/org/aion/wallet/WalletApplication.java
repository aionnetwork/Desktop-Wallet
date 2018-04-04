package org.aion.wallet;

import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.ui.MainWindow;

public class WalletApplication {
    public static void main(String args[]) {
        System.setProperty(BlockchainConnector.WALLET_API_ENABLED_FLAG, "true");
        javafx.application.Application.launch(MainWindow.class, args);
    }
}
