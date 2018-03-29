package org.aion.wallet;

import org.aion.api.server.ApiAion;
import org.aion.base.type.ITransaction;
import org.aion.base.type.ITxReceipt;
import org.aion.evtmgr.impl.evt.EventTx;
import org.aion.zero.impl.blockchain.AionImpl;
import org.aion.zero.impl.types.AionBlockSummary;

public class WalletApi extends ApiAion {
    public WalletApi() {
        super(AionImpl.inst());
    }

    @Override
    protected void onBlock(final AionBlockSummary cbs) {

    }

    @Override
    protected void pendingTxReceived(final ITransaction _tx) {

    }

    @Override
    protected void pendingTxUpdate(final ITxReceipt _txRcpt, final EventTx.STATE _state) {

    }
}
