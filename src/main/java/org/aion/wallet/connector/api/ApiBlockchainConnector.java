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
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.dto.SendTransactionDTO;
import org.aion.wallet.connector.dto.SyncInfoDTO;
import org.aion.wallet.connector.dto.TransactionDTO;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.dto.LightAppSettings;
import org.aion.wallet.events.AccountEvent;
import org.aion.wallet.events.EventBusFactory;
import org.aion.wallet.events.EventPublisher;
import org.aion.wallet.events.SettingsEvent;
import org.aion.wallet.exception.NotFoundException;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.storage.ApiType;
import org.aion.wallet.util.AionConstants;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    private LightAppSettings lightAppSettings = getLightweightWalletSettings(ApiType.JAVA);

    private Future<?> connectionFuture;

    private String connectionString;

    public ApiBlockchainConnector() {
        connect(getConnectionString());
        backgroundExecutor.submit(() -> processNewTransactions(0, getAccountManager().getAddresses()));
        EventPublisher.fireApplicationSettingsChanged(lightAppSettings);
        registerEventBusConsumer();
    }

    private void registerEventBusConsumer() {
        EventBusFactory.getBus(AccountEvent.ID).register(this);
        EventBusFactory.getBus(SettingsEvent.ID).register(this);
    }

    private void connect(final String newConnectionString) {
        connectionString = newConnectionString;
        if (connectionFuture != null) {
            connectionFuture.cancel(true);
        }
        connectionFuture = backgroundExecutor.submit(() -> {
            API.connect(newConnectionString, true);
            EventPublisher.fireConnectionEstablished();
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
    protected String sendTransactionInternal(final SendTransactionDTO dto) {
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
                    new ByteArrayWrapper((getAccountManager().getAccount(dto.getFrom())).getPrivateKey()),
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
        long lastBlockToCheck = getAccountManager().getLastTxInfo(address).getLastCheckedBlock();
        processNewTransactions(lastBlockToCheck, Collections.singleton(address));
        return new ArrayList<>(getAccountManager().getTransactions(address));
    }

    @Override
    public boolean getConnectionStatus() {
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
        final AccountDTO account = event.getAccount();
        if (AccountEvent.Type.CHANGED.equals(event.getType())) {
            getAccountManager().updateAccount(account);
        } else if (AccountEvent.Type.ADDED.equals(event.getType())) {
            backgroundExecutor.submit(() -> processNewTransactions(0, Collections.singleton(account.getPublicAddress())));
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

    private BigInteger getLatestTransactionNonce(final String address) {
        final TxInfo transactionInfo = getAccountManager().getLastTxInfo(address);
        final long lastCheckedBlock = transactionInfo.getLastCheckedBlock();
        long lastKnownTxCount = transactionInfo.getTxCount();
        if (lastCheckedBlock >= 0) {
            processNewTransactions(lastCheckedBlock, Collections.singleton(address));
        }
        return BigInteger.valueOf(lastKnownTxCount + 1);
    }

    private void processNewTransactions(final long lastBlockToCheck, final Set<String> addresses) {
        if (API.isConnected() && !addresses.isEmpty()) {
            final long latest = getLatest();
            for (long i = latest; i > lastBlockToCheck; i -= BLOCK_BATCH_SIZE) {
                List<Long> blockBatch = LongStream.iterate(i, j -> j - 1).limit(BLOCK_BATCH_SIZE).boxed().collect(Collectors.toList());
                List<BlockDetails> blk = getBlockDetailsByNumbers(blockBatch);
                blk.forEach(getBlockDetailsConsumer(latest, addresses));
            }
            for (String address : addresses) {
                final long txCount = getAccountManager().getLastTxInfo(address).getTxCount();
                getAccountManager().updateTxInfo(address, new TxInfo(latest, txCount));
            }
        }
    }

    private Consumer<BlockDetails> getBlockDetailsConsumer(final long latest, final Set<String> addresses) {
        return blockDetails -> {
            if (blockDetails != null) {
                final long timestamp = blockDetails.getTimestamp();
                final long blockNumber = blockDetails.getNumber();
                for (final String address : addresses) {
                    Set<TransactionDTO> txs = getAccountManager().getTransactions(address);
                    txs.addAll(blockDetails.getTxDetails().stream()
                            .filter(t -> TypeConverter.toJsonHex(t.getFrom().toString()).equals(address)
                                    || TypeConverter.toJsonHex(t.getTo().toString()).equals(address))
                            .map(t -> recordTransaction(address, t, timestamp, latest, blockNumber))
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
                transaction.getBlockNumber(),
                transaction.getNonce());
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
                transaction.getNonce());
    }

    private TransactionDTO recordTransaction(final String address, final TxDetails transaction, final long timeStamp, final long lastCheckedBlock, final long blockNumber) {
        final TransactionDTO transactionDTO = mapTransaction(transaction, timeStamp, blockNumber);
        final long txCount = getAccountManager().getLastTxInfo(address).getTxCount();
        if (transactionDTO.getFrom().equals(address)) {
            final long txNonce = transaction.getNonce().longValue();
            if (txCount < txNonce) {
                getAccountManager().updateTxInfo(address, new TxInfo(lastCheckedBlock, txNonce));
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
}
