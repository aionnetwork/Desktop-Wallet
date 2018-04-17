package org.aion.wallet.connector;

import org.aion.wallet.connector.api.ApiBlockchainConnector;
import org.aion.wallet.connector.core.CoreBlockchainConnector;
import org.aion.wallet.connector.dto.SendRequestDTO;
import org.aion.wallet.connector.dto.SyncInfoDTO;
import org.aion.wallet.connector.dto.TransactionDTO;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.exception.NotFoundException;
import org.aion.wallet.exception.ValidationException;

import java.math.BigInteger;
import java.util.List;

public abstract class BlockchainConnector {
    public static final String WALLET_API_ENABLED_FLAG = "wallet.api.enabled";
    private static BlockchainConnector connector;

    public static BlockchainConnector getInstance() {
        if (connector != null) {
            return connector;
        }
        if (Boolean.valueOf(System.getProperty(WALLET_API_ENABLED_FLAG))) {
            connector = new ApiBlockchainConnector();
        } else {
            connector = new CoreBlockchainConnector();
        }
        return connector;
    }

    public abstract AccountDTO getAccount(final String address);

    public abstract String sendTransaction(final SendRequestDTO dto) throws ValidationException;

    public abstract List<AccountDTO> getAccounts();

    public abstract TransactionDTO getTransaction(final String txHash) throws NotFoundException;

    public abstract List<TransactionDTO> getLatestTransactions(final String address);

    public abstract boolean getConnectionStatusByConnectedPeers();

    public abstract SyncInfoDTO getSyncInfo();

    public abstract BigInteger getBalance(final String address) throws Exception;

    public abstract AccountDTO addKeystoreUTCFile(final byte[] file, final String password) throws ValidationException;

    public abstract int getPeerCount();

    // todo: Add balances with different currencies in AccountDTO
    public abstract String getCurrency();

    public abstract void close();
}
