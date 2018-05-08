package org.aion.wallet.ui.events;

import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.util.DataUpdater;

public class EventPublisher {
    public static final String ACCOUNT_CHANGE_EVENT_ID = "account.changed";
    public static final String ACCOUNT_UNLOCK_EVENT_ID = "account.unlock";

    public static void fireAccountChanged(AccountDTO account) {
        if (account != null) {
            EventBusFactory.getBus(ACCOUNT_CHANGE_EVENT_ID).post(account);
        }
    }

    public static void fireUnlockAccount(AccountDTO account) {
        if (account != null) {
            EventBusFactory.getBus(ACCOUNT_UNLOCK_EVENT_ID).post(account);
        }
    }

    public static void fireTransactionFinished(){
        EventBusFactory.getBus(DataUpdater.UI_DATA_REFRESH).post(new RefreshEvent(RefreshEvent.Type.OPERATION_FINISHED));
    }
}
