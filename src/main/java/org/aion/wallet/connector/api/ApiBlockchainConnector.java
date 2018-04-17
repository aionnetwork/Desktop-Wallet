package org.aion.wallet.connector.api;

import com.google.common.eventbus.Subscribe;
import org.aion.api.IAionAPI;
import org.aion.api.impl.AionAPIImpl;
import org.aion.api.type.*;
import org.aion.base.type.Address;
import org.aion.base.util.ByteArrayWrapper;
import org.aion.base.util.TypeConverter;
import org.aion.crypto.ECKey;
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
import org.aion.wallet.storage.WalletStorage;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.EventPublisher;
import org.aion.wallet.util.AionConstants;
import org.aion.wallet.util.BalanceFormatter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ApiBlockchainConnector extends BlockchainConnector {

    private final static IAionAPI API = AionAPIImpl.inst();
    private final WalletStorage walletStorage = WalletStorage.getInstance();
    private Map<String, ExtendedAccountDTO> accounts = new HashMap<>();

    public ApiBlockchainConnector() {
        if (API.isConnected()) {
            return;
        }
        API.connect(IAionAPI.LOCALHOST_URL, true);
        EventBusFactory.getBus(EventPublisher.ACCOUNT_CHANGE_EVENT_ID).register(this);
    }

    public AccountDTO getAccount(final String publicAddress) {
        final String name = walletStorage.getAccountName(publicAddress);
        final String balance = BalanceFormatter.formatBalance(getBalance(publicAddress));
        return new AccountDTO(name, publicAddress, balance, getCurrency());
    }

    @Override
    public String sendTransaction(SendRequestDTO dto) throws ValidationException {
        if (dto == null || !dto.isValid()) {
            throw new ValidationException("Invalid transaction request data");
        }
        TxArgs txArgs = new TxArgs.TxArgsBuilder()
                .from(new Address(TypeConverter.toJsonHex(dto.getFrom())))
                .to(new Address(TypeConverter.toJsonHex(dto.getTo())))
                .value(dto.getValue())
                .nonce(dto.getNonce())
                .data(new ByteArrayWrapper(dto.getData()))
                .nrgPrice(dto.getNrgPrice())
                .nrgLimit(dto.getNrg()).createTxArgs();
        final MsgRsp response = API.getTx().sendSignedTransaction(
                txArgs,
                new ByteArrayWrapper(accounts.get(dto.getFrom()).getPrivateKey()),
                dto.getPassword()
        ).getObject();

        return String.valueOf(response.getTxHash());
    }

    @Override
    public List<AccountDTO> getAccounts() {
        return new ArrayList<>(accounts.values());
    }

    private AccountDTO convertToAccountDto(Address address) {
        final String publicAddress = address.toString();
        return getAccount(publicAddress);
    }

    @Override
    public TransactionDTO getTransaction(String txHash) throws NotFoundException {
        throw new NotFoundException();
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
            final String name = walletStorage.getAccountName(address);
            final String balance = BalanceFormatter.formatBalance(getBalance(address));
            ExtendedAccountDTO account = new ExtendedAccountDTO(name, address, balance, getCurrency());
            account.setPrivateKey(key.getPrivKeyBytes());
            accounts.put(account.getPublicAddress(), account);
            return account;
        } catch (Exception e) {
            throw new ValidationException("Unsupported key type");
        }
    }

    private byte[] getPrivateKeyFromUTCKeystoreFile(byte[] key, String password) {
        return KeystoreFormat.fromKeystore(key, password).getPrivKeyBytes();
    }

    @Override
    public void close() {
        walletStorage.save();
        API.destroyApi();
    }

    @Subscribe
    private void handleAccountChanged(final AccountDTO account) {
        if (!account.getName().equalsIgnoreCase(walletStorage.getAccountName(account.getPublicAddress()))) {
            walletStorage.setAccountName(account.getPublicAddress(), account.getName());
            final String name = walletStorage.getAccountName(account.getPublicAddress());
            System.out.println(name);
        }
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
