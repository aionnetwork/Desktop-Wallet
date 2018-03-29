package org.aion.wallet.connector.dto;

import org.aion.base.type.Address;
import org.aion.base.util.ByteArrayWrapper;
import org.aion.base.util.ByteUtil;

import java.math.BigInteger;

public class SendRequestDTO implements UnlockableAccount{
    private Address from;
    private String password;
    private Address to;
    private Long nrg;
    private Long nrgPrice;
    private BigInteger value;

    public byte[] getData() {
        return ByteArrayWrapper.NULL_BYTE;
    }

    public BigInteger getNonce() {
        return BigInteger.ZERO;
    }

    @Override
    public Address getAddress() {
        return this.from;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Address getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = Address.wrap(ByteUtil.hexStringToBytes(from));
    }

    public Address getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = Address.wrap(ByteUtil.hexStringToBytes(to));
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

    public BigInteger getValue() {
        return value;
    }

    public void setValue(BigInteger value) {
        this.value = value;
    }
}
