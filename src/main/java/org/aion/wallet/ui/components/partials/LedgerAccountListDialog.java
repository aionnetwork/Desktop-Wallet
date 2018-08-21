package org.aion.wallet.ui.components.partials;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
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
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class LedgerAccountListDialog implements Initializable {
    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    @FXML
    private VBox ledgerAccountList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fillLedgerAccountList();
    }

    private void fillLedgerAccountList() {
        HBox account1 = new HBox();
        account1.setSpacing(10);
        account1.setAlignment(Pos.CENTER_LEFT);
//        account1.setPrefWidth(400);
        RadioButton r1 = new RadioButton();
        Label l1 = new Label();
        l1.setText("0xa096057bc9dfc89759eef9346fd4675370a8f7987424dae571dcb70a10a57176");
        l1.setPrefWidth(500);
        Label b1 = new Label();
        b1.setText("1.09978996 AION");
        account1.getChildren().addAll(r1, l1, b1);

        HBox account2 = new HBox();
        account2.setSpacing(10);
        account2.setAlignment(Pos.CENTER_LEFT);
//        account2.setPrefWidth(400);
        RadioButton r2 = new RadioButton();
        Label l2 = new Label();
        l2.setText("0xa0326bb6badfa70c3025c90df3404c1700d660f5473aca6dd9fd63f398f10695");
        l2.setPrefWidth(500);
        Label b2 = new Label();
        b2.setText("0 AION");
        account2.getChildren().addAll(r2, l2, b2);

        ledgerAccountList.getChildren().addAll(account1, account2);

        for (int i = 0; i < 1; i++) {
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
            RadioButton radioButton = new RadioButton(TypeConverter.toJsonHex(hexAddress));
            Label address = new Label("");
            address.setPrefWidth(500);
            Label balance = new Label("");
            account.getChildren().addAll(radioButton, address, balance);
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
