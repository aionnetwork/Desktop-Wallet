package org.aion.wallet.connector.api;

import com.google.common.eventbus.Subscribe;
import org.aion.api.IAionAPI;
import org.aion.api.impl.AionAPIImpl;
import org.aion.api.impl.internal.Message;
import org.aion.api.log.LogEnum;
import org.aion.api.type.*;
import org.aion.api.type.core.tx.AionTransaction;
import org.aion.base.type.Address;
import org.aion.base.type.Hash256;
import org.aion.base.util.ByteArrayWrapper;
import org.aion.base.util.TypeConverter;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.dto.*;
import org.aion.wallet.console.ConsoleManager;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.dto.LightAppSettings;
import org.aion.wallet.events.*;
import org.aion.wallet.exception.NotFoundException;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.storage.ApiType;
import org.aion.wallet.util.AionConstants;
import org.slf4j.Logger;

import java.math.BigInteger;
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

    private static final int DISCONNECT_TIMER = 3000;

    private static final List<Integer> ACCEPTED_TRANSACTION_RESPONSE_STATUSES = Arrays.asList(Message.Retcode.r_tx_Init_VALUE, Message.Retcode.r_tx_Recved_VALUE, Message.Retcode.r_tx_NewPending_VALUE, Message.Retcode.r_tx_Pending_VALUE, Message.Retcode.r_tx_Included_VALUE);

    private final ExecutorService backgroundExecutor;

    private LightAppSettings lightAppSettings = getLightweightWalletSettings(ApiType.JAVA);

    private Future<?> connectionFuture;

    private String connectionString;

    private long startingBlock;

    public ApiBlockchainConnector() {
        backgroundExecutor = Executors.newFixedThreadPool(getCores());
        connect(getConnectionString());
        EventPublisher.fireApplicationSettingsChanged(lightAppSettings);
        registerEventBusConsumer();
    }

    private int getCores() {
        int cores = Runtime.getRuntime().availableProcessors();
        if (cores > 1) {
            cores = cores / 2;
        }
        return cores;
    }

    private void registerEventBusConsumer() {
        EventBusFactory.getBus(AbstractAccountEvent.ID).register(this);
        EventBusFactory.getBus(SettingsEvent.ID).register(this);
    }

    private void connect(final String newConnectionString) {
        connectionString = newConnectionString;
        if (connectionFuture != null) {
            connectionFuture.cancel(true);
        }
        connectionFuture = backgroundExecutor.submit(() -> {
            API.connect(newConnectionString, true);
            final Block latestBlock = getLatestBlock();
            startingBlock = Math.max(0, latestBlock.getNumber() - 30 * BLOCK_BATCH_SIZE);
            EventPublisher.fireConnectionEstablished();
            processTransactionsOnReconnect();
        });
    }

    private void disconnect() {
        storeLightweightWalletSettings(lightAppSettings);
        lock();
        try {
            API.destroyApi().getObject();
            EventPublisher.fireConnectionBroken();
        } finally {
            unLock();
        }
    }

    @Override
    public final BigInteger getBalance(final String address) {
        lock();
        final BigInteger balance;
        try {
            if (isConnectionUnLocked() && API.isConnected()) {
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
    protected TransactionResponseDTO sendTransactionInternal(final SendTransactionDTO dto) throws ValidationException {
        final String fromAddress = dto.getFrom().getPublicAddress();
        final BigInteger latestTransactionNonce = getLatestTransactionNonce(fromAddress);
        final AionTransaction transaction = new AionTransaction(
                latestTransactionNonce.toByteArray(),
                new Address(dto.getTo()),
                dto.getValue().toByteArray(),
                dto.getData(),
                dto.getNrg(),
                dto.getNrgPrice()
        );
        byte[] encodedTransaction = getTransactionSigner(dto.getFrom()).sign(transaction);
        final MsgRsp response;
        lock();
        try {
            ConsoleManager.addLog("Sending transaction", ConsoleManager.LogType.TRANSACTION, ConsoleManager.LogLevel.INFO);
            response = API.getTx().sendRawTransaction(ByteArrayWrapper.wrap(encodedTransaction)).getObject();
        } finally {
            unLock();
        }

        final TransactionResponseDTO transactionResponseDTO = mapTransactionResponse(response);
        final int responseStatus = transactionResponseDTO.getStatus();
        if (!ACCEPTED_TRANSACTION_RESPONSE_STATUSES.contains(responseStatus)) {
            getAccountManager().addTimedOutTransaction(dto);
        }
        return transactionResponseDTO;
    }

    private TransactionResponseDTO mapTransactionResponse(final MsgRsp response) {
        return new TransactionResponseDTO(response.getStatus(), response.getTxHash(), response.getError());
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
    public Set<TransactionDTO> getLatestTransactions(final String address) {
        backgroundExecutor.submit(this::processTransactionsFromOldestRegisteredSafeBlock);
        return getAccountManager().getTransactions(address);
    }

    @Override
    public boolean getConnectionStatus() {
        boolean connected = false;
        lock();
        try {
            if (isConnectionUnLocked()) {
                connected = API.isConnected();
            }
        } finally {
            unLock();
        }
        return connected;
    }

    @Override
    public SyncInfoDTO getSyncInfo() {
        long chainBest;
        long netBest;
        SyncInfo syncInfo = null;
        try {
            lock();
            try {
                if (isConnectionUnLocked()) {
                    syncInfo = API.getNet().syncInfo().getObject();
                }
            } finally {
                unLock();
            }
            chainBest = syncInfo.getChainBestBlock();
            netBest = syncInfo.getNetworkBestBlock();
        } catch (Exception e) {
            final Block latestBlock = getLatestBlock();
            if (latestBlock != null) {
                chainBest = latestBlock.getNumber();
            } else {
                chainBest = 0;
            }
            netBest = chainBest;
        }
        return new SyncInfoDTO(chainBest, netBest);
    }

    @Override
    public int getPeerCount() {
        final int size;
        lock();
        try {
            if (isConnectionUnLocked() && API.isConnected()) {
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
    public final String getCurrency() {
        return AionConstants.CCY;
    }

    @Override
    public void close() {
        super.close();
        disconnect();
    }

    @Override
    public void reloadSettings(final LightAppSettings settings) {
        if (!lightAppSettings.equals(settings)) {
            super.reloadSettings(settings);
            lightAppSettings = getLightweightWalletSettings(ApiType.JAVA);
            final String newConnectionString = getConnectionString();
            if (!newConnectionString.equalsIgnoreCase(this.connectionString)) {
                disconnect();
                try {
                    Thread.sleep(DISCONNECT_TIMER);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
                connect(newConnectionString);
            }
            EventPublisher.fireApplicationSettingsApplied(settings);
        }
    }

    @Override
    public LightAppSettings getSettings() {
        return lightAppSettings;
    }

    @Subscribe
    private void handleAccountEvent(final AccountEvent event) {
        final AccountDTO account = event.getPayload();
        if (AbstractAccountEvent.Type.CHANGED.equals(event.getType())) {
            getAccountManager().updateAccount(account);
        } else if (AbstractAccountEvent.Type.ADDED.equals(event.getType())) {
            backgroundExecutor.submit(() -> processTransactionsFromBlock(null, Collections.singleton(account.getPublicAddress())));
        }
    }

    @Subscribe
    private void handleAccountListEvent(final AccountListEvent event) {
        if (AbstractAccountEvent.Type.RECOVERED.equals(event.getType())) {
            final Set<String> addresses = event.getPayload();
            final BlockDTO oldestSafeBlock = getOldestSafeBlock(addresses, i -> {});
            backgroundExecutor.submit(() -> processTransactionsFromBlock(oldestSafeBlock, addresses));
            final Iterator<String> addressesIterator = addresses.iterator();
            AccountDTO account = getAccount(addressesIterator.next());
            account.setActive(true);
            EventPublisher.fireAccountChanged(account);
        }
    }

    @Subscribe
    private void handleSettingsChanged(final SettingsEvent event) {
        if (SettingsEvent.Type.CHANGED.equals(event.getType())) {
            final LightAppSettings settings = event.getSettings();
            if (settings != null) {
                reloadSettings(settings);
            }
        }
    }

    private void processTransactionsOnReconnect() {
        final Set<String> addresses = getAccountManager().getAddresses();
        final BlockDTO oldestSafeBlock = getOldestSafeBlock(addresses, i -> {
        });
        processTransactionsFromBlock(oldestSafeBlock, addresses);
    }

    private void processTransactionsFromOldestRegisteredSafeBlock() {
        final Set<String> addresses = getAccountManager().getAddresses();
        final Consumer<Iterator<String>> nullSafeBlockFilter = Iterator::remove;
        final BlockDTO oldestSafeBlock = getOldestSafeBlock(addresses, nullSafeBlockFilter);
        if (oldestSafeBlock != null) {
            processTransactionsFromBlock(oldestSafeBlock, addresses);
        }
    }

    private BlockDTO getOldestSafeBlock(final Set<String> addresses, final Consumer<Iterator<String>> safeBlockFilter) {
        BlockDTO oldestSafeBlock = null;
        final Iterator<String> addressIterator = addresses.iterator();
        while (addressIterator.hasNext()) {
            final String address = addressIterator.next();
            final BlockDTO lastSafeBlock = getAccountManager().getLastSafeBlock(address);
            if (lastSafeBlock != null) {
                if (oldestSafeBlock == null || oldestSafeBlock.getNumber() > lastSafeBlock.getNumber()) {
                    oldestSafeBlock = lastSafeBlock;
                }
            } else {
                safeBlockFilter.accept(addressIterator);
            }
        }
        return oldestSafeBlock;
    }

    private void processTransactionsFromBlock(final BlockDTO lastSafeBlock, final Set<String> addresses) {
        if (API.isConnected()) {
            if (!addresses.isEmpty()) {
                final long latest = getLatestBlock().getNumber();
                final long previousSafe = lastSafeBlock != null ? lastSafeBlock.getNumber() : startingBlock;
                log.debug("Processing transactions from block: {} to block: {}, for addresses: {}", previousSafe, latest, addresses);
                if (previousSafe > startingBlock) {
                    final Block lastSupposedSafe = getBlock(previousSafe);
                    if (lastSafeBlock == null || !Arrays.equals(lastSafeBlock.getHash(), (lastSupposedSafe.getHash().toBytes()))) {
                        EventPublisher.fireFatalErrorEncountered("A re-organization happened too far back. Please restart Wallet!");
                    }
                    removeTransactionsFromBlock(addresses, previousSafe);
                }
                for (long i = latest; i > previousSafe; i -= BLOCK_BATCH_SIZE) {
                    List<Long> blockBatch = LongStream.iterate(i, j -> j - 1).limit(BLOCK_BATCH_SIZE).boxed().collect(Collectors.toList());
                    List<BlockDetails> blk = getBlockDetailsByNumbers(blockBatch);
                    blk.forEach(getBlockDetailsConsumer(addresses));
                }
                final long newSafeBlockNumber = latest - BLOCK_BATCH_SIZE;
                final Block newSafe;
                if (newSafeBlockNumber > 0) {
                    newSafe = getBlock(newSafeBlockNumber);
                    for (String address : addresses) {
                        getAccountManager().updateLastSafeBlock(address, new BlockDTO(newSafe.getNumber(), newSafe.getHash().toBytes()));
                    }
                }
                log.debug("finished processing for addresses: {}", addresses);
            }
        } else {
            log.warn("WIll not process transactions from block: {} for addresses: {} because API is disconnected or no addresses", lastSafeBlock, addresses);
        }
    }

    private void removeTransactionsFromBlock(final Set<String> addresses, final long previousSafe) {
        for (String address : addresses) {
            final List<TransactionDTO> txs = new ArrayList<>(getAccountManager().getTransactions(address));
            final Iterator<TransactionDTO> iterator = txs.iterator();
            final List<TransactionDTO> oldTxs = new ArrayList<>();
            while (iterator.hasNext()) {
                final TransactionDTO t = iterator.next();
                if (t.getBlockNumber() > previousSafe) {
                    oldTxs.add(t);
                } else {
                    break;
                }
            }
            getAccountManager().removeTransactions(address, oldTxs);
        }
    }

    private BigInteger getLatestTransactionNonce(final String address) {
        final BigInteger txCount;
        lock();
        try {
            if (API.isConnected()) {
                txCount = API.getChain().getNonce(Address.wrap(address)).getObject();
            } else {
                txCount = BigInteger.ZERO;
            }
        } finally {
            unLock();
        }
        return txCount;
    }

    private Consumer<BlockDetails> getBlockDetailsConsumer(final Set<String> addresses) {
        return blockDetails -> {
            if (blockDetails != null) {
                final long timestamp = blockDetails.getTimestamp();
                final long blockNumber = blockDetails.getNumber();
                for (final String address : addresses) {
                    final List<TransactionDTO> newTxs = blockDetails.getTxDetails().stream()
                            .filter(t -> TypeConverter.toJsonHex(t.getFrom().toString()).equals(address)
                                    || TypeConverter.toJsonHex(t.getTo().toString()).equals(address))
                            .map(t -> mapTransaction(t, timestamp, blockNumber))
                            .collect(Collectors.toList());
                    getAccountManager().addTransactions(address, newTxs);
                }
            }
        };
    }

    private Block getLatestBlock() {
        final Block block;
        lock();
        try {
            if (isConnectionUnLocked() && API.isConnected()) {
                final long latest = API.getChain().blockNumber().getObject();
                block = API.getChain().getBlockByNumber(latest).getObject();
            } else {
                block = null;
            }
        } finally {
            unLock();
        }
        return block;
    }

    private Block getBlock(final long blockNumber) {
        final Block lastSupposedSafe;
        lock();
        try {
            lastSupposedSafe = API.getChain().getBlockByNumber(blockNumber).getObject();
        } finally {
            unLock();
        }
        return lastSupposedSafe;
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
                transaction.getBlockNumber(),
                transaction.getNonce(),
                transaction.getTransactionIndex());
    }

    private TransactionDTO mapTransaction(final TxDetails transaction, final long timeStamp, final long blockNumber) {
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
                blockNumber,
                transaction.getNonce(),
                transaction.getTxIndex());
    }

    private String getConnectionString() {
        final String protocol = lightAppSettings.getProtocol();
        final String ip = lightAppSettings.getAddress();
        final String port = lightAppSettings.getPort();
        return protocol + "://" + ip + ":" + port;
    }
}
