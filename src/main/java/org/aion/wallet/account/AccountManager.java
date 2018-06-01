package org.aion.wallet.account;

import com.google.common.eventbus.Subscribe;
import io.github.novacrypto.bip39.MnemonicGenerator;
import io.github.novacrypto.bip39.SeedCalculator;
import io.github.novacrypto.bip39.Words;
import io.github.novacrypto.bip39.wordlists.English;
import org.aion.api.log.LogEnum;
import org.aion.base.util.ByteUtil;
import org.aion.base.util.TypeConverter;
import org.aion.crypto.ECKey;
import org.aion.crypto.ECKeyFac;
import org.aion.mcf.account.Keystore;
import org.aion.mcf.account.KeystoreFormat;
import org.aion.mcf.account.KeystoreItem;
import org.aion.wallet.connector.api.TxInfo;
import org.aion.wallet.connector.dto.TransactionDTO;
import org.aion.wallet.crypto.SeededECKeyEd25519;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.dto.LightAppSettings;
import org.aion.wallet.events.EventBusFactory;
import org.aion.wallet.events.EventPublisher;
import org.aion.wallet.events.SettingsEvent;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.storage.WalletStorage;
import org.aion.wallet.util.BalanceUtils;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.Key;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class AccountManager {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private static final String DEFAULT_MNEMONIC_SALT = "";

    private final Comparator<? super TransactionDTO> transactionComparator = new TransactionComparator();

    private final WalletStorage walletStorage = WalletStorage.getInstance();

    private final Map<String, AccountDTO> addressToAccount = new HashMap<>();

    private final Map<String, SortedSet<TransactionDTO>> addressToTransactions = Collections.synchronizedMap(new HashMap<>());

    private final Map<String, TxInfo> addressToLastTxInfo = Collections.synchronizedMap(new HashMap<>());

    private final Map<String, byte[]> addressToKeystoreContent = Collections.synchronizedMap(new HashMap<>());

    private final KeystoreFormat keystoreFormat = new KeystoreFormat();

    private final Timer lockTimer = new Timer(true);

    private final Function<String, BigInteger> balanceProvider;

    private final Supplier<String> currencySupplier;

    private Duration lockTimeOut;

    public AccountManager(final Function<String, BigInteger> balanceProvider, final Supplier<String> currencySupplier) {
        this.balanceProvider = balanceProvider;
        this.currencySupplier = currencySupplier;
        for (String address : Keystore.list()) {
            addressToAccount.put(address, getNewAccount(address));
            addressToTransactions.put(address, new TreeSet<>(transactionComparator));
            addressToLastTxInfo.put(address, new TxInfo(0, -1));
        }
        registerEventBusConsumer();
    }

    private void registerEventBusConsumer() {
        EventBusFactory.getBus(SettingsEvent.ID).register(this);
    }

    public String createAccount(final String password, final String name) throws ValidationException {
        final StringBuilder mnemonicBuilder = new StringBuilder();
        final byte[] entropy = new byte[Words.TWELVE.byteLength()];
        new SecureRandom().nextBytes(entropy);
        new MnemonicGenerator(English.INSTANCE).createMnemonic(entropy, mnemonicBuilder::append);
        final String mnemonic = mnemonicBuilder.toString();
        final byte[] seed = getNewAccountSeed(mnemonic);
        final ECKey ecKey = new SeededECKeyEd25519(seed);
        if(Keystore.exist(ByteUtil.toHexString(ecKey.getAddress()))) {
            throw new ValidationException("Account already exists");
        }

        final String address = Keystore.create(password, ecKey);
        if (address.equals("0x")) {
            log.error("An exception occurred while creating the new account");
            return null;
        } else {
            final byte[] fileContent = keystoreFormat.toKeystore(ecKey, password);
            final AccountDTO account = createAccountWithPrivateKey(address, ecKey.getPrivKeyBytes());
            if (account == null) {
                return null;
            } else {
                account.setName(name);
                processAccountAdded(account, fileContent, true);
                storeAccountName(address, name);
                if(getAccounts().size() > 1 && getAccounts().stream().filter(p -> p.getPublicAddress().equals(account.getPublicAddress())).findAny().isPresent()) {
                    return null;
                }
                return mnemonic;
            }
        }
    }


    private byte[] getNewAccountSeed(String mnemonic) {
        if(getAccounts().size() > 0 && getAccounts().stream().filter(p -> p.isActive()).findAny().isPresent()) {
            AccountDTO activeAccount = getAccounts().stream().filter(p -> p.isActive()).findAny().get();
            if(activeAccount != null) {
                return new SeedCalculator().calculateSeed(activeAccount.getPublicAddress(), DEFAULT_MNEMONIC_SALT);
            }
        }
        return new SeedCalculator().calculateSeed(mnemonic, DEFAULT_MNEMONIC_SALT);
    }

    public AccountDTO importKeystore(final byte[] file, final String password, final boolean shouldKeep) throws ValidationException {
        try {
            ECKey key = KeystoreFormat.fromKeystore(file, password);
            if (key == null) {
                throw new ValidationException("Could Not extract ECKey from keystore file");
            }
            return getUnlockedAccountFromKey(key, file, password, shouldKeep);
        } catch (final Exception e) {
            throw new ValidationException(e);
        }
    }

    public AccountDTO importPrivateKey(final byte[] raw, final String password, final boolean shouldKeep) throws ValidationException {
        try {
            ECKey key = ECKeyFac.inst().fromPrivate(raw);
            final byte[] keystoreContent = keystoreFormat.toKeystore(key, password);
            return getUnlockedAccountFromKey(key, keystoreContent, password, shouldKeep);
        } catch (final Exception e) {
            throw new ValidationException(e);
        }
    }

    public AccountDTO importMnemonic(final String mnemonic, final String password, boolean shouldKeep) throws ValidationException {
        try {
            byte[] seed = new SeedCalculator().calculateSeed(mnemonic, DEFAULT_MNEMONIC_SALT);
            final ECKey key = new SeededECKeyEd25519(seed);
            final byte[] fileContent = keystoreFormat.toKeystore(key, password);
            return getUnlockedAccountFromKey(key, fileContent, password, shouldKeep);
        } catch (final Exception e) {
            throw new ValidationException(e);
        }
    }

    private AccountDTO getUnlockedAccountFromKey(final ECKey key, final byte[] fileContent, final String password, final boolean shouldKeep) throws UnsupportedEncodingException, ValidationException {
        String address = TypeConverter.toJsonHex(KeystoreItem.parse(fileContent).getAddress());
        final AccountDTO accountDTO;
        if (shouldKeep) {
            if (!Keystore.exist(address)) {
                address = Keystore.create(password, key);
                if (!address.equals("0x")) {
                    accountDTO = createAccountWithPrivateKey(address, key.getPrivKeyBytes());
                } else {
                    throw new ValidationException("Failed to save keystore file");
                }
            } else {
                throw new ValidationException("Account already exists!");
            }
        } else {
            if (!addressToAccount.keySet().contains(address)) {
                accountDTO = createAccountWithPrivateKey(address, key.getPrivKeyBytes());
            } else {
                throw new ValidationException("Account already exists!");
            }
        }
        if (accountDTO == null) {
            throw new ValidationException("Failed to create account");
        }
        processAccountAdded(accountDTO, fileContent, false);
        return accountDTO;
    }

    public SortedSet<TransactionDTO> getTransactions(final String address) {
        return addressToTransactions.getOrDefault(address, Collections.emptySortedSet());
    }

    public TxInfo getLastTxInfo(final String address) {
        return addressToLastTxInfo.get(address);
    }

    public void updateTxInfo(final String address, final TxInfo txInfo) {
        addressToLastTxInfo.put(address, txInfo);
    }

    public List<AccountDTO> getAccounts() {
        for (Map.Entry<String, AccountDTO> entry : addressToAccount.entrySet()) {
            AccountDTO account = entry.getValue();
            account.setBalance(BalanceUtils.formatBalance(balanceProvider.apply(account.getPublicAddress())));
            entry.setValue(account);
        }
        return new ArrayList<>(addressToAccount.values());
    }

    public Set<String> getAddresses() {
        return addressToAccount.keySet();
    }

    public AccountDTO getAccount(final String address) {
        return Optional.ofNullable(addressToAccount.get(address)).orElse(getNewAccount(address));
    }

    public void updateAccount(final AccountDTO account) {
        if (!account.getName().equalsIgnoreCase(getStoredAccountName(account.getPublicAddress()))) {
            storeAccountName(account.getPublicAddress(), account.getName());
        }
    }

    public void unlockAccount(final AccountDTO account, final String password) throws ValidationException {
        final Optional<byte[]> fileContent = Optional.ofNullable(addressToKeystoreContent.get(account.getPublicAddress()));
        final ECKey storedKey;
        if (fileContent.isPresent()) {
            storedKey = KeystoreFormat.fromKeystore(fileContent.get(), password);
        } else {
            storedKey = Keystore.getKey(account.getPublicAddress(), password);
        }

        if (storedKey != null) {
            account.setActive(true);
            account.setPrivateKey(storedKey.getPrivKeyBytes());
            scheduleAccountLock(account);
            EventPublisher.fireAccountChanged(account);
        } else {
            throw new ValidationException("The password is incorrect!");
        }

    }

    @Subscribe
    private void handleSettingsChanged(final SettingsEvent event) {
        if (SettingsEvent.Type.CHANGED.equals(event.getType())) {
            final LightAppSettings settings = event.getSettings();
            if (settings != null) {
                lockTimeOut = settings.getUnlockTimeout();
            }
        }
    }

    private AccountDTO createAccountWithPrivateKey(final String address, final byte[] privateKeyBytes) {
        if (address == null) {
            log.error("Can't create account with null address");
            return null;
        }
        if (privateKeyBytes == null || privateKeyBytes.length == 0) {
            log.error("Can't create account without private key");
            return null;
        }
        final String name = getStoredAccountName(address);
        final String balance = BalanceUtils.formatBalance(balanceProvider.apply(address));
        AccountDTO account = new AccountDTO(name, address, balance, currencySupplier.get());
        account.setPrivateKey(privateKeyBytes);
        account.setActive(true);
        addressToAccount.put(account.getPublicAddress(), account);
        return account;
    }

    private void processAccountAdded(final AccountDTO account, final byte[] keystoreContent, final boolean isCreated) {
        if (account == null || keystoreContent == null) {
            throw new IllegalArgumentException(String.format("account %s ; keystoreContent: %s", account, Arrays.toString(keystoreContent)));
        }
        final String address = account.getPublicAddress();
        addressToLastTxInfo.put(address, new TxInfo(isCreated ? -1 : 0, -1));
        addressToTransactions.put(address, new TreeSet<>(transactionComparator));
        addressToKeystoreContent.put(address, keystoreContent);
        scheduleAccountLock(account);
        EventPublisher.fireAccountAdded(account);
    }

    private void scheduleAccountLock(final AccountDTO account) {
        lockTimer.schedule(getAccountLockTask(account), lockTimeOut.toMillis());
    }

    private String getStoredAccountName(final String publicAddress) {
        return walletStorage.getAccountName(publicAddress);
    }

    private AccountDTO getNewAccount(final String publicAddress) {
        final String name = getStoredAccountName(publicAddress);
        final String balance = BalanceUtils.formatBalance(balanceProvider.apply(publicAddress));
        return new AccountDTO(name, publicAddress, balance, currencySupplier.get());
    }

    private void storeAccountName(final String address, final String name) {
        walletStorage.setAccountName(address, name);
    }

    private TimerTask getAccountLockTask(final AccountDTO accountDTO) {
        return new TimerTask() {
            @Override
            public void run() {
                accountDTO.setPrivateKey(null);
                accountDTO.setActive(false);
                EventPublisher.fireAccountLocked(accountDTO);
            }
        };
    }

    private class TransactionComparator implements Comparator<TransactionDTO> {
        @Override
        public int compare(final TransactionDTO tx1, final TransactionDTO tx2) {
            return tx1 == null ?
                    (tx2 == null ? 0 : -1) :
                    (tx2 == null ? 1 : Long.compare(tx2.getTimeStamp(), tx1.getTimeStamp()));
        }
    }
}
