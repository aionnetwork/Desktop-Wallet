package org.aion.wallet.ui.components;

import com.google.common.eventbus.Subscribe;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.console.ConsoleManager;
import org.aion.wallet.dto.LightAppSettings;
import org.aion.wallet.events.EventBusFactory;
import org.aion.wallet.events.EventPublisher;
import org.aion.wallet.events.HeaderPaneButtonEvent;
import org.aion.wallet.events.SettingsEvent;

import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;

public class SettingsController extends AbstractController {

    private static final String DEFAULT_PROTOCOL = "tcp";

    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();
    @FXML
    public TextField address;
    @FXML
    public TextField port;
    @FXML
    public Label notification;
    @FXML
    private TextField timeout;
    @FXML
    private ComboBox<String> timeoutMeasurementUnit;

    private LightAppSettings settings;

    @Override
    protected void internalInit(final URL location, final ResourceBundle resources) {
        reloadView();
    }

    @Override
    protected void registerEventBusConsumer() {
        EventBusFactory.getBus(HeaderPaneButtonEvent.ID).register(this);
        EventBusFactory.getBus(SettingsEvent.ID).register(this);
    }

    public void changeSettings() {
        final LightAppSettings newSettings;
        newSettings = new LightAppSettings(
                address.getText().trim(),
                port.getText().trim(),
                DEFAULT_PROTOCOL,
                settings.getType(),
                Integer.parseInt(timeout.getText()),
                getSelectedTimeoutMeasurementUnit()

        );
        displayNotification("", false);
        EventPublisher.fireApplicationSettingsChanged(newSettings);
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
            displayNotification("Changes applied", false);
            ConsoleManager.addLog("Settings updated", ConsoleManager.LogType.SETTINGS);
        }
    }

    private void reloadView() {
        settings = blockchainConnector.getSettings();
        address.setText(settings.getAddress());
        port.setText(settings.getPort());
        timeout.setText(String.valueOf(settings.getLockTimeout()));
        timeoutMeasurementUnit.setItems(getTimeoutMeasurementUnits());
        setInitialMeasurementUnit(settings.getLockTimeoutMeasurementUnit());
        displayNotification("", false);
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
}
