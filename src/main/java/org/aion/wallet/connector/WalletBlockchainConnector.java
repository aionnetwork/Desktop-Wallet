package org.aion.wallet.connector;

import org.aion.api.server.ApiAion;
import org.aion.api.server.types.ArgTxCall;
import org.aion.base.type.Address;
import org.aion.wallet.WalletApi;
import org.aion.wallet.connector.dto.SendRequestDTO;
import org.aion.wallet.connector.dto.UnlockableAccount;
import org.aion.wallet.exception.ValidationException;
import org.aion.zero.impl.types.AionBlock;
import org.aion.zero.types.AionTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WalletBlockchainConnector implements BlockchainConnector {
    // todo: will we be able to access this from AccountManager?
    private static final Integer DEFAULT_UNLOCK_DURATION = 1000;
    private static final Integer MAX_BLOCKS_FOR_TRANSACTIONS_QUERY = 500;

    private ApiAion aionApi = new WalletApi();

    @Override
    public byte[] sendTransaction(SendRequestDTO dto) throws ValidationException {
        if(dto == null) {
            throw new ValidationException("Invalid transaction request data");
        }
        dto.validate();
        ArgTxCall transactionParams = new ArgTxCall(dto.getFrom(), dto.getTo(), dto.getData(),
                dto.getNonce(), dto.getValue(), dto.getNrg(), dto.getNrgPrice());

        return aionApi.sendTransaction(transactionParams);
    }

    @Override
    public boolean unlock(UnlockableAccount account) {

        return aionApi.unlockAccount(account.getAddress(), account.getPassword(), DEFAULT_UNLOCK_DURATION);
    }

    @Override
    public List<String> getAccounts() {
        return aionApi.getAccounts();
    }

    @Override
    public AionTransaction getTransaction(byte[] txHash) {
        return aionApi.getTransactionByHash(txHash);
    }

    @Override
    public List<AionTransaction> getTransactions(Address address) {
       return getTransactions(address, MAX_BLOCKS_FOR_TRANSACTIONS_QUERY);
    }

    private List<AionTransaction> getTransactions(final Address addr, long nrOfBlocksToCheck) {
        AionBlock latest = aionApi.getBestBlock();
        long blockOffset = latest.getNumber() - nrOfBlocksToCheck;
        if(blockOffset < 0){
            blockOffset = 0;
        }
        List<AionTransaction> txs = new ArrayList<>();
        for(long i = latest.getNumber(); i > blockOffset; i--){
            AionBlock blk = aionApi.getBlock(i);
            if(blk == null) {
                continue;
            }
            txs.addAll(blk.getTransactionsList().stream()
                    .filter(t -> t.getFrom().equals(addr) || t.getTo().equals(addr))
                    .collect(Collectors.toList()));
        }
        return txs;
    }
}
