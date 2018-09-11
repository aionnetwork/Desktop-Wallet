package org.aion.wallet.dto;

import org.aion.wallet.storage.ApiType;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

public class LightAppSettings {
    private static final String DEFAULT_LOCK_TIMEOUT = "1";
    private static final String DEFAULT_LOCK_TIMEOUT_MEASUREMENT_UNIT = "minutes";

    private static final String NAME = ".name";
    private static final String ADDRESS = ".address";
    private static final String PORT = ".port";
    private static final String PROTOCOL = ".protocol";
    private static final String ACCOUNTS = "accounts";

    private static final String DEFAULT_IP = "aion-main.bdnodes.net";
    private static final String DEFAULT_PORT = "8547";
    private static final String DEFAULT_PROTOCOL = "tcp";
    private static final String LOCK_TIMEOUT = ".lock_timeout";
    private static final String LOCK_TIMEOUT_MEASUREMENT_UNIT = ".lock_timeout_measurement_unit";
    private static final String DEFAULT_NAME = "Unnamed connection";

    private final ApiType type;
    private final String lockTimeoutMeasurementUnit;
    private final int lockTimeout;
    private ConnectionDetails connectionDetails;

    public LightAppSettings(final Properties lightSettingsProps, final ApiType type) {
        this.type = type;
        String address = Optional.ofNullable(lightSettingsProps.getProperty(type + ADDRESS)).orElse(DEFAULT_IP);
        String port = Optional.ofNullable(lightSettingsProps.getProperty(type + PORT)).orElse(DEFAULT_PORT);
        String protocol = Optional.ofNullable(lightSettingsProps.getProperty(type + PROTOCOL)).orElse(DEFAULT_PROTOCOL);
        String name = Optional.ofNullable(lightSettingsProps.getProperty(type + NAME)).orElse(DEFAULT_NAME);
        connectionDetails = new ConnectionDetails(name, protocol, address, port);
        lockTimeout = Integer.parseInt(Optional.ofNullable(lightSettingsProps.getProperty(ACCOUNTS + LOCK_TIMEOUT)).orElse(DEFAULT_LOCK_TIMEOUT));
        lockTimeoutMeasurementUnit = Optional.ofNullable(lightSettingsProps.getProperty(ACCOUNTS + LOCK_TIMEOUT_MEASUREMENT_UNIT)).orElse(DEFAULT_LOCK_TIMEOUT_MEASUREMENT_UNIT);
    }

    public LightAppSettings(final String name, final String address, final String port, final String protocol, final ApiType type, final Integer timeout, final String lockTimeoutMeasurementUnit) {
        this.type = type;
        this.connectionDetails = new ConnectionDetails(name, protocol, address, port);
        this.lockTimeout = timeout;
        this.lockTimeoutMeasurementUnit = lockTimeoutMeasurementUnit;
    }

    public ApiType getType() {
        return type;
    }

    public int getLockTimeout() {
        return lockTimeout;
    }

    public String getLockTimeoutMeasurementUnit() {
        return lockTimeoutMeasurementUnit;
    }

    public final Properties getSettingsProperties() {
        final Properties properties = new Properties();
        properties.setProperty(type + ADDRESS, connectionDetails.getAddress());
        properties.setProperty(type + PORT, connectionDetails.getPort());
        properties.setProperty(type + PROTOCOL, connectionDetails.getProtocol());
        properties.setProperty(type + NAME, connectionDetails.getName());
        properties.setProperty(ACCOUNTS + LOCK_TIMEOUT, String.valueOf(lockTimeout));
        properties.setProperty(ACCOUNTS + LOCK_TIMEOUT_MEASUREMENT_UNIT, lockTimeoutMeasurementUnit);
        return properties;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        LightAppSettings that = (LightAppSettings) other;
        return type == that.type &&
                Objects.equals(connectionDetails, that.connectionDetails) &&
                Objects.equals(lockTimeout, that.lockTimeout) &&
                Objects.equals(lockTimeoutMeasurementUnit, that.lockTimeoutMeasurementUnit);
    }

    @Override
    public int hashCode() {

        return Objects.hash(type, connectionDetails, lockTimeout, lockTimeoutMeasurementUnit);
    }

    public ConnectionDetails getConnectionDetails() {
        return connectionDetails;
    }
}
