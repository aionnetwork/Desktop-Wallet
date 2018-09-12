package org.aion.wallet.dto;

import org.aion.api.log.LogEnum;
import org.aion.wallet.log.WalletLoggerFactory;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class ConnectionProvider {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());
    public static final String DEFAULT_NAME = "Default";
    public static final String DEFAULT_IP = "aion-main.bdnodes.net";
    public static final String DEFAULT_PORT = "8547";
    public static final String DEFAULT_PROTOCOL = "tcp";
    private static final String DEFAULT_ID = "default-id-12345";

    private final Map<ConnectionDetails, String> addressToKey = new LinkedHashMap<>();

    public ConnectionProvider(final Properties connectionKeys) {
        if(addressToKey.isEmpty()) {
            final ConnectionDetails defaultNode = new ConnectionDetails(DEFAULT_ID, DEFAULT_NAME, DEFAULT_PROTOCOL, DEFAULT_IP, DEFAULT_PORT);

            addressToKey.put(defaultNode, "*y4vDwz$LA1b[+Yop>SkCm-H17UkaGd@3@A?*h%.");
        }
        for (Map.Entry<Object, Object> connectionToKey : connectionKeys.entrySet()) {
            final String connection = (String) connectionToKey.getKey();
            final String secureKey = (String) connectionToKey.getValue();
            try {
                addressToKey.put(new ConnectionDetails(connection), secureKey);
            } catch (IllegalArgumentException e) {
                log.error("skipping configured connection key for - " + connection);
                log.error(e.getMessage(), e);
            }
        }
    }

    public final String getKey(final ConnectionDetails connection) {
        return addressToKey.get(connection);
    }


    public Map<ConnectionDetails, String> getAddressToKey() {
        return addressToKey;
    }

    public final Map<String, String> getConnectionProperties() {
        return addressToKey.entrySet().stream().collect(
                Collectors.toMap(
                        e -> e.getKey().serialized(),
                        Map.Entry::getValue
                )
        );
    }

    public ConnectionDetails getConnectionDetailsById(final String id) {
        for (ConnectionDetails connectionDetails : addressToKey.keySet()) {
            if(connectionDetails.getId().equals(id)) {
                return connectionDetails;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionProvider that = (ConnectionProvider) o;
        return Objects.equals(addressToKey, that.addressToKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addressToKey);
    }
}
