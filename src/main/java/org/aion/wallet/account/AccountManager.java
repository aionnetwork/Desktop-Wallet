package org.aion.wallet.account;

import com.google.common.eventbus.Subscribe;
import io.github.novacrypto.bip39.MnemonicGenerator;
import io.github.novacrypto.bip39.SeedCalculator;
import io.github.novacrypto.bip39.Words;
import io.github.novacrypto.bip39.wordlists.English;
import org.aion.api.log.LogEnum;
import org.aion.base.util.TypeConverter;
import org.aion.crypto.ECKey;
import org.aion.crypto.ECKeyFac;
import org.aion.mcf.account.Keystore;
import org.aion.mcf.account.KeystoreFormat;
import org.aion.mcf.account.KeystoreItem;
import org.aion.wallet.connector.api.TxInfo;
import org.aion.wallet.connector.dto.TransactionDTO;
import org.aion.wallet.crypto.ExtendedKey;
import org.aion.wallet.crypto.SeededECKeyEd25519;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.dto.LightAppSettings;
import org.aion.wallet.events.EventBusFactory;
import org.aion.wallet.events.EventPublisher;
import org.aion.wallet.events.SettingsEvent;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.storage.WalletStorage;
import org.aion.wallet.util.AddressUtils;
import org.aion.wallet.util.BalanceUtils;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final Function<String, BigInteger> balanceProvider;

    private final Supplier<String> currencySupplier;

    private int lockTimeOut;

    private String lockTimeOutMeasurementUnit;

    private ExtendedKey root;

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

    public String createMasterAccount(final String password, final String name) throws ValidationException {
        final StringBuilder mnemonicBuilder = new StringBuilder();
        final byte[] entropy = new byte[Words.TWELVE.byteLength()];
        new SecureRandom().nextBytes(entropy);
        new MnemonicGenerator(English.INSTANCE).createMnemonic(entropy, mnemonicBuilder::append);
        final String mnemonic = mnemonicBuilder.toString();

        final AccountDTO account = processMasterAccount(mnemonic, password);
        if (account == null) {
            return null;
        }
        account.setName(name);
        storeAccountName(account.getPublicAddress(), name);
        return mnemonic;
    }

    public void importMasterAccount(final String mnemonic, final String password) throws ValidationException {
        try {
            processMasterAccount(mnemonic, password);
        } catch (final Exception e) {
            throw new ValidationException(e);
        }
    }

    private AccountDTO processMasterAccount(String mnemonic, String password) throws ValidationException {
        final byte[] seed = new SeedCalculator().calculateSeed(mnemonic, DEFAULT_MNEMONIC_SALT);
        final ECKey rootEcKey = new SeededECKeyEd25519(seed);

        root = new ExtendedKey(rootEcKey);
        walletStorage.setMasterAccountMnemonic(mnemonic, password);
        return addInternalAccount();
    }

    public void unlockMasterAccount(String password) throws ValidationException {
        if (!walletStorage.hasMasterAccount()) {
            return;
        }

        final byte[] seed = new SeedCalculator().calculateSeed(walletStorage.getMasterAccountMnemonic(password), DEFAULT_MNEMONIC_SALT);
        ECKey rootEcKey = new SeededECKeyEd25519(seed);
        root = new ExtendedKey(rootEcKey);

        for (int i = 0; i < walletStorage.getMasterAccountDerivations(); i++) {
            addInternalAccount(i);
        }
    }

    public boolean isMasterAccountUnlocked() {
        return root != null;
    }

    public void createAccount() throws ValidationException {
        addInternalAccount();
    }

    public AccountDTO importKeystore(final byte[] file, final String password, final boolean shouldKeep) throws ValidationException {
        try {
            ECKey key = KeystoreFormat.fromKeystore(file, password);
            if (key == null) {
                throw new ValidationException("Could Not extract ECKey from keystore file");
            }
            return addExternalAccount(key, file, password, shouldKeep);
        } catch (final Exception e) {
            throw new ValidationException(e);
        }
    }

    public AccountDTO importPrivateKey(final byte[] raw, final String password, final boolean shouldKeep) throws ValidationException {
        try {
            ECKey key = ECKeyFac.inst().fromPrivate(raw);
            final byte[] keystoreContent = keystoreFormat.toKeystore(key, password);
            return addExternalAccount(key, keystoreContent, password, shouldKeep);
        } catch (final Exception e) {
            throw new ValidationException(e);
        }
    }


    private AccountDTO addInternalAccount(int derivation) throws ValidationException {
        if (root == null) {
            return null;
        }
        final ECKey firstDerivation = root.deriveHardened(new int[]{44, 60, 0, 0, derivation}).getEcKey();
        AccountDTO account = createAccountWithPrivateKey(TypeConverter.toJsonHex(firstDerivation.computeAddress(firstDerivation.getPubKey())), firstDerivation.getPrivKeyBytes(), false, derivation);
        EventPublisher.fireTransactionFinished();
        return account;
    }

    private AccountDTO addInternalAccount() throws ValidationException {
        AccountDTO dto = addInternalAccount(walletStorage.getMasterAccountDerivations());
        walletStorage.incrementMasterAccountDerivations();
        return dto;
    }

    private AccountDTO addExternalAccount(final ECKey key, final byte[] fileContent, final String password, final boolean shouldKeep) throws UnsupportedEncodingException, ValidationException {
        String address = TypeConverter.toJsonHex(KeystoreItem.parse(fileContent).getAddress());
        final AccountDTO accountDTO;
        if (shouldKeep) {
            if (!Keystore.exist(address)) {
                address = Keystore.create(password, key);
                if (AddressUtils.isValid(address)) {
                    accountDTO = createImportedAccountFromPrivateKey(address, key.getPrivKeyBytes());
                } else {
                    throw new ValidationException("Failed to save keystore file");
                }
            } else {
                throw new ValidationException("Account already exists!");
            }
        } else {
            if (!addressToAccount.keySet().contains(address)) {
                accountDTO = createImportedAccountFromPrivateKey(address, key.getPrivKeyBytes());
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
        List<AccountDTO> accounts = new ArrayList<>(addressToAccount.values());
        accounts.sort((AccountDTO o1, AccountDTO o2) -> {
            if (!o1.isImported() && !o2.isImported()) {
                return o1.getDerivationIndex() - o2.getDerivationIndex();
            }
            return o1.isImported() ? 1 : -1;
        });
        return accounts;
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
                lockTimeOutMeasurementUnit = settings.getLockTimeoutMeasurementUnit();
            }
        }
    }

    private AccountDTO createImportedAccountFromPrivateKey(final String address, final byte[] privateKeyBytes) {
        return createAccountWithPrivateKey(address, privateKeyBytes, true, -1);
    }

    private AccountDTO createAccountWithPrivateKey(final String address, final byte[] privateKeyBytes, boolean isImported, int derivation) {
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

        AccountDTO account = new AccountDTO(name, address, balance, currencySupplier.get(), isImported, derivation);
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
        scheduler.schedule(getAccountLockTask(account), computeDelay(lockTimeOut, lockTimeOutMeasurementUnit), TimeUnit.MILLISECONDS);
    }

    private long computeDelay(int lockTimeOut, String lockTimeOutMeasurementUnit) {
        switch (lockTimeOutMeasurementUnit) {
            case "seconds":
                return lockTimeOut * 1000;
            case "minutes":
                return lockTimeOut * 60 * 1000;
            case "hours":
                return lockTimeOut * 3600 * 1000;
        }
        return 0;
    }

    private String getStoredAccountName(final String publicAddress) {
        return walletStorage.getAccountName(publicAddress);
    }

    private AccountDTO getNewAccount(final String publicAddress) {
        final String name = getStoredAccountName(publicAddress);
        final String balance = BalanceUtils.formatBalance(balanceProvider.apply(publicAddress));
        return new AccountDTO(name, publicAddress, balance, currencySupplier.get(), true, -1);
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
                    (tx2 == null ? 1 : tx2.getBlockNumber().compareTo(tx1.getBlockNumber()));
        }
    }
}
