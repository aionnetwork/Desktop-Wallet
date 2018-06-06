package org.aion.wallet.connector.dto;

import org.aion.base.type.Hash256;

public class TransactionResponseDTO {
    private byte status;
    private Hash256 txHash;
    private String error;

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    public Hash256 getTxHash() {
        return txHash;
    }

    public void setTxHash(Hash256 txHash) {
        this.txHash = txHash;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
