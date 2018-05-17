package org.aion.wallet.ui.components;

import com.google.common.eventbus.Subscribe;
import javafx.fxml.FXML;
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
            newSettings = new LightAppSettings(address.getText().trim(), port.getText().trim(),
                    protocol.getText().trim(), this.settings.getType(), timeout.getText());
            notification.setText("");
            EventPublisher.fireApplicationSettingsChanged(newSettings);
        } catch (ValidationException e) {
            log.error(e.getMessage(), e);
            notification.getStyleClass().add(ERROR_STYLE);
            notification.setText(e.getMessage());
        }
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
            notification.getStyleClass().removeAll(ERROR_STYLE);
            notification.setText("Changes applied");
        }
    }

    private void reloadView() {
        settings = blockchainConnector.getSettings();
        protocol.setText(settings.getProtocol());
        address.setText(settings.getAddress());
        port.setText(settings.getPort());
        timeout.setText(settings.getUnlockTimeout().toString().substring(2).toLowerCase());
        notification.getStyleClass().removeAll(ERROR_STYLE);
        notification.setText("");
    }
}
