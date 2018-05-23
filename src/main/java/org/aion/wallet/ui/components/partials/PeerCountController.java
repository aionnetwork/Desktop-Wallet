package org.aion.wallet.ui.components.partials;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.events.RefreshEvent;
import org.aion.wallet.ui.components.AbstractController;

import java.net.URL;
import java.util.ResourceBundle;

public class PeerCountController extends AbstractController {
    @FXML
    private Label peerCount;

    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();

    @Override
    public void internalInit(final URL location, final ResourceBundle resources) {
    }

    @Override
    protected final void refreshView(final RefreshEvent event) {
        if (RefreshEvent.Type.TIMER.equals(event.getType())) {
            final Task<Integer> getPeerCountTask = getApiTask(o -> blockchainConnector.getPeerCount(), null);
            runApiTask(
                    getPeerCountTask,
                    evt -> setPeerCount(getPeerCountTask.getValue()),
                    getErrorEvent(throwable -> {}, getPeerCountTask),
                    getEmptyEvent()
            );
        }
    }

    private void setPeerCount(int numberOfPeers) {
        final String description;
        if (numberOfPeers == 1) {
            description = " peer";
        } else {
            description = " peers";
        }
        peerCount.setText(numberOfPeers + description);
    }
}
