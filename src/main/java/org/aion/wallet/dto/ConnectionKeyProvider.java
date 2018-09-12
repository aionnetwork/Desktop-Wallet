package org.aion.wallet.dto;

import org.aion.api.log.LogEnum;
import org.aion.wallet.log.WalletLoggerFactory;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class ConnectionKeyProvider {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private final Map<ConnectionDetails, String> addressToKey = new LinkedHashMap<>();

    public ConnectionKeyProvider(final Properties connectionKeys) {
        final ConnectionDetails unSecuredNode = new ConnectionDetails(LightAppSettings.DEFAULT_NAME, LightAppSettings.DEFAULT_PROTOCOL, LightAppSettings.DEFAULT_IP, LightAppSettings.DEFAULT_PORT);
        final ConnectionDetails securedNode = new ConnectionDetails("AION secured", "tcp", "52.231.152.219", "8549");
        addressToKey.put(unSecuredNode, "");
        addressToKey.put(securedNode, "*y4vDwz$LA1b[+Yop>SkCm-H17UkaGd@3@A?*h%.");
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionKeyProvider that = (ConnectionKeyProvider) o;
        return Objects.equals(addressToKey, that.addressToKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addressToKey);
    }
}
