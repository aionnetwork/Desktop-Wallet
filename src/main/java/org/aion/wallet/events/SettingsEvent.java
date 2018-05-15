package org.aion.wallet.events;

import org.aion.wallet.dto.LightAppSettings;

public class SettingsEvent extends AbstractEvent<SettingsEvent.Type> {

    public static final String ID = "settings.changed";

    private final LightAppSettings settings;

    public SettingsEvent(final Type type, final LightAppSettings settings) {
        super(type);

        this.settings = settings;
    }

    public LightAppSettings getSettings() {
        return settings;
    }

    public enum Type {
        CHANGED
    }
}
