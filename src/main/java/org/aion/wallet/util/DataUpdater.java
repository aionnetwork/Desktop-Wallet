package org.aion.wallet.util;

import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import org.aion.wallet.events.EventBusFactory;
import org.aion.wallet.events.RefreshEvent;

import java.util.TimerTask;

public class DataUpdater extends TimerTask {

    private final EventBus eventBus = EventBusFactory.getBus(RefreshEvent.ID);

    @Override
    public void run() {
        Platform.runLater(() -> eventBus.post(new RefreshEvent(RefreshEvent.Type.TIMER, null)));
    }
}
