package org.aion.wallet.ui.components.partials;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.events.RefreshEvent;
import org.aion.wallet.ui.components.AbstractController;

import java.net.URL;
import java.util.EnumSet;
import java.util.ResourceBundle;

public class ConnectivityStatusController extends AbstractController {

    private static final Tooltip CONNECTION_ENCRYPTED = new Tooltip("Connection encrypted");
    private static final Tooltip CONNECTION_UNENCRYPTED = new Tooltip("Connection not encrypted");
    private static final Tooltip CONNECTION_MISSING = new Tooltip("Connection not established");

    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();

    @FXML
    private Label connectivityLabel;

    @Override
    public void internalInit(final URL location, final ResourceBundle resources) {}

    @Override
    protected final void refreshView(final RefreshEvent event) {
        if (RefreshEvent.Type.TIMER.equals(event.getType())) {
            final Task<Boolean> getConnectedStatusTask = getApiTask(o -> blockchainConnector.isConnected(), null);
            runApiTask(
                    getConnectedStatusTask,
                    evt -> updateConnectionStatus(getConnectedStatusTask.getValue()),
                    getErrorEvent(t -> {}, getConnectedStatusTask),
                    getEmptyEvent());
        } else if (EnumSet.of(RefreshEvent.Type.CONNECTING, RefreshEvent.Type.CONNECTED, RefreshEvent.Type.DISCONNECTING, RefreshEvent.Type.DISCONNECTED).contains(event.getType())) {
            Platform.runLater(() -> updateConnectivityLabel(event));
        }
    }

    private void updateConnectivityLabel(final RefreshEvent connected) {
        connectivityLabel.setText(String.valueOf(connected.getType()));
        final ObservableList<String> styleClass = connectivityLabel.getStyleClass();
        connected.getPayload().ifPresentOrElse(
                secure -> setConnectionLabelStyle(styleClass, secure),
                () -> setNotConnectedStyle(styleClass)
        );
    }

    private void setConnectionLabelStyle(final ObservableList<String> styleClass, final Boolean secure) {
        if (secure) {
            setConnectedStyle(styleClass);
        } else {
            setConnectedInsecureStyle(styleClass);
        }
    }

    private void setConnectedStyle(final ObservableList<String> styleClass) {
        styleClass.clear();
        styleClass.add("connected-label");
        Tooltip.install(connectivityLabel, CONNECTION_ENCRYPTED);
    }

    private void setConnectedInsecureStyle(final ObservableList<String> styleClass) {
        styleClass.clear();
        styleClass.add("connected-insecure");
        Tooltip.install(connectivityLabel, CONNECTION_UNENCRYPTED);
    }

    private void setNotConnectedStyle(final ObservableList<String> styleClass) {
        styleClass.clear();
        styleClass.add("not-connected");
        Tooltip.install(connectivityLabel, CONNECTION_MISSING);
    }

    private void updateConnectionStatus(final boolean isConnected) {
        if (!isConnected) {
            if (connectivityLabel.getText().equals(RefreshEvent.Type.CONNECTED.toString())) {
                connectivityLabel.setText(RefreshEvent.Type.DISCONNECTED.toString());
                setNotConnectedStyle(connectivityLabel.getStyleClass());
            }
        }
    }
}
