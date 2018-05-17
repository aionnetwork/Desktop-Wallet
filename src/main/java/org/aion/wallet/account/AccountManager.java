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
import java.security.SecureRandom;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;

public class AccountManager {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private static final String DEFAULT_MNEMONIC_SALT = "";

    private final Comparator<? super TransactionDTO> transactionComparator = new TransactionComparator();

    private final WalletStorage walletStorage = WalletStorage.getInstance();

    private final Map<String, AccountDTO> addressToAccount = new HashMap<>();

    private final Map<String, Future> addressToLockFutures = new HashMap<>();

    private final Map<String, SortedSet<TransactionDTO>> addressToTransactions = Collections.synchronizedMap(new HashMap<>());

    private final Map<String, TxInfo> addressToLastTxInfo = Collections.synchronizedMap(new HashMap<>());

    private final Map<String, byte[]> addressToKeystoreContent = Collections.synchronizedMap(new HashMap<>());

    private final KeystoreFormat keystoreFormat = new KeystoreFormat();

    private Duration unlockTimeOut;

    private Function<String, BigInteger> balanceProvider;

    private Supplier<String> currencySupplier;

    public AccountManager(final LightAppSettings lightAppSettings, final Function<String, BigInteger> balanceProvider, final Supplier<String> currencySupplier) {
        this.unlockTimeOut = lightAppSettings.getUnlockTimeout();
        this.balanceProvider = balanceProvider;
        this.currencySupplier = currencySupplier;
        for (String address : Keystore.list()) {
            addressToAccount.put(address, getAccount(address));
            addressToTransactions.put(address, new TreeSet<>(transactionComparator));
            addressToLastTxInfo.put(address, new TxInfo(0, -1));
        }
        registerEventBusConsumer();
        final Timer unlockTimer = new Timer();
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                
            }
        };
        unlockTimer.schedule(task, unlockTimeOut.get(ChronoUnit.SECONDS));
    }

    private void registerEventBusConsumer() {
        EventBusFactory.getBus(SettingsEvent.ID).register(this);
    }

    @Subscribe
    private void handleSettingsChanged(final SettingsEvent event) {
        if (SettingsEvent.Type.CHANGED.equals(event.getType())) {
            final LightAppSettings settings = event.getSettings();
            if (settings != null) {
                unlockTimeOut = settings.getUnlockTimeout();
            }
        }
    }

    public String  createAccount(final String password, final String name) {
        final StringBuilder mnemonicBuilder = new StringBuilder();
        final byte[] entropy = new byte[Words.TWELVE.byteLength()];
        new SecureRandom().nextBytes(entropy);
        new MnemonicGenerator(English.INSTANCE).createMnemonic(entropy, mnemonicBuilder::append);
        final String mnemonic = mnemonicBuilder.toString();
        final byte[] seed = new SeedCalculator().calculateSeed(mnemonic, DEFAULT_MNEMONIC_SALT);
        final ECKey ecKey = new SeededECKeyEd25519(seed);
        final String address = Keystore.create(password, ecKey);
        if (address.equals("0x")) {
            log.error("An exception occurred while creating the new account");
            return null;
        } else {
            final byte[] fileContent = keystoreFormat.toKeystore(ecKey, password);
            final AccountDTO account = createAccountWithPrivateKey(address, ecKey.getPrivKeyBytes());
            account.setName(name);
            processAccountAdded(account, fileContent, true);
            storeAccountName(address, name);
            return mnemonic;
        }
    }

    private AccountDTO createAccountWithPrivateKey(final String address, final byte[] privateKeyBytes) {
        final String name = getStoredAccountName(address);
        final String balance = BalanceUtils.formatBalance(balanceProvider.apply(address));
        AccountDTO account = new AccountDTO(name, address, balance, currencySupplier.get());
        account.setPrivateKey(privateKeyBytes);
        addressToAccount.put(account.getPublicAddress(), account);
        return account;
    }

    private void processAccountAdded(final AccountDTO account, final byte[] keystoreContent, final boolean isCreated) {
        final String address = account.getPublicAddress();
        addressToLastTxInfo.put(address, new TxInfo(isCreated ? -1 : 0, -1));
        addressToTransactions.put(address, new TreeSet<>(transactionComparator));
        addressToKeystoreContent.put(address, keystoreContent);
        EventPublisher.fireAccountAdded(account);
    }

    public Set<String> getAddresses() {
        return addressToAccount.keySet();
    }

    public AccountDTO getAccount(final String publicAddress) {
        final String name = getStoredAccountName(publicAddress);
        final String balance = BalanceUtils.formatBalance(balanceProvider.apply(publicAddress));
        return new AccountDTO(name, publicAddress, balance, currencySupplier.get());
    }

    public final String getStoredAccountName(final String publicAddress) {
        return walletStorage.getAccountName(publicAddress);
    }

    public final void storeAccountName(final String address, final String name) {
        walletStorage.setAccountName(address, name);
    }

    public List<AccountDTO> getAccounts() {
        for (Map.Entry<String, AccountDTO> entry : addressToAccount.entrySet()) {
            AccountDTO account = entry.getValue();
            account.setBalance(BalanceUtils.formatBalance(balanceProvider.apply(account.getPublicAddress())));
            entry.setValue(account);
        }
        return new ArrayList<>(addressToAccount.values());
    }

    public void updateAccount(final AccountDTO account) {
        if (!account.getName().equalsIgnoreCase(getStoredAccountName(account.getPublicAddress()))) {
            storeAccountName(account.getPublicAddress(), account.getName());
        }
    }

    public AccountDTO importKeystore(final byte[] file, final String password, final boolean shouldKeep) throws ValidationException {
        try {
            ECKey key = KeystoreFormat.fromKeystore(file, password);
            if (key == null) {
                throw new ValidationException("Could Not extract ECKey from keystore file");
            }
            return getAccountFromKey(key, file, password, shouldKeep);
        } catch (final Exception e) {
            throw new ValidationException(e);
        }
    }

    public AccountDTO importPrivateKey(final byte[] raw, final String password, final boolean shouldKeep) throws ValidationException {
        try {
            ECKey key = ECKeyFac.inst().fromPrivate(raw);
            final byte[] keystoreContent = keystoreFormat.toKeystore(key, password);
            return getAccountFromKey(key, keystoreContent, password, shouldKeep);
        } catch (final Exception e) {
            throw new ValidationException(e);
        }
    }

    public AccountDTO importMnemonic(final String mnemonic, final String password, boolean shouldKeep) throws ValidationException {
        try {
            byte[] seed = new SeedCalculator().calculateSeed(mnemonic, DEFAULT_MNEMONIC_SALT);
            final ECKey key = new SeededECKeyEd25519(seed);
            final byte[] fileContent = keystoreFormat.toKeystore(key, password);
            return getAccountFromKey(key, fileContent, password, shouldKeep);
        } catch (final Exception e) {
            throw new ValidationException(e);
        }
    }

    private AccountDTO getAccountFromKey(final ECKey key, final byte[] fileContent, final String password, final boolean shouldKeep) throws UnsupportedEncodingException, ValidationException {
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

    private class TransactionComparator implements Comparator<TransactionDTO> {
        @Override
        public int compare(final TransactionDTO tx1, final TransactionDTO tx2) {
            return tx1 == null ?
                    (tx2 == null ? 0 : -1) :
                    (tx2 == null ? 1 : Long.compare(tx2.getTimeStamp(), tx1.getTimeStamp()));
        }
    }
}