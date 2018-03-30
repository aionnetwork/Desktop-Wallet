package org.aion.wallet.ui.components;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.aion.base.util.TypeConverter;
import org.aion.log.AionLoggerFactory;
import org.aion.log.LogEnum;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.WalletBlockchainConnector;
import org.aion.wallet.connector.dto.SendRequestDTO;
import org.aion.wallet.exception.NotFoundException;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.util.BalanceFormatter;
import org.slf4j.Logger;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

import static org.aion.wallet.util.WalletUtils.*;

public class SendPane implements Initializable {
    private static final Logger log = AionLoggerFactory.getLogger(LogEnum.WLT.name());
    private static final int MAX_TX_STATUS_RETRY_COUNT = 6;

    private final BlockchainConnector blockchainConnector = new WalletBlockchainConnector();

    @FXML
    private ComboBox fromInput;
    @FXML
    private PasswordField passwordInput;
    @FXML
    private TextField toInput;
    @FXML
    private TextField nrgInput;
    @FXML
    private TextField nrgPriceInput;
    @FXML
    private TextField valueInput;
    @FXML
    private Label txStatusLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        reloadAccounts();
        setDefaults();
    }

    private void setDefaults() {
        nrgInput.setText(DEFAULT_NRG);
        nrgPriceInput.setText(DEFAULT_NRG_PRICE);

        toInput.setText("");
        valueInput.setText("");
        passwordInput.setText("");
    }

    private void reloadAccounts() {
        fromInput.setItems(FXCollections.observableArrayList(blockchainConnector.getAccounts()));
    }

    public void onSendAionClicked() {
        try {
            final String txHash = sendAion();
            this.setDefaults();
            this.displayTxStatus(txHash);
        } catch (ValidationException e) {
            txStatusLabel.setText(e.getMessage() != null ? e.getMessage() : "An error has occured");
        }

    }

    private String sendAion() throws ValidationException {
        SendRequestDTO dto = mapFormData();
        txStatusLabel.setText("Unlocking wallet");
        if (!blockchainConnector.unlock(dto)) {
            throw new ValidationException("Failed to unlock wallet");
        }
        txStatusLabel.setText("Sending transaction");
        return blockchainConnector.sendTransaction(dto);
    }

    private void displayTxStatus(final String txHash) {
        txStatusLabel.setText("Transaction pending");
        final Timer timer = new Timer();
        timer.schedule(new TransactionStatusTimedTask(timer, txHash, MAX_TX_STATUS_RETRY_COUNT), BLOCK_MINING_TIME, BLOCK_MINING_TIME);
    }

    private SendRequestDTO mapFormData() {
        SendRequestDTO dto = new SendRequestDTO();
        dto.setFrom((String) fromInput.getValue());
        dto.setTo(toInput.getText());
        dto.setPassword(passwordInput.getText());
        dto.setNrg(TypeConverter.StringNumberAsBigInt(nrgInput.getText()).longValue());
        dto.setNrgPrice(TypeConverter.StringNumberAsBigInt(nrgPriceInput.getText()).longValue());
        dto.setValue(BalanceFormatter.extractBalance(valueInput.getText()));
        return dto;
    }

    private class TransactionStatusTimedTask extends TimerTask {
        private final Timer timer;
        private final String txHash;
        private final int maxRetryCount;
        private int retryCount = 0;

        private TransactionStatusTimedTask(Timer timer, String txHash, int maxRetryCount) {
            this.timer = timer;
            this.txHash = txHash;
            this.maxRetryCount = maxRetryCount;
        }

        @Override
        public void run() {
            Platform.runLater(() -> {
                if (retryCount >= maxRetryCount) {
                    purge();
                    setTxStatusLabel("Transaction status could not be loaded!");
                    return;
                }
                try {
                    blockchainConnector.getTransaction(txHash);
                    setTxStatusLabel("Transaction finished");
                    purge();
                } catch (NotFoundException e) {
                    retryCount++;
                    if (txStatusLabel.getText().endsWith("...")) {
                        setTxStatusLabel("Transaction pending");
                    }
                    setTxStatusLabel(txStatusLabel.getText() + ".");
                }
            });
        }

        private void purge() {
            timer.cancel();
            timer.purge();
        }

        private void setTxStatusLabel(String value) {
            txStatusLabel.setText(value);

        }
    }

}
