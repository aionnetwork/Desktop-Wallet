package org.aion.wallet.hardware;

import org.aion.wallet.dto.AccountType;
import org.aion.wallet.hardware.ledger.LedgerWallet;

import java.util.EnumMap;
import java.util.Map;

public class HardwareWalletFactory {

    private static final Map<AccountType, HardwareWallet> walletMap = new EnumMap<>(AccountType.class);

    public static HardwareWallet getHardwareWallet(final AccountType accountType) {
        switch (accountType) {
            case LEDGER:
                return walletMap.computeIfAbsent(accountType, a -> new LedgerWallet());
            default:
                throw new IllegalArgumentException("No HardwareWallet implemented for account of type: " + accountType.getDisplayString());
        }
    }
}
