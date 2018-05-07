package org.aion.wallet.storage;

public class TxInfo {
    private final long lastCheckedBlock;
    private final long txCount;

    public TxInfo(final long lastCheckedBlock, final long txCount) {
        this.lastCheckedBlock = lastCheckedBlock;
        this.txCount = txCount;
    }

    public long getLastCheckedBlock() {
        return lastCheckedBlock;
    }

    public long getTxCount() {
        return txCount;
    }
}
