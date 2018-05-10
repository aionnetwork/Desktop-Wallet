package org.aion.wallet.connector.api;

import com.google.common.eventbus.Subscribe;
import org.aion.api.IAionAPI;
import org.aion.api.impl.AionAPIImpl;
import org.aion.api.log.LogEnum;
import org.aion.api.type.*;
import org.aion.base.type.Address;
import org.aion.base.type.Hash256;
import org.aion.base.util.ByteArrayWrapper;
import org.aion.base.util.TypeConverter;
import org.aion.crypto.ECKey;
import org.aion.crypto.ECKeyFac;
import org.aion.mcf.account.Keystore;
import org.aion.mcf.account.KeystoreFormat;
import org.aion.mcf.account.KeystoreItem;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.dto.SendRequestDTO;
import org.aion.wallet.connector.dto.SyncInfoDTO;
import org.aion.wallet.connector.dto.TransactionDTO;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.dto.LightAppSettings;
import org.aion.wallet.exception.NotFoundException;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.storage.ApiType;
import org.aion.wallet.storage.WalletStorage;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.EventPublisher;
import org.aion.wallet.util.AionConstants;
import org.aion.wallet.util.BalanceUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class ApiBlockchainConnector extends BlockchainConnector {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private final static IAionAPI API = AionAPIImpl.inst();

    private static final int BLOCK_BATCH_SIZE = 300;
    public static final int DISCONNECT_TIMER = 3000;

    private final Map<String, AccountDTO> addressToAccount = new HashMap<>();

    private final Map<String, SortedSet<TransactionDTO>> addressToTransactions = Collections.synchronizedMap(new HashMap<>());

    private final Map<String, TxInfo> addressToLastTxInfo = Collections.synchronizedMap(new HashMap<>());

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    private final Comparator<? super TransactionDTO> transactionComparator = new TransactionComparator();

    private LightAppSettings lightAppSettings = getLightweightWalletSettings(ApiType.JAVA);

    private Future<?> connectionFuture;


    public ApiBlockchainConnector() {
        connect();
        loadLocallySavedAccounts();
        backgroundExecutor.submit(() -> processNewTransactions(0, addressToAccount.keySet()));
        EventBusFactory.getBus(EventPublisher.ACCOUNT_CHANGE_EVENT_ID).register(this);
        EventBusFactory.getBus(EventPublisher.SETTINGS_CHANGED_ID).register(this);
    }

    private void connect() {
        if (connectionFuture != null) {
            connectionFuture.cancel(true);
        }
        connectionFuture = backgroundExecutor.submit(() -> {
            API.connect(getConnectionString(), true);
            EventPublisher.fireOperationFinished();
        });
    }

    private void disconnect() {
        storeLightweightWalletSettings(lightAppSettings);
        lock();
        try {
            API.destroyApi().getObject();
        } finally {
            unLock();
        }
    }

    private void loadLocallySavedAccounts() {
        for (String address : Keystore.list()) {
            addressToAccount.put(address, getAccount(address));
            addressToTransactions.put(address, new TreeSet<>(transactionComparator));
            addressToLastTxInfo.put(address, new TxInfo(0, -1));
        }
    }

    @Override
    public void createAccount(final String password, final String name) {
        final String address = Keystore.create(password);
        final ECKey ecKey = Keystore.getKey(address, password);
        if (ecKey != null) {
            final AccountDTO account = createAccountWithPrivateKey(address, ecKey.getPrivKeyBytes());
            account.setName(name);
            processAccountAdded(address, true);
            storeAccountName(address, name);
        } else {
            log.error("An exception occurred while creating the new account: ");
        }
    }

    @Override
    public AccountDTO addKeystoreUTCFile(final byte[] file, final String password, final boolean shouldKeep) throws ValidationException {
        try {
            ECKey key = KeystoreFormat.fromKeystore(file, password);
            if (key == null) {
                throw new ValidationException("Could Not extract ECKey from keystore file");
            }
            KeystoreItem keystoreItem = KeystoreItem.parse(file);
            String address = keystoreItem.getAddress();
            final AccountDTO accountDTO;
            if (!Keystore.exist(address) && shouldKeep) {
                address = Keystore.create(password, key);
                accountDTO = createAccountWithPrivateKey(address, key.getPrivKeyBytes());
            } else {
                accountDTO = addressToAccount.getOrDefault(address, createAccountWithPrivateKey(address, key.getPrivKeyBytes()));
            }
            processAccountAdded(accountDTO.getPublicAddress(), false);
            return accountDTO;
        } catch (final Exception e) {
            throw new ValidationException("Could not open Keystore File", e);
        }
    }

    @Override
    public AccountDTO addPrivateKey(final byte[] raw, final String password, final boolean shouldKeep) throws ValidationException {
        try {
            ECKey key = ECKeyFac.inst().fromPrivate(raw);
            String address = Keystore.create(password, key);
            if (!address.equals("0x")) {
                if (!shouldKeep) {
                    removeKeystoreFile(address);
                }
                log.info("The private key was imported, the address is: " + address);
                final AccountDTO account = createAccountWithPrivateKey(address, raw);
                processAccountAdded(account.getPublicAddress(), false);
                return account;
            } else {
                log.info("Failed to import the private key. Already exists?");
                return null;
            }
        } catch (Exception e) {
            throw new ValidationException("Unsupported key type", e);
        }
    }

    private void processAccountAdded(final String address, final boolean isCreated) {
        addressToLastTxInfo.put(address, new TxInfo(isCreated ? -1 : 0, -1));
        addressToTransactions.put(address, new TreeSet<>(transactionComparator));
        backgroundExecutor.submit(() -> processNewTransactions(0, Collections.singleton(address)));
    }

    private AccountDTO createAccountWithPrivateKey(final String address, final byte[] privKeyBytes) {
        final String name = getStoredAccountName(address);
        final String balance = BalanceUtils.formatBalance(getBalance(address));
        AccountDTO account = new AccountDTO(name, address, balance, getCurrency());
        account.setPrivateKey(privKeyBytes);
        addressToAccount.put(account.getPublicAddress(), account);
        return account;
    }

    private void removeKeystoreFile(final String address) {
        if (Keystore.exist(address)) {
            final String unwrappedAddress = address.substring(2);
            Arrays.stream(Keystore.list())
                    .filter(s -> s.contains(unwrappedAddress))
                    .forEach(account -> removeAssociatedKeyStoreFile(unwrappedAddress));
        }
    }

    private void removeAssociatedKeyStoreFile(final String unwrappedAddress) {
        try {
            for (Path keystoreFile : Files.newDirectoryStream(WalletStorage.KEYSTORE_PATH)) {
                if (keystoreFile.toString().contains(unwrappedAddress)) {
                    Files.deleteIfExists(keystoreFile);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public AccountDTO getAccount(final String publicAddress) {
        final String name = getStoredAccountName(publicAddress);
        final String balance = BalanceUtils.formatBalance(getBalance(publicAddress));
        return new AccountDTO(name, publicAddress, balance, getCurrency());
    }

    @Override
    public List<AccountDTO> getAccounts() {
        for (Map.Entry<String, AccountDTO> entry : addressToAccount.entrySet()) {
            AccountDTO account = entry.getValue();
            account.setBalance(BalanceUtils.formatBalance(getBalance(account.getPublicAddress())));
            entry.setValue(account);
        }
        return new ArrayList<>(addressToAccount.values());
    }

    @Override
    public BigInteger getBalance(final String address) {
        lock();
        final BigInteger balance;
        try {
            if (API.isConnected()) {
                balance = API.getChain().getBalance(new Address(address)).getObject();
            } else {
                balance = null;
            }
        } finally {
            unLock();
        }
        return balance;
    }

    @Override
    protected String sendTransactionInternal(final SendRequestDTO dto) {
        final BigInteger latestTransactionNonce = getLatestTransactionNonce(dto.getFrom());
        TxArgs txArgs = new TxArgs.TxArgsBuilder()
                .from(new Address(TypeConverter.toJsonHex(dto.getFrom())))
                .to(new Address(TypeConverter.toJsonHex(dto.getTo())))
                .value(dto.getValue())
                .nonce(latestTransactionNonce)
                .data(new ByteArrayWrapper(dto.getData()))
                .nrgPrice(dto.getNrgPrice())
                .nrgLimit(dto.getNrg())
                .createTxArgs();
        final MsgRsp response;
        lock();
        try {
            response = API.getTx().sendSignedTransaction(
                    txArgs,
                    new ByteArrayWrapper((addressToAccount.get(dto.getFrom())).getPrivateKey()),
                    dto.getPassword()
            ).getObject();
        } finally {
            unLock();
        }

        return String.valueOf(response.getTxHash());
    }

    @Override
    public TransactionDTO getTransaction(final String txHash) throws NotFoundException {
        final ApiMsg txReceiptMsg;
        lock();
        try {
            txReceiptMsg = API.getChain().getTransactionByHash(Hash256.wrap(txHash));
        } finally {
            unLock();
        }
        if (txReceiptMsg == null || txReceiptMsg.getObject() == null) {
            throw new NotFoundException();
        }
        Transaction receipt = txReceiptMsg.getObject();
        return mapTransaction(receipt);
    }

    @Override
    public List<TransactionDTO> getLatestTransactions(final String address) {
        long lastBlockToCheck = addressToLastTxInfo.get(address).getLastCheckedBlock();
        processNewTransactions(lastBlockToCheck, Collections.singleton(address));
        return new ArrayList<>(addressToTransactions.getOrDefault(address, Collections.emptySortedSet()));
    }

    @Override
    public boolean getConnectionStatusByConnectedPeers() {
        final boolean connected;
        lock();
        try {
            connected = API.isConnected();
        } finally {
            unLock();
        }
        return connected;
    }

    @Override
    public SyncInfoDTO getSyncInfo() {
        long chainBest;
        long netBest;
        SyncInfo syncInfo;
        try {
            lock();
            try {
                syncInfo = API.getNet().syncInfo().getObject();
            } finally {
                unLock();
            }
            chainBest = syncInfo.getChainBestBlock();
            netBest = syncInfo.getNetworkBestBlock();
        } catch (Exception e) {
            chainBest = getLatest();
            netBest = chainBest;
        }
        SyncInfoDTO syncInfoDTO = new SyncInfoDTO();
        syncInfoDTO.setChainBestBlkNumber(chainBest);
        syncInfoDTO.setNetworkBestBlkNumber(netBest);
        return syncInfoDTO;
    }

    @Override
    public int getPeerCount() {
        final int size;
        lock();
        try {
            if (API.isConnected()) {
                size = ((List) API.getNet().getActiveNodes().getObject()).size();
            } else {
                size = 0;
            }
        } finally {
            unLock();
        }
        return size;
    }

    @Override
    public String getCurrency() {
        return AionConstants.CCY;
    }

    @Override
    public void close() {
        super.close();
        disconnect();
    }

    @Override
    public void reloadSettings(final LightAppSettings settings) {
        super.reloadSettings(settings);
        lightAppSettings = getLightweightWalletSettings(ApiType.JAVA);
        disconnect();
        try {
            Thread.sleep(DISCONNECT_TIMER);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        connect();
    }

    @Override
    public LightAppSettings getSettings() {
        return lightAppSettings;
    }

    @Subscribe
    private void handleAccountChanged(final AccountDTO account) {
        if (!account.getName().equalsIgnoreCase(getStoredAccountName(account.getPublicAddress()))) {
            storeAccountName(account.getPublicAddress(), account.getName());
        }
    }

    @Subscribe
    private void handleSettingsChanged(final LightAppSettings settings) {
        if (settings != null) {
            reloadSettings(settings);
        }
    }

    private BigInteger getLatestTransactionNonce(final String address) {
        final TxInfo transactionInfo = addressToLastTxInfo.get(address);
        final long lastCheckedBlock = transactionInfo.getLastCheckedBlock();
        long lastKnownTxCount = transactionInfo.getTxCount();
        if (lastCheckedBlock >= 0) {
            processNewTransactions(lastCheckedBlock, Collections.singleton(address));
        }
        return BigInteger.valueOf(lastKnownTxCount + 1);
    }

    private void processNewTransactions(final long lastBlockToCheck, final Set<String> addresses) {
        if (API.isConnected() && !addresses.isEmpty()) {
            final long start = System.nanoTime();
            final long latest = getLatest();
            for (long i = latest; i > lastBlockToCheck; i -= BLOCK_BATCH_SIZE) {
                List<Long> blockBatch = LongStream.iterate(i, j -> j - 1).limit(BLOCK_BATCH_SIZE).boxed().collect(Collectors.toList());
                List<BlockDetails> blk = getBlockDetailsByNumbers(blockBatch);
                blk.forEach(getBlockDetailsConsumer(latest, addresses));
            }
            final long end = System.nanoTime();
            System.out.println((end - start) + "ns from: " + lastBlockToCheck + " to: " + latest + " for: " + addresses);
            for (String address : addresses) {
                final long txCount = addressToLastTxInfo.get(address).getTxCount();
                addressToLastTxInfo.put(address, new TxInfo(latest, txCount));
            }
        }
    }

    private Consumer<BlockDetails> getBlockDetailsConsumer(final long latest, final Set<String> addresses) {
        return blockDetails -> {
            if (blockDetails != null) {
                final long timestamp = blockDetails.getTimestamp();
                for (final String address : addresses) {
                    Set<TransactionDTO> txs = addressToTransactions.get(address);
                    txs.addAll(blockDetails.getTxDetails().stream()
                            .filter(t -> TypeConverter.toJsonHex(t.getFrom().toString()).equals(address)
                                    || TypeConverter.toJsonHex(t.getTo().toString()).equals(address))
                            .map(t -> recordTransaction(address, t, timestamp, latest))
                            .collect(Collectors.toList()));
                }
            }
        };
    }

    private Long getLatest() {
        final Long latest;
        lock();
        try {
            if (API.isConnected()) {
                latest = API.getChain().blockNumber().getObject();
            } else {
                latest = 0L;
            }
        } finally {
            unLock();
        }
        return latest;
    }

    private List<BlockDetails> getBlockDetailsByNumbers(final List<Long> numbers) {
        List<BlockDetails> blockDetails;
        lock();
        try {
            blockDetails = API.getAdmin().getBlockDetailsByNumber(numbers).getObject();
        } finally {
            unLock();
        }
        return blockDetails;
    }

    private TransactionDTO mapTransaction(final Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        return new TransactionDTO(
                transaction.getFrom().toString(),
                transaction.getTo().toString(),
                transaction.getTxHash().toString(),
                TypeConverter.StringHexToBigInteger(TypeConverter.toJsonHex(transaction.getValue())),
                transaction.getNrgConsumed(),
                transaction.getNrgPrice(),
                transaction.getTimeStamp(),
                TxState.FINISHED
        );
    }

    private TransactionDTO mapTransaction(final TxDetails transaction, final long timeStamp) {
        if (transaction == null) {
            return null;
        }
        return new TransactionDTO(
                transaction.getFrom().toString(),
                transaction.getTo().toString(),
                transaction.getTxHash().toString(),
                TypeConverter.StringHexToBigInteger(TypeConverter.toJsonHex(transaction.getValue())),
                transaction.getNrgConsumed(),
                transaction.getNrgPrice(),
                timeStamp,
                TxState.FINISHED
        );
    }

    private TransactionDTO recordTransaction(final String address, TxDetails transaction, final long timeStamp, long lastCheckedBlock) {
        final TransactionDTO transactionDTO = mapTransaction(transaction, timeStamp);
        final long txCount = addressToLastTxInfo.get(address).getTxCount();
        if (transactionDTO.getFrom().equals(address)) {
            final long txNonce = transaction.getNonce().longValue();
            if (txCount < txNonce) {
                addressToLastTxInfo.put(address, new TxInfo(lastCheckedBlock, txNonce));
            }
        }
        return transactionDTO;
    }

    private String getConnectionString() {
        final String protocol = lightAppSettings.getProtocol();
        final String ip = lightAppSettings.getAddress();
        final String port = lightAppSettings.getPort();
        return protocol + "://" + ip + ":" + port;
    }

    private class TransactionComparator implements Comparator<TransactionDTO> {
        @Override
        public int compare(final TransactionDTO tx1, final TransactionDTO tx2) {
            return Long.compare(tx2.getTimeStamp(), tx1.getTimeStamp());
        }
    }
}
