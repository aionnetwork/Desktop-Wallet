package org.aion.wallet.ui.components.partials;

import com.google.common.eventbus.Subscribe;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.ui.components.AbstractController;
import org.aion.wallet.ui.events.RefreshEvent;

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
        if(numberOfPeers == 1) {
            peerCount.setText(numberOfPeers + " peer");
            return;
        }
        peerCount.setText(numberOfPeers + " peers");
    }
}
