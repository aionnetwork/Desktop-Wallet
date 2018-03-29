package org.aion.wallet.ui.components.partials;

import com.google.common.eventbus.Subscribe;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import org.aion.api.server.types.SyncInfo;
import org.aion.wallet.WalletApi;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.TimerEvent;
import org.aion.wallet.util.DataUpdater;
import org.aion.wallet.util.SyncStatusFormatter;

import java.net.URL;
import java.util.ResourceBundle;

public class SyncStatus implements Initializable {

    private WalletApi walletApi = new WalletApi();

    @FXML
    private ProgressBar syncProgressBar;

    @FXML
    private Label progressBarLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        registerEventBusConsumer();
    }

    private void registerEventBusConsumer() {
        final EventBusFactory eventBusFactory = EventBusFactory.getInstance();
        eventBusFactory.getBus(DataUpdater.FOOTER_BUS_EVENT_ID).register(this);
    }

    @Subscribe
    private void handleConnectivityStatusEvent(TimerEvent event) {
        SyncInfo syncInfo = walletApi.getSync();
        setSyncBarProgress(syncInfo);
        setSyncStatus(syncInfo);
    }

    private void setSyncStatus(SyncInfo syncInfo) {
        progressBarLabel.setText(SyncStatusFormatter.formatSyncStatus(syncInfo));
    }

    private void setSyncBarProgress(SyncInfo syncInfo) {
        if (syncInfo != null && syncInfo.networkBestBlkNumber > 0) {
            syncProgressBar.setProgress((double) syncInfo.chainBestBlkNumber / syncInfo.networkBestBlkNumber);
        }
    }
}
