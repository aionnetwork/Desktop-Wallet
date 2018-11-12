package org.aion.wallet.dto;

import org.aion.wallet.exception.ValidationException;

public class TokenDetails {
    private final String contractAddress;
    private final String name;
    private final String symbol;
    private final long granularity;

    public TokenDetails(final String contractAddress, final String name, final String symbol, final long granularity) {
        this.contractAddress = contractAddress;
        this.name = name;
        this.symbol = symbol;
        this.granularity = granularity;
    }

    public TokenDetails(final String symbol, final String serializedDetails) throws ValidationException {
        this.symbol = symbol;
        try {
            final String[] split = serializedDetails.split(":");
            this.contractAddress = split[0];
            this.name = split[1];
            this.granularity = Integer.parseInt(split[2]);
        } catch (Exception e) {
            throw new ValidationException(e);
        }
    }

    public String getContractAddress() { return contractAddress; }

    public String getSymbol() { return symbol; }

    public String getName() {return name;}

    public long getGranularity() {return granularity;}

    public String serialized() { return String.format("%s:%s:%s", contractAddress, name, granularity); }
}
