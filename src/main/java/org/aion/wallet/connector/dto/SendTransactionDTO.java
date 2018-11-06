package org.aion.wallet.connector.dto;

import org.aion.base.util.ByteArrayWrapper;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.util.AddressUtils;

import java.math.BigInteger;
import java.util.Arrays;

public class SendTransactionDTO {
    private final AccountDTO from;
    private final String to;
    private final Long nrg;
    private final BigInteger value;
    private final BigInteger nonce = BigInteger.ZERO;
    private final byte[] data;
    private BigInteger nrgPrice;

    public SendTransactionDTO(
            final AccountDTO from, final String to, final Long nrg, final BigInteger nrgPrice, final BigInteger value, final byte[] data
    ) {
        this.from = from;
        this.to = to;
        this.nrg = nrg;
        this.nrgPrice = nrgPrice;
        this.value = value;
        this.data = data;
    }

    public AccountDTO getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public Long getNrg() {
        return nrg;
    }

    public Long getNrgPrice() {
        return nrgPrice.longValue();
    }

    public BigInteger getValue() {
        return value;
    }

    public BigInteger estimateValue() {
        return value.add(nrgPrice.multiply(BigInteger.valueOf(nrg)));
    }

    public byte[] getData() {
        return data;
    }

    public BigInteger getNonce() {
        return nonce;
    }

    public boolean validate() throws ValidationException {
        if (!AddressUtils.isValid(from.getPublicAddress())) {
            throw new ValidationException("Invalid from address");
        }
        if (!AddressUtils.isValid(to)) {
            throw new ValidationException("Invalid to address");
        }
        if (Arrays.equals(ByteArrayWrapper.NULL_BYTE, data)) {
            if (value == null || value.compareTo(BigInteger.ZERO) <= 0) {
                throw new ValidationException("A value greater than zero must be provided");
            }
        }
        if (nrg == null || nrg < 0) {
            throw new ValidationException("Invalid nrg value");
        }
        if (nrgPrice == null || nrgPrice.longValue() < 0) {
            throw new ValidationException("Invalid nrg price");
        }
        return true;
    }

    public void setNrgPrice(final BigInteger valueOf) {
        nrgPrice = valueOf;
    }
}
