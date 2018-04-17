package org.aion.wallet.ui.components.partials;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import org.aion.mcf.account.Keystore;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.EventPublisher;
import org.aion.wallet.ui.events.HeaderPaneButtonEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ImportAccountDialog {
    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();

    @FXML
    private PasswordField privateKeyPassword;

    @FXML
    private PasswordField keystorePassword;

    private final Popup popup = new Popup();

    public void uploadKeystoreFile() throws IOException, ValidationException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open UTC Keystore File");
        File file = fileChooser.showOpenDialog(popup.getOwnerWindow());
        String password = keystorePassword.getText();
        AccountDTO account = blockchainConnector.addKeystoreUTCFile(Files.readAllBytes(file.toPath()), password);
        EventPublisher.fireAccountChanged(account);
    }

    public void uploadPrivateKey() {

    }

    public void open(MouseEvent mouseEvent) {
        popup.setAutoHide(true);
        popup.setAutoFix(true);

        Pane importAccountDialog = null;
        try {
            importAccountDialog = FXMLLoader.load(getClass().getResource("ImportAccountDialog.fxml"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        popup.getScene().getWindow().centerOnScreen();

        Node eventSource = (Node) mouseEvent.getSource();
        popup.setX(eventSource.getScene().getWindow().getX() + eventSource.getScene().getWidth() / 2 - importAccountDialog.getPrefWidth() / 2);
        popup.setY(eventSource.getScene().getWindow().getY() + eventSource.getScene().getHeight() / 2 - importAccountDialog.getPrefHeight() / 2);
        popup.getContent().addAll(importAccountDialog);
        popup.show(eventSource.getScene().getWindow());
    }

    public void close() {
        popup.hide();
    }
}
