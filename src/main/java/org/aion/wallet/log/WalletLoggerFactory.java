package org.aion.wallet.log;

import org.aion.api.log.AionLoggerFactory;
import org.aion.wallet.util.ConfigUtils;
import org.slf4j.Logger;

public class WalletLoggerFactory {

    public static Logger getLogger(String id) {
        if (ConfigUtils.isEmbedded()) {
            return org.aion.log.AionLoggerFactory.getLogger(id);
        } else {
            return AionLoggerFactory.getLogger(id);
        }
    }
}
