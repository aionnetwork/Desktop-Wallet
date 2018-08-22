package org.aion.wallet.ui.components.partials;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
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
import org.aion.base.util.TypeConverter;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.dto.AccountType;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.hardware.AionAccountDetails;
import org.aion.wallet.hardware.HardwareWallet;
import org.aion.wallet.hardware.HardwareWalletFactory;
import org.aion.wallet.hardware.ledger.LedgerException;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.util.BalanceUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class LedgerAccountListDialog implements Initializable {
    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());
    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();

    @FXML
    private VBox ledgerAccountList;

    @FXML
    private ProgressBar loadingLedgerAccountsProgressBar;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fillLedgerAccountList();
    }

    private void fillLedgerAccountList() {
        loadingLedgerAccountsProgressBar.setVisible(true);

        for (int i = 0; i < 10; i++) {
            HBox account = new HBox();
            account.setSpacing(10);
            account.setAlignment(Pos.CENTER_LEFT);
            final HardwareWallet hardwareWallet = HardwareWalletFactory.getHardwareWallet(AccountType.LEDGER);
            String hexAddress = "0x00";
            try {
                hexAddress = hardwareWallet.getAccountDetails(i).getAddress();
            } catch (LedgerException e) {
                log.error(e.getMessage(), e);
            }
            RadioButton radioButton = new RadioButton();
            TextField address = new TextField(TypeConverter.toJsonHex(hexAddress));
            address.setPrefWidth(575);
            address.setEditable(false);
            address.getStyleClass().add("copyable-textfield");
            Label balance = new Label(BalanceUtils.formatBalance(blockchainConnector.getBalance(hexAddress))
                    + " AION");
            balance.getStyleClass().add("copyable-label");
            account.getChildren().addAll(radioButton, address, balance);

            ledgerAccountList.getChildren().add(account);
        }
        loadingLedgerAccountsProgressBar.setVisible(false);
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
        Scene secondScene = new Scene(pane, ledgerAccountListDialog.getPrefWidth(), ledgerAccountListDialog.getPrefHeight());
        secondScene.setFill(Color.TRANSPARENT);

        Stage popup = new Stage();
        popup.setTitle("Ledger accounts");
        popup.setScene(secondScene);

        Node eventSource = (Node) mouseEvent.getSource();
        popup.setX(eventSource.getScene().getWindow().getX() + eventSource.getScene().getWidth() / 2 - ledgerAccountListDialog.getPrefWidth() / 2);
        popup.setY(eventSource.getScene().getWindow().getY() + eventSource.getScene().getHeight() / 2 - ledgerAccountListDialog.getPrefHeight() / 2);
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
            final String address = HardwareWalletFactory.getHardwareWallet(AccountType.LEDGER).getAccountDetails(0).getAddress();
            BlockchainConnector.getInstance().importHardwareWallet(0, address, AccountType.LEDGER);
        } catch (ValidationException | LedgerException e) {
            e.printStackTrace();
        }
        close(eventSource);
    }
}
