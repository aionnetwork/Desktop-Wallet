package org.aion.wallet.connector;

import org.aion.api.log.LogEnum;
import org.aion.api.type.core.tx.AionTransaction;
import org.aion.base.util.TimeInstant;
import org.aion.base.util.TypeConverter;
import org.aion.crypto.ECKey;
import org.aion.crypto.ed25519.Ed25519Signature;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.dto.AccountType;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.hardware.HardwareWallet;
import org.aion.wallet.hardware.HardwareWalletException;
import org.aion.wallet.hardware.HardwareWalletFactory;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.util.CryptoUtils;
import org.slf4j.Logger;

public class AionTransactionSigner {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private final AccountDTO account;

    public AionTransactionSigner(final AccountDTO from) {
        account = from;
    }

    public byte[] sign(final AionTransaction transaction) throws ValidationException {
        final AccountType accountType = account.getType();
        switch (accountType) {
            case LOCAL:
            case EXTERNAL:
                final ECKey ecKey = CryptoUtils.getECKey(account.getPrivateKey());
                transaction.sign(ecKey);
                break;
            case LEDGER:
            case TREZOR:
                final HardwareWallet wallet = HardwareWalletFactory.getHardwareWallet(accountType);
                final String publicKey;
                transaction.setTimeStamp(TimeInstant.now().toEpochMicro());
                try {
                    publicKey = wallet.getAccountDetails(account.getDerivationIndex()).getPublicKey();
                } catch (HardwareWalletException e) {
                    log.error(e.getMessage(), e);
                    throw new ValidationException(accountType.getDisplayString() + " is not responding. Reconnect!");
                }

                if (!transaction.getFrom().toString().equals(publicKey)) {
                    throw new ValidationException("Wrong " + accountType.getDisplayString() + " connected! Account not found!");
                }

                final String signature;
                try {
                    signature = wallet.signMessage(account.getDerivationIndex(), transaction.getEncodedRaw());
                    transaction.setSignature(new Ed25519Signature(TypeConverter.StringHexToByteArray(publicKey), TypeConverter.StringHexToByteArray(signature)));
                } catch (HardwareWalletException e) {
                    log.error(e.getMessage(), e);
                    throw new ValidationException(accountType.getDisplayString() + " transaction declined by user");
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported account type for transaction signing: " + accountType.getDisplayString());
        }
        return transaction.getEncoded();
    }
}
