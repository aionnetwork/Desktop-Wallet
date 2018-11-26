package org.aion.wallet.ui.components.partials;

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
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.log.WalletLoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class UnlockMasterAccountDialog implements Initializable {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());
    private final Popup popup = new Popup();
    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label validationError;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        Platform.runLater(() -> passwordField.requestFocus());
    }

    public void open(MouseEvent mouseEvent) {
        popup.setAutoHide(true);
        popup.setAutoFix(true);

        Pane addAccountDialog;
        try {
            addAccountDialog = FXMLLoader.load(getClass().getResource("UnlockMasterAccountDialog.fxml"));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return;
        }

        Node eventSource = (Node) mouseEvent.getSource();
        final double windowX = eventSource.getScene().getWindow().getX();
        final double windowY = eventSource.getScene().getWindow().getY();
        popup.setX(windowX + eventSource.getScene().getWidth() / 2 - addAccountDialog.getPrefWidth() / 2);
        popup.setY(windowY + eventSource.getScene().getHeight() / 2 - addAccountDialog.getPrefHeight() / 2);
        popup.getContent().addAll(addAccountDialog);
        popup.show(eventSource.getScene().getWindow());
    }

    public void close(InputEvent eventSource) {
        ((Node) eventSource.getSource()).getScene().getWindow().hide();
    }

    @FXML
    private void submitOnEnterPressed(final KeyEvent event) {
        if (event.getCode().equals(KeyCode.ENTER)) {
            unlockMasterAccount(event);
        }
    }

    @FXML
    private void unlockMasterAccount(final InputEvent mouseEvent) {
        try {
            blockchainConnector.unlockMasterAccount(passwordField.getText());
            ConsoleManager.addLog("Master account unlocked", ConsoleManager.LogType.ACCOUNT);
            close(mouseEvent);
        } catch (ValidationException e) {
            ConsoleManager.addLog("Could not unlock master account", ConsoleManager.LogType.ACCOUNT, ConsoleManager.LogLevel.WARNING);
            showInvalidFieldsError(e.getMessage());
        }
    }

    public void resetValidation() {
        validationError.setVisible(false);
    }

    private void showInvalidFieldsError(String message) {
        validationError.setVisible(true);
        validationError.setText(message);
    }
}
