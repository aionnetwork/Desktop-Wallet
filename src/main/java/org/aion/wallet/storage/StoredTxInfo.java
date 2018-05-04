package org.aion.wallet.storage;

public class StoredTxInfo {
    private final int lastCheckedBlock;
    private final int txCount;

    public StoredTxInfo(final int lastCheckedBlock, final int txCount) {
        this.lastCheckedBlock = lastCheckedBlock;
        this.txCount = txCount;
    }

    public int getLastCheckedBlock() {
        return lastCheckedBlock;
    }

    public int getTxCount() {
        return txCount;
    }
}
