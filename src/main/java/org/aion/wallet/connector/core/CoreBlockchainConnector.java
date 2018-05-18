package org.aion.wallet.connector.core;

import com.google.common.eventbus.Subscribe;
import org.aion.api.log.LogEnum;
import org.aion.api.server.types.ArgTxCall;
import org.aion.api.server.types.SyncInfo;
import org.aion.base.type.Address;
import org.aion.base.util.ByteUtil;
import org.aion.base.util.TypeConverter;
import org.aion.mcf.account.Keystore;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.api.TxState;
import org.aion.wallet.connector.dto.SendRequestDTO;
import org.aion.wallet.connector.dto.SyncInfoDTO;
import org.aion.wallet.connector.dto.TransactionDTO;
import org.aion.wallet.connector.dto.UnlockableAccount;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.dto.LightAppSettings;
import org.aion.wallet.exception.NotFoundException;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.EventPublisher;
import org.aion.wallet.util.AionConstants;
import org.aion.wallet.util.BalanceUtils;
import org.aion.zero.impl.types.AionBlock;
import org.aion.zero.types.AionTransaction;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CoreBlockchainConnector extends BlockchainConnector {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private final static WalletApi API = new WalletApi();

    public CoreBlockchainConnector() {
        EventBusFactory.getBus(EventPublisher.ACCOUNT_CHANGE_EVENT_ID).register(this);
    }

    public void createAccount(final String password, final String name) {
        final String address = Keystore.create(password);
        AccountDTO account = getAccount(address);
        account.setName(name);
        storeAccountName(address, name);
    }

    @Override
    public AccountDTO addKeystoreUTCFile(byte[] file, String password, final boolean shouldKeep) throws ValidationException {
        throw new ValidationException("Unsupported operation");
    }

    @Override
    public AccountDTO addPrivateKey(byte[] raw, String password, final boolean shouldKeep) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AccountDTO getAccount(final String publicAddress) {
        final String name = getStoredAccountName(publicAddress);
        return new AccountDTO(name, publicAddress, BalanceUtils.formatBalance(getBalance(publicAddress)), getCurrency());
    }

    @Override
    public List<AccountDTO> getAccounts() {
        final List<AccountDTO> accounts = new ArrayList<>();
        for (final String publicAddress : (List<String>) API.getAccounts()) {
            accounts.add(getAccount(publicAddress));
        }
        return accounts;
    }

    @Override
    public BigInteger getBalance(String address){
        try {
            return API.getBalance(address);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return BigInteger.ZERO;
        }
    }

    @Override
    protected String sendTransactionInternal(SendRequestDTO dto) throws ValidationException {
        if (!unlock(dto)) {
            throw new ValidationException("Failed to unlock wallet");
        }
        ArgTxCall transactionParams = new ArgTxCall(Address.wrap(ByteUtil.hexStringToBytes(dto.getFrom()))
                , Address.wrap(ByteUtil.hexStringToBytes(dto.getTo())), dto.getData(),
                dto.getNonce(), dto.getValue(), dto.getNrg(), dto.getNrgPrice());

        return TypeConverter.toJsonHex(API.sendTransaction(transactionParams));
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
    public String getCurrency() {
        return AionConstants.CCY;
    }

    @Override
    public LightAppSettings getSettings() {
        throw new UnsupportedOperationException();
    }

    private boolean unlock(UnlockableAccount account) {
        return API.unlockAccount(account.getAddress(), account.getPassword(), AionConstants.DEFAULT_WALLET_UNLOCK_DURATION);
    }

    @Override
    public int getPeerCount() {
        return API.peerCount();
    }

    @Override
    public AccountDTO importAccountWithMnemonic(final String mnemonic, final String password) {
        throw new UnsupportedOperationException();
    }

    @Subscribe
    private void handleAccountChanged(final AccountDTO account) {
        storeAccountName(account.getPublicAddress(), account.getName());
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
        return new TransactionDTO(
                transaction.getFrom().toString(),
                transaction.getTo().toString(),
                ByteUtil.toHexString(transaction.getHash()),
                TypeConverter.StringHexToBigInteger(TypeConverter.toJsonHex(transaction.getValue())),
                transaction.getNrg(),
                transaction.getNrgPrice(),
                transaction.getTimeStampBI().longValue(),
                TxState.FINISHED);
    }
}
