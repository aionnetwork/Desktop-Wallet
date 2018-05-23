package org.aion.wallet.events;

import com.google.common.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;

public class EventBusFactory {

    private static final EventBusFactory INSTANCE = new EventBusFactory();

    private final Map<String, EventBus> busMap = new HashMap<>();

    public static EventBus getBus(final String identifier) {
        return INSTANCE.getBusById(identifier);
    }

    private EventBusFactory() {}

    private EventBus getBusById(final String identifier) {
        return busMap.computeIfAbsent(identifier, EventBus::new);
    }
}
