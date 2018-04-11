package org.aion.wallet.ui.components.partials;

import com.google.common.eventbus.Subscribe;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.dto.SyncInfoDTO;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.TimerEvent;
import org.aion.wallet.util.DataUpdater;
import org.aion.wallet.util.SyncStatusFormatter;

import java.net.URL;
import java.util.ResourceBundle;

public class SyncStatus implements Initializable {

    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();

    @FXML
    private Label progressBarLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        registerEventBusConsumer();
    }

    private void registerEventBusConsumer() {
        EventBusFactory.getInstance().getBus(DataUpdater.FOOTER_BUS_EVENT_ID).register(this);
    }

    @Subscribe
    private void handleConnectivityStatusEvent(TimerEvent event) {
        SyncInfoDTO syncInfo = blockchainConnector.getSyncInfo();
        setSyncStatus(syncInfo);
    }

    private void setSyncStatus(SyncInfoDTO syncInfo) {
        progressBarLabel.setText(SyncStatusFormatter.formatSyncStatusByBlockNumbers(syncInfo));
    }
}
