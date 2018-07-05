package org.aion.wallet.ui.components.partials;

import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Popup;
import org.aion.api.log.LogEnum;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.console.ConsoleManager;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.events.AccountEvent;
import org.aion.wallet.events.EventBusFactory;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.log.WalletLoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class UnlockAccountDialog implements Initializable {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private final Popup popup = new Popup();
    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();
    @FXML
    private PasswordField unlockPassword;
    @FXML
    private Label validationError;
    private AccountDTO account;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        registerEventBusConsumer();
        Platform.runLater(() -> unlockPassword.requestFocus());
    }

    public void open(final MouseEvent mouseEvent) {
        popup.setAutoHide(true);
        popup.setAutoFix(true);

        Pane unlockAccountDialog;
        try {
            unlockAccountDialog = FXMLLoader.load(getClass().getResource("UnlockAccountDialog.fxml"));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return;
        }

        Node eventSource = (Node) mouseEvent.getSource();
        final double windowX = eventSource.getScene().getWindow().getX();
        final double windowY = eventSource.getScene().getWindow().getY();
        popup.setX(windowX + eventSource.getScene().getWidth() / 2 - unlockAccountDialog.getPrefWidth() / 2);
        popup.setY(windowY + eventSource.getScene().getHeight() / 2 - unlockAccountDialog.getPrefHeight() / 2);
        popup.getContent().addAll(unlockAccountDialog);
        popup.show(eventSource.getScene().getWindow());
    }

    private void close(final InputEvent event) {
        ((Node) event.getSource()).getScene().getWindow().hide();
    }

    public void unlockAccount(final InputEvent event) {
        final String password = unlockPassword.getText();
        if (password != null && !password.isEmpty()) {
            try {
                blockchainConnector.unlockAccount(account, password);
                ConsoleManager.addLog("Account" + account.getPublicAddress() + " unlocked", ConsoleManager.LogType.ACCOUNT);
                close(event);
            } catch (ValidationException e) {
                ConsoleManager.addLog("Account" + account.getPublicAddress() + " could not be unlocked", ConsoleManager.LogType.ACCOUNT, ConsoleManager.LogLevel.WARNING);
                validationError.setText(e.getMessage());
                validationError.setVisible(true);
            }
        } else {
            validationError.setText("Please insert a password!");
            validationError.setVisible(true);
        }
    }

    public void resetValidation() {
        validationError.setVisible(false);
    }

    @Subscribe
    private void handleUnlockStarted(final AccountEvent event) {
        if (AccountEvent.Type.UNLOCKED.equals(event.getType())) {
            this.account = event.getPayload();
        }
    }

    @FXML
    private void submitOnEnterPressed(final KeyEvent event) {
        if (event.getCode().equals(KeyCode.ENTER)) {
            unlockAccount(event);
        }
    }

    private void registerEventBusConsumer() {
        EventBusFactory.getBus(AccountEvent.ID).register(this);
    }
}
