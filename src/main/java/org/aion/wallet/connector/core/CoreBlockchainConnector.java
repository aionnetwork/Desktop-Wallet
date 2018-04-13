package org.aion.wallet.connector.core;

import com.google.common.eventbus.Subscribe;
import org.aion.api.server.ApiAion;
import org.aion.api.server.types.ArgTxCall;
import org.aion.api.server.types.SyncInfo;
import org.aion.base.type.Address;
import org.aion.base.util.ByteUtil;
import org.aion.base.util.TypeConverter;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.dto.SendRequestDTO;
import org.aion.wallet.connector.dto.SyncInfoDTO;
import org.aion.wallet.connector.dto.TransactionDTO;
import org.aion.wallet.connector.dto.UnlockableAccount;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.exception.NotFoundException;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.storage.WalletStorage;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.EventPublisher;
import org.aion.wallet.util.AionConstants;
import org.aion.wallet.util.BalanceFormatter;
import org.aion.zero.impl.types.AionBlock;
import org.aion.zero.types.AionTransaction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CoreBlockchainConnector implements BlockchainConnector {

    private final static ApiAion API = new WalletApi();

    private final WalletStorage walletStorage = WalletStorage.getInstance();

    public CoreBlockchainConnector() {
        EventBusFactory.getBus(EventPublisher.ACCOUNT_CHANGE_EVENT_ID).register(this);
    }

    @Override
    public String sendTransaction(SendRequestDTO dto) throws ValidationException {
        if (dto == null || !dto.isValid()) {
            throw new ValidationException("Invalid transaction request data");
        }
        if (!unlock(dto)) {
            throw new ValidationException("Failed to unlock wallet");
        }
        ArgTxCall transactionParams = new ArgTxCall(Address.wrap(ByteUtil.hexStringToBytes(dto.getFrom()))
                , Address.wrap(ByteUtil.hexStringToBytes(dto.getTo())), dto.getData(),
                dto.getNonce(), dto.getValue(), dto.getNrg(), dto.getNrgPrice());

        return TypeConverter.toJsonHex(API.sendTransaction(transactionParams));
    }

    @Override
    public List<AccountDTO> getAccounts() {
        final List<AccountDTO> accounts = new ArrayList<>();
        for (final String publicAddress : (List<String>) API.getAccounts()) {
            accounts.add(getAccount(publicAddress));
        }
        return accounts;
    }

    public AccountDTO getAccount(final String publicAddress) {
        final String name = walletStorage.getAccountName(publicAddress);
        String balance;
        try {
            balance = BalanceFormatter.formatBalance(getBalance(publicAddress));
        } catch (Exception e) {
            balance = BalanceFormatter.formatBalance(BigInteger.ZERO);
        }
        return new AccountDTO(name, publicAddress, balance, getCurrency());
    }

    @Override
    public TransactionDTO getTransaction(String txHash) throws NotFoundException {
        TransactionDTO transaction = mapTransaction(API.getTransactionByHash(TypeConverter.StringHexToByteArray(txHash)));
        if (transaction == null) {
            throw new NotFoundException();
        }
        return transaction;
    }

    @Override
    public List<TransactionDTO> getLatestTransactions(String address) {
        return getTransactions(address, AionConstants.MAX_BLOCKS_FOR_LATEST_TRANSACTIONS_QUERY);
    }

    @Override
    public boolean getConnectionStatusByConnectedPeers() {
        return API.peerCount() > 0;
    }

    @Override
    public SyncInfoDTO getSyncInfo() {
        return mapSyncInfo(API.getSync());
    }

    @Override
    public BigInteger getBalance(String address) throws Exception {
        return API.getBalance(address);
    }

    @Override
    public String getCurrency() {
        return AionConstants.CCY;
    }

    private boolean unlock(UnlockableAccount account) {
        return API.unlockAccount(account.getAddress(), account.getPassword(), AionConstants.DEFAULT_WALLET_UNLOCK_DURATION);
    }

    @Override
    public int getPeerCount() {
        return API.peerCount();
    }

    @Subscribe
    private void handleAccountChanged(final AccountDTO account) {
        walletStorage.setAccountName(account.getPublicAddress(), account.getName());
    }

    @Override
    public void close() {
        walletStorage.save();
    }

    private SyncInfoDTO mapSyncInfo(SyncInfo sync) {
        SyncInfoDTO syncInfoDTO = new SyncInfoDTO();
        syncInfoDTO.setChainBestBlkNumber(sync.chainBestBlkNumber);
        syncInfoDTO.setNetworkBestBlkNumber(sync.networkBestBlkNumber);
        return syncInfoDTO;
    }

    private List<TransactionDTO> getTransactions(final String addr, long nrOfBlocksToCheck) {
        AionBlock latest = API.getBestBlock();
        long blockOffset = latest.getNumber() - nrOfBlocksToCheck;
        if (blockOffset < 0) {
            blockOffset = 0;
        }
        final String parsedAddr = TypeConverter.toJsonHex(addr);
        List<TransactionDTO> txs = new ArrayList<>();
        for (long i = latest.getNumber(); i > blockOffset; i--) {
            AionBlock blk = API.getBlock(i);
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
