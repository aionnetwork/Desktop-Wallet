package org.aion.wallet.connector.api;

import org.aion.api.IAionAPI;
import org.aion.api.impl.AionAPIImpl;
import org.aion.api.type.BlockDetails;
import org.aion.api.type.TxArgs;
import org.aion.api.type.TxDetails;
import org.aion.base.type.Address;
import org.aion.base.util.ByteArrayWrapper;
import org.aion.base.util.TypeConverter;
import org.aion.mcf.account.KeystoreFormat;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.dto.SendRequestDTO;
import org.aion.wallet.connector.dto.SyncInfoDTO;
import org.aion.wallet.connector.dto.TransactionDTO;
import org.aion.wallet.exception.NotFoundException;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.util.WalletUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ApiBlockchainConnector implements BlockchainConnector {

    private final static IAionAPI aionApi = AionAPIImpl.inst();

    public ApiBlockchainConnector() {
        if (aionApi.isConnected()) {
            return;
        }
        aionApi.connect(IAionAPI.LOCALHOST_URL, true);
    }

    @Override
    public String sendTransaction(SendRequestDTO dto) throws ValidationException {
        if (dto == null || !dto.isValid()) {
            throw new ValidationException("Invalid transaction request data");
        }
        TxArgs txArgs = new TxArgs.TxArgsBuilder()
                .from(new Address(dto.getFrom()))
                .to(new Address(dto.getTo()))
                .value(dto.getValue())
                .nonce(dto.getNonce())
                .data(new ByteArrayWrapper(dto.getData()))
                .nrgPrice(dto.getNrgPrice())
                .nrgLimit(dto.getNrg()).createTxArgs();
        byte[] privateKey = null;
        return aionApi.getTx().sendSignedTransaction(txArgs, new ByteArrayWrapper(privateKey), dto.getPassword()).getObject();
    }

    @Override
    public List<String> getAccounts() {
        return Collections.emptyList();
    }

    @Override
    public TransactionDTO getTransaction(String txHash) throws NotFoundException {
        throw new NotFoundException();
    }

    @Override
    public List<TransactionDTO> getLatestTransactions(String address) {
        return getTransactions(address, WalletUtils.MAX_BLOCKS_FOR_LATEST_TRANSACTIONS_QUERY);
    }

    @Override
    public boolean getConnectionStatusByConnectedPeers() {
        return aionApi.isConnected();
    }

    @Override
    public SyncInfoDTO getSyncInfo() {
        SyncInfoDTO syncInfoDTO = new SyncInfoDTO();
        syncInfoDTO.setChainBestBlkNumber(aionApi.getChain().blockNumber().getObject());
        syncInfoDTO.setNetworkBestBlkNumber(aionApi.getChain().blockNumber().getObject());
        return syncInfoDTO;
    }

    @Override
    public BigInteger getBalance(String address) throws Exception {
        return aionApi.getChain().getBalance(new Address(address)).getObject();
    }

    private byte[] getPrivateKeyFromUTCKeystoreFile(byte[] key, String password) {
        return KeystoreFormat.fromKeystore(key, password).getPrivKeyBytes();
    }

    private List<TransactionDTO> getTransactions(final String addr, long nrOfBlocksToCheck) {
        Long latest = aionApi.getChain().blockNumber().getObject();
        long blockOffset = latest - nrOfBlocksToCheck;
        if (blockOffset < 0) {
            blockOffset = 0;
        }
        final String parsedAddr = TypeConverter.toJsonHex(addr);
        List<TransactionDTO> txs = new ArrayList<>();
        for (long i = latest; i > blockOffset; i--) {
            BlockDetails blk = getBlockDetailsByNumber(i);
            if (blk == null || blk.getTxDetails().size() == 0) {
                continue;
            }
            txs.addAll(blk.getTxDetails().stream()
                    .filter(t -> TypeConverter.toJsonHex(t.getFrom().toString()).equals(parsedAddr)
                            || TypeConverter.toJsonHex(t.getTo().toString()).equals(parsedAddr))
                    .map(this::mapTransaction)
                    .collect(Collectors.toList()));
        }
        return txs;
    }

    private BlockDetails getBlockDetailsByNumber(Long number) {
        return ((List<BlockDetails>) aionApi.getAdmin().getBlockDetailsByNumber(number.toString()).getObject()).get(0);
    }

    private TransactionDTO mapTransaction(TxDetails transaction) {
        if (transaction == null) {
            return null;
        }
        TransactionDTO dto = new TransactionDTO();
        dto.setFrom(transaction.getFrom().toString());
        dto.setTo(transaction.getTo().toString());
        dto.setValue(TypeConverter.StringHexToBigInteger(TypeConverter.toJsonHex(transaction.getValue())));
        dto.setNrg(transaction.getNrgConsumed());
        dto.setNrgPrice(transaction.getNrgPrice());
        return dto;
    }
}
