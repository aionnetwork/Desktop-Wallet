package org.aion.wallet.ui.components.partials;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Popup;
import org.aion.mcf.account.Keystore;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.HeaderPaneButtonEvent;

import java.io.IOException;

public class AddAccountDialog {

    @FXML
    private TextField newPassword;

    @FXML
    private TextField retypedPassword;

    private final Popup popup = new Popup();

    public void createAccount() {
        if (newPassword.getText() != null && retypedPassword.getText() != null && newPassword.getText().equals(retypedPassword.getText())) {
            Keystore.create(newPassword.getText());
        }
        EventBusFactory.getInstance().getBus(HeaderPaneButtonEvent.ID).post(new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.HOME));
    }

    public void unlockAccount() {

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
