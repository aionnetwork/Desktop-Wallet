package org.aion.wallet.connector;

import org.aion.api.type.core.tx.AionTransaction;
import org.aion.base.util.TypeConverter;
import org.aion.crypto.ECKey;
import org.aion.crypto.ed25519.Ed25519Signature;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.dto.AccountType;
import org.aion.wallet.hardware.HardwareWallet;
import org.aion.wallet.hardware.HardwareWalletFactory;
import org.aion.wallet.hardware.ledger.LedgerException;
import org.aion.wallet.util.CryptoUtils;

import java.io.IOException;

public class AionTransactionSigner {

    private final AccountDTO account;

    public AionTransactionSigner(final AccountDTO from) {
        account = from;
    }

    public byte[] sign(final AionTransaction transaction) {
        final AccountType accountType = account.getType();
        switch (accountType) {
            case LOCAL:
            case IMPORTED:
                final ECKey ecKey = CryptoUtils.getECKey(account.getPrivateKey());
                transaction.sign(ecKey);
                break;
            case LEDGER:
            case TREZOR:
                final HardwareWallet wallet = HardwareWalletFactory.getHardwareWallet(accountType);
                final String publicKey;
                try {
                    publicKey = wallet.getAccountDetails(account.getDerivationIndex()).getPublicKey();
                    final String signature = wallet.signMessage(account.getDerivationIndex(), transaction.getRawHash());
                    transaction.setSignature(new Ed25519Signature(TypeConverter.StringHexToByteArray(publicKey), TypeConverter.StringHexToByteArray(signature)));
                } catch (LedgerException e) {
                    e.printStackTrace();
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported account type for transaction signing: " + accountType.getDisplayString());
        }
        return transaction.getEncoded();
    }
}
