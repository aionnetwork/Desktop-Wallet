package org.aion.wallet.connector;

import org.aion.api.server.ApiAion;
import org.aion.api.server.types.ArgTxCall;
import org.aion.base.type.Address;
import org.aion.base.util.ByteUtil;
import org.aion.base.util.TypeConverter;
import org.aion.wallet.WalletApi;
import org.aion.wallet.connector.dto.SendRequestDTO;
import org.aion.wallet.connector.dto.TransactionDTO;
import org.aion.wallet.connector.dto.UnlockableAccount;
import org.aion.wallet.exception.NotFoundException;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.util.WalletUtils;
import org.aion.zero.impl.types.AionBlock;
import org.aion.zero.types.AionTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WalletBlockchainConnector implements BlockchainConnector {

    private ApiAion aionApi = new WalletApi();
//
    @Override
    public String sendTransaction(SendRequestDTO dto) throws ValidationException {
        if (dto == null || !dto.isValid()) {
            throw new ValidationException("Invalid transaction request data");
        }
        ArgTxCall transactionParams = new ArgTxCall(Address.wrap(ByteUtil.hexStringToBytes(dto.getFrom()))
                , Address.wrap(ByteUtil.hexStringToBytes(dto.getTo())), dto.getData(),
                dto.getNonce(), dto.getValue(), dto.getNrg(), dto.getNrgPrice());

        return TypeConverter.toJsonHex(aionApi.sendTransaction(transactionParams));
    }

    @Override
    public boolean unlock(UnlockableAccount account) {
        return aionApi.unlockAccount(account.getAddress(), account.getPassword(), WalletUtils.DEFAULT_WALLET_UNLOCK_DURATION);
    }

    @Override
    public List<String> getAccounts() {
        return aionApi.getAccounts();
    }

    @Override
    public TransactionDTO getTransaction(String txHash) throws NotFoundException {
        TransactionDTO transaction = mapTransaction(aionApi.getTransactionByHash(TypeConverter.StringHexToByteArray(txHash)));
        if (transaction == null) {
            throw new NotFoundException();
        }
        return transaction;
    }

    @Override
    public List<TransactionDTO> getLatestTransactions(String address) {
        return getTransactions(address, WalletUtils.MAX_BLOCKS_FOR_LATEST_TRANSACTIONS_QUERY);
    }

    private List<TransactionDTO> getTransactions(final String addr, long nrOfBlocksToCheck) {
        AionBlock latest = aionApi.getBestBlock();
        long blockOffset = latest.getNumber() - nrOfBlocksToCheck;
        if (blockOffset < 0) {
            blockOffset = 0;
        }
        final String parsedAddr = TypeConverter.toJsonHex(addr);
        List<TransactionDTO> txs = new ArrayList<>();
        for (long i = latest.getNumber(); i > blockOffset; i--) {
            AionBlock blk = aionApi.getBlock(i);
            if (blk == null || blk.getTransactionsList().size() == 0) {
                continue;
            }
            txs.addAll(blk.getTransactionsList().stream()
                    .filter(t -> TypeConverter.toJsonHex(t.getFrom().toString()).equals(parsedAddr)
                            || TypeConverter.toJsonHex(t.getTo().toString()).equals(parsedAddr))
                    .map(this::mapTransaction)
                    .collect(Collectors.toList()));
        }
        return txs;
    }

    private TransactionDTO mapTransaction(AionTransaction transaction) {
        if (transaction == null) {
            return null;
        }
        TransactionDTO dto = new TransactionDTO();
        dto.setFrom(transaction.getFrom().toString());
        dto.setTo(transaction.getTo().toString());
        dto.setValue(TypeConverter.StringHexToBigInteger(TypeConverter.toJsonHex(transaction.getValue())));
        dto.setNrg(transaction.getNrg());
        dto.setNrgPrice(transaction.getNrgPrice());
        return dto;
    }
}
