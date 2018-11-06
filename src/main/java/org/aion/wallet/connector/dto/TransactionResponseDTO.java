package org.aion.wallet.connector.dto;

public class TransactionResponseDTO {
    private final byte status;
    private final String txHash;
    private final String error;

    public TransactionResponseDTO(final byte status, final String txHash, final String error){
        this.status = status;
        this.txHash = txHash;
        this.error = error;
    }

    public byte getStatus() {
        return status;
    }

    public String getTxHash() {
        return txHash;
    }

    public String getError() {
        return error;
    }

    @Override
    public String toString() {
        return "TransactionResponseDTO{" +
                "status=" + status +
                ", txHash=" + txHash +
                ", error='" + error + '\'' +
                '}';
    }
}
