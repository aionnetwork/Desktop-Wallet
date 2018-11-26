package org.aion.wallet.connector.core;

import com.google.common.eventbus.Subscribe;
import org.aion.api.log.LogEnum;
import org.aion.api.server.types.SyncInfo;
import org.aion.base.util.ByteUtil;
import org.aion.base.util.TypeConverter;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.dto.*;
import org.aion.wallet.dto.LightAppSettings;
import org.aion.wallet.dto.TokenDetails;
import org.aion.wallet.events.AccountEvent;
import org.aion.wallet.events.EventBusFactory;
import org.aion.wallet.events.EventPublisher;
import org.aion.wallet.exception.NotFoundException;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.storage.ApiType;
import org.aion.wallet.util.AionConstants;
import org.aion.zero.impl.types.AionBlock;
import org.aion.zero.types.AionTransaction;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class CoreBlockchainConnector extends BlockchainConnector {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private static final WalletApi API = new WalletApi();

    public CoreBlockchainConnector() {
        EventBusFactory.getBus(AccountEvent.ID).register(this);
        EventPublisher.fireApplicationSettingsChanged(getLightweightWalletSettings(ApiType.CORE));
    }

    @Override
    public BigInteger getBalance(final String address) {
        try {
            return API.getBalance(address);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return BigInteger.ZERO;
        }
    }

    @Override
    public BigInteger getTokenBalance(final String tokenAddress, final String accountAddress) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getTokenSendData(final String tokenAddress, final String accountAddress, final String destinationAddress, final BigInteger value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TokenDetails getTokenDetails(final String tokenAddress, final String accountAddress) throws ValidationException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected TransactionResponseDTO sendTransactionInternal(final SendTransactionDTO dto) {
        throw new UnsupportedOperationException();
        //TODO
    }

    @Override
    public TransactionDTO getTransaction(final String txHash) throws NotFoundException {
        TransactionDTO transaction = mapTransaction(API.getTransactionByHash(TypeConverter.StringHexToByteArray(txHash)));
        if (transaction == null) {
            throw new NotFoundException();
        }
        return transaction;
    }

    @Override
    public Set<TransactionDTO> getLatestTransactions(final String address) {
        final BlockDTO lastSafeBlock = getAccountManager().getLastSafeBlock(address);
        processNewTransactions(lastSafeBlock, Collections.singleton(address));
        return getAccountManager().getTransactions(address);
    }

    private void processNewTransactions(final BlockDTO lastCheckedBlock, final Set<String> addresses) {
        if (!addresses.isEmpty()) {
            final AionBlock latest = API.getBestBlock();
            long lastBlockToCheck = lastCheckedBlock != null ? lastCheckedBlock.getNumber() : 0;
            for (long i = latest.getNumber(); i > lastBlockToCheck; i -= 1) {
                AionBlock blk = API.getBlock(i);
                if (blk == null || blk.getTransactionsList().size() == 0) {
                    continue;
                }
                for (final String address : addresses) {
                    getAccountManager().addTransactions(address,
                            blk.getTransactionsList().stream()
                                    .filter(t -> TypeConverter.toJsonHex(t.getFrom().toString()).equals(address)
                                            || TypeConverter.toJsonHex(t.getTo().toString()).equals(address))
                                    .map(this::mapTransaction)
                                    .collect(Collectors.toList()));
                }
            }
            for (String address : addresses) {
                getAccountManager().updateLastSafeBlock(address, new BlockDTO(latest.getNumber(), latest.getHash()));
            }
        }
    }

    @Override
    public boolean isConnected() {
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
    protected boolean isSecuredConnection() {
        return true;
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
            getAccountManager().updateAccount(event.getPayload());
        }
    }

    private SyncInfoDTO mapSyncInfo(final SyncInfo sync) {
        return new SyncInfoDTO(sync.chainBestBlkNumber, sync.networkBestBlkNumber);
    }

    private TransactionDTO mapTransaction(final AionTransaction transaction) {
        if (transaction == null) {
            return null;
        }
        final SendData sendData = getSendData(transaction.getFrom().toString(), transaction.getTo().toString(), new BigInteger(transaction.getValue()), transaction.getData());
        return new TransactionDTO(
                sendData.getFrom(),
                sendData.getTo(),
                sendData.getValue(),
                sendData.getCoin(),
                ByteUtil.toHexString(transaction.getHash()),
                transaction.getNrg(),
                transaction.getNrgPrice(),
                transaction.getTimeStampBI().longValue(),
                transaction.getBlockNumber(),
                transaction.getNonceBI(),
                (int) transaction.getTxIndexInBlock()
        );
    }
}
