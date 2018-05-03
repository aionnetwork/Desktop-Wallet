package org.aion.wallet.ui.components.partials;

import com.google.common.eventbus.Subscribe;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.ui.components.AbstractController;
import org.aion.wallet.ui.events.TimerEvent;

import java.net.URL;
import java.util.ResourceBundle;

public class PeerCount extends AbstractController {
    @FXML
    private Label peerCount;

    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();

    @Override
    public void internalInit(URL location, ResourceBundle resources) {
        registerEventBusConsumer();
    }

    @Subscribe
    private void handleConnectivityStatusEvent(TimerEvent event) {
        final Task<Integer> getSyncInfoTask = getApiTask(o -> blockchainConnector.getPeerCount(), null);
        runApiTask(
                getSyncInfoTask,
                evt -> setPeerCount(blockchainConnector.getPeerCount()),
                getErrorEvent(throwable -> {}, getSyncInfoTask),
                getEmptyEvent()
        );
    }

    private void setPeerCount(int numberOfPeers) {
        if(numberOfPeers == 1) {
            peerCount.setText(numberOfPeers + " peer");
            return;
        }
        peerCount.setText(numberOfPeers + " peers");
    }
}
