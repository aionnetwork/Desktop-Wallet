package org.aion.wallet.connector.dto;

import org.aion.base.util.TypeConverter;

import java.math.BigInteger;

public class TransactionDTO {
    private final String from;
    private final String to;
    private final String hash;
    private final BigInteger value;
    private final long nrg;
    private final long nrgPrice;
    private final long timeStamp;
    private final long blockNumber;
    private final BigInteger nonce;

    public TransactionDTO(final String from, final String to, final String hash, final BigInteger value, final long nrg, final long nrgPrice, final long timeStamp, final long blockNumber, BigInteger nonce) {
        this.from = TypeConverter.toJsonHex(from);
        this.to = TypeConverter.toJsonHex(to);
        this.hash = hash;
        this.value = value;
        this.nrg = nrg;
        this.nrgPrice = nrgPrice;
        this.timeStamp = timeStamp;
        this.blockNumber = blockNumber;
        this.nonce = nonce;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getHash() {
        return hash;
    }

    public BigInteger getValue() {
        return value;
    }

    public Long getNrg() {
        return nrg;
    }

    public Long getNrgPrice() {
        return nrgPrice;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public long getBlockNumber() {
        return blockNumber;
    }

    public BigInteger getNonce() {
        return nonce;
    }
}
