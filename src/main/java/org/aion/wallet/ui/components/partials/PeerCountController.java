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

    private static final String ONE = " peer";

    private static final String MANY = " peers";

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

    private void setPeerCount(final int numberOfPeers) {
        peerCount.setText(numberOfPeers + numberOfPeers == 1 ? ONE : MANY);
    }
}
