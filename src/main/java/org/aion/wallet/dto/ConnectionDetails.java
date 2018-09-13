package org.aion.wallet.dto;

import java.util.Objects;

public class ConnectionDetails {

    private final String id;
    private final String name;
    private final String protocol;
    private final String address;
    private final String port;
    private final String secureKey;

    public ConnectionDetails(final String id, final String name, final String protocol, final String address, final String port, final String secureKey) {
        this.id = id;
        if (name != null && !name.isEmpty()) {
            this.name = name;
        } else {
            this.name = "Unnamed connection";
        }
        this.protocol = protocol;
        this.address = address;
        this.port = port;
        this.secureKey = secureKey;
    }

    public ConnectionDetails(final String connection, final String secureKey) {
        try {
            final String[] split = connection.split(":");
            this.id = split[0];
            this.name = split[1];
            this.protocol = split[2];
            this.address = split[3].substring(2);
            this.port = split[4];
            this.secureKey = secureKey;
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
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionDetails that = (ConnectionDetails) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name;
    }

    public final String serialized() {
        return id + ":" + name + ":" + toConnectionString();
    }

    public String toConnectionString() {
        return protocol + "://" + address + ":" + port;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getId() {
        return id;
    }

    public String getSecureKey() {
        return secureKey;
    }

    public boolean isSecureConnection() {
        return secureKey != null && !secureKey.isEmpty();
    }
}
