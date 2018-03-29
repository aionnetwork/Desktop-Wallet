package org.aion.wallet.ui.events;

import com.google.common.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;

public class EventBusFactory {

    private static final EventBusFactory INSTANCE = new EventBusFactory();

    private final Map<String, EventBus> busMap = new HashMap<>();

    public static EventBusFactory getInstance() {
        return INSTANCE;
    }

    private EventBusFactory() {
    }

    public EventBus getBus(final String identifier) {
        return busMap.computeIfAbsent(identifier, EventBus::new);
    }
}
