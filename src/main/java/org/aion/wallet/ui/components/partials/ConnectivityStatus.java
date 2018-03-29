package org.aion.wallet.ui.components.partials;

import com.google.common.eventbus.Subscribe;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.aion.wallet.WalletApi;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.TimerEvent;
import org.aion.wallet.util.DataUpdater;

import java.net.URL;
import java.util.ResourceBundle;

public class ConnectivityStatus implements Initializable {

    public static final String CONNECTIVITY_STATUS_CONNECTED = "CONNECTED";
    public static final String CONNECTIVITY_STATUS_DISCONNECTED = "DISCONNECTED";
    @FXML
    private ImageView connectedImage;

    @FXML
    private ImageView disConnectedImage;

    @FXML
    private Label connectivityLabel;

    private WalletApi walletApi = new WalletApi();

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
        int peerCount = walletApi.peerCount();
        setConnectivityImage(peerCount);
        setConnectivityLabel(peerCount);
    }

    private void setConnectivityImage(int peerCount) {
        if (peerCount > 0) {
            connectedImage.setVisible(true);
            disConnectedImage.setVisible(false);
        } else {
            connectedImage.setVisible(false);
            disConnectedImage.setVisible(true);
        }
    }

    public void setConnectivityLabel(int peerCount) {
        if (peerCount > 0) {
            connectivityLabel.setText(CONNECTIVITY_STATUS_CONNECTED);
        } else {
            connectivityLabel.setText(CONNECTIVITY_STATUS_DISCONNECTED);
        }
    }
}
