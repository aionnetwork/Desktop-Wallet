package org.aion.wallet.ui.components.partials;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Popup;
import org.aion.mcf.account.Keystore;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.HeaderPaneButtonEvent;

import java.io.IOException;

public class AddAccountDialog {

    @FXML
    private PasswordField newPassword;

    @FXML
    private PasswordField retypedPassword;

    @FXML
    private Label validationError;

    private final Popup popup = new Popup();

    public void createAccount() {
        if (validateFields()) {
            Keystore.create(newPassword.getText());
            EventBusFactory.getInstance().getBus(HeaderPaneButtonEvent.ID).post(new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.HOME));
        }
    }

    private boolean validateFields() {
        resetValidation();
        boolean result = newPassword.getText() != null && !newPassword.getText().isEmpty()
                && retypedPassword.getText() != null && !retypedPassword.getText().isEmpty()
                && newPassword.getText().equals(retypedPassword.getText());
        if(!result) {
            String error = "";
            if(newPassword.getText().isEmpty() || retypedPassword.getText().isEmpty()) {
                error = "Please complete the fields!";
            }
            else if(!newPassword.getText().equals(retypedPassword.getText())) {
                error = "Passwords don't match!";
            }
            showInvalidFieldsError(error);
        }
        return result;
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
        popup.setX(eventSource.getScene().getWindow().getX() + eventSource.getScene().getWidth() / 2 - addAccountDialog.getPrefWidth() / 2);
        popup.setY(eventSource.getScene().getWindow().getY() + eventSource.getScene().getHeight() / 2 - addAccountDialog.getPrefHeight() / 2);
        popup.getContent().addAll(addAccountDialog);
        popup.show(eventSource.getScene().getWindow());
    }

    public void close() {
        popup.hide();
    }
}
