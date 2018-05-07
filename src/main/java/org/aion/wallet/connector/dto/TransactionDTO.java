package org.aion.wallet.connector.dto;

import org.aion.base.util.TypeConverter;
import org.aion.wallet.connector.api.TxState;

import java.math.BigInteger;

public class TransactionDTO {
    private final String from;
    private final String to;
    private final BigInteger value;
    private final long nrg;
    private final long nrgPrice;
    private final long timeStamp;
    private final TxState state;

    public TransactionDTO(final String from, final String to, final BigInteger value, final long nrg, final long nrgPrice, final long timeStamp, final TxState state) {
        this.from = TypeConverter.toJsonHex(from);
        this.to = TypeConverter.toJsonHex(to);
        this.value = value;
        this.nrg = nrg;
        this.nrgPrice = nrgPrice;
        this.timeStamp = timeStamp;
        this.state = state;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
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

    public TxState getState() {
        return state;
    }
}
