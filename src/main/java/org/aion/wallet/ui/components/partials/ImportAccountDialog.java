package org.aion.wallet.ui.components.partials;

import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.ui.events.EventPublisher;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ResourceBundle;

public class ImportAccountDialog implements Initializable {
    private static final String PK_RADIO_BUTTON_ID = "PK_RB";
    private static final String KEYSTORE_RADIO_BUTTON_ID = "KEYSTORE_RB";
    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();

    @FXML
    public TextField privateKeyInput;

    @FXML
    private PasswordField privateKeyPassword;

    @FXML
    private TextField keystoreTextView;

    @FXML
    private PasswordField keystorePassword;

    @FXML
    private RadioButton privateKeyRadioButton;

    @FXML
    private RadioButton keystoreRadioButton;

    @FXML
    private ToggleGroup accountTypeToggleGroup;

    @FXML
    private VBox importKeystoreView;

    @FXML
    private VBox importPrivateKeyView;

    private byte[] keystoreFile;

    public void uploadKeystoreFile() throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open UTC Keystore File");
        File file = fileChooser.showOpenDialog(null);
        if (file == null) {
            return;
        }
        keystoreTextView.setText(file.getName());
        keystorePassword.requestFocus();
        keystoreFile = Files.readAllBytes(file.toPath());
    }

    public void importAccount(MouseEvent mouseEvent) {
        if (importKeystoreView.isVisible()) {
            String password = keystorePassword.getText();
            if (!password.isEmpty() && keystoreFile != null) {
                AccountDTO account;
                try {
                    account = blockchainConnector.addKeystoreUTCFile(keystoreFile, password);
                } catch (final ValidationException e) {
                    e.printStackTrace();
                    return;
                }
                EventPublisher.fireAccountChanged(account);
            }
        } else {
            //todo: import private key
            String password = privateKeyPassword.getText();
            String privateKey = privateKeyInput.getText();
        }
        this.close(mouseEvent);
    }

    public void open(MouseEvent mouseEvent) {
        StackPane pane = new StackPane();
        Pane importAccountDialog;
        try {
            importAccountDialog = FXMLLoader.load(getClass().getResource("ImportAccountDialog.fxml"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        pane.getChildren().add(importAccountDialog);
        Scene secondScene = new Scene(pane, importAccountDialog.getPrefWidth(), importAccountDialog.getPrefHeight());
        secondScene.setFill(Color.TRANSPARENT);

        Stage popup = new Stage();
        popup.setTitle("Import account");
        popup.setScene(secondScene);

        Node eventSource = (Node) mouseEvent.getSource();
        popup.setX(eventSource.getScene().getWindow().getX() + eventSource.getScene().getWidth() / 2 - importAccountDialog.getPrefWidth() / 2);
        popup.setY(eventSource.getScene().getWindow().getY() + eventSource.getScene().getHeight() / 2 - importAccountDialog.getPrefHeight() / 2);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.TRANSPARENT);

        popup.show();
    }

    public void close(MouseEvent mouseEvent) {
        ((Node) mouseEvent.getSource()).getScene().getWindow().hide();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        privateKeyRadioButton.setUserData(PK_RADIO_BUTTON_ID);
        keystoreRadioButton.setUserData(KEYSTORE_RADIO_BUTTON_ID);
        accountTypeToggleGroup.selectedToggleProperty().addListener(this::radioButtonChanged);
    }

    private void radioButtonChanged(ObservableValue<? extends Toggle> ov, Toggle oldToggle, Toggle newToggle) {
        if (accountTypeToggleGroup.getSelectedToggle() != null) {
            switch ((String) accountTypeToggleGroup.getSelectedToggle().getUserData()) {
                case PK_RADIO_BUTTON_ID:
                    importPrivateKeyView.setVisible(true);
                    importKeystoreView.setVisible(false);
                    break;
                case KEYSTORE_RADIO_BUTTON_ID:
                    importPrivateKeyView.setVisible(false);
                    importKeystoreView.setVisible(true);
                    break;
            }
        }
    }
}
