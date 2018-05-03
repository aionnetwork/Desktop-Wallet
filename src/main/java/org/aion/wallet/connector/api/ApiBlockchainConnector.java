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
import org.aion.wallet.dto.ExtendedAccountDTO;
import org.aion.wallet.exception.NotFoundException;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.EventPublisher;
import org.aion.wallet.util.AionConstants;
import org.aion.wallet.util.BalanceUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ApiBlockchainConnector extends BlockchainConnector {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private final static IAionAPI API = AionAPIImpl.inst();

    private static final String USER_DIR = "user.dir";

    private static final Path KEYSTORE_PATH = Paths.get(System.getProperty(USER_DIR) + File.separator + "keystore");

    private final Map<String, AccountDTO> addressToAccount = new HashMap<>();

    public ApiBlockchainConnector() {
        if (API.isConnected()) {
            return;
        }
        API.connect(IAionAPI.LOCALHOST_URL, true);
        EventBusFactory.getBus(EventPublisher.ACCOUNT_CHANGE_EVENT_ID).register(this);
    }

    @Override
    public void createAccount(final String password, final String name) {
        final String address = Keystore.create(password);
        final ECKey ecKey = Keystore.getKey(address, password);
        final ExtendedAccountDTO account = createExtendedAccountDTO(address, ecKey.getPrivKeyBytes());
        account.setName(name);
        storeAccountName(address, name);
    }

    @Override
    public AccountDTO addKeystoreUTCFile(byte[] file, String password, final boolean shouldKeep) throws ValidationException {
        try {
            ECKey key = KeystoreFormat.fromKeystore(file, password);
            KeystoreItem keystoreItem = KeystoreItem.parse(file);
            String address = keystoreItem.getAddress();
            if (!Keystore.exist(address) && shouldKeep) {
                address = Keystore.create(password, key);
                return createExtendedAccountDTO(address, key.getPrivKeyBytes());
            } else {
                return addressToAccount.getOrDefault(address, createExtendedAccountDTO(address, key.getPrivKeyBytes()));
            }
        } catch (final Exception e) {
            throw new ValidationException("Could not open Keystore File", e);
        }
    }

    @Override
    public AccountDTO addPrivateKey(byte[] raw, String password, final boolean shouldKeep) throws ValidationException {
        try {
            ECKey key = ECKeyFac.inst().fromPrivate(raw);
            String address = Keystore.create(password, key);
            if (!address.equals("0x")) {
                if (!shouldKeep) {
                    removeKeystoreFile(address);
                }
                log.info("The private key was imported, the address is: " + address);
                return createExtendedAccountDTO(address, raw);
            } else {
                log.info("Failed to import the private key. Already exists?");
                return null;
            }
        } catch (Exception e) {
            throw new ValidationException("Unsupported key type", e);
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

    private void removeKeystoreFile(String address) {
        if (Keystore.exist(address)) {
            final String unwrappedAddress = address.substring(2);
            Arrays.stream(Keystore.list())
                    .filter(s -> s.contains(unwrappedAddress))
                    .forEach(account -> removeAssociatedKeyStoreFile(unwrappedAddress));
        }
    }

    private void removeAssociatedKeyStoreFile(final String unwrappedAddress) {
        try {
            for (Path keystoreFile : Files.newDirectoryStream(KEYSTORE_PATH)) {
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
    public BigInteger getBalance(String address) {
        return API.getChain().getBalance(new Address(address)).getObject();
    }

    @Override
    protected String sendTransactionInternal(SendRequestDTO dto) {
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
        final MsgRsp response = API.getTx().sendSignedTransaction(
                txArgs,
                new ByteArrayWrapper(((ExtendedAccountDTO) addressToAccount.get(dto.getFrom())).getPrivateKey()),
                dto.getPassword()
        ).getObject();

        return String.valueOf(response.getTxHash());
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
        long chainBest;
        long netBest;
        SyncInfo syncInfo;
        try {
            syncInfo = API.getNet().syncInfo().getObject();
            chainBest = syncInfo.getChainBestBlock();
            netBest = syncInfo.getNetworkBestBlock();
        } catch (Exception e) {
            chainBest = API.getChain().blockNumber().getObject();
            netBest = chainBest;
        }
        SyncInfoDTO syncInfoDTO = new SyncInfoDTO();
        syncInfoDTO.setChainBestBlkNumber(chainBest);
        syncInfoDTO.setNetworkBestBlkNumber(netBest);
        return syncInfoDTO;
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
    public void close() {
        super.close();
        API.destroyApi();
    }

    @Subscribe
    private void handleAccountChanged(final AccountDTO account) {
        if (!account.getName().equalsIgnoreCase(getStoredAccountName(account.getPublicAddress()))) {
            storeAccountName(account.getPublicAddress(), account.getName());
        }
    }

    private BigInteger getLatestTransactionNonce(String addr) {
        Long latest = API.getChain().blockNumber().getObject();
        final String parsedAddr = TypeConverter.toJsonHex(addr);
        for (long i = latest; i > 0; i--) {
            BlockDetails blk = null;
            try {
                blk = getBlockDetailsByNumber(i);
            } catch (Exception e) {
                log.warn("Exception occurred while searching for the latest account transaction");
            }
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
