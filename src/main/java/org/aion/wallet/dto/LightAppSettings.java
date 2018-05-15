package org.aion.wallet.dto;

import org.aion.wallet.storage.ApiType;

import java.time.Duration;
import java.util.Optional;
import java.util.Properties;

public class LightAppSettings {

    public static final String DEFAULT_UNLOCK_TIMEOUT = "3m";

    private static final String ADDRESS = ".address";
    private static final String PORT = ".port";
    private static final String PROTOCOL = ".protocol";
    private static final String ACCOUNTS = "accounts";

    private static final String DEFAULT_IP = "127.0.0.1";
    private static final String DEFAULT_PORT = "8547";
    private static final String DEFAULT_PROTOCOL = "tcp";
    private static final String UNLOCK_TIMEOUT = ".unlock_timeout";


    private final ApiType type;
    private final String address;
    private final String port;
    private final String protocol;
    private final Duration unlockTimeout;

    public LightAppSettings(final Properties lightSettingsProps, final ApiType type) {
        this.type = type;
        address = Optional.ofNullable(lightSettingsProps.getProperty(type + ADDRESS)).orElse(DEFAULT_IP);
        port = Optional.ofNullable(lightSettingsProps.getProperty(type + PORT)).orElse(DEFAULT_PORT);
        protocol = Optional.ofNullable(lightSettingsProps.getProperty(type + PROTOCOL)).orElse(DEFAULT_PROTOCOL);
        unlockTimeout = Duration.parse(Optional.ofNullable(lightSettingsProps.getProperty(ACCOUNTS + UNLOCK_TIMEOUT)).orElse(convertToDurationString(DEFAULT_UNLOCK_TIMEOUT)));
    }

    public LightAppSettings(final String address, final String port, final String protocol, final ApiType type, final String timeout) {
        this.type = type;
        this.address = address;
        this.port = port;
        this.protocol = protocol;
        this.unlockTimeout = Duration.parse(convertToDurationString(timeout));
    }

    public final String getAddress() {
        return address;
    }

    public final String getPort() {
        return port;
    }

    public final String getProtocol() {
        return protocol;
    }

    public ApiType getType() {
        return type;
    }

    public Duration getUnlockTimeout() {
        return unlockTimeout;
    }

    public final Properties getSettingsProperties() {
        final Properties properties = new Properties();
        properties.setProperty(type + ADDRESS, address);
        properties.setProperty(type + PORT, port);
        properties.setProperty(type + PROTOCOL, protocol);
        properties.setProperty(ACCOUNTS + UNLOCK_TIMEOUT, unlockTimeout.toString());
        return properties;
    }

    private String convertToDurationString(final String string) {
        return "PT" + string.toUpperCase();
    }
}
