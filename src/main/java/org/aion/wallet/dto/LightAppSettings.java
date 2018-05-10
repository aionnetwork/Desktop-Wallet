package org.aion.wallet.dto;

import org.aion.wallet.storage.ApiType;

import java.util.Optional;
import java.util.Properties;

public class LightAppSettings {

    private static final String ADDRESS = ".address";
    private static final String PORT = ".port";
    private static final String PROTOCOL = ".protocol";
    private static final String DEFAULT_IP = "127.0.0.1";
    private static final String DEFAULT_PORT = "8547";
    private static final String DEFAULT_PROTOCOL = "tcp";

    private final ApiType type;
    private final String address;
    private final String port;
    private final String protocol;

    public LightAppSettings(final Properties lightSettingsProps, final ApiType type) {
        this.type = type;
        address = Optional.ofNullable(lightSettingsProps.getProperty(type + ADDRESS)).orElse(DEFAULT_IP);
        port = Optional.ofNullable(lightSettingsProps.getProperty(type + PORT)).orElse(DEFAULT_PORT);
        protocol = Optional.ofNullable(lightSettingsProps.getProperty(type + PROTOCOL)).orElse(DEFAULT_PROTOCOL);
    }

    public LightAppSettings(final String address, final String port, final String protocol, final ApiType type) {
        this.type = type;
        this.address = address;
        this.port = port;
        this.protocol = protocol;
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

    public final Properties getSettingsProperties() {
        final Properties properties = new Properties();
        properties.setProperty(type + ADDRESS, address);
        properties.setProperty(type + PORT, port);
        properties.setProperty(type + PROTOCOL, protocol);
        return properties;
    }
}
