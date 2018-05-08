package org.aion.wallet.ui.events;

import org.aion.wallet.dto.AccountDTO;

public class EventPublisher {
    public static final String ACCOUNT_CHANGE_EVENT_ID = "account.changed";
    public static final String ACCOUNT_UNLOCK_EVENT_ID = "account.unlock";

    public static void fireAccountChanged(AccountDTO dto) {
        EventBusFactory.getBus(ACCOUNT_CHANGE_EVENT_ID).post(dto);
    }

    public static void fireUnlockAccount(AccountDTO accountDTO) {
        EventBusFactory.getBus(ACCOUNT_UNLOCK_EVENT_ID).post(accountDTO);
    }
}
