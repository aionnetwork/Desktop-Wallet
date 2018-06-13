package org.aion.wallet.ui.components.partials;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.InputEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import org.aion.api.log.LogEnum;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.dto.SendTransactionDTO;
import org.aion.wallet.events.EventPublisher;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.util.BalanceUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class TransactionResubmissionDialog implements Initializable {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());
    private final Popup popup = new Popup();
    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();

    @FXML
    private VBox transactions;

    public void open(final MouseEvent mouseEvent) {
        popup.setAutoHide(true);
        popup.setAutoFix(true);

        Pane resubmitTransactionDialog;
        try {
            resubmitTransactionDialog = FXMLLoader.load(getClass().getResource("TransactionResubmissionDialog.fxml"));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return;
        }

        Node eventSource = (Node) mouseEvent.getSource();
        final double windowX = eventSource.getScene().getWindow().getX();
        final double windowY = eventSource.getScene().getWindow().getY();
        popup.setX(windowX + eventSource.getScene().getWidth() / 2 - resubmitTransactionDialog.getPrefWidth() / 2);
        popup.setY(windowY + eventSource.getScene().getHeight() / 2 - resubmitTransactionDialog.getPrefHeight() / 2);
        popup.getContent().addAll(resubmitTransactionDialog);
        popup.show(eventSource.getScene().getWindow());
    }

    public void close(InputEvent eventSource) {
        ((Node) eventSource.getSource()).getScene().getWindow().hide();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        displayTransactions();
    }

    private void displayTransactions() {
        addHeaderForTable();
        for (SendTransactionDTO unsentTransaction : blockchainConnector.getAccountManager().getTimedoutTransactions(blockchainConnector.getAccounts().stream().filter(p -> p.isActive()).findAny().get().getPublicAddress())) {
            HBox row = new HBox();
            row.setSpacing(10);
            row.setAlignment(Pos.CENTER);
            row.setPrefWidth(600);

            Label to = new Label(unsentTransaction.getTo());
            to.setPrefWidth(350);
            to.getStyleClass().add("transaction-row-text");
            row.getChildren().add(to);

            Label value = new Label(BalanceUtils.formatBalance(unsentTransaction.getValue()));
            value.setPrefWidth(100);
            value.setPadding(new Insets(0.0, 0.0, 0.0, 10.0));
            value.getStyleClass().add("transaction-row-text");
            row.getChildren().add(value);

            Label nonce = new Label(unsentTransaction.getNonce().toString());
            nonce.setPrefWidth(50);
            nonce.setPadding(new Insets(0.0, 0.0, 0.0, 5.0));
            nonce.getStyleClass().add("transaction-row-text");
            row.getChildren().add(nonce);

            Button resubmitTransaction = new Button();
            resubmitTransaction.setText("Resubmit");
            resubmitTransaction.setPrefWidth(100);
            resubmitTransaction.getStyleClass().add("submit-button-small");
            resubmitTransaction.setOnMouseClicked(event -> {
                close(event);
                blockchainConnector.getAccountManager().removeTimedOutTransaction(unsentTransaction);
                EventPublisher.fireTransactionResubmited(unsentTransaction);
            });
            row.getChildren().add(resubmitTransaction);

            transactions.getChildren().add(row);
        }
    }

    private void addHeaderForTable() {
        HBox header = new HBox();
        header.setSpacing(10);
        header.setPrefWidth(400);
        header.setAlignment(Pos.CENTER);
        header.getStyleClass().add("transaction-row");

        Label to = new Label("To address");
        to.setPrefWidth(250);
        to.getStyleClass().add("transaction-table-header-text");
        header.getChildren().add(to);

        Label value = new Label("Value");
        value.setPrefWidth(75);
        value.getStyleClass().add("transaction-table-header-text");
        header.getChildren().add(value);

        Label nonce = new Label("Nonce");
        nonce.setPrefWidth(75);
        nonce.getStyleClass().add("transaction-table-header-text");
        header.getChildren().add(nonce);

        transactions.getChildren().add(header);
    }
}
