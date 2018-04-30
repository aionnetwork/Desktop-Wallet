package org.aion.wallet.ui.components.partials;

import com.google.common.eventbus.Subscribe;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.TimerEvent;
import org.aion.wallet.util.DataUpdater;

import java.net.URL;
import java.util.ResourceBundle;

public class PeerCount implements Initializable{
    @FXML
    private Label peerCount;

    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        registerEventBusConsumer();
    }

    private void registerEventBusConsumer() {
        EventBusFactory.getBus(DataUpdater.UI_DATA_REFRESH).register(this);
    }

    @Subscribe
    private void handleConnectivityStatusEvent(TimerEvent event) {
         setPeerCount(blockchainConnector.getPeerCount());
    }

    private void setPeerCount(int numberOfPeers) {
        if(numberOfPeers == 1) {
            peerCount.setText(numberOfPeers + " peer");
            return;
        }
        peerCount.setText(numberOfPeers + " peers");
    }
}
