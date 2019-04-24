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
import org.aion.wallet.hardware.AionAccountDetails;
import org.aion.wallet.hardware.HardwareWallet;
import org.aion.wallet.hardware.HardwareWalletException;
import org.aion.wallet.hardware.HardwareWalletFactory;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.util.CryptoUtils;
import org.slf4j.Logger;

public class AionTransactionSigner {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private final AccountDTO account;

    AionTransactionSigner(final AccountDTO from) {
        account = from;
    }

    /**
     * Signs the transaction using the accounts private key
     * @param transaction the transaction to be signed
     * @return the encoded bytes of a signed transaction
     * @throws ValidationException
     * @throws IllegalArgumentException if the account type is not defined
     */
    public byte[] sign(final AionTransaction transaction) throws ValidationException {
        final AccountType accountType = account.getType();
        if (accountType == AccountType.LOCAL || accountType == AccountType.EXTERNAL) { // if no HW wallet is available sign the transaction with software
            signWithDesktopWallet(transaction);
        } else if (accountType == AccountType.LEDGER || accountType == AccountType.TREZOR) {// if the HW wallet is available sign with the HW wallet
            signWithHardwareWallet(transaction, accountType);
        } else { // otherwise throw
            throw new IllegalArgumentException("Unsupported account type for transaction signing: " + accountType.getDisplayString());
        }
        return transaction.getEncoded();
    }

    private void signWithDesktopWallet(AionTransaction transaction) {
        final ECKey ecKey = CryptoUtils.getECKey(account.getPrivateKey());
        transaction.sign(ecKey);
    }

    private void signWithHardwareWallet(AionTransaction transaction, AccountType accountType) throws ValidationException {

        final HardwareWallet wallet = HardwareWalletFactory.getHardwareWallet(accountType);
        final AionAccountDetails accountDetails;
        transaction.setTimeStamp(TimeInstant.now().toEpochMicro());
        try {
            accountDetails = wallet.getAccountDetails(account.getDerivationIndex());
        } catch (HardwareWalletException e) {
            log.error(e.getMessage(), e);
            throw new ValidationException(accountType.getDisplayString() + " is not responding. Reconnect!");
        }

        if (!account.getPublicAddress().equals(accountDetails.getAddress())) {
            throw new ValidationException("Wrong " + accountType.getDisplayString() + " connected! Account not found!");
        }

        final byte[] signature;
        try {
            signature = wallet.signMessage(account.getDerivationIndex(), transaction.getEncodedRaw());
            transaction.setSignature(new Ed25519Signature(TypeConverter.StringHexToByteArray(accountDetails.getPublicKey()), signature));
        } catch (HardwareWalletException e) {
            log.error(e.getMessage(), e);
            throw new ValidationException(accountType.getDisplayString() + " transaction declined by user");
        }
    }
}
