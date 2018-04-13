package org.aion.wallet.connector;

import org.aion.wallet.connector.api.ApiBlockchainConnector;
import org.aion.wallet.connector.core.CoreBlockchainConnector;
import org.aion.wallet.connector.dto.SendRequestDTO;
import org.aion.wallet.connector.dto.SyncInfoDTO;
import org.aion.wallet.connector.dto.TransactionDTO;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.exception.NotFoundException;
import org.aion.wallet.exception.ValidationException;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

public interface BlockchainConnector {
    String WALLET_API_ENABLED_FLAG = "wallet.api.enabled";

    static BlockchainConnector getInstance() {
        if (Boolean.valueOf(System.getProperty(WALLET_API_ENABLED_FLAG))) {
            return new ApiBlockchainConnector();
        } else {
            return new CoreBlockchainConnector();
        }
    }

    AccountDTO getAccount(final String address);

    String sendTransaction(final SendRequestDTO dto) throws ValidationException;

    List<AccountDTO> getAccounts();

    TransactionDTO getTransaction(final String txHash) throws NotFoundException;

    List<TransactionDTO> getLatestTransactions(final String address);

    boolean getConnectionStatusByConnectedPeers();

    SyncInfoDTO getSyncInfo();

    BigInteger getBalance(final String address) throws Exception;

    int getPeerCount();

    String getCurrency();

    void close();
}
