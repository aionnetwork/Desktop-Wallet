package org.aion.wallet.hardware;

import org.aion.wallet.dto.AccountType;
import org.aion.wallet.hardware.ledger.LedgerWallet;

public class HardwareWalletFactory {

    private static LedgerWallet ledgerWallet;

    public static HardwareWallet getHardwareWallet(final AccountType accountType) {
        switch (accountType) {
            case LEDGER:
                if (ledgerWallet == null) {
                    ledgerWallet = new LedgerWallet();
                }
                return ledgerWallet;
            default:
                throw new IllegalArgumentException("No HardwareWallet implemented for account of type: " + accountType.getDisplayString());
        }
    }
}
