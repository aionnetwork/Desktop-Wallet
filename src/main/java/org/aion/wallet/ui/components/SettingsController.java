package org.aion.wallet.ui.components;

import com.google.common.eventbus.Subscribe;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.aion.api.log.LogEnum;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.dto.LightAppSettings;
import org.aion.wallet.events.EventBusFactory;
import org.aion.wallet.events.EventPublisher;
import org.aion.wallet.events.HeaderPaneButtonEvent;
import org.aion.wallet.events.SettingsEvent;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.log.WalletLoggerFactory;
import org.slf4j.Logger;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController extends AbstractController {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();

    @FXML
    public TextField protocol;
    @FXML
    public TextField address;
    @FXML
    public TextField port;
    @FXML
    private TextField timeout;
    @FXML
    private ComboBox timeoutMeasurementUnit;
    @FXML
    public Label notification;

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
        try {
            newSettings = new LightAppSettings(
                    address.getText().trim(),
                    port.getText().trim(),
                    protocol.getText().trim(),
                    settings.getType(),
                    Integer.parseInt(timeout.getText()),
                    getSelectedTimeoutMeasurementUnit()

            );
            displayNotification("", false);
            EventPublisher.fireApplicationSettingsChanged(newSettings);
        } catch (ValidationException e) {
            log.error(e.getMessage(), e);
            displayNotification(e.getMessage(), true);
        }
    }

    private String getSelectedTimeoutMeasurementUnit() {
        String measurementUnit = null;
        switch (timeoutMeasurementUnit.getSelectionModel().getSelectedIndex()) {
            case 0 :
                measurementUnit = "seconds";
                break;
            case 1 :
                measurementUnit = "minutes";
                break;
            case 2 :
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
        }
    }

    private void reloadView() {
        settings = blockchainConnector.getSettings();
        protocol.setText(settings.getProtocol());
        address.setText(settings.getAddress());
        port.setText(settings.getPort());
        timeout.setText(settings.getUnlockTimeout().toString());
        timeoutMeasurementUnit.setItems(getTimeoutMeasurementUnits());
        setInitialMeasurementUnit(settings.getLockTimeoutMeasurementUnit());
        displayNotification("", false);
    }

    private void setInitialMeasurementUnit(String unlockTimeoutMeasurementUnit) {
        int initialIndex = 0;
        switch (unlockTimeoutMeasurementUnit) {
            case "seconds" :
                initialIndex = 0;
                break;
            case "minutes" :
                initialIndex = 1;
                break;
            case "hours" :
                initialIndex = 2;
                break;
        }
        timeoutMeasurementUnit.getSelectionModel().select(initialIndex);
    }

    private ObservableList getTimeoutMeasurementUnits() {
        ObservableList<String> options =
                FXCollections.observableArrayList(
                        "seconds",
                        "minutes",
                        "hours"
                );
        return options;
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
