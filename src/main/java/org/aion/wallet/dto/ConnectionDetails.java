package org.aion.wallet.dto;

import java.util.Objects;

public class ConnectionDetails {

    private final String name;
    private final String protocol;
    private final String address;
    private final String port;

    public ConnectionDetails(final String name, final String protocol, final String address, final String port) {
        if(name != null && !name.isEmpty()) {
            this.name = name;
        }
        else {
            this.name = "Unnamed connection";
        }
        this.protocol = protocol;
        this.address = address;
        this.port = port;
    }

    public ConnectionDetails(final String connection) {
        try {
            final String[] split = connection.split(":");
            name = "Unnamed connection";
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

    public String getName() {
        return name;
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
        return name;
    }

    public String toConnectionString() {
        return protocol + "://" + address + ":" + port;
    }

    public String getProtocol() {
        return protocol;
    }
}
