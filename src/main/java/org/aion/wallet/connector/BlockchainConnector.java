package org.aion.wallet.connector;

import org.aion.base.util.TypeConverter;
import org.aion.wallet.account.AccountManager;
import org.aion.wallet.connector.api.ApiBlockchainConnector;
import org.aion.wallet.connector.dto.SendTransactionDTO;
import org.aion.wallet.connector.dto.SyncInfoDTO;
import org.aion.wallet.connector.dto.TransactionDTO;
import org.aion.wallet.connector.dto.TransactionResponseDTO;
import org.aion.wallet.dto.*;
import org.aion.wallet.events.EventPublisher;
import org.aion.wallet.exception.NotFoundException;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.storage.ApiType;
import org.aion.wallet.storage.WalletStorage;
import org.aion.wallet.util.ConfigUtils;

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public abstract class BlockchainConnector {

    private static final String CORE_CONNECTOR_CLASS = "org.aion.wallet.connector.core.CoreBlockchainConnector";

    private static final String SEND = "f0a147ad";

    private static BlockchainConnector INST;

    private final WalletStorage walletStorage = WalletStorage.getInstance();

    private final ReentrantLock lock = new ReentrantLock();

    private final AccountManager accountManager;

    private boolean connectionLocked = false;

    protected BlockchainConnector() {
        this.accountManager = new AccountManager(this::getBalance, this::getCurrency);
    }

    public static BlockchainConnector getInstance() {
        if (INST != null) {
            return INST;
        }
        if (ConfigUtils.isEmbedded()) {
            try {
                INST = (BlockchainConnector) Class.forName(CORE_CONNECTOR_CLASS).getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException
                    | InvocationTargetException e) {
                throw new RuntimeException("Could not instantiate class: " + CORE_CONNECTOR_CLASS, e);
            }
        } else {
            INST = new ApiBlockchainConnector();
        }
        return INST;
    }

    public final boolean hasMasterAccount() {
        return walletStorage.hasMasterAccount();
    }

    public final boolean isMasterAccountUnlocked() {
        return accountManager.isMasterAccountUnlocked();
    }

    public final String createMasterAccount(final String password, final String name) throws ValidationException {
        return accountManager.createMasterAccount(password, name);
    }

    public final void importMasterAccount(final String mnemonic, final String password) throws ValidationException {
        accountManager.importMasterAccount(mnemonic, password);
    }

    public final void unlockMasterAccount(final String password) throws ValidationException {
        accountManager.unlockMasterAccount(password);
    }

    public final void createAccount() throws ValidationException {
        accountManager.createAccount();
    }

    public final AccountDTO importKeystoreFile(final byte[] file, final String password, final boolean shouldKeep)
            throws ValidationException {
        return accountManager.importKeystore(file, password, shouldKeep);
    }

    public final AccountDTO importPrivateKey(final byte[] privateKey, final String password, final boolean
            shouldKeep) throws ValidationException {
        return accountManager.importPrivateKey(privateKey, password, shouldKeep);
    }

    public final AccountDTO importHardwareWallet(final int derivationIndex, final String address, final AccountType
            accountType) throws ValidationException {
        return accountManager.importHardwareWallet(accountType, derivationIndex, address);
    }

    public final void exportAccount(final AccountDTO account, final String password, final String destinationDir)
            throws ValidationException {
        accountManager.exportAccount(account, password, destinationDir);
    }

    public final void unlockAccount(final AccountDTO account, final String password) throws ValidationException {
        accountManager.unlockAccount(account, password);
    }

    public final AccountDTO getAccount(final String publicAddress) {
        return accountManager.getAccount(publicAddress);
    }

    public final List<AccountDTO> getAccounts() {
        return accountManager.getAccounts();
    }

    public void connect() {}

    public void close() {
        walletStorage.save();
    }

    public void reloadSettings(final LightAppSettings settings) {
        walletStorage.saveLightAppSettings(settings);
    }

    public void lockAll() {
        connectionLocked = true;
        EventPublisher.fireConnectionBroken();
        accountManager.lockAll();
    }

    public void unlockConnection() {
        if (connectionLocked) {
            connectionLocked = false;
            if (isConnected()) {
                EventPublisher.fireConnectionEstablished(isSecuredConnection());
            } else {
                connect();
            }
        }
    }

    public final AccountManager getAccountManager() {
        return accountManager;
    }

    public abstract BigInteger getBalance(final String address);

    public abstract BigInteger getTokenBalance(final String tokenAddress, final String accountAddress) throws ValidationException;

    public abstract byte[] getTokenSendData(final String tokenAddress, final String accountAddress, final String destinationAddress, final BigInteger value);

    public abstract TokenDetails getTokenDetails(final String tokenAddress, final String accountAddress) throws ValidationException;

    public abstract TransactionDTO getTransaction(final String txHash) throws NotFoundException;

    public abstract Set<TransactionDTO> getLatestTransactions(final String address);

    public abstract boolean isConnected();

    public abstract SyncInfoDTO getSyncInfo();

    public abstract int getPeerCount();

    public abstract LightAppSettings getSettings();

    public final TransactionResponseDTO sendTransaction(final SendTransactionDTO transactionWrapper) throws ValidationException {
        if (transactionWrapper == null || !transactionWrapper.validate()) {
            throw new ValidationException("Invalid transaction request data");
        }
        if (transactionWrapper.estimateValue().compareTo(getBalance(transactionWrapper.getFrom().getPublicAddress())) >= 0) {
            throw new ValidationException("Insufficient funds");
        }
        return sendTransactionInternal(transactionWrapper);
    }

    protected abstract TransactionResponseDTO sendTransactionInternal(final SendTransactionDTO dto) throws ValidationException;

    protected abstract String getCurrency();

    protected abstract boolean isSecuredConnection();

    protected AionTransactionSigner getTransactionSigner(final AccountDTO from) {
        return new AionTransactionSigner(from);
    }

    protected final boolean isConnectionUnLocked() {
        return !connectionLocked;
    }

    protected final void lock() {
        lock.lock();
    }

    protected final void unLock() {
        lock.unlock();
    }

    protected final LightAppSettings getLightweightWalletSettings(final ApiType type) {
        return walletStorage.getLightAppSettings(type);
    }

    protected final void storeLightweightWalletSettings(final LightAppSettings lightAppSettings) {
        walletStorage.saveLightAppSettings(lightAppSettings);
    }

    public final ConnectionProvider getConnectionKeyProvider() {
        return walletStorage.getConnectionProvider();
    }

    public void storeConnectionKeys(final ConnectionProvider connectionProvider) {
        walletStorage.saveConnectionProperties(connectionProvider);
    }

    public final void saveToken(final TokenDetails newTokenDetails) {
        walletStorage.saveToken(newTokenDetails);
    }

    public List<TokenDetails> getAccountTokenDetails(final String address) {
        return walletStorage.getAccountTokenDetails(address);
    }

    public final void addAccountToken(final String address, final String tokenSymbol) {
        walletStorage.addAccountToken(address, tokenSymbol);
    }

    protected final SendData getSendData(String initialFrom, String initialTo, BigInteger initialValue, byte[] data) {
        BigInteger value = initialValue;
        String coin;
        String to = initialTo;
        if (BigInteger.ZERO.equals(value)) {
            try {
                final String dataString = TypeConverter.toJsonHex(data);
                final String function = dataString.substring(2, 10);
                if (SEND.equalsIgnoreCase(function)) {
                    to = dataString.substring(10, 74);
                    value = TypeConverter.StringHexToBigInteger(dataString.substring(74, 106));
                    final TokenDetails tokenDetails = getTokenDetails(initialTo, initialFrom);
                    coin = tokenDetails.getSymbol();
                } else {
                    coin = "";
                }
            } catch (Exception e) {
                value = BigInteger.ZERO;
                if (data.length == 0) {
                    coin = getCurrency();
                } else {
                    coin = "";
                }
            }
        } else {
            coin = getCurrency();
        }
        return new SendData(initialFrom, to, coin, value);
    }

    protected static class SendData {
        private final String from;
        private final String to;
        private final String coin;
        private final BigInteger value;

        private SendData(String from, String to, String coin, BigInteger value) {
            this.from = from;
            this.coin = coin;
            this.to = to;
            this.value = value;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public String getCoin() {
            return coin;
        }

        public BigInteger getValue() {
            return value;
        }
    }
}
