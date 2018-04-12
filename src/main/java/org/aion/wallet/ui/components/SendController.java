package org.aion.wallet.ui.components;

import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.aion.base.util.TypeConverter;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.dto.SendRequestDTO;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.exception.NotFoundException;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.EventPublisher;
import org.aion.wallet.util.AionConstants;
import org.aion.wallet.util.BalanceFormatter;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class SendController implements Initializable {

    private static final int MAX_TX_STATUS_RETRY_COUNT = 6;

    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();

    @FXML
    private Label fromLabel;
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

    private AccountDTO account;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        registerEventBusConsumer();
        setDefaults();
    }

    @Subscribe
    private void handleAccountChanged(AccountDTO account) {
        this.account = account;
        fromLabel.setText("From: " + account.getPublicAddress());
    }

    private void registerEventBusConsumer() {
        EventBusFactory.getBus(EventPublisher.ACCOUNT_CHANGE_EVENT_ID).register(this);
    }

    private void setDefaults() {
        fromLabel.setText("Please select an account!");
        nrgInput.setText(AionConstants.DEFAULT_NRG);
        nrgPriceInput.setText(AionConstants.DEFAULT_NRG_PRICE);

        toInput.setText("");
        valueInput.setText("");
        passwordInput.setText("");
    }

    public void onSendAionClicked() {
        if (account == null) {
            txStatusLabel.setText("You must select an account before sending Aion!");
            return;
        }
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
        txStatusLabel.setText("Sending transaction");
        return blockchainConnector.sendTransaction(dto);
    }

    private void displayTxStatus(final String txHash) {
        txStatusLabel.setText("Transaction pending");
        final Timer timer = new Timer();
        timer.schedule(
                new TransactionStatusTimedTask(
                        timer,
                        txHash,
                        MAX_TX_STATUS_RETRY_COUNT),
                AionConstants.BLOCK_MINING_TIME_MILLIS,
                AionConstants.BLOCK_MINING_TIME_MILLIS
        );
    }

    private SendRequestDTO mapFormData() {
        SendRequestDTO dto = new SendRequestDTO();
        dto.setFrom(account.getPublicAddress());
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
