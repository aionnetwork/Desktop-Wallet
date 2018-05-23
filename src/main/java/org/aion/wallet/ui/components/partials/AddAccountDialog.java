package org.aion.wallet.ui.components.partials;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Popup;
import org.aion.api.log.LogEnum;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.events.EventPublisher;
import org.aion.wallet.log.WalletLoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;

public class AddAccountDialog {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private final ImportAccountDialog importAccountDialog = new ImportAccountDialog();

    private final MnemonicDialog mnemonicDialog = new MnemonicDialog();

    @FXML
    private TextField newAccountName;

    @FXML
    private PasswordField newPassword;

    @FXML
    private PasswordField retypedPassword;

    @FXML
    private Label validationError;

    private final Popup popup = new Popup();

    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();

    public void createAccount(final InputEvent mouseEvent) {
        resetValidation();

        if (validateFields()) {
            String mnemonic = blockchainConnector.createAccount(newPassword.getText(), newAccountName.getText());
            if(mnemonic != null) {
                mnemonicDialog.open(mouseEvent);
                EventPublisher.fireMnemonicCreated(mnemonic);
            }
        } else {
            String error = "";
            if (newPassword.getText().isEmpty() || retypedPassword.getText().isEmpty()) {
                error = "Please complete the fields!";
            } else if (!newPassword.getText().equals(retypedPassword.getText())) {
                error = "Passwords don't match!";
            }
            showInvalidFieldsError(error);
        }
    }

    public void uploadKeystoreFile(final MouseEvent e) {
        importAccountDialog.open(e);
    }

    private boolean validateFields() {
        if (newPassword == null || newPassword.getText() == null || retypedPassword == null || retypedPassword.getText() == null) {
            return false;
        }

        return newPassword.getText() != null && !newPassword.getText().isEmpty()
                && retypedPassword.getText() != null && !retypedPassword.getText().isEmpty()
                && newPassword.getText().equals(retypedPassword.getText());
    }

    public void resetValidation() {
        validationError.setVisible(false);
    }

    private void showInvalidFieldsError(String message) {
        validationError.setVisible(true);
        validationError.setText(message);
    }

    public void open(MouseEvent mouseEvent) {
        popup.setAutoHide(true);
        popup.setAutoFix(true);

        Pane addAccountDialog;
        try {
            addAccountDialog = FXMLLoader.load(getClass().getResource("AddAccountDialog.fxml"));
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

    public void close() {
        popup.hide();
    }

    @FXML
    private void submitOnEnterPressed(final KeyEvent event) {
        if (event.getCode().equals(KeyCode.ENTER)) {
            createAccount(event);
        }
    }
}
