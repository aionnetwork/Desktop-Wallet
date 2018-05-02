package org.aion.wallet.connector;

import org.aion.wallet.connector.api.ApiBlockchainConnector;
import org.aion.wallet.connector.dto.SendRequestDTO;
import org.aion.wallet.connector.dto.SyncInfoDTO;
import org.aion.wallet.connector.dto.TransactionDTO;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.exception.NotFoundException;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.storage.WalletStorage;
import org.aion.wallet.util.ConfigUtils;

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public abstract class BlockchainConnector {

    private static final String CORE_CONNECTOR_CLASS = "org.aion.wallet.connector.core.CoreBlockchainConnector";

    private static BlockchainConnector INST;

    private final WalletStorage walletStorage = WalletStorage.getInstance();

    public static BlockchainConnector getInstance() {
        if (INST != null) {
            return INST;
        }
        if (ConfigUtils.isEmbedded()) {
            try {
                INST = (BlockchainConnector) Class.forName(CORE_CONNECTOR_CLASS).getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException e) {
                throw new RuntimeException("Could not instantiate class: " + CORE_CONNECTOR_CLASS, e);
            }
        } else {
            INST = new ApiBlockchainConnector();
        }
        return INST;
    }

    public abstract void createAccount(final String password, final String name);

    public abstract AccountDTO getAccount(final String address);

    public String sendTransaction(final SendRequestDTO dto) throws ValidationException {
        if (dto == null || !dto.validate()) {
            throw new ValidationException("Invalid transaction request data");
        }
        if (dto.estimateValue().compareTo(getBalance(dto.getFrom())) >= 0) {
            throw new ValidationException("Insufficient funds");
        }
        return sendTransactionInternal(dto);
    }

    protected abstract String sendTransactionInternal(final SendRequestDTO dto) throws ValidationException;

    public abstract List<AccountDTO> getAccounts();

    public abstract TransactionDTO getTransaction(final String txHash) throws NotFoundException;

    public abstract List<TransactionDTO> getLatestTransactions(final String address);

    public abstract boolean getConnectionStatusByConnectedPeers();

    public abstract SyncInfoDTO getSyncInfo();

    public abstract BigInteger getBalance(final String address);

    public abstract AccountDTO addKeystoreUTCFile(final byte[] file, final String password) throws ValidationException;

    public abstract int getPeerCount();

    // todo: Add balances with different currencies in AccountDTO
    public abstract String getCurrency();

    public void close() {
        walletStorage.save();
    }

    protected String getStoredAccountName(final String publicAddress) {
        return walletStorage.getAccountName(publicAddress);
    }

    protected void storeAccountName(final String address, final String name) {
        walletStorage.setAccountName(address, name);
    }

    public abstract AccountDTO addPrivateKey(byte[] raw, String password) throws ValidationException;
}
