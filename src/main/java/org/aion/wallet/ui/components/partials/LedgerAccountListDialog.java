package org.aion.wallet.ui.components.partials;

import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.InputEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.aion.api.log.LogEnum;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.console.ConsoleManager;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.dto.AccountType;
import org.aion.wallet.events.EventBusFactory;
import org.aion.wallet.events.EventPublisher;
import org.aion.wallet.events.UiMessageEvent;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.hardware.AionAccountDetails;
import org.aion.wallet.hardware.HardwareWallet;
import org.aion.wallet.hardware.HardwareWalletException;
import org.aion.wallet.hardware.HardwareWalletFactory;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.util.BalanceUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LedgerAccountListDialog implements Initializable {
    public static final int AION_BALANCE_DECIMAL_PLACES = 6;
    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());
    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();
    private int currentDerivationIndex;
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    @FXML
    private VBox ledgerAccountList;

    @FXML
    private ProgressBar loadingLedgerAccountsProgressBar;

    @FXML
    private ToggleGroup ledgerAccountsToggleGroup;

    @FXML
    private Button ledgerContinueButton;

    @FXML
    private Label nextAddressesLink;

    @FXML
    private Label previousAddressesLink;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        registerEventBusConsumer();
        ledgerContinueButton.setDisable(true);
        ledgerAccountsToggleGroup.selectedToggleProperty().addListener(this::ledgerAccountChanged);

        initializeLedgerAccountList();
    }

    private void ledgerAccountChanged(final Observable observable) {
        if (ledgerAccountsToggleGroup.getSelectedToggle() != null) {
            ledgerContinueButton.setDisable(false);
        }
    }

    @Subscribe
    private void handleLedgerConnected(UiMessageEvent event) {
        if (UiMessageEvent.Type.LEDGER_CONNECTED.equals(event.getType())) {
            initializeLedgerAccountList();
        }
    }

    private void registerEventBusConsumer() {
        EventBusFactory.getBus(UiMessageEvent.ID).register(this);
    }

    private void initializeLedgerAccountList() {
        loadingLedgerAccountsProgressBar.setVisible(true);
        ledgerAccountList.setVisible(false);
        nextAddressesLink.setVisible(false);
        previousAddressesLink.setVisible(false);
        backgroundExecutor.submit(() -> {
            List<AionAccountDetails> aionAccountDetails = generateLedgerAddresses(0,5);
            Platform.runLater(() -> {
                updateLedgerAddresses(aionAccountDetails);
                loadingLedgerAccountsProgressBar.setVisible(false);
                ledgerAccountList.setVisible(true);
                nextAddressesLink.setVisible(true);
                previousAddressesLink.setVisible(false);
            });
        });
    }

    private List<AionAccountDetails> generateLedgerAddresses(final int startIndex, final int stopIndex) {
        currentDerivationIndex = stopIndex;
        final HardwareWallet hardwareWallet = HardwareWalletFactory.getHardwareWallet(AccountType.LEDGER);

        try {
            return hardwareWallet.getMultipleAccountDetails(startIndex, stopIndex);
        } catch (HardwareWalletException e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    private void updateLedgerAddresses(List<AionAccountDetails> aionAccountDetails) {
        ledgerAccountList.getChildren().clear();
        for (AionAccountDetails accountDetails : aionAccountDetails) {
            HBox account = new HBox();
            account.setSpacing(5);
            account.setAlignment(Pos.CENTER_LEFT);
            account.setStyle("-fx-border-color: #ececec; -fx-border-width: 0 0 1 0;");

            RadioButton radioButton = new RadioButton();
            radioButton.setUserData(accountDetails);
            radioButton.setToggleGroup(ledgerAccountsToggleGroup);

            TextField offset = new TextField(String.valueOf(accountDetails.getDerivationIndex()));
            offset.setPrefWidth(40);
            offset.setEditable(false);
            offset.getStyleClass().add("copyable-textfield");

            TextField address = new TextField(accountDetails.getAddress());
            address.setPrefWidth(550);
            address.setEditable(false);
            address.getStyleClass().add("copyable-textfield");
            Label balance = new Label(
                    BalanceUtils.formatBalanceWithNumberOfDecimals(
                            blockchainConnector.getBalance(accountDetails.getAddress()), AION_BALANCE_DECIMAL_PLACES)
                            + " AION");
            balance.getStyleClass().add("copyable-label");
            balance.setPrefWidth(100);
            account.getChildren().addAll(radioButton, offset, address, balance);

            ledgerAccountList.getChildren().add(account);
        }
    }

    public void open(MouseEvent mouseEvent) {
        StackPane pane = new StackPane();
        Pane ledgerAccountListDialog;
        try {
            ledgerAccountListDialog = FXMLLoader.load(getClass().getResource("LedgerAccountListDialog.fxml"));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return;
        }
        pane.getChildren().add(ledgerAccountListDialog);
        Scene secondScene = new Scene(pane, ledgerAccountListDialog.getPrefWidth(), ledgerAccountListDialog
                .getPrefHeight());
        secondScene.setFill(Color.TRANSPARENT);

        Stage popup = new Stage();
        popup.setTitle("Ledger accounts");
        popup.setScene(secondScene);

        Node eventSource = (Node) mouseEvent.getSource();
        popup.setX(eventSource.getScene().getWindow().getX() + eventSource.getScene().getWidth() / 2 -
                ledgerAccountListDialog.getPrefWidth() / 2);
        popup.setY(eventSource.getScene().getWindow().getY() + eventSource.getScene().getHeight() / 2 -
                ledgerAccountListDialog.getPrefHeight() / 2);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.TRANSPARENT);

        popup.show();
    }

    public void close(final InputEvent eventSource) {
        ((Node) eventSource.getSource()).getScene().getWindow().hide();
    }

    @FXML
    private void addAccount(final InputEvent eventSource) {
        try {
            if (ledgerAccountsToggleGroup.getSelectedToggle() != null
                    && ledgerAccountsToggleGroup.getSelectedToggle().getUserData() != null) {
                AionAccountDetails accountDetails = (AionAccountDetails) ledgerAccountsToggleGroup.getSelectedToggle
                        ().getUserData();
                AccountDTO account = BlockchainConnector.getInstance().importHardwareWallet(accountDetails
                                .getDerivationIndex(),
                        accountDetails.getAddress(), AccountType.LEDGER);
                EventPublisher.fireLedgerAccountSelected(eventSource);
                EventPublisher.fireAccountChanged(account);
                ConsoleManager.addLog("Added account : " + accountDetails.getAddress(), ConsoleManager.LogType
                        .ACCOUNT);
                close(eventSource);
            }
        } catch (ValidationException e) {
            log.error(e.getMessage(), e);
            if (e.getMessage().equals("Account already exists!")) {
                close(eventSource);
            }
        }
    }

    public void nextAddresses() {
        ledgerContinueButton.setDisable(true);
        loadingLedgerAccountsProgressBar.setVisible(true);
        ledgerAccountList.setVisible(false);
        nextAddressesLink.setVisible(false);
        previousAddressesLink.setVisible(false);

        backgroundExecutor.submit(() -> {
            List<AionAccountDetails> aionAccountDetails = generateLedgerAddresses(currentDerivationIndex, currentDerivationIndex + 5);
            Platform.runLater(() -> {
                updateLedgerAddresses(aionAccountDetails);
                loadingLedgerAccountsProgressBar.setVisible(false);
                ledgerAccountList.setVisible(true);
                nextAddressesLink.setVisible(true);
                if(currentDerivationIndex > 5) {
                    previousAddressesLink.setVisible(true);
                }
            });
        });
    }

    public void previousAddresses() {
        ledgerContinueButton.setDisable(true);
        loadingLedgerAccountsProgressBar.setVisible(true);
        ledgerAccountList.setVisible(false);
        nextAddressesLink.setVisible(false);
        previousAddressesLink.setVisible(false);

        backgroundExecutor.submit(() -> {
            List<AionAccountDetails> aionAccountDetails = generateLedgerAddresses(currentDerivationIndex-10, currentDerivationIndex-5);
            Platform.runLater(() -> {
                updateLedgerAddresses(aionAccountDetails);
                loadingLedgerAccountsProgressBar.setVisible(false);
                ledgerAccountList.setVisible(true);
                nextAddressesLink.setVisible(true);
                if(currentDerivationIndex > 5) {
                    previousAddressesLink.setVisible(true);
                }
            });
        });
    }
}
