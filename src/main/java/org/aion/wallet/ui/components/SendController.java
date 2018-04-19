package org.aion.wallet.ui.components;

import com.google.common.eventbus.Subscribe;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.aion.base.util.TypeConverter;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.dto.SendRequestDTO;
import org.aion.wallet.dto.AccountDTO;
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

public class SendController extends AbstractController {

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
    protected void internalInit(final URL location, final ResourceBundle resources) {
        setDefaults();
        if (!ConfigUtils.isEmbedded()) {
            passwordInput.setVisible(false);
            passwordInput.setManaged(false);
        }
    }

    @Subscribe
    private void handleAccountChanged(final AccountDTO account) {
        this.account = account;

        accountAddress.setText(account.getPublicAddress());

        accountBalance.setVisible(true);
        setAccountBalanceText();

        equivalentEUR.setVisible(true);
        equivalentEUR.setText(convertBalanceToCcy(account, AionConstants.AION_TO_EUR) + " " + AionConstants.EUR_CCY);
        UIUtils.setWidth(equivalentEUR);

        equivalentUSD.setVisible(true);
        equivalentUSD.setText(convertBalanceToCcy(account, AionConstants.AION_TO_USD) + " " + AionConstants.USD_CCY);
        UIUtils.setWidth(equivalentUSD);
    }

    private double convertBalanceToCcy(final AccountDTO account, final double exchangeRate) {
        return Double.parseDouble(account.getBalance()) * exchangeRate;
    }

    @Subscribe
    private void handleHeaderPaneButtonEvent(final HeaderPaneButtonEvent event) {
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

    protected void registerEventBusConsumer() {
        super.registerEventBusConsumer();
        EventBusFactory.getBus(HeaderPaneButtonEvent.ID).register(this);
        EventBusFactory.getBus(EventPublisher.ACCOUNT_CHANGE_EVENT_ID).register(this);
    }

    private void setDefaults() {
        nrgInput.setText(AionConstants.DEFAULT_NRG);
        nrgPriceInput.setText(AionConstants.DEFAULT_NRG_PRICE.toString());

        toInput.setText("");
        valueInput.setText("");
        passwordInput.setText("");
    }

    public void onSendAionClicked() {
        final SendRequestDTO dto;
        try {
            dto = mapFormData();
        } catch (ValidationException e) {
            e.printStackTrace();
            return;
        }
        txStatusLabel.setText("Sending transaction...");

        final Task<String> sendTransactionTask = getApiTask(this::sendTransaction, dto);

        final EventHandler<WorkerStateEvent> successHandler = evt -> {
            setDefaults();
            txStatusLabel.setText("Transaction Finished");
        };
        runApiTask(
                sendTransactionTask,
                successHandler,
                evt -> txStatusLabel.setText("An error has occurred: " + sendTransactionTask.getException().getMessage()),
                getEmptyEvent()
        );
    }

    private String sendTransaction(final SendRequestDTO sendRequestDTO) {
        try {
            return blockchainConnector.sendTransaction(sendRequestDTO);
        } catch (ValidationException e) {
            throw new RuntimeException(e);
        }
    }

    private SendRequestDTO mapFormData() throws ValidationException {
        final SendRequestDTO dto = new SendRequestDTO();
        dto.setFrom(account.getPublicAddress());
        dto.setTo(toInput.getText());
        dto.setPassword(passwordInput.getText());

        try {
            dto.setNrg(TypeConverter.StringNumberAsBigInt(nrgInput.getText()).longValue());
        } catch (NumberFormatException e) {
            throw new ValidationException("Nrg must be a valid number");
        }

        try {
            dto.setNrgPrice(TypeConverter.StringNumberAsBigInt(nrgPriceInput.getText()));
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

}
