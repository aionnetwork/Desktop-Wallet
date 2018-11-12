package org.aion.wallet.ui.components.partials;

import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.InputEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.aion.api.log.LogEnum;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.dto.TokenDetails;
import org.aion.wallet.events.AccountEvent;
import org.aion.wallet.events.EventBusFactory;
import org.aion.wallet.events.EventPublisher;
import org.aion.wallet.events.UiMessageEvent;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.util.BalanceUtils;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TokenBalanceController implements Initializable {
    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());
    private static final int DISPLAY_DECIMALS = 6;
    private static final String ROW = "transaction-row";
    private static final String ROW_TEXT = "transaction-row-text";

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();

    @FXML
    private AnchorPane tokenBalancePane;
    @FXML
    private VBox tokenBalances;
    @FXML
    private VBox customTokenForm;
    @FXML
    private Label customTokenLink;
    @FXML
    private TextField customTokenContractAddress;
    @FXML
    private Label customTokenValidation;
    @FXML
    private ScrollPane tokenBalancesScrollPane;

    private String accountAddress;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        registerEventBusConsumer();
    }

    @Subscribe
    private void handleAccountChanged(final AccountEvent event) {
        if (AccountEvent.Type.LOCKED.equals(event.getType())) {
            if (event.getPayload().getPublicAddress().equals(accountAddress)) {
                accountAddress = null;
            }
            tokenBalancePane.setVisible(false);
        }
    }

    @Subscribe
    private void handleOpenRequest(final UiMessageEvent event) {
        if (UiMessageEvent.Type.TOKEN_BALANCES_SHOW.equals(event.getType())) {
            accountAddress = event.getMessage();
            backgroundExecutor.submit(() -> Platform.runLater(this::displayListOfBalances));
        }
    }

    private void registerEventBusConsumer() {
        EventBusFactory.getBus(AccountEvent.ID).register(this);
        EventBusFactory.getBus(UiMessageEvent.ID).register(this);
    }

    public void close(final InputEvent eventSource) {
        ((Node) eventSource.getSource()).getScene().getWindow().hide();
    }

    private void displayListOfBalances() {
        final List<TokenDetails> accountTokenDetails = blockchainConnector.getAccountTokenDetails(accountAddress);
        if (accountTokenDetails.size() > 0) {
            tokenBalancesScrollPane.setVisible(true);
            for (TokenDetails tokenDetails : accountTokenDetails) {
                tokenBalances.getChildren().add(getTokenRow(tokenDetails));
            }
        } else {
            tokenBalancesScrollPane.setVisible(false);
        }
    }

    private HBox getTokenRow(final TokenDetails tokenDetails) {
        final HBox row = new HBox();
        row.setSpacing(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add(ROW);

        final Label tokenSymbol = getSymbolLabel(tokenDetails);
        final Label labelBalance = getBalanceLabel(tokenDetails);

        row.getChildren().add(tokenSymbol);
        row.getChildren().add(labelBalance);
        return row;
    }

    private Label getSymbolLabel(final TokenDetails tokenDetails) {
        Label tokenSymbol = new Label(tokenDetails.getSymbol());
        tokenSymbol.setPrefWidth(70);
        tokenSymbol.getStyleClass().add(ROW_TEXT);
        return tokenSymbol;
    }

    private Label getBalanceLabel(final TokenDetails tokenDetails) {
        BigInteger tokenBalance;
        try {
            tokenBalance = blockchainConnector.getTokenBalance(tokenDetails.getContractAddress(), accountAddress);
        } catch (ValidationException e) {
            log.error(e.getMessage());
            tokenBalance = BigInteger.ZERO;
        }
        final String balance = BalanceUtils.formatBalanceWithNumberOfDecimals(tokenBalance, DISPLAY_DECIMALS);
        final Label labelBalance = new Label(balance);
        labelBalance.getStyleClass().add(ROW_TEXT);
        return labelBalance;
    }

    public void addCustomToken() {
        tokenBalancePane.setPrefHeight(400);
        customTokenForm.setVisible(true);
        customTokenLink.setVisible(false);
    }

    public void cancelCustomToken() {
        tokenBalancePane.setPrefHeight(300);
        customTokenForm.setVisible(false);
        customTokenLink.setVisible(true);

        customTokenContractAddress.setText("");

        resetValidationError();
    }

    public void saveCustomToken() {
        resetValidationError();

        try {
            final TokenDetails newToken = blockchainConnector.getTokenDetails(customTokenContractAddress.getText(), accountAddress);
            blockchainConnector.saveToken(newToken);
            blockchainConnector.addAccountToken(accountAddress, newToken.getSymbol());

            reloadTokenList();
            EventPublisher.fireTokenAdded(newToken.getContractAddress());
            cancelCustomToken();
        } catch (ValidationException exception) {
            customTokenValidation.setVisible(true);
            customTokenValidation.setText(exception.getMessage());
        }
    }

    private void reloadTokenList() {
        tokenBalances.getChildren().clear();
        displayListOfBalances();
    }

    private void resetValidationError() {
        customTokenValidation.setVisible(false);
        customTokenValidation.setText("");
    }
}
