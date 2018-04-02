package org.aion.wallet.connector.dto;

public class SyncInfoDTO {
    private long chainBestBlkNumber;
    private long networkBestBlkNumber;

    public long getNetworkBestBlkNumber() {
        return networkBestBlkNumber;
    }

    public void setNetworkBestBlkNumber(long networkBestBlkNumber) {
        this.networkBestBlkNumber = networkBestBlkNumber;
    }

    public long getChainBestBlkNumber() {
        return chainBestBlkNumber;
    }

    public void setChainBestBlkNumber(long chainBestBlkNumber) {
        this.chainBestBlkNumber = chainBestBlkNumber;
    }
}
