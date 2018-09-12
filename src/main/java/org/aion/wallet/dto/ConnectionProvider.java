package org.aion.wallet.dto;

import org.aion.api.log.LogEnum;
import org.aion.wallet.log.WalletLoggerFactory;
import org.slf4j.Logger;

import java.util.*;

public class ConnectionProvider {

    private static final String DEFAULT_NAME = "Default";
    private static final String DEFAULT_IP = "aion-main.bdnodes.net";
    private static final String DEFAULT_PORT = "8547";
    private static final String DEFAULT_PROTOCOL = "tcp";
    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());
    private static final String DEFAULT_ID = "default-id-12345";
    public static final ConnectionDetails DEFAULT_NODE = new ConnectionDetails(DEFAULT_ID, DEFAULT_NAME, DEFAULT_PROTOCOL,
            DEFAULT_IP, DEFAULT_PORT, "*y4vDwz$LA1b[+Yop>SkCm-H17UkaGd@3@A?*h%.");

    private final Set<ConnectionDetails> connections = new LinkedHashSet<>();

    public ConnectionProvider(final Properties connectionKeys) {
        if (connections.isEmpty()) {
            connections.add(DEFAULT_NODE);
        }
        for (Map.Entry<Object, Object> connectionToKey : connectionKeys.entrySet()) {
            final String connection = (String) connectionToKey.getKey();
            final String secureKey = (String) connectionToKey.getValue();
            try {
                connections.add(new ConnectionDetails(connection, secureKey));
            } catch (IllegalArgumentException e) {
                log.error("skipping configured connection key for - " + connection);
                log.error(e.getMessage(), e);
            }
        }
    }

    public Set<ConnectionDetails> getAllConnections() {
        return connections;
    }

    public Optional<ConnectionDetails> getConnectionDetails(final String id) {
        return connections.stream().filter(p -> p.getId().equals(id)).findFirst();
    }

    public void addConnection(final ConnectionDetails connectionDetails) {
        this.connections.add(connectionDetails);
    }
}
