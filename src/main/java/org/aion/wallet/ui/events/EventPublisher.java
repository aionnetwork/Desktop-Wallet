package org.aion.wallet.ui.events;

import org.aion.wallet.dto.AccountDTO;

public class EventPublisher {
    public static final String ACCOUNT_CHANGE_EVENT_ID = "account.changed";

    public static void fireAccountChanged(AccountDTO dto) {
        EventBusFactory.getInstance().getBus(ACCOUNT_CHANGE_EVENT_ID).post(dto);
    }
}
