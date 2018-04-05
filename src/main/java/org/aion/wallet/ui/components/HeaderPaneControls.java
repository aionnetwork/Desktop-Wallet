package org.aion.wallet.ui.components;

import com.google.common.eventbus.Subscribe;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import org.aion.log.AionLoggerFactory;
import org.aion.log.LogEnum;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.EventPublisher;
import org.aion.wallet.ui.events.HeaderPaneButtonEvent;
import org.slf4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class HeaderPaneControls implements Initializable {

    private static final Logger log = AionLoggerFactory.getLogger(LogEnum.WLT.name());

    private static final String AION_URL = "http://www.aion.network";
    private static final String STYLE_DEFAULT = "default";
    private static final String STYLE_PRESSED = "pressed";
    private static final double DEFAULT_ACCOUNT_WIDTH = 520;
    private static final double DEFAULT_BALANCE_WIDTH = 180;

    private final Map<Node, HeaderPaneButtonEvent> headerButtons = new HashMap<>();

    @FXML
    private TextField accountBalance;
    @FXML
    private TextField activeAccount;
    @FXML
    private VBox homeButton;
    @FXML
    private VBox sendButton;
    @FXML
    private VBox receiveButton;
    @FXML
    private VBox historyButton;
    @FXML
    private VBox contractsButton;
    @FXML
    private VBox settingsButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        EventBusFactory.getInstance().getBus(EventPublisher.ACCOUNT_CHANGE_EVENT_ID).register(this);
        headerButtons.put(homeButton, new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.HOME));
        headerButtons.put(sendButton, new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.SEND));
        headerButtons.put(receiveButton, new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.RECEIVE));
        headerButtons.put(historyButton, new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.HISTORY));
        headerButtons.put(contractsButton, new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.CONTRACTS));
        headerButtons.put(settingsButton, new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.SETTINGS));
        activeAccount.setPrefWidth(DEFAULT_ACCOUNT_WIDTH);
        accountBalance.setPrefWidth(DEFAULT_BALANCE_WIDTH);

        clickButton(homeButton);
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
                HeaderPaneButtonEvent headerPaneButtonEvent = headerButtons.get(headerButton);
                sendPressedEvent(headerPaneButtonEvent);
            } else {
                styleClass.add(STYLE_DEFAULT);
            }
        }
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
        EventBusFactory.getInstance().getBus(HeaderPaneButtonEvent.ID).post(event);
    }

    @Subscribe
    private void handleAccountChanged(AccountDTO account) {
        activeAccount.setText(account.getPublicAddress());
        accountBalance.setText(account.getBalance() + account.getCurrency());
        accountBalance.requestFocus();
    }
}
