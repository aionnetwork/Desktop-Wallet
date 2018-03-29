package org.aion.wallet.ui.components;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import org.aion.base.util.TypeConverter;
import org.aion.log.AionLoggerFactory;
import org.aion.log.LogEnum;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.WalletBlockchainConnector;
import org.aion.wallet.connector.dto.SendRequestDTO;
import org.aion.zero.types.AionTransaction;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

import static org.aion.wallet.util.BalanceFormatter.WEI_MULTIPLIER;

public class SendPane implements Initializable {
    private static final Logger log = AionLoggerFactory.getLogger(LogEnum.WLT.name());
    private final BlockchainConnector blockchainConnector = new WalletBlockchainConnector();
    private final static Long DEFAULT_BLOCK_MINING_TIME = 10000L;
    private final static int MAX_TX_STATUS_RETRY_COUNT = 6;

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

    private final String DEFAULT_NRG = "100000";
    private final String DEFAULT_NRG_PRICE = "1000000";
    // todo: remove debug code
    private String DEFAULT_DEBUG_ADDR_TO_SEND = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        reloadAccounts();
        setDefaults();
    }

    private void setDefaults() {
        toInput.setText(DEFAULT_DEBUG_ADDR_TO_SEND);
        nrgInput.setText(DEFAULT_NRG);
        nrgPriceInput.setText(DEFAULT_NRG_PRICE);
        valueInput.setText("");
        passwordInput.setText("");
    }

    private void reloadAccounts() {
        List<String> accounts = blockchainConnector.getAccounts();
        fromInput.setItems(FXCollections.observableArrayList(accounts));
        // todo: remove debug code
        if (accounts.size() > 1) {
            DEFAULT_DEBUG_ADDR_TO_SEND = accounts.get(1);
        }
    }

    public void sendAion(MouseEvent mouseEvent) {
        SendRequestDTO dto;
        try {
            dto = validateForm();
        } catch (Exception e) {
            txStatusLabel.setText("Invalid data");
            return;
        }
        txStatusLabel.setText("Unlocking wallet");
        if (!blockchainConnector.unlock(dto)) {
            txStatusLabel.setText("Failed to unlock wallet");
            return;
        }
        ;
        txStatusLabel.setText("Sending transaction");
        final byte[] txHash = blockchainConnector.sendTransaction(dto);

        this.setDefaults();
        this.displayTxStatus(txHash);
    }

    private void displayTxStatus(final byte[] txHash) {
        txStatusLabel.setText("Transaction pending");
        final Timer timer = new Timer();
        timer.schedule(new TransactionStatusTimedTask(timer, txHash, MAX_TX_STATUS_RETRY_COUNT), DEFAULT_BLOCK_MINING_TIME, DEFAULT_BLOCK_MINING_TIME);
    }

    private SendRequestDTO validateForm() throws Exception {
        SendRequestDTO dto = new SendRequestDTO();
        dto.setFrom((String) fromInput.getValue());
        dto.setTo(toInput.getText());
        dto.setPassword(passwordInput.getText());
        dto.setNrg(TypeConverter.StringNumberAsBigInt(nrgInput.getText()).longValue());
        dto.setNrgPrice(TypeConverter.StringNumberAsBigInt(nrgPriceInput.getText()).longValue());
        dto.setValue(new BigDecimal(valueInput.getText()).multiply(WEI_MULTIPLIER).toBigInteger());
        if (!isValid(dto.getNrg()) || !isValid(dto.getNrgPrice())
                || dto.getValue() == null) {
            throw new Exception("Invalid tx data");
        }
        return dto;
    }

    private boolean isValid(Long l) {
        return l != null && l > 0;
    }

    private class TransactionStatusTimedTask extends TimerTask {
        private final Timer timer;
        private final byte[] txHash;
        private final int maxRetryCount;
        private int retryCount = 0;

        private TransactionStatusTimedTask(Timer timer, byte[] txHash, int maxRetryCount) {
            this.timer = timer;
            this.txHash = txHash;
            this.maxRetryCount = maxRetryCount;
        }

        @Override
        public void run() {
            Platform.runLater(() -> {
                if(retryCount >= MAX_TX_STATUS_RETRY_COUNT) {
                    purge();
                    setTxStatusLabel("Transaction status could not be loaded!");
                    return;
                }
                final AionTransaction tx = blockchainConnector.getTransaction(txHash);

                if (tx != null) {
                    setTxStatusLabel("Transaction finished");
                    purge();
                } else {
                    retryCount ++;
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
