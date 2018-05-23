package org.aion.wallet.events;

import org.aion.wallet.dto.AccountDTO;

public class AccountEvent extends AbstractEvent<AccountEvent.Type> {

    public static final String ID = "account.update";

    private final AccountDTO account;

    protected AccountEvent(final Type eventType, final AccountDTO account) {
        super(eventType);
        this.account = account;
    }

    public AccountDTO getAccount() {
        return account;
    }

    public enum Type {
        CHANGED, UNLOCKED, ADDED, LOCKED
    }
}
