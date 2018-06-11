package org.aion.wallet.ui.components;

import com.google.common.eventbus.Subscribe;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import org.aion.api.log.LogEnum;
import org.aion.base.util.TypeConverter;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.dto.SendTransactionDTO;
import org.aion.wallet.connector.dto.TransactionResponseDTO;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.events.*;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.ui.components.partials.TransactionResubmissionDialog;
import org.aion.wallet.util.*;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class SendController extends AbstractController {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private static final String PENDING_MESSAGE = "Sending transaction...";

    private static final String SUCCESS_MESSAGE = "Transaction finished";

    private static final Tooltip NRG_LIMIT_TOOLTIP = new Tooltip("NRG limit");

    private static final Tooltip NRG_PRICE_TOOLTIP = new Tooltip("NRG price");

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
    @FXML
    private Label timedoutTransactionsLabel;

    private AccountDTO account;

    private boolean connected;

    private TransactionResubmissionDialog transactionResubmissionDialog = new TransactionResubmissionDialog();

    private SendTransactionDTO transactionToResubmit;

    @Override
    protected void registerEventBusConsumer() {
        super.registerEventBusConsumer();
        EventBusFactory.getBus(HeaderPaneButtonEvent.ID).register(this);
        EventBusFactory.getBus(AccountEvent.ID).register(this);
        EventBusFactory.getBus(TransactionEvent.ID).register(this);
    }

    @Override
    protected void internalInit(final URL location, final ResourceBundle resources) {
        nrgInput.setTooltip(NRG_LIMIT_TOOLTIP);
        nrgPriceInput.setTooltip(NRG_PRICE_TOOLTIP);
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
        setTimedoutTransactionsLabelText();
    }

    public void onSendAionClicked() {
        if (account == null) {
            return;
        }
        final SendTransactionDTO dto;
        try {
            if(transactionToResubmit != null) {
                dto = transactionToResubmit;
            }
            else {
                dto = mapFormData();
            }
        } catch (ValidationException e) {
            log.error(e.getMessage(), e);
            displayStatus(e.getMessage(), true);
            return;
        }
        displayStatus(PENDING_MESSAGE, false);

        final Task<TransactionResponseDTO> sendTransactionTask = getApiTask(this::sendTransaction, dto);

        runApiTask(
                sendTransactionTask,
                evt -> handleTransactionFinished(sendTransactionTask.getValue().getTxHash().toString()),
                getErrorEvent(t -> Optional.ofNullable(t.getCause()).ifPresent(cause -> displayStatus(cause.getMessage(), true)), sendTransactionTask),
                getEmptyEvent()
        );
    }

    public void onTimedoutTransactionsClick(MouseEvent mouseEvent) {
        transactionResubmissionDialog.open(mouseEvent);
    }

    private void handleTransactionFinished(final String txHash) {
        setTimedoutTransactionsLabelText();
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

    private TransactionResponseDTO sendTransaction(final SendTransactionDTO sendTransactionDTO) {
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

        setTimedoutTransactionsLabelText();
    }

    private void setTimedoutTransactionsLabelText() {
        if(account != null) {
            final List<SendTransactionDTO> timedoutTransactions = blockchainConnector.getAccountManager().getTimedoutTransactions(account.getPublicAddress());
            if(!timedoutTransactions.isEmpty()) {
                timedoutTransactionsLabel.setVisible(true);
                timedoutTransactionsLabel.getStyleClass().add("warning-link-style");
                timedoutTransactionsLabel.setText("You have transactions that require your attention!");
            }
        }
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

    @Subscribe
    private void handleTransactionResubmitEvent(final TransactionEvent event) {
        SendTransactionDTO sendTransaction = event.getTransaction();
        toInput.setText(sendTransaction.getTo());
        nrgInput.setText(sendTransaction.getNrg().toString());
        nrgPriceInput.setText(String.valueOf(sendTransaction.getNrgPrice()*2));
        valueInput.setText(BalanceUtils.formatBalance(sendTransaction.getValue()));
        txStatusLabel.setText("");
        timedoutTransactionsLabel.setVisible(false);
        transactionToResubmit = sendTransaction;
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
                getErrorEvent(t -> {}, getBalanceTask),
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
