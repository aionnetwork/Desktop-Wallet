package org.aion.wallet.dto;

public class AccountDTO {
    private final String currency;
    private String publicAddress;
    private String balance;
    private boolean active;

    public AccountDTO(final String currency) {
        this.currency = currency;
    }

    public String getCurrency() {
        return currency;
    }

    public String getPublicAddress() {
        return publicAddress;
    }

    public void setPublicAddress(String publicAddress) {
        this.publicAddress = publicAddress;
    }

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
