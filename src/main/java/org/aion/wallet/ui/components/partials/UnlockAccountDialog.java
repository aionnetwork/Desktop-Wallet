package org.aion.wallet.ui.components.partials;

import com.google.common.eventbus.Subscribe;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.aion.api.log.AionLoggerFactory;
import org.aion.api.log.LogEnum;
import org.aion.crypto.ECKey;
import org.aion.mcf.account.Keystore;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.EventPublisher;
import org.aion.wallet.util.DataUpdater;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

public class UnlockAccountDialog implements Initializable{
    private static final Logger log = AionLoggerFactory.getLogger(LogEnum.WLT.name());

    @FXML
    private PasswordField unlockPassword;

    @FXML
    private Label validationError;

    private AccountDTO account;

    private final Popup popup = new Popup();

    public void open(MouseEvent mouseEvent) {
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

    public void unlockAccount(InputEvent event) {
        if(unlockPassword.getText() != null && !unlockPassword.getText().isEmpty()) {
            ECKey storedKey = Keystore.getKey(account.getPublicAddress(), unlockPassword.getText());
            if(storedKey != null) {
                account.setPrivateKey(storedKey.getPrivKeyBytes());
                EventPublisher.fireAccountChanged(account);
                this.close(event);
            }
            else {
                validationError.setText("The password is incorrect!");
                validationError.setVisible(true);
            }
        }
        else {
            validationError.setText("Please insert a password!");
            validationError.setVisible(true);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        registerEventBusConsumer();
    }

    public void resetValidation(MouseEvent mouseEvent) {
        validationError.setVisible(false);
    }

    public void close(InputEvent event) {
        ((Node) event.getSource()).getScene().getWindow().hide();
    }

    @Subscribe
    private void handleUnlockStarted(AccountDTO account) {
        this.account = account;
    }

    @FXML
    private void submitOnEnterPressed(final KeyEvent event) {
        if (event.getCode().equals(KeyCode.ENTER)) {
            unlockAccount(event);
        }
    }
    private void registerEventBusConsumer() {
        EventBusFactory.getBus(EventPublisher.ACCOUNT_UNLOCK_EVENT_ID).register(this);
    }
}
