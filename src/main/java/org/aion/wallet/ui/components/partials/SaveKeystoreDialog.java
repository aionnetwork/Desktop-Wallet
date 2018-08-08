package org.aion.wallet.ui.components.partials;

import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.aion.api.log.LogEnum;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.console.ConsoleManager;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.events.AccountEvent;
import org.aion.wallet.events.EventBusFactory;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.storage.LocalKeystore;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SaveKeystoreDialog implements Initializable {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private static final Tooltip LOCKED_PASSWORD = new Tooltip("Can't change stored account password");
    private static final Tooltip NEW_PASSWORD = new Tooltip("New keystore file password");
    private static final String PASSWORD_PLACEHOLDER = "             ";
    private static final String CHOOSER_TITLE = "Keystore Destination";

    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();
    private AccountDTO account;
    private String destinationDirectory;

    @FXML
    private PasswordField keystorePassword;
    @FXML
    private Label validationError;
    @FXML
    private TextField keystoreTextView;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        registerEventBusConsumer();
        Platform.runLater(() -> keystorePassword.requestFocus());
    }

    private void registerEventBusConsumer() {
        EventBusFactory.getBus(AccountEvent.ID).register(this);
    }

    public void open(final MouseEvent mouseEvent) {
        final StackPane pane = new StackPane();
        final Pane saveKeystoreDialog;
        try {
            saveKeystoreDialog = FXMLLoader.load(getClass().getResource("SaveKeystoreDialog.fxml"));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return;
        }
        pane.getChildren().add(saveKeystoreDialog);
        final Scene secondScene = new Scene(pane, saveKeystoreDialog.getPrefWidth(), saveKeystoreDialog.getPrefHeight());
        secondScene.setFill(Color.TRANSPARENT);

        final Stage popup = new Stage();
        popup.setTitle("Export Account");
        popup.setScene(secondScene);

        final Node eventSource = (Node) mouseEvent.getSource();
        popup.setX(eventSource.getScene().getWindow().getX() + eventSource.getScene().getWidth() / 2 - saveKeystoreDialog.getPrefWidth() / 2);
        popup.setY(eventSource.getScene().getWindow().getY() + eventSource.getScene().getHeight() / 2 - saveKeystoreDialog.getPrefHeight() / 2);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.TRANSPARENT);

        popup.show();
    }

    @Subscribe
    private void handleUnlockStarted(final AccountEvent event) {
        if (AccountEvent.Type.EXPORT.equals(event.getType())) {
            this.account = event.getPayload();
            if (isRemembered()) {
                keystorePassword.setEditable(false);
                keystorePassword.setText(PASSWORD_PLACEHOLDER);
                Tooltip.uninstall(keystorePassword, NEW_PASSWORD);
                Tooltip.install(keystorePassword, LOCKED_PASSWORD);
            } else {
                keystorePassword.setEditable(true);
                keystorePassword.setText("");
                Tooltip.uninstall(keystorePassword, LOCKED_PASSWORD);
                Tooltip.install(keystorePassword, NEW_PASSWORD);
            }
        }
    }

    @FXML
    private void saveKeystore(final InputEvent event) {
        final String password = keystorePassword.getText();
        if (isRemembered() || (password != null && !password.isEmpty())) {
            try {
                blockchainConnector.exportAccount(account, password, destinationDirectory);
                final String infoMsg = "Account: " + account.getPublicAddress() + " exported to " + destinationDirectory;
                ConsoleManager.addLog(infoMsg, ConsoleManager.LogType.ACCOUNT);
                log.info(infoMsg);
                close(event);
            } catch (ValidationException e) {
                ConsoleManager.addLog("Account: " + account.getPublicAddress() + " could not be exported", ConsoleManager.LogType.ACCOUNT, ConsoleManager.LogLevel.WARNING);
                validationError.setText(e.getMessage());
                validationError.setVisible(true);
                log.error(e.getMessage(), e);
            }
        } else {
            validationError.setText("Please insert a password!");
            validationError.setVisible(true);
        }
    }

    @FXML
    private void chooseExportLocation(final MouseEvent mouseEvent) {
        resetValidation();
        final DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(CHOOSER_TITLE);
        final File file = chooser.showDialog(null);
        if (file != null) {
            destinationDirectory = file.getAbsolutePath();
            keystoreTextView.setText(destinationDirectory);
        }
    }

    @FXML
    private void submitOnEnterPressed(final KeyEvent event) {
        if (event.getCode().equals(KeyCode.ENTER)) {
            saveKeystore(event);
        }
    }

    @FXML
    private void clickPassword(final MouseEvent mouseEvent) {
        if (!keystorePassword.isEditable()) {
            validationError.setText(LOCKED_PASSWORD.getText());
            validationError.setVisible(true);
        } else {
            resetValidation();
        }
    }

    @FXML
    private void close(final InputEvent event) {
        ((Node) event.getSource()).getScene().getWindow().hide();
    }

    private boolean isRemembered() {
        return account.isImported() && LocalKeystore.exist(account.getPublicAddress());
    }

    private void resetValidation() {
        validationError.setVisible(false);
        validationError.setText("");
    }
}
