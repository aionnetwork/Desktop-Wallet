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

    private static class MockWallet implements HardwareWallet {

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public AionAccountDetails getAccountDetails(final int derivationIndex) {
            switch (derivationIndex) {
                case 0:
                    return new AionAccountDetails("0x27df1cc71c6dba7ee8c40d40a0d48c838332d131d34874920258efb37a4050a4", ("0xa0849f3732b70b7c1075eddb4a99d643c59b3627adeac642f589de999154cac5"));
                case 1:
                    return new AionAccountDetails("0x00", ("0xa096057bc9dfc89759eef9346fd4675370a8f7987424dae571dcb70a10a57176"));
            }
            return null;
        }

        @Override
        public String signMessage(final int derivationIndex, final byte[] message) {
            return "0x00";
        }
    }
}