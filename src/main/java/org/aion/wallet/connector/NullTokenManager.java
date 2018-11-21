package org.aion.wallet.connector;

import org.aion.wallet.exception.ValidationException;

import java.math.BigInteger;

public class NullTokenManager implements TokenManager {
    @Override
    public String getName(final String tokenAddress, final String accountAddress) throws ValidationException {
        throw new ValidationException("Not implemented");
    }

    @Override
    public String getSymbol(final String tokenAddress, final String accountAddress) throws ValidationException {
        throw new ValidationException("Not implemented");
    }

    @Override
    public long getGranularity(final String tokenAddress, final String accountAddress) throws ValidationException {
        throw new ValidationException("Not implemented");
    }

    @Override
    public BigInteger getBalance(final String tokenAddress, final String accountAddress) throws ValidationException {
        throw new ValidationException("Not implemented");
    }

    @Override
    public byte[] getEncodedSendTokenData(final String tokenAddress, final String accountAddress, final String destinationAddress, final BigInteger value) throws ValidationException {
        throw new ValidationException("Not implemented");
    }
}
