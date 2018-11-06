package org.aion.wallet.events;

import javafx.scene.input.InputEvent;
import org.aion.wallet.connector.dto.SendTransactionDTO;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.dto.LightAppSettings;

import java.util.Set;

public class EventPublisher {

    public static void fireFatalErrorEncountered(final String message) {
        EventBusFactory.getBus(ErrorEvent.ID).post(new ErrorEvent(ErrorEvent.Type.FATAL, message));
    }

    public static void fireMnemonicCreated(final String mnemonic) {
        if (mnemonic != null) {
            EventBusFactory.getBus(UiMessageEvent.ID)
                    .post(new UiMessageEvent(UiMessageEvent.Type.MNEMONIC_CREATED, mnemonic));
        }
    }

    public static void fireOpenTokenBalances(final String accountAddress) {
        if (accountAddress != null) {
            EventBusFactory.getBus(UiMessageEvent.ID)
                    .post(new UiMessageEvent(UiMessageEvent.Type.TOKEN_BALANCES_SHOW, accountAddress));
        }
    }

    public static void fireTokenAdded(final String accountAddress) {
        if (accountAddress != null) {
            EventBusFactory.getBus(UiMessageEvent.ID)
                    .post(new UiMessageEvent(UiMessageEvent.Type.TOKEN_ADDED, accountAddress));
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

    public static void fireAccountExport(final AccountDTO account) {
        if (account != null) {
            EventBusFactory.getBus(AccountEvent.ID).post(new AccountEvent(AccountEvent.Type.EXPORT, account));
        }
    }

    public static void fireAccountLocked(final AccountDTO account) {
        if (account != null) {
            EventBusFactory.getBus(AbstractAccountEvent.ID)
                    .post(new AccountEvent(AbstractAccountEvent.Type.LOCKED, account));
        }
    }

    public static void fireAccountsRecovered(final Set<String> addresses) {
        if (addresses != null && !addresses.isEmpty()) {
            EventBusFactory.getBus(AbstractAccountEvent.ID)
                    .post(new AccountListEvent(AbstractAccountEvent.Type.RECOVERED, addresses));
        }
    }

    public static void fireTransactionFinished() {
        EventBusFactory.getBus(RefreshEvent.ID).post(new RefreshEvent(RefreshEvent.Type.TRANSACTION_FINISHED, null));
    }

    public static void fireConnectAttmpted(final boolean isSecuredConnection) {
        EventBusFactory.getBus(RefreshEvent.ID).post(new RefreshEvent(RefreshEvent.Type.CONNECTING, isSecuredConnection));
    }

    public static void fireConnectionEstablished(final boolean isSecuredConnection) {
        EventBusFactory.getBus(RefreshEvent.ID).post(new RefreshEvent(RefreshEvent.Type.CONNECTED, isSecuredConnection));
    }

    public static void fireConnectionBroken() {
        EventBusFactory.getBus(RefreshEvent.ID).post(new RefreshEvent(RefreshEvent.Type.DISCONNECTED, null));
    }

    public static void fireDisconnectAttempted() {
        EventBusFactory.getBus(RefreshEvent.ID).post(new RefreshEvent(RefreshEvent.Type.DISCONNECTING, null));
    }

    public static void fireApplicationSettingsChanged(final LightAppSettings settings) {
        EventBusFactory.getBus(SettingsEvent.ID).post(new SettingsEvent(SettingsEvent.Type.CHANGED, settings));
    }

    public static void fireApplicationSettingsApplied(final LightAppSettings settings) {
        EventBusFactory.getBus(SettingsEvent.ID).post(new SettingsEvent(SettingsEvent.Type.APPLIED, settings));
    }

    public static void fireTransactionResubmited(final SendTransactionDTO transaction) {
        EventBusFactory.getBus(TransactionEvent.ID)
                .post(new TransactionEvent(TransactionEvent.Type.RESUBMIT, transaction));
    }

    public static void fireLedgerConnected() {
        EventBusFactory.getBus(UiMessageEvent.ID).post(new UiMessageEvent(UiMessageEvent.Type.LEDGER_CONNECTED, ""));
    }

    public static void fireLedgerAccountSelected(final InputEvent eventSource) {
        EventBusFactory.getBus(UiMessageEvent.ID).post(new UiMessageEvent(UiMessageEvent.Type.LEDGER_ACCOUNT_SELECTED, eventSource));
    }
}
