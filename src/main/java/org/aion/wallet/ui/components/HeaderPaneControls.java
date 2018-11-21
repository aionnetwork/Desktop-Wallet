package org.aion.wallet.ui.components;

import com.google.common.eventbus.Subscribe;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import org.aion.api.log.LogEnum;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.events.*;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.util.BalanceUtils;
import org.aion.wallet.util.UIUtils;
import org.aion.wallet.util.URLManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class HeaderPaneControls extends AbstractController {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private static final String STYLE_DEFAULT = "header-button-default";

    private static final String STYLE_PRESSED = "header-button-pressed";

    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();

    private final Map<Node, HeaderPaneButtonEvent> headerButtons = new HashMap<>();

    @FXML
    private TextField accountBalance;
    @FXML
    private TextField activeAccount;
    @FXML
    private Label activeAccountLabel;
    @FXML
    private VBox homeButton;
    @FXML
    private VBox sendButton;
    @FXML
    private VBox receiveButton;
    @FXML
    private VBox historyButton;
    @FXML
    private VBox settingsButton;

    @FXML
    private HBox toggleTokenBalance;

    private String accountAddress = "";

    @Override
    public void internalInit(URL location, ResourceBundle resources) {
        headerButtons.put(homeButton, new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.OVERVIEW));
        headerButtons.put(sendButton, new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.SEND));
        headerButtons.put(receiveButton, new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.RECEIVE));
        headerButtons.put(historyButton, new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.HISTORY));
//        headerButtons.put(contractsButton, new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.CONTRACTS));
        headerButtons.put(settingsButton, new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.SETTINGS));

        clickButton(homeButton);
    }

    @Override
    protected void registerEventBusConsumer() {
        super.registerEventBusConsumer();
        EventBusFactory.getBus(AccountEvent.ID).register(this);
    }

    public void openAionWebSite() {
        URLManager.openDashboard();
    }

    public void handleButtonPressed(final MouseEvent pressed) {
        for (final Node headerButton : headerButtons.keySet()) {
            headerButton.getStyleClass().clear();
            if (pressed.getSource().equals(headerButton)) {
                headerButton.getStyleClass().add(STYLE_PRESSED);
                setStyleToChildren(headerButton, "header-button-label-pressed");
                HeaderPaneButtonEvent headerPaneButtonEvent = headerButtons.get(headerButton);
                sendPressedEvent(headerPaneButtonEvent);
            } else {
                headerButton.getStyleClass().add(STYLE_DEFAULT);
                setStyleToChildren(headerButton, "header-button-label");
            }
        }
    }

    private void setStyleToChildren(Node headerButton, String styleClassToSet) {
        VBox vbox = (VBox) ((VBox) headerButton).getChildren().get(0);
        ObservableList<String> styleClass = vbox.getChildren().get(0).getStyleClass();
        styleClass.clear();
        styleClass.add(styleClassToSet);
    }

    private void clickButton(final Node button) {
        final double layoutX = button.getLayoutX();
        final double layoutY = button.getLayoutY();
        final MouseEvent clickOnButton = new MouseEvent(MouseEvent.MOUSE_CLICKED,
                layoutX, layoutY, layoutX, layoutY, MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, true, false, false, null);
        Event.fireEvent(button, clickOnButton);
    }

    private void sendPressedEvent(final HeaderPaneButtonEvent event) {
        EventBusFactory.getBus(HeaderPaneButtonEvent.ID).post(event);
    }

    @Subscribe
    private void handleAccountEvent(final AccountEvent event) {
        final AccountDTO account = event.getPayload();
        if (EnumSet.of(AccountEvent.Type.CHANGED, AccountEvent.Type.ADDED).contains(event.getType())) {
            if (account.isActive()) {
                accountBalance.setText(account.getFormattedBalance() + BalanceUtils.CCY_SEPARATOR + account.getCurrency());
                accountBalance.setVisible(true);
                activeAccount.setText(account.getName());
                activeAccountLabel.setVisible(true);
                accountAddress = account.getPublicAddress();
                UIUtils.setWidth(activeAccount);
                UIUtils.setWidth(accountBalance);
            }
            toggleTokenBalance.setVisible(true);
        } else if (AccountEvent.Type.LOCKED.equals(event.getType())) {
            if (account.getPublicAddress().equals(accountAddress)) {
                accountAddress = "";
                activeAccountLabel.setVisible(false);
                accountBalance.setVisible(false);
                activeAccount.setText("");
            }
            toggleTokenBalance.setVisible(false);
        }
    }

    @Override
    protected final void refreshView(final RefreshEvent event) {
        if (!accountAddress.isEmpty()) {
            final String[] text = accountBalance.getText().split(BalanceUtils.CCY_SEPARATOR);
            final String currency = text[1];
            final Task<BigInteger> getBalanceTask = getApiTask(blockchainConnector::getBalance, accountAddress);
            runApiTask(
                    getBalanceTask,
                    evt -> updateNewBalance(currency, getBalanceTask.getValue()),
                    getErrorEvent(t -> {
                    }, getBalanceTask),
                    getEmptyEvent()
            );
        }
    }

    private void updateNewBalance(final String currency, final BigInteger bigInteger) {
        final String newBalance = BalanceUtils.formatBalance(bigInteger) + BalanceUtils.CCY_SEPARATOR + currency;
        if (!newBalance.equalsIgnoreCase(accountBalance.getText())) {
            accountBalance.setText(newBalance);
            UIUtils.setWidth(accountBalance);
        }
    }

    public void openTokenBalance(MouseEvent mouseEvent) {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setAutoFix(true);

        Pane tokenBalanceDialog;
        try {
            tokenBalanceDialog = FXMLLoader.load(getClass().getResource("partials/TokenBalance.fxml"));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return;
        }

        Node eventSource = (Node) mouseEvent.getSource();
        final double windowX = eventSource.getScene().getWindow().getX();
        final double windowY = eventSource.getScene().getWindow().getY();
        popup.setX(windowX + eventSource.getScene().getWidth() / 1.07 - tokenBalanceDialog.getPrefWidth() / 1.07);
        popup.setY(windowY + eventSource.getScene().getHeight() / 4.75 - tokenBalanceDialog.getPrefHeight() / 4.75);
        popup.getContent().addAll(tokenBalanceDialog);
        popup.show(eventSource.getScene().getWindow());

        EventPublisher.fireOpenTokenBalances(accountAddress);
    }

}
