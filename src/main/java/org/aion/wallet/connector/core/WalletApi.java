package org.aion.wallet.connector.core;

import org.aion.api.server.ApiAion;
import org.aion.api.server.types.ArgTxCall;
import org.aion.api.server.types.SyncInfo;
import org.aion.base.type.ITransaction;
import org.aion.base.type.ITxReceipt;
import org.aion.evtmgr.impl.evt.EventTx;
import org.aion.zero.impl.blockchain.AionImpl;
import org.aion.zero.impl.types.AionBlockSummary;
import org.aion.zero.types.AionTransaction;

public class WalletApi extends ApiAion {
    public WalletApi() {
        super(AionImpl.inst());
    }

    @Override
    public int peerCount() {
        return super.peerCount();
    }

    @Override
    public SyncInfo getSync() {
        return super.getSync();
    }

    @Override
    public AionTransaction getTransactionByHash(byte[] hash) {
        return super.getTransactionByHash(hash);
    }

    @Override
    public byte[] sendTransaction(ArgTxCall _params) {
        return super.sendTransaction(_params);
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
