package org.aion.wallet.connector.dto;

import org.aion.base.type.Hash256;

public class TransactionResponseDTO {
    private final byte status;
    private final Hash256 txHash;
    private final String error;

    public TransactionResponseDTO() {
        status = 0;
        txHash = null;
        error = null;
    }

    public byte getStatus() {
        return status;
    }

    public Hash256 getTxHash() {
        return txHash;
    }

    public String getError() {
        return error;
    }
}
