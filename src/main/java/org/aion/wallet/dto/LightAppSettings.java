package org.aion.wallet.dto;

import org.aion.wallet.storage.ApiType;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

public class LightAppSettings {
    private static final String DEFAULT_LOCK_TIMEOUT = "1";
    private static final String DEFAULT_LOCK_TIMEOUT_MEASUREMENT_UNIT = "minutes";

    private static final String ACCOUNTS = "accounts";

    private static final String LOCK_TIMEOUT = ".lock_timeout";
    private static final String LOCK_TIMEOUT_MEASUREMENT_UNIT = ".lock_timeout_measurement_unit";
    private static final String CONNECTION_ID = ".connection_id";
    private static final String DEFAULT_ID = "default-id-12345";

    private final ApiType type;
    private final String lockTimeoutMeasurementUnit;
    private final int lockTimeout;
    private ConnectionDetails connectionDetails;

    public LightAppSettings(final Properties lightSettingsProps, final ApiType type, final ConnectionProvider connectionProvider) {
        this.type = type;
        String connectionId = Optional.ofNullable(lightSettingsProps.getProperty(type + CONNECTION_ID)).orElse
                (DEFAULT_ID);
        connectionDetails = connectionProvider.getConnectionDetails(connectionId).orElse(ConnectionProvider.DEFAULT_NODE);
        lockTimeout = Integer.parseInt(Optional.ofNullable(lightSettingsProps.getProperty(ACCOUNTS + LOCK_TIMEOUT)).orElse(DEFAULT_LOCK_TIMEOUT));
        lockTimeoutMeasurementUnit = Optional.ofNullable(lightSettingsProps.getProperty(ACCOUNTS + LOCK_TIMEOUT_MEASUREMENT_UNIT)).orElse(DEFAULT_LOCK_TIMEOUT_MEASUREMENT_UNIT);
    }

    public LightAppSettings(final String id, final String name, final String address, final String port, final String protocol, final ApiType type, final Integer timeout, final String lockTimeoutMeasurementUnit) {
        this.type = type;
        this.connectionDetails = new ConnectionDetails(id, name, protocol, address, port,
                "*y4vDwz$LA1b[+Yop>SkCm-H17UkaGd@3@A?*h%.");
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
        properties.setProperty(type + CONNECTION_ID, connectionDetails.getId());
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
