package org.aion.wallet.ui.components.partials;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.InputEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import org.aion.api.log.LogEnum;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.dto.SendTransactionDTO;
import org.aion.wallet.log.WalletLoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

public class TransactionResubmissionDialog implements Initializable{

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
        for(SendTransactionDTO unsentTransaction : blockchainConnector.getAccountManager().getTimedoutTransactions(blockchainConnector.getAccounts().stream().filter(p -> p.isActive()).findAny().get().getPublicAddress())) {
            HBox row = new HBox();
            row.setSpacing(10);
            AnchorPane.setLeftAnchor(row, 10.0);
            AnchorPane.setTopAnchor(row, 10.0);
            Label to = new Label(unsentTransaction.getTo());
            Label value = new Label(unsentTransaction.getValue().toString());
            Label nonce = new Label(unsentTransaction.getNonce().toString());
            row.getChildren().addAll(Arrays.asList(to, value, nonce));
            transactions.getChildren().add(row);
        }
    }
}
