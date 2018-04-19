package org.aion.wallet.connector.api;

import com.google.common.eventbus.Subscribe;
import org.aion.api.IAionAPI;
import org.aion.api.impl.AionAPIImpl;
import org.aion.api.type.*;
import org.aion.base.type.Address;
import org.aion.base.type.Hash256;
import org.aion.base.util.ByteArrayWrapper;
import org.aion.base.util.TypeConverter;
import org.aion.crypto.ECKey;
import org.aion.crypto.ECKeyFac;
import org.aion.mcf.account.KeystoreFormat;
import org.aion.mcf.account.KeystoreItem;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.dto.SendRequestDTO;
import org.aion.wallet.connector.dto.SyncInfoDTO;
import org.aion.wallet.connector.dto.TransactionDTO;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.dto.ExtendedAccountDTO;
import org.aion.wallet.exception.NotFoundException;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.EventPublisher;
import org.aion.wallet.util.AionConstants;
import org.aion.wallet.util.BalanceUtils;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class ApiBlockchainConnector extends BlockchainConnector {

    private final static IAionAPI API = AionAPIImpl.inst();
    private final Map<String, ExtendedAccountDTO> addressToAccount = new HashMap<>();

    public ApiBlockchainConnector() {
        if (API.isConnected()) {
            return;
        }
        API.connect(IAionAPI.LOCALHOST_URL, true);
        EventBusFactory.getBus(EventPublisher.ACCOUNT_CHANGE_EVENT_ID).register(this);
    }

    @Override
    public void createAccount(final String password, final String name) {
        final ApiMsg response = API.getAccount().accountCreate(Collections.singletonList(password), true);
        final Key createdKey = ((List<Key>) response.getObject()).get(0);
        final String address = createdKey.getPubKey().toString();
        final ExtendedAccountDTO account = createExtendedAccountDTO(address, createdKey.getPriKey().toBytes());
        account.setName(name);
        storeAccountName(address, name);
    }

    public AccountDTO getAccount(final String publicAddress) {
        final String name = getStoredAccountName(publicAddress);
        final String balance = BalanceUtils.formatBalance(getBalance(publicAddress));
        return new AccountDTO(name, publicAddress, balance, getCurrency());
    }

    @Override
    protected String sendTransactionInternal(SendRequestDTO dto) {
        TxArgs txArgs = new TxArgs.TxArgsBuilder()
                .from(new Address(TypeConverter.toJsonHex(dto.getFrom())))
                .to(new Address(TypeConverter.toJsonHex(dto.getTo())))
                .value(dto.getValue())
                .nonce(getLatestTransactionNonce(dto.getFrom()))
                .data(new ByteArrayWrapper(dto.getData()))
                .nrgPrice(dto.getNrgPrice())
                .nrgLimit(dto.getNrg()).createTxArgs();
        final MsgRsp response = API.getTx().sendSignedTransaction(
                txArgs,
                new ByteArrayWrapper(addressToAccount.get(dto.getFrom()).getPrivateKey()),
                dto.getPassword()
        ).getObject();

        return String.valueOf(response.getTxHash());
    }

    @Override
    public List<AccountDTO> getAccounts() {
        for (Map.Entry<String, ExtendedAccountDTO> entry : addressToAccount.entrySet()) {
            ExtendedAccountDTO account = entry.getValue();
            account.setBalance(BalanceUtils.formatBalance(getBalance(account.getPublicAddress())));
            entry.setValue(account);
        }
        return new ArrayList<>(addressToAccount.values());
    }

    @Override
    public TransactionDTO getTransaction(String txHash) throws NotFoundException {
        ApiMsg txReceiptMsg = API.getChain().getTransactionByHash(Hash256.wrap(txHash));
        if (txReceiptMsg == null || txReceiptMsg.getObject() == null) {
            throw new NotFoundException();
        }
        Transaction receipt = txReceiptMsg.getObject();
        return mapTransaction(receipt);
    }

    @Override
    public List<TransactionDTO> getLatestTransactions(String address) {
        return getTransactions(address, AionConstants.MAX_BLOCKS_FOR_LATEST_TRANSACTIONS_QUERY);
    }

    @Override
    public boolean getConnectionStatusByConnectedPeers() {
        return API.isConnected();
    }

    @Override
    public SyncInfoDTO getSyncInfo() {
        SyncInfoDTO syncInfoDTO = new SyncInfoDTO();
        syncInfoDTO.setChainBestBlkNumber(API.getChain().blockNumber().getObject());
        syncInfoDTO.setNetworkBestBlkNumber(((SyncInfo) API.getNet().syncInfo().getObject()).getNetworkBestBlock());
        return syncInfoDTO;
    }

    @Override
    public BigInteger getBalance(String address) {
        return API.getChain().getBalance(new Address(address)).getObject();
    }

    @Override
    public int getPeerCount() {
        return ((List) API.getNet().getActiveNodes().getObject()).size();
    }

    @Override
    public String getCurrency() {
        return AionConstants.CCY;
    }

    @Override
    public AccountDTO addKeystoreUTCFile(byte[] file, String password) throws ValidationException {
        try {
            ECKey key = KeystoreFormat.fromKeystore(file, password);
            KeystoreItem keystoreItem = KeystoreItem.parse(file);
            final String address = keystoreItem.getAddress();
            return createExtendedAccountDTO(address, key.getPrivKeyBytes());
        } catch (Exception e) {
            throw new ValidationException("Unsupported key type");
        }
    }

    private ExtendedAccountDTO createExtendedAccountDTO(final String address, final byte[] privKeyBytes) {
        final String name = getStoredAccountName(address);
        final String balance = BalanceUtils.formatBalance(getBalance(address));
        ExtendedAccountDTO account = new ExtendedAccountDTO(name, address, balance, getCurrency());
        account.setPrivateKey(privKeyBytes);
        addressToAccount.put(account.getPublicAddress(), account);
        return account;
    }

    public AccountDTO addPrivateKey(byte[] raw, String password) throws ValidationException {
        try {
            // todo: get correct public key from ECKey
            ECKey key = ECKeyFac.inst().fromPrivate(raw);
            throw new UnsupportedOperationException("ApiBlockhainConnector.addPrivateKey");
        } catch (Exception e) {
            throw new ValidationException("Unsupported key type");
        }
    }

    @Override
    public void close() {
        super.close();
        API.destroyApi();
    }

    @Subscribe
    private void handleAccountChanged(final AccountDTO account) {
        if (!account.getName().equalsIgnoreCase(getStoredAccountName(account.getPublicAddress()))) {
            storeAccountName(account.getPublicAddress(), account.getName());
            final String name = getStoredAccountName(account.getPublicAddress());
            System.out.println(name);
        }
    }

    private BigInteger getLatestTransactionNonce(String addr) {
        Long latest = API.getChain().blockNumber().getObject();
        final String parsedAddr = TypeConverter.toJsonHex(addr);
        for (long i = latest; i > 0; i--) {
            BlockDetails blk = getBlockDetailsByNumber(i);
            if (blk == null || blk.getTxDetails().size() == 0) {
                continue;
            }
            BigInteger nonce = null;
            for (TxDetails t : blk.getTxDetails()) {
                if (!TypeConverter.toJsonHex(t.getFrom().toString()).equals(parsedAddr)) {
                    continue;
                }
                if (nonce == null || nonce.compareTo(t.getNonce()) < 0) {
                    nonce = t.getNonce();
                }
            }
            if (nonce != null) {
                return nonce.add(BigInteger.ONE);
            }
        }
        return BigInteger.ZERO;
    }


    private List<TransactionDTO> getTransactions(final String addr, long nrOfBlocksToCheck) {
        Long latest = API.getChain().blockNumber().getObject();
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
        return ((List<BlockDetails>) API.getAdmin().getBlockDetailsByNumber(number.toString()).getObject()).get(0);
    }

    private TransactionDTO mapTransaction(Transaction transaction) {
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
