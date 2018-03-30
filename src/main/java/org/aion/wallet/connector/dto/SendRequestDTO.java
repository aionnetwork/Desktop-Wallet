package org.aion.wallet.connector.dto;

import org.aion.base.util.ByteArrayWrapper;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.util.AddressUtils;

import java.math.BigInteger;

public class SendRequestDTO implements UnlockableAccount {
    private String from;
    private String password;
    private String to;
    private Long nrg;
    private Long nrgPrice;
    private BigInteger value;

    @Override
    public String getAddress() {
        return this.from;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

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

    public byte[] getData() {
        return ByteArrayWrapper.NULL_BYTE;
    }

    public BigInteger getNonce() {
        return BigInteger.ZERO;
    }

    public boolean isValid(){
        return AddressUtils.isValid(from) && AddressUtils.isValid(to)
                && value != null && value.compareTo(BigInteger.ZERO) > 0;
    }
}
