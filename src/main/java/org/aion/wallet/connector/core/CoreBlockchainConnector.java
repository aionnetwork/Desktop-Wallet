package org.aion.wallet.connector.core;

import com.google.common.eventbus.Subscribe;
import org.aion.api.log.LogEnum;
import org.aion.api.server.types.ArgTxCall;
import org.aion.api.server.types.SyncInfo;
import org.aion.base.type.Address;
import org.aion.base.util.ByteUtil;
import org.aion.base.util.TypeConverter;
import org.aion.wallet.account.AccountManager;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.api.TxState;
import org.aion.wallet.connector.dto.SendRequestDTO;
import org.aion.wallet.connector.dto.SyncInfoDTO;
import org.aion.wallet.connector.dto.TransactionDTO;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.dto.LightAppSettings;
import org.aion.wallet.events.AccountEvent;
import org.aion.wallet.events.EventBusFactory;
import org.aion.wallet.exception.NotFoundException;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.storage.ApiType;
import org.aion.wallet.util.AionConstants;
import org.aion.zero.impl.types.AionBlock;
import org.aion.zero.types.AionTransaction;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CoreBlockchainConnector extends BlockchainConnector {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private static final WalletApi API = new WalletApi();

    private LightAppSettings lightAppSettings = getLightweightWalletSettings(ApiType.CORE);

    private AccountManager accountManager;

    public CoreBlockchainConnector() {
        EventBusFactory.getBus(AccountEvent.ID).register(this);
        accountManager = new AccountManager(lightAppSettings, this::getBalance, this::getCurrency);
    }

    public String createAccount(final String password, final String name) {
        return accountManager.createAccount(password, name);
    }

    @Override
    public AccountDTO importKeystoreFile(byte[] file, String password, final boolean shouldKeep) throws ValidationException {
        return accountManager.importKeystore(file, password, shouldKeep);
    }

    @Override
    public AccountDTO importPrivateKey(byte[] raw, String password, final boolean shouldKeep) throws ValidationException {
        return accountManager.importPrivateKey(raw, password, shouldKeep);
    }

    @Override
    public AccountDTO importMnemonic(final String mnemonic, final String password, boolean shouldKeep) throws ValidationException {
        return accountManager.importMnemonic(mnemonic, password, shouldKeep);
    }

    @Override
    public AccountDTO getAccount(final String publicAddress) {
        return accountManager.getAccount(publicAddress);
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
        if (!API.unlockAccount(dto.getFrom(), dto.getPassword(), (int) getSettings().getUnlockTimeout().get(ChronoUnit.SECONDS))) {
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

    @Override
    public int getPeerCount() {
        return API.peerCount();
    }

    @Subscribe
    private void handleAccountEvent(final AccountEvent event) {
        if (AccountEvent.Type.CHANGED.equals(event.getType())) {
            accountManager.updateAccount(event.getAccount());
        }
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
