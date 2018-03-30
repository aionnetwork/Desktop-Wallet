package org.aion.wallet.connector.dto;

import java.math.BigInteger;

public class TransactionDTO {
    private String from;
    private String to;
    private BigInteger value;
    private Long nrg;
    private Long nrgPrice;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public BigInteger getValue() {
        return value;
    }

    public void setValue(BigInteger value) {
        this.value = value;
    }

    public Long getNrg() {
        return nrg;
    }

    public void setNrg(Long nrg) {
        this.nrg = nrg;
    }

    public Long getNrgPrice() {
        return nrgPrice;
    }

    public void setNrgPrice(Long nrgPrice) {
        this.nrgPrice = nrgPrice;
    }
}
