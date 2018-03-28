package org.aion.wallet.ui.components.partials;

import com.google.common.eventbus.EventBus;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.WindowControlsEvent;

public class WindowControls {

    private final WindowControlsEvent closeEvent = new WindowControlsEvent(WindowControlsEvent.Type.CLOSE, null);
    private final EventBus eventBus = EventBusFactory.getInstance().getBus(WindowControlsEvent.ID);

    @FXML
    private void minimize(final MouseEvent mouseEvent) {
        Node source = (Node) mouseEvent.getSource();
        final WindowControlsEvent minimizeEvent = new WindowControlsEvent(WindowControlsEvent.Type.MINIMIZE, source);
        eventBus.post(minimizeEvent);
    }

    @FXML
    private void close() {
        eventBus.post(closeEvent);
    }
}
