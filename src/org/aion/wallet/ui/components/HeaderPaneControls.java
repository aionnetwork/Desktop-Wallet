package org.aion.wallet.ui.components;

import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import org.aion.log.AionLoggerFactory;
import org.aion.log.LogEnum;
import org.aion.wallet.ui.events.EventBusFactory;
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

    @FXML
    private AnchorPane homeButton;
    @FXML
    private AnchorPane sendButton;
    @FXML
    private AnchorPane receiveButton;
    @FXML
    private AnchorPane historyButton;
    @FXML
    private AnchorPane contractsButton;
    @FXML
    private AnchorPane settingsButton;

    private final Map<AnchorPane, HeaderPaneButtonEvent> headerButtons = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        headerButtons.put(homeButton, new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.HOME));
        headerButtons.put(sendButton, new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.SEND));
        headerButtons.put(receiveButton, new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.RECEIVE));
        headerButtons.put(historyButton, new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.HISTORY));
        headerButtons.put(contractsButton, new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.CONTRACTS));
        headerButtons.put(settingsButton, new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.SETTINGS));

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
        for (final AnchorPane headerButton : headerButtons.keySet()) {
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
}
