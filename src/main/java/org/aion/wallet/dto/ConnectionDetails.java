package org.aion.wallet.dto;

import java.util.Objects;

public class ConnectionDetails {

    private final String protocol;
    private final String address;
    private final String port;

    public ConnectionDetails(final String protocol, final String address, final String port) {
        this.protocol = protocol;
        this.address = address;
        this.port = port;
    }

    public ConnectionDetails(final String connection) {
        try {
            final String[] split = connection.split(":");
            protocol = split[0];
            address = split[1].substring(2);
            port = split[2];
        } catch (final Exception e) {
            throw new IllegalArgumentException("Invalid connection string: " + connection, e);
        }
    }

    public String getAddress() {
        return address;
    }

    public String getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionDetails that = (ConnectionDetails) o;
        return Objects.equals(address, that.address) &&
                Objects.equals(port, that.port);
    }

    @Override
    public int hashCode() {

        return Objects.hash(address, port);
    }

    @Override
    public String toString() {
        return protocol + "://" + address + ":" + port;
    }
}
