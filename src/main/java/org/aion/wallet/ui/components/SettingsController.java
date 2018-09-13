package org.aion.wallet.ui.components;

import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.console.ConsoleManager;
import org.aion.wallet.dto.ConnectionDetails;
import org.aion.wallet.dto.ConnectionProvider;
import org.aion.wallet.dto.LightAppSettings;
import org.aion.wallet.events.*;

import java.net.URL;
import java.util.EnumSet;
import java.util.ResourceBundle;
import java.util.UUID;

public class SettingsController extends AbstractController {

    private static final String DEFAULT_PROTOCOL = "tcp";

    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();

    @FXML
    public Label notification;
    @FXML
    private TextField timeout;
    @FXML
    private ComboBox<ConnectionDetails> connectionDetailsComboBox;
    @FXML
    private TextField connectionName;
    @FXML
    private TextField connectionURL;
    @FXML
    private TextField connectionPort;
    @FXML
    private PasswordField connectionKey;
    @FXML
    private Button editConnectionButton;
    @FXML
    private Button saveConnectionButton;
    @FXML
    private Button deleteConnectionButton;
    @FXML
    private ComboBox<String> timeoutMeasurementUnit;

    private LightAppSettings settings;
    private ConnectionProvider connectionProvider;

    @Override
    protected void internalInit(final URL location, final ResourceBundle resources) {
        reloadView();
    }

    @Override
    protected void registerEventBusConsumer() {
        super.registerEventBusConsumer();
        EventBusFactory.getBus(SettingsEvent.ID).register(this);
        EventBusFactory.getBus(HeaderPaneButtonEvent.ID).register(this);
    }

    public void changeSettings() {
        final LightAppSettings newSettings;
        ConnectionDetails selectedConnection = connectionDetailsComboBox.getSelectionModel().getSelectedItem();
        if (isValidConnection(selectedConnection)) {
            newSettings = new LightAppSettings(
                    selectedConnection.getId(),
                    connectionName.getText().trim(),
                    selectedConnection.getAddress().trim(),
                    selectedConnection.getPort().trim(),
                    DEFAULT_PROTOCOL,
                    settings.getType(),
                    Integer.parseInt(timeout.getText()),
                    getSelectedTimeoutMeasurementUnit()

            );
            Platform.runLater(() -> EventPublisher.fireApplicationSettingsChanged(newSettings));
            displayNotification("", false);
        } else {
            displayNotification("The selected connection is invalid!", true);
        }
    }

    private boolean isValidConnection(final ConnectionDetails selectedConnection) {
        return selectedConnection != null &&
                selectedConnection.getAddress() != null && !selectedConnection.getAddress().isEmpty() &&
                selectedConnection.getPort() != null && !selectedConnection.getPort().isEmpty();
    }

    public void openConsole() {
        ConsoleManager.show();
    }

    private String getSelectedTimeoutMeasurementUnit() {
        String measurementUnit = null;
        switch (timeoutMeasurementUnit.getSelectionModel().getSelectedIndex()) {
            case 0:
                measurementUnit = "seconds";
                break;
            case 1:
                measurementUnit = "minutes";
                break;
            case 2:
                measurementUnit = "hours";
                break;
        }
        return measurementUnit;
    }

    @Subscribe
    private void handleHeaderPaneButtonEvent(final HeaderPaneButtonEvent event) {
        if (event.getType().equals(HeaderPaneButtonEvent.Type.SETTINGS)) {
            reloadView();
        }
    }

    @Subscribe
    private void handleSettingsChanged(final SettingsEvent event) {
        if (SettingsEvent.Type.APPLIED.equals(event.getType())) {
            Platform.runLater(() -> displayNotification("Changes applied", false));
            ConsoleManager.addLog("Settings updated", ConsoleManager.LogType.SETTINGS);
        }
    }

    private void reloadView() {
        settings = blockchainConnector.getSettings();
        connectionProvider = blockchainConnector.getConnectionKeyProvider();
        timeout.setText(String.valueOf(settings.getLockTimeout()));
        connectionDetailsComboBox.setItems(getConnectionDetails());
        connectionDetailsComboBox.getSelectionModel().select(settings.getConnectionDetails());
        connectionName.setText(connectionDetailsComboBox.getSelectionModel().getSelectedItem().getName());
        connectionURL.setText(connectionDetailsComboBox.getSelectionModel().getSelectedItem().getAddress());
        connectionPort.setText(connectionDetailsComboBox.getSelectionModel().getSelectedItem().getPort());
        connectionKey.setText(connectionDetailsComboBox.getSelectionModel().getSelectedItem().getSecureKey());
        connectionDetailsComboBox.valueProperty().addListener((observableValue, oldValue, newValue) -> {
            if (oldValue != null && newValue != null && !oldValue.equals(newValue)) {
                if (newValue.getName().equals("Add connection")) {
                    connectionName.setText("");
                    connectionName.setDisable(false);
                    connectionURL.setText("");
                    connectionURL.setDisable(false);
                    connectionPort.setText("");
                    connectionPort.setDisable(false);
                    connectionKey.setText("");
                    connectionKey.setDisable(false);
                    editConnectionButton.setVisible(false);
                    saveConnectionButton.setDisable(false);
                } else {
                    connectionName.setText(newValue.getName());
                    connectionName.setDisable(true);
                    connectionURL.setText(newValue.getAddress());
                    connectionURL.setDisable(true);
                    connectionPort.setText(newValue.getPort());
                    connectionPort.setDisable(true);
                    connectionKey.setText(newValue.getSecureKey());
                    connectionKey.setDisable(true);
                    editConnectionButton.setVisible(true);
                    editConnectionButton.setDisable(false);
                    saveConnectionButton.setDisable(true);
                }
            }
        });
        timeoutMeasurementUnit.setItems(getTimeoutMeasurementUnits());
        setInitialMeasurementUnit(settings.getLockTimeoutMeasurementUnit());
        displayNotification("", false);
    }

    @Override
    protected void refreshView(final RefreshEvent event) {
        if (EnumSet.of(RefreshEvent.Type.CONNECTED, RefreshEvent.Type.DISCONNECTED).contains(event.getType())) {
            displayNotification("", false);
        }
    }

    private void setInitialMeasurementUnit(String unlockTimeoutMeasurementUnit) {
        int initialIndex = 0;
        switch (unlockTimeoutMeasurementUnit) {
            case "seconds":
                initialIndex = 0;
                break;
            case "minutes":
                initialIndex = 1;
                break;
            case "hours":
                initialIndex = 2;
                break;
        }
        timeoutMeasurementUnit.getSelectionModel().select(initialIndex);
    }

    private ObservableList<String> getTimeoutMeasurementUnits() {
        return FXCollections.observableArrayList(
                "seconds",
                "minutes",
                "hours"
        );
    }

    private void displayNotification(final String message, final boolean isError) {
        if (isError) {
            notification.getStyleClass().add(ERROR_STYLE);
        } else {
            notification.getStyleClass().removeAll(ERROR_STYLE);
        }
        notification.setText(message);
    }

    private ObservableList<ConnectionDetails> getConnectionDetails() {
        ObservableList<ConnectionDetails> connectionDetails = FXCollections.observableArrayList();
        connectionDetails.addAll(connectionProvider.getAllConnections());
        connectionDetails.add(new ConnectionDetails("", "Add connection", null, null, null, ""));
        return connectionDetails;
    }

    public void editConnection() {
        connectionName.setDisable(false);
        connectionURL.setDisable(false);
        connectionPort.setDisable(false);
        connectionKey.setDisable(false);
        editConnectionButton.setDisable(true);
        saveConnectionButton.setDisable(false);
        deleteConnectionButton.setDisable(true);
    }

    public void saveConnection() {
        final String connectionNameText = connectionName.getText();
        final String connectionURLText = connectionURL.getText();
        final String connectionPortText = connectionPort.getText();
        final String connectionKeyText = connectionKey.getText();

        if (connectionNameText != null && !connectionNameText.isEmpty() &&
                connectionURLText != null && !connectionURLText.isEmpty() &&
                connectionPortText != null && !connectionPortText.isEmpty() &&
                connectionPortText.matches("[-+]?\\d*\\.?\\d+")) {


            ConnectionDetails selectedConnection = connectionDetailsComboBox.getSelectionModel().getSelectedItem();
            if (selectedConnection.getName().equals("Add connection")) {
                ConnectionDetails connectionDetails = new ConnectionDetails(UUID.randomUUID().toString(),
                        connectionNameText, DEFAULT_PROTOCOL,
                        connectionURLText, connectionPortText, connectionKeyText);
                connectionProvider.addConnection(connectionDetails);
            } else {
                ConnectionDetails connectionDetails = new ConnectionDetails(selectedConnection.getId(),
                        connectionNameText, DEFAULT_PROTOCOL,
                        connectionURLText, connectionPortText, connectionKeyText);
                connectionProvider.getAllConnections().remove(connectionDetailsComboBox.getSelectionModel().getSelectedItem());
                connectionProvider.getAllConnections().add(connectionDetails);
            }

            blockchainConnector.storeConnectionKeys(connectionProvider);
            connectionName.setDisable(true);
            connectionURL.setDisable(true);
            connectionPort.setDisable(true);
            connectionKey.setDisable(true);
            editConnectionButton.setDisable(false);
            saveConnectionButton.setDisable(true);
            deleteConnectionButton.setDisable(false);
            reloadView();
        } else {
            displayNotification("The connection details are invalid!", true);
        }
    }

    public void deleteConnection() {
        if (!connectionDetailsComboBox.getSelectionModel().getSelectedItem().getName().equals("Add connection")) {
            connectionProvider.getAllConnections().remove(connectionDetailsComboBox.getSelectionModel()
                    .getSelectedItem());
            blockchainConnector.storeConnectionKeys(connectionProvider);
            reloadView();
        }
    }
}
