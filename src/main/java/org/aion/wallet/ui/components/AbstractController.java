package org.aion.wallet.ui.components;

import com.google.common.eventbus.Subscribe;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.TimerEvent;
import org.aion.wallet.util.DataUpdater;

import java.net.URL;
import java.util.ResourceBundle;

public abstract class AbstractController implements Initializable {

    @FXML
    private Node parent;

    @Override
    public final void initialize(final URL location, final ResourceBundle resources) {
        registerEventBusConsumer();
        internalInit(location, resources);
    }

    protected void registerEventBusConsumer() {
        EventBusFactory.getBus(DataUpdater.UI_DATA_REFRESH).register(this);
    }

    @Subscribe
    private void handleConnectivityStatusEvent(final TimerEvent event) {
        if (isInView()) {
            refreshView();
        }
    }

    protected final boolean isInView() {
        return parent.isVisible();
    }

    protected void refreshView() {
    }

    protected abstract void internalInit(final URL location, final ResourceBundle resources);
}
