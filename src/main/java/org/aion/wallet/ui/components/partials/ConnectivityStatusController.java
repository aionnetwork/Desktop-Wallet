package org.aion.wallet.ui.components.partials;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.events.RefreshEvent;
import org.aion.wallet.ui.components.AbstractController;

import java.net.URL;
import java.util.EnumSet;
import java.util.ResourceBundle;

public class ConnectivityStatusController extends AbstractController {

    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();

    @FXML
    private Label connectivityLabel;

    @Override
    public void internalInit(final URL location, final ResourceBundle resources) {}

    @Override
    protected final void refreshView(final RefreshEvent event) {
        if (RefreshEvent.Type.TIMER.equals(event.getType())) {
            final Task<Boolean> getConnectedStatusTask = getApiTask(o -> blockchainConnector.getConnectionStatus(), null);
            runApiTask(
                    getConnectedStatusTask,
                    evt -> updateConnectionStatus(getConnectedStatusTask.getValue()),
                    getErrorEvent(t -> {}, getConnectedStatusTask),
                    getEmptyEvent());
        } else if (EnumSet.of(RefreshEvent.Type.CONNECTING, RefreshEvent.Type.CONNECTED, RefreshEvent.Type.DISCONNECTING, RefreshEvent.Type.DISCONNECTED).contains(event.getType())) {
            updateConnectivityLabel(event);
        }
    }

    private void updateConnectivityLabel(final RefreshEvent connected) {
        connectivityLabel.setText(String.valueOf(connected.getType()));
        connected.getPayload().ifPresent(isSecuredConnection -> {
//            hint to user that connection is secured (maybe a different color for the label)
//            payload only exists for CONNECTING and CONNECTED types
        });
    }

    private void updateConnectionStatus(final boolean isConnected) {
        if (isConnected) {
            if (connectivityLabel.getText().equals(RefreshEvent.Type.DISCONNECTED.toString())) {
                connectivityLabel.setText(RefreshEvent.Type.CONNECTED.toString());
            }
        } else {
            if (connectivityLabel.getText().equals(RefreshEvent.Type.CONNECTED.toString())) {
                connectivityLabel.setText(RefreshEvent.Type.DISCONNECTED.toString());
            }
        }
    }
}
