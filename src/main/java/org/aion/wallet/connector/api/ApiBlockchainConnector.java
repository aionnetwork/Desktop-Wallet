package org.aion.wallet.connector.api;

import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import org.aion.api.IAionAPI;
import org.aion.api.impl.AionAPIImpl;
import org.aion.api.impl.internal.Message;
import org.aion.api.log.LogEnum;
import org.aion.api.type.*;
import org.aion.api.type.core.tx.AionTransaction;
import org.aion.base.type.Address;
import org.aion.base.type.Hash256;
import org.aion.base.util.ByteArrayWrapper;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.NullTokenManager;
import org.aion.wallet.connector.TokenManager;
import org.aion.wallet.connector.dto.*;
import org.aion.wallet.console.ConsoleManager;
import org.aion.wallet.dto.*;
import org.aion.wallet.events.*;
import org.aion.wallet.exception.NotFoundException;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.storage.ApiType;
import org.aion.wallet.util.AddressUtils;
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

    private static final List<Integer> ACCEPTED_TRANSACTION_RESPONSE_STATUSES = Arrays.asList(Message.Retcode
            .r_tx_Init_VALUE, Message.Retcode.r_tx_Recved_VALUE, Message.Retcode.r_tx_NewPending_VALUE, Message
            .Retcode.r_tx_Pending_VALUE, Message.Retcode.r_tx_Included_VALUE);

    private static final String FAILED_TOKEN_TRANSFER_FORMAT = "Transaction: %s was registered but the Token transfer has failed due to a small energy limit";

    private final TokenManager tokenManager;

    private final ExecutorService backgroundExecutor;

    private final Map<String, TokenDetails> tokenAddressToDetails = new HashMap<>();

    private LightAppSettings lightAppSettings = getLightweightWalletSettings(ApiType.JAVA);

    private ConnectionProvider connectionProvider = getConnectionKeyProvider();

    private Future<?> connectionFuture;

    private ConnectionDetails connectionDetails;

    private long startingBlock;

    public ApiBlockchainConnector() {
        backgroundExecutor = Executors.newFixedThreadPool(getCores());
        connect();
        EventPublisher.fireApplicationSettingsChanged(lightAppSettings);
        registerEventBusConsumer();
        tokenManager = new NullTokenManager();
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

    @Override
    public void connect() {
        connect(getConnectionDetails());
    }

    private void connect(final ConnectionDetails newConnectionDetails) {
        connectionDetails = newConnectionDetails;
        if (connectionFuture != null) {
            if (!connectionFuture.isDone()) {
                connectionFuture.cancel(true);
            }
        }
        connectionFuture = backgroundExecutor.submit(() -> {
            final String connectionKey = newConnectionDetails.getSecureKey();
            Platform.runLater(() -> EventPublisher.fireConnectAttmpted(newConnectionDetails.isSecureConnection()));
            final ApiMsg connect = API.connect(
                    newConnectionDetails.toConnectionString(),
                    true,
                    1,
                    60_000,
                    connectionKey
            );
            if (connect.getObject()) {
                Platform.runLater(
                        () -> EventPublisher.fireConnectionEstablished(newConnectionDetails.isSecureConnection())
                );
                final Block latestBlock = getLatestBlock();
                startingBlock = Math.max(0, latestBlock.getNumber() - 30 * BLOCK_BATCH_SIZE);
                processTransactionsOnReconnect();
            } else {
                Platform.runLater(EventPublisher::fireConnectionBroken);
            }
        });
    }

    private void disconnect() {
        storeLightweightWalletSettings(lightAppSettings);
        storeConnectionKeys(connectionProvider);
        lock();
        try {
            API.destroyApi().getObject();
            Platform.runLater(EventPublisher::fireConnectionBroken);
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
    public BigInteger getTokenBalance(final String tokenAddress, final String accountAddress) throws ValidationException {
        lock();
        try {
            return new BigInteger(String.valueOf(tokenManager.getBalance(tokenAddress, accountAddress)));
        } finally {
            unLock();
        }
    }

    public byte[] getTokenSendData(final String tokenAddress, final String accountAddress, final String destinationAddress, final BigInteger value) throws ValidationException {
        return tokenManager.getEncodedSendTokenData(tokenAddress, accountAddress, destinationAddress, value);
    }

    @Override
    public TokenDetails getTokenDetails(final String tokenAddress, final String accountAddress) throws ValidationException {
        if (!AddressUtils.isValid(tokenAddress)) {
            throw new ValidationException("Invalid token address");
        }
        TokenDetails tokenDetails = tokenAddressToDetails.get(tokenAddress);
        if (tokenDetails == null) {
            tokenDetails = createTokenDetails(tokenAddress, accountAddress);
            tokenAddressToDetails.put(tokenAddress, tokenDetails);
        }
        return tokenDetails;
    }

    private TokenDetails createTokenDetails(String tokenAddress, String accountAddress) throws ValidationException {
        final String name;
        final String symbol;
        final long granularity;
        lock();
        try {
            name = tokenManager.getName(tokenAddress, accountAddress);
            symbol = tokenManager.getSymbol(tokenAddress, accountAddress);
            granularity = tokenManager.getGranularity(tokenAddress, accountAddress);
        } finally {
            unLock();
        }
        return new TokenDetails(tokenAddress, name, symbol, granularity);
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
            ConsoleManager.addLog(
                    "Sending transaction", ConsoleManager.LogType.TRANSACTION, ConsoleManager.LogLevel.INFO
            );
            response = API.getTx().sendRawTransaction(ByteArrayWrapper.wrap(encodedTransaction)).getObject();
        } finally {
            unLock();
        }

        final TransactionResponseDTO transactionResponseDTO = mapTransactionResponse(response);
        final int responseStatus = transactionResponseDTO.getStatus();
        if (!ACCEPTED_TRANSACTION_RESPONSE_STATUSES.contains(responseStatus)) {
            getAccountManager().addTimedOutTransaction(dto);
        } else if (response.getError() != null && response.getError().equals("OUT_OF_NRG")) {
            final String message = String.format(FAILED_TOKEN_TRANSFER_FORMAT, transactionResponseDTO.getTxHash());
            ConsoleManager.addLog(message, ConsoleManager.LogType.TRANSACTION, ConsoleManager.LogLevel.ERROR);
            log.error(message);
            throw new ValidationException("Token Transfer failed. See logs for details");
        }
        return transactionResponseDTO;
    }

    private TransactionResponseDTO mapTransactionResponse(final MsgRsp response) {
        return new TransactionResponseDTO(response.getStatus(), response.getTxHash().toString(), response.getError());
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
    public boolean isConnected() {
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
    protected boolean isSecuredConnection() {
        return connectionDetails.isSecureConnection();
    }

    @Override
    public void close() {
        disconnect();
        backgroundExecutor.shutdown();
        super.close();
    }

    @Override
    public void reloadSettings(final LightAppSettings settings) {
        boolean settingsChanged = false;
        if (!lightAppSettings.equals(settings)) {
            super.reloadSettings(settings);
            settingsChanged = true;
        }
        lightAppSettings = getLightweightWalletSettings(ApiType.JAVA);
        final ConnectionDetails newConnectionDetails = getConnectionDetails();
        if (isConnected()) {
            if (!connectionDetails.toConnectionString().equals(newConnectionDetails.toConnectionString())) {
                Platform.runLater(EventPublisher::fireDisconnectAttempted);
                disconnect();
                try {
                    Thread.sleep(DISCONNECT_TIMER);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
                connect(newConnectionDetails);
                settingsChanged = true;
            }
        } else {
            connect(newConnectionDetails);
            settingsChanged = true;
        }
        if(settingsChanged) {
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
            backgroundExecutor.submit(() -> processTransactionsFromBlock(null, Collections.singleton(account
                    .getPublicAddress())));
        }
    }

    @Subscribe
    private void handleAccountListEvent(final AccountListEvent event) {
        if (AbstractAccountEvent.Type.RECOVERED.equals(event.getType())) {
            final Set<String> addresses = event.getPayload();
            final BlockDTO oldestSafeBlock = getOldestSafeBlock(addresses, i -> {});
            backgroundExecutor.submit(() -> processTransactionsFromBlock(oldestSafeBlock, addresses));
        }
    }

    @Subscribe
    private void handleSettingsChanged(final SettingsEvent event) {
        if (SettingsEvent.Type.CHANGED.equals(event.getType())) {
            final LightAppSettings settings = event.getSettings();
            if (settings != null) {
                backgroundExecutor.submit(() -> reloadSettings(settings));
            }
        }
    }

    private void processTransactionsOnReconnect() {
        final Set<String> addresses = getAccountManager().getAddresses();
        final BlockDTO oldestSafeBlock = getOldestSafeBlock(addresses, i -> {});
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
                log.debug("Processing transactions from block: {} to block: {}, for addresses: {}", previousSafe,
                        latest, addresses);
                if (previousSafe > startingBlock) {
                    final Block lastSupposedSafe = getBlock(previousSafe);
                    if (lastSafeBlock == null || !Arrays.equals(lastSafeBlock.getHash(), (lastSupposedSafe.getHash().toBytes()))) {
                        EventPublisher.fireFatalErrorEncountered("A re-organization happened too far back. Please " +
                                "restart Wallet!");
                    }
                    removeTransactionsFromBlock(addresses, previousSafe);
                }
                for (long i = latest; i > previousSafe; i -= BLOCK_BATCH_SIZE) {
                    List<Long> blockBatch = LongStream.iterate(i, j -> j - 1).limit(BLOCK_BATCH_SIZE).boxed().collect
                            (Collectors.toList());
                    List<BlockDetails> blk = getBlockDetailsByNumbers(blockBatch);
                    blk.forEach(getBlockDetailsConsumer(addresses));
                }
                long newSafeBlockNumber = Math.max(1, latest - BLOCK_BATCH_SIZE);
                final Block newSafe = getBlock(newSafeBlockNumber);
                for (String address : addresses) {
                    getAccountManager().updateLastSafeBlock(address, new BlockDTO(newSafe.getNumber(), newSafe.getHash().toBytes()));
                }
                log.debug("finished processing for addresses: {}", addresses);
            }
        } else {
            log.warn("WIll not process transactions from block: {} for addresses: {} because API is disconnected or " +
                    "no addresses", lastSafeBlock, addresses);
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
                            .filter(t -> isMatchingTransaction(address, t))
                            .map(t -> mapTransaction(t, timestamp, blockNumber))
                            .collect(Collectors.toList());
                    getAccountManager().addTransactions(address, newTxs);
                }
            }
        };
    }

    private boolean isMatchingTransaction(String address, TxDetails t) {
        SendData sendData = getSendData(t.getFrom().toString(), t.getTo().toString(), t.getValue(), t.getData().getData());
        return AddressUtils.equals(sendData.getFrom(), address) || AddressUtils.equals(sendData.getTo(), address);
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
        final SendData sendData = getSendData(
                transaction.getFrom().toString(),
                transaction.getTo().toString(),
                transaction.getValue(),
                transaction.getData().getData()
        );
        return new TransactionDTO(
                sendData.getFrom(), sendData.getTo(), sendData.getValue(), sendData.getCoin(),
                transaction.getTxHash().toString(),
                transaction.getNrgConsumed(),
                transaction.getNrgPrice(),
                transaction.getTimeStamp(),
                transaction.getBlockNumber(),
                transaction.getNonce(),
                transaction.getTransactionIndex()
        );
    }

    private TransactionDTO mapTransaction(final TxDetails transaction, final long timeStamp, final long blockNumber) {
        if (transaction == null) {
            return null;
        }
        final SendData sendData = getSendData(
                transaction.getFrom().toString(),
                transaction.getTo().toString(),
                transaction.getValue(),
                transaction.getData().getData()
        );
        return new TransactionDTO(
                sendData.getFrom(), sendData.getTo(), sendData.getValue(), sendData.getCoin(),
                transaction.getTxHash().toString(),
                transaction.getNrgConsumed(),
                transaction.getNrgPrice(),
                timeStamp,
                blockNumber,
                transaction.getNonce(),
                transaction.getTxIndex()
        );
    }

    private ConnectionDetails getConnectionDetails() {
        return lightAppSettings.getConnectionDetails();
    }

    @Override
    public void storeConnectionKeys(final ConnectionProvider connectionProvider) {
        super.storeConnectionKeys(connectionProvider);
        this.connectionProvider = getConnectionKeyProvider();
    }
}
