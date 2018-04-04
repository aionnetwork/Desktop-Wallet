package org.aion.wallet.ui.components.partials;

import com.google.common.eventbus.Subscribe;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.TimerEvent;
import org.aion.wallet.util.DataUpdater;

import java.net.URL;
import java.util.ResourceBundle;

public class ConnectivityStatus implements Initializable {

    private static final String CONNECTIVITY_STATUS_CONNECTED = "CONNECTED";
    private static final String CONNECTIVITY_STATUS_DISCONNECTED = "DISCONNECTED";

    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();

    @FXML
    private ImageView connectedImage;

    @FXML
    private ImageView disConnectedImage;

    @FXML
    private Label connectivityLabel;

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
        boolean connected = blockchainConnector.getConnectionStatusByConnectedPeers();
        setConnectivityImage(connected);
        setConnectivityLabel(connected);
    }

    private void setConnectivityImage(boolean connected) {
        if (connected) {
            connectedImage.setVisible(true);
            disConnectedImage.setVisible(false);
        } else {
            connectedImage.setVisible(false);
            disConnectedImage.setVisible(true);
        }
    }

    private void setConnectivityLabel(boolean connected) {
        if (connected) {
            connectivityLabel.setText(CONNECTIVITY_STATUS_CONNECTED);
        } else {
            connectivityLabel.setText(CONNECTIVITY_STATUS_DISCONNECTED);
        }
    }
}
