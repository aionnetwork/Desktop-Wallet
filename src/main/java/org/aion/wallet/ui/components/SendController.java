package org.aion.wallet.ui.components;

import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.aion.base.util.TypeConverter;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.dto.SendRequestDTO;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.exception.NotFoundException;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.EventPublisher;
import org.aion.wallet.ui.events.HeaderPaneButtonEvent;
import org.aion.wallet.util.AionConstants;
import org.aion.wallet.util.BalanceUtils;
import org.aion.wallet.util.ConfigUtils;
import org.aion.wallet.util.UIUtils;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class SendController implements Initializable {

    private static final int MAX_TX_STATUS_RETRY_COUNT = 6;

    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();

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
    @FXML
    private TextArea accountAddress;
    @FXML
    private TextField accountBalance;
    @FXML
    private TextField equivalentEUR;
    @FXML
    private TextField equivalentUSD;

    private AccountDTO account;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        registerEventBusConsumer();
        setDefaults();
        if (!ConfigUtils.isEmbedded()) {
            passwordInput.setVisible(false);
            passwordInput.setManaged(false);
        }
    }

    @Subscribe
    private void handleAccountChanged(AccountDTO account) {
        this.account = account;

        accountAddress.setText(account.getPublicAddress());

        accountBalance.setVisible(true);
        setAccountBalanceText();

        equivalentEUR.setVisible(true);
        equivalentEUR.setText(Double.parseDouble(account.getBalance()) * AionConstants.AION_TO_EUR + " " + AionConstants.EUR_CCY);
        UIUtils.setWidth(equivalentEUR);

        equivalentUSD.setVisible(true);
        equivalentUSD.setText(Double.parseDouble(account.getBalance()) * AionConstants.AION_TO_USD + " " + AionConstants.USD_CCY);
        UIUtils.setWidth(equivalentUSD);
    }

    @Subscribe
    private void handleHeaderPaneButtonEvent(HeaderPaneButtonEvent event) {
        if (event.getType().equals(HeaderPaneButtonEvent.Type.SEND)) {
            refreshAccountBalance();
        }
    }

    private void setAccountBalanceText() {
        accountBalance.setText(account.getBalance() + " " + AionConstants.CCY);
        UIUtils.setWidth(accountBalance);

    }

    private void refreshAccountBalance() {
        if (this.account == null) {
            return;
        }
        this.account.setBalance(BalanceUtils.formatBalance(blockchainConnector.getBalance(this.account.getPublicAddress())));
        setAccountBalanceText();
    }

    private void registerEventBusConsumer() {
        EventBusFactory.getBus(HeaderPaneButtonEvent.ID).register(this);
        EventBusFactory.getBus(EventPublisher.ACCOUNT_CHANGE_EVENT_ID).register(this);
    }

    private void setDefaults() {
        nrgInput.setText(AionConstants.DEFAULT_NRG);
        nrgPriceInput.setText(BalanceUtils.formatBalance(AionConstants.DEFAULT_NRG_PRICE));

        toInput.setText("");
        valueInput.setText("");
        passwordInput.setText("");
    }

    public void onSendAionClicked() {
        Platform.runLater(() -> {
            if (account == null) {
                txStatusLabel.setText("You must select an account before sending Aion!");
                return;
            }
            try {
                final String txHash = sendAion();
                setDefaults();
                displayTxStatus(txHash);
            } catch (ValidationException e) {
                txStatusLabel.setText(e.getMessage() != null ? e.getMessage() : "An error has occured");
            }
        });
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

    private SendRequestDTO mapFormData() throws ValidationException {
        SendRequestDTO dto = new SendRequestDTO();
        dto.setFrom(account.getPublicAddress());
        dto.setTo(toInput.getText());
        dto.setPassword(passwordInput.getText());
        try {
            dto.setNrg(TypeConverter.StringNumberAsBigInt(nrgInput.getText()).longValue());
        } catch (NumberFormatException e) {
            throw new ValidationException("Nrg must be a valid number");
        }
        try {
            dto.setNrgPrice(BalanceUtils.extractBalance(nrgPriceInput.getText()).longValue());
        } catch (NumberFormatException e) {
            throw new ValidationException("Nrg price must be a valid number");
        }
        try {
            dto.setValue(BalanceUtils.extractBalance(valueInput.getText()));
        } catch (NumberFormatException e) {
            throw new ValidationException("Value must be a number");
        }
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
