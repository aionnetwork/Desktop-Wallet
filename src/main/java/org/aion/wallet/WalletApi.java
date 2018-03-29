package org.aion.wallet;

import org.aion.api.server.ApiAion;
import org.aion.zero.impl.blockchain.AionImpl;

public class WalletApi extends ApiAion {
    public WalletApi() {
        super(AionImpl.inst());
    }
}
