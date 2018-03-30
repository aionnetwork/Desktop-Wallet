package org.aion.wallet.connector;

import org.aion.wallet.connector.dto.SendRequestDTO;
import org.aion.wallet.connector.dto.TransactionDTO;
import org.aion.wallet.connector.dto.UnlockableAccount;
import org.aion.wallet.exception.NotFoundException;
import org.aion.wallet.exception.ValidationException;

import java.util.List;

public interface BlockchainConnector {
    byte[] sendTransaction(SendRequestDTO dto) throws ValidationException;
    boolean unlock(UnlockableAccount account);
    List<String> getAccounts();
    TransactionDTO getTransaction(byte[] txHash) throws NotFoundException;
    List<TransactionDTO> getTransactions(String address);
}
