package org.aion.wallet.connector;

import org.aion.wallet.exception.ValidationException;

import java.math.BigInteger;

public interface TokenManager {

    String getName(final String tokenAddress, final String accountAddress) throws ValidationException;

    String getSymbol(final String tokenAddress, final String accountAddress) throws ValidationException;

    long getGranularity(final String tokenAddress, final String accountAddress) throws ValidationException;

    BigInteger getBalance(final String tokenAddress, final String accountAddress) throws ValidationException;

    byte[] getEncodedSendTokenData(
            final String tokenAddress,
            final String accountAddress,
            final String destinationAddress,
            final BigInteger value
    ) throws ValidationException;
}
