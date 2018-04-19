package org.aion.wallet.ui.components.partials;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Popup;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.storage.WalletStorage;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.HeaderPaneButtonEvent;

import java.io.IOException;

public class AddAccountDialog {

    private static final double DEFAULT_ACCOUNT_DIALOG_HEIGHT = 400.0;
    private static final double DEFAULT_ACCOUNT_DIALOG_WIDTH = 350.0;

    private ImportAccountDialog importAccountDialog = new ImportAccountDialog();

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

    public void createAccount() {
        resetValidation();
        if (validateFields()) {
            blockchainConnector.createAccount(newPassword.getText(), newAccountName.getText());
            EventBusFactory.getBus(HeaderPaneButtonEvent.ID).post(new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.OVERVIEW));
        }
        else {
            String error = "";
            if(newPassword.getText().isEmpty() || retypedPassword.getText().isEmpty()) {
                error = "Please complete the fields!";
            }
            else if(!newPassword.getText().equals(retypedPassword.getText())) {
                error = "Passwords don't match!";
            }
            showInvalidFieldsError(error);
        }
    }

    public void uploadKeystoreFile(MouseEvent e) {
        importAccountDialog.open(e);
    }

    private boolean validateFields() {
        if(newPassword == null || newPassword.getText() == null || retypedPassword == null || retypedPassword.getText() == null) {
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

        Pane addAccountDialog = null;
        try {
            addAccountDialog = FXMLLoader.load(getClass().getResource("AddAccountDialog.fxml"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Node eventSource = (Node) mouseEvent.getSource();
        popup.setX(eventSource.getScene().getWindow().getX() + eventSource.getScene().getWidth() / 2 - getAccountDialogWidth(addAccountDialog) / 2);
        popup.setY(eventSource.getScene().getWindow().getY() + eventSource.getScene().getHeight() / 2 - getAccountDialogHeight(addAccountDialog) / 2);
        popup.getContent().addAll(addAccountDialog);
        popup.show(eventSource.getScene().getWindow());
    }

    private double getAccountDialogHeight(Pane addAccountDialog) {
        if(addAccountDialog != null) {
            return addAccountDialog.getPrefHeight();
        }
        return DEFAULT_ACCOUNT_DIALOG_HEIGHT;
    }

    private double getAccountDialogWidth(Pane addAccountDialog) {
        if(addAccountDialog != null) {
            return addAccountDialog.getPrefWidth();
        }
        return DEFAULT_ACCOUNT_DIALOG_WIDTH;
    }

    public void close() {
        popup.hide();
    }
}
