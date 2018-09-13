package org.aion.wallet.connector.dto;

public class SyncInfoDTO {

    private final long chainBestBlkNumber;
    private final long networkBestBlkNumber;

    public SyncInfoDTO(final long chainBestBlkNumber, final long networkBestBlkNumber) {
        this.chainBestBlkNumber = chainBestBlkNumber;
        this.networkBestBlkNumber = networkBestBlkNumber;
    }

    public long getNetworkBestBlkNumber() {
        return networkBestBlkNumber;
    }

    public long getChainBestBlkNumber() {
        return chainBestBlkNumber;
    }
}
