package org.aion.wallet.util;

import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.TimerEvent;

import java.util.TimerTask;

public class DataUpdater extends TimerTask {

    public static final String FOOTER_BUS_EVENT_ID = "footer";
    private final EventBus eventBus = EventBusFactory.getInstance().getBus(FOOTER_BUS_EVENT_ID);

    @Override
    public void run() {
        Platform.runLater(() -> {
            eventBus.post(new TimerEvent(null));
            eventBus.post(new TimerEvent(null));
        });
    }
}
