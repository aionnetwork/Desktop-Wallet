package org.aion.wallet.ui.components;

import com.google.common.eventbus.Subscribe;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.aion.api.log.LogEnum;
import org.aion.base.util.TypeConverter;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.dto.SendTransactionDTO;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.events.*;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.util.*;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class SendController extends AbstractController {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private static final String PENDING_MESSAGE = "Sending transaction...";

    private static final String SUCCESS_MESSAGE = "Transaction finished";

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
    private Button sendButton;

    private AccountDTO account;

    private boolean connected;

    @Override
    protected void registerEventBusConsumer() {
        super.registerEventBusConsumer();
        EventBusFactory.getBus(HeaderPaneButtonEvent.ID).register(this);
        EventBusFactory.getBus(AccountEvent.ID).register(this);
    }

    @Override
    protected void internalInit(final URL location, final ResourceBundle resources) {
        setDefaults();
        if (!ConfigUtils.isEmbedded()) {
            passwordInput.setVisible(false);
            passwordInput.setManaged(false);
        }
    }

    @Override
    protected void refreshView(final RefreshEvent event) {
        switch (event.getType()) {
            case CONNECTED:
                connected = true;
                if (account != null) {
                    sendButton.setDisable(false);
                }
                break;
            case DISCONNECTED:
                connected = false;
                sendButton.setDisable(true);
                break;
            case TRANSACTION_FINISHED:
                setDefaults();
                break;
            default:
        }
    }

    public void onSendAionClicked() {
        if (account == null) {
            return;
        }
        final SendTransactionDTO dto;
        try {
            dto = mapFormData();
        } catch (ValidationException e) {
            log.error(e.getMessage(), e);
            displayStatus(e.getMessage(), true);
            return;
        }
        displayStatus(PENDING_MESSAGE, false);

        final Task<String> sendTransactionTask = getApiTask(this::sendTransaction, dto);

        runApiTask(
                sendTransactionTask,
                evt -> handleTransactionFinished(sendTransactionTask.getValue()),
                getErrorEvent(t -> Optional.ofNullable(t.getCause()).ifPresent(cause -> displayStatus(cause.getMessage(), true)), sendTransactionTask),
                getEmptyEvent()
        );
    }

    private void handleTransactionFinished(final String txHash) {
        log.info("%s: %s", SUCCESS_MESSAGE, txHash);
        displayStatus(SUCCESS_MESSAGE, false);
        EventPublisher.fireTransactionFinished();
    }

    private void displayStatus(final String message, final boolean isError) {
        if (isError) {
            txStatusLabel.getStyleClass().add(ERROR_STYLE);
        } else {
            txStatusLabel.getStyleClass().removeAll(ERROR_STYLE);
        }
        txStatusLabel.setText(message);
    }

    private String sendTransaction(final SendTransactionDTO sendTransactionDTO) {
        try {
            return blockchainConnector.sendTransaction(sendTransactionDTO);
        } catch (ValidationException e) {
            throw new RuntimeException(e);
        }
    }

    private void setDefaults() {
        nrgInput.setText(AionConstants.DEFAULT_NRG);
        nrgPriceInput.setText(AionConstants.DEFAULT_NRG_PRICE.toString());

        toInput.setText("");
        valueInput.setText("");
        passwordInput.setText("");
    }

    @Subscribe
    private void handleAccountEvent(final AccountEvent event) {
        final AccountDTO account = event.getAccount();
        if (AccountEvent.Type.CHANGED.equals(event.getType())) {
            if (account.isActive()) {
                this.account = account;
                sendButton.setDisable(!connected);
                accountAddress.setText(this.account.getPublicAddress());
                accountBalance.setVisible(true);
                setAccountBalanceText();
            }
        } else if (AccountEvent.Type.LOCKED.equals(event.getType())) {
            if (account.equals(this.account)) {
                sendButton.setDisable(true);
                accountAddress.setText("");
                accountBalance.setVisible(false);
                this.account = null;
            }
        }
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
        if (account == null) {
            return;
        }
        final Task<BigInteger> getBalanceTask = getApiTask(blockchainConnector::getBalance, account.getPublicAddress());
        runApiTask(
                getBalanceTask,
                evt -> {
                    account.setBalance(BalanceUtils.formatBalance(getBalanceTask.getValue()));
                    setAccountBalanceText();
                },
                getErrorEvent(throwable -> {
                }, getBalanceTask),
                getEmptyEvent()
        );
    }

    private SendTransactionDTO mapFormData() throws ValidationException {
        final SendTransactionDTO dto = new SendTransactionDTO();
        dto.setFrom(account.getPublicAddress());

        if (!AddressUtils.isValid(toInput.getText())) {
            throw new ValidationException("Address is not a valid AION address!");
        }
        dto.setTo(toInput.getText());

        try {
            final long nrg = TypeConverter.StringNumberAsBigInt(nrgInput.getText()).longValue();
            if (nrg <= 0) {
                throw new ValidationException("Nrg must be greater than 0!");
            }
            dto.setNrg(nrg);
        } catch (NumberFormatException e) {
            throw new ValidationException("Nrg must be a valid number!");
        }

        try {
            final BigInteger nrgPrice = TypeConverter.StringNumberAsBigInt(nrgPriceInput.getText());
            dto.setNrgPrice(nrgPrice);
            if (nrgPrice.compareTo(AionConstants.DEFAULT_NRG_PRICE) < 0) {
                throw new ValidationException(String.format("Nrg price must be greater than %s!", AionConstants.DEFAULT_NRG_PRICE));
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("Nrg price must be a valid number!");
        }

        try {
            final BigInteger value = BalanceUtils.extractBalance(valueInput.getText());
            if (value.compareTo(BigInteger.ZERO) <= 0) {
                throw new ValidationException("Amount must be greater than 0");
            }
            dto.setValue(value);
        } catch (NumberFormatException e) {
            throw new ValidationException("Amount must be a number");
        }

        return dto;
    }

}
