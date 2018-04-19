package org.aion.wallet.dto;

import java.util.Objects;

public class AccountDTO {
    private final String currency;
    private final String publicAddress;
    private final String balance;
    private String name; //TODO this has to be BigInteger
    private boolean active;

    public AccountDTO(final String name, final String publicAddress, final String balance, final String currency) {
        this.name = name;
        this.publicAddress = publicAddress;
        this.balance = balance;
        this.currency = currency;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getCurrency() {
        return currency;
    }

    public String getPublicAddress() {
        return publicAddress;
    }

    public String getBalance() {
        return balance;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AccountDTO that = (AccountDTO) o;
        return Objects.equals(currency, that.currency) &&
                Objects.equals(publicAddress, that.publicAddress);
    }

    @Override
    public int hashCode() {

        return Objects.hash(currency, publicAddress);
    }
}
