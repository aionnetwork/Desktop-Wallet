package org.aion.wallet.log;

import org.aion.wallet.util.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class WalletLoggerFactory {

    private static final Map<String, Logger> LOGGER_MAP = new HashMap<>();

    public static Logger getLogger(final Class clazz) {
        return getLogger(clazz.getName());
    }

    public static Logger getLogger(final String id) {
        Logger logger = LOGGER_MAP.get(id);
        if (logger == null) {
            if (ConfigUtils.isEmbedded()) {
                logger = org.aion.log.AionLoggerFactory.getLogger(id);
            } else {
                logger = LoggerFactory.getLogger(id);
            }
            LOGGER_MAP.put(id, logger);
        }
        return logger;
    }
}
