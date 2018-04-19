package org.aion.wallet.ui.components;

import com.google.common.eventbus.Subscribe;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import org.aion.log.AionLoggerFactory;
import org.aion.log.LogEnum;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.EventPublisher;
import org.aion.wallet.ui.events.HeaderPaneButtonEvent;
import org.aion.wallet.ui.events.TimerEvent;
import org.aion.wallet.util.UIUtils;
import org.aion.wallet.util.BalanceUtils;
import org.aion.wallet.util.DataUpdater;
import org.slf4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

public class HeaderPaneControls implements Initializable {

    private static final Logger log = AionLoggerFactory.getLogger(LogEnum.WLT.name());

    private static final String AION_URL = "http://www.aion.network";

    private static final String STYLE_DEFAULT = "default";

    private static final String STYLE_PRESSED = "pressed";



    private static final String CCY_SEPARATOR = " ";

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
//    @FXML
//    private VBox contractsButton;
    @FXML
    private VBox settingsButton;

    private String accountAddress;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        registerEventBusConsumer();
        headerButtons.put(homeButton, new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.OVERVIEW));
        headerButtons.put(sendButton, new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.SEND));
        headerButtons.put(receiveButton, new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.RECEIVE));
        headerButtons.put(historyButton, new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.HISTORY));
//        headerButtons.put(contractsButton, new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.CONTRACTS));
        headerButtons.put(settingsButton, new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.SETTINGS));

        clickButton(homeButton);
    }

    private void registerEventBusConsumer() {
        EventBusFactory.getBus(EventPublisher.ACCOUNT_CHANGE_EVENT_ID).register(this);
        EventBusFactory.getBus(DataUpdater.UI_DATA_REFRESH).register(this);
    }

    public void openAionWebSite() {
        try {
            Desktop.getDesktop().browse(new URI(AION_URL));
        } catch (IOException | URISyntaxException e) {
            log.error("Exception occurred trying to open website: %s", e.getMessage(), e);
        }
    }

    public void handleButtonPressed(final MouseEvent pressed) {
        for (final Node headerButton : headerButtons.keySet()) {
            ObservableList<String> styleClass = headerButton.getStyleClass();
            styleClass.clear();
            if (pressed.getSource().equals(headerButton)) {
                styleClass.add(STYLE_PRESSED);
                setStyleToChildren(headerButton, "header-button-label-pressed");
                HeaderPaneButtonEvent headerPaneButtonEvent = headerButtons.get(headerButton);
                sendPressedEvent(headerPaneButtonEvent);
            } else {
                styleClass.add(STYLE_DEFAULT);
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
    private void handleAccountChanged(final AccountDTO account) {
        accountBalance.setVisible(true);
        activeAccountLabel.setVisible(true);
        activeAccount.setText(account.getName());
        accountAddress = account.getPublicAddress();
        accountBalance.setText(account.getBalance() + CCY_SEPARATOR + account.getCurrency());
        UIUtils.setWidth(activeAccount);
        UIUtils.setWidth(accountBalance);
    }

    @Subscribe
    private void handleConnectivityStatusEvent(final TimerEvent event) {
        final String accountName = activeAccount.getText();
        if (!accountName.isEmpty()) {
            Optional<BigInteger> balance = Optional.empty();
            try {
                balance = Optional.ofNullable(blockchainConnector.getBalance(accountAddress));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            final String[] text = accountBalance.getText().split(CCY_SEPARATOR);
            final String currency = text[1];
            balance.ifPresent(bigInteger -> updateNewBalance(currency, bigInteger));
        }
    }

    private void updateNewBalance(final String currency, final BigInteger bigInteger) {
        final String newBalance = BalanceUtils.formatBalance(bigInteger) + CCY_SEPARATOR + currency;
        if (newBalance.equalsIgnoreCase(accountBalance.getText())) {
            accountBalance.setText(newBalance);
        }
    }
}
