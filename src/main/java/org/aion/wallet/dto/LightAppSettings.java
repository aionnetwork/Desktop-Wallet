package org.aion.wallet.dto;

import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.storage.ApiType;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

public class LightAppSettings {

    private static final SettingsValidator VALIDATOR = new SettingsValidator();

    private static final String DEFAULT_UNLOCK_TIMEOUT = "PT3M";

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
        unlockTimeout = Duration.parse(Optional.ofNullable(lightSettingsProps.getProperty(ACCOUNTS + UNLOCK_TIMEOUT)).orElse(DEFAULT_UNLOCK_TIMEOUT));
    }

    public LightAppSettings(final String address, final String port, final String protocol, final ApiType type, final String timeout) throws ValidationException {
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

    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        LightAppSettings that = (LightAppSettings) other;
        return type == that.type &&
                Objects.equals(address, that.address) &&
                Objects.equals(port, that.port) &&
                Objects.equals(protocol, that.protocol) &&
                Objects.equals(unlockTimeout, that.unlockTimeout);
    }

    @Override
    public int hashCode() {

        return Objects.hash(type, address, port, protocol, unlockTimeout);
    }

    private String convertToDurationString(final String string) throws ValidationException {
        final String formattedString = "PT" + string.toUpperCase();
        if (!VALIDATOR.validateTimeout(formattedString)) {
            throw new ValidationException(String.format("Invalid timeout pattern: %s, should be like: XhYmZ[.zzz]s", string));
        }
        return formattedString;
    }

    private static final class SettingsValidator {
        private static final Pattern timeoutPattern = Pattern.compile("^(-?)P(?=\\d|T\\d)(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)([DW]))?(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+(?:\\.\\d+)?)S)?)?$");

        private boolean validateTimeout(final String timeoutString) {
            return timeoutPattern.matcher(timeoutString).matches();
        }
    }
}
