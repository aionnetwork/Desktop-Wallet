package org.aion.wallet.events;

import org.aion.wallet.connector.dto.SendTransactionDTO;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.dto.LightAppSettings;

public class EventPublisher {

    public static void fireMnemonicCreated(final String mnemonic) {
        if (mnemonic != null) {
            EventBusFactory.getBus(UiMessageEvent.ID)
                    .post(new UiMessageEvent(UiMessageEvent.Type.MNEMONIC_CREATED, mnemonic));
        }
    }

    public static void fireAccountAdded(final AccountDTO account) {
        if (account != null) {
            EventBusFactory.getBus(AccountEvent.ID).post(new AccountEvent(AccountEvent.Type.ADDED, account));
        }
    }

    public static void fireAccountChanged(final AccountDTO account) {
        if (account != null) {
            EventBusFactory.getBus(AccountEvent.ID).post(new AccountEvent(AccountEvent.Type.CHANGED, account));
        }
    }

    public static void fireAccountUnlocked(final AccountDTO account) {
        if (account != null) {
            EventBusFactory.getBus(AccountEvent.ID).post(new AccountEvent(AccountEvent.Type.UNLOCKED, account));
        }
    }

    public static void fireAccountLocked(final AccountDTO account) {
        if (account != null) {
            EventBusFactory.getBus(AccountEvent.ID).post(new AccountEvent(AccountEvent.Type.LOCKED, account));
        }
    }

    public static void fireTransactionFinished() {
        EventBusFactory.getBus(RefreshEvent.ID).post(new RefreshEvent(RefreshEvent.Type.TRANSACTION_FINISHED));
    }

    public static void fireConnectionEstablished() {
        EventBusFactory.getBus(RefreshEvent.ID).post(new RefreshEvent(RefreshEvent.Type.CONNECTED));
    }

    public static void fireConnectionBroken() {
        EventBusFactory.getBus(RefreshEvent.ID).post(new RefreshEvent(RefreshEvent.Type.DISCONNECTED));
    }

    public static void fireApplicationSettingsChanged(final LightAppSettings settings) {
        EventBusFactory.getBus(SettingsEvent.ID).post(new SettingsEvent(SettingsEvent.Type.CHANGED, settings));
    }

    public static void fireApplicationSettingsApplied(final LightAppSettings settings) {
        EventBusFactory.getBus(SettingsEvent.ID).post(new SettingsEvent(SettingsEvent.Type.APPLIED, settings));
    }

    public static void fireTransactionResubmited(final SendTransactionDTO transaction) {
        EventBusFactory.getBus(TransactionEvent.ID).post(new TransactionEvent(TransactionEvent.Type.RESUBMIT, transaction));
    }
}
