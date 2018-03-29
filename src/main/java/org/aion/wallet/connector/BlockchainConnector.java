package org.aion.wallet.connector;

import org.aion.base.type.Address;
import org.aion.wallet.connector.dto.SendRequestDTO;
import org.aion.wallet.connector.dto.UnlockableAccount;
import org.aion.zero.types.AionTransaction;

import java.util.List;

public interface BlockchainConnector {
    byte[] sendTransaction(SendRequestDTO dto);
    boolean unlock(UnlockableAccount account);
    List<String> getAccounts();
    AionTransaction getTransaction(byte[] txHash);
    //todo: create transaction dto so we cut the core dep
    List<AionTransaction> getTransactions(Address address);
}
