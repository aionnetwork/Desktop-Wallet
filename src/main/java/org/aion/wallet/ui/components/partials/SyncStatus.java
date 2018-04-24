package org.aion.wallet.ui.components.partials;

import com.google.common.eventbus.Subscribe;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.dto.SyncInfoDTO;
import org.aion.wallet.ui.components.AbstractController;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.TimerEvent;
import org.aion.wallet.util.DataUpdater;
import org.aion.wallet.util.SyncStatusFormatter;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Function;

public class SyncStatus extends AbstractController {

    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();

    @FXML
    private Label progressBarLabel;

    @Override
    protected void internalInit(URL location, ResourceBundle resources) {}

    @Subscribe
    private void handleConnectivityStatusEvent(TimerEvent event) {
        final Task<SyncInfoDTO> getSyncInfoTask = getApiTask(o -> blockchainConnector.getSyncInfo(), null);
        runApiTask(
                getSyncInfoTask,
                evt -> setSyncStatus(getSyncInfoTask.getValue()),
                getErrorEvent(throwable -> {}, getSyncInfoTask),
                getEmptyEvent()
        );
    }

    private void setSyncStatus(SyncInfoDTO syncInfo) {
        progressBarLabel.setText(SyncStatusFormatter.formatSyncStatusByBlockNumbers(syncInfo));
    }
}
