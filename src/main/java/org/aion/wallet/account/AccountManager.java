package org.aion.wallet.account;

import io.github.novacrypto.bip39.MnemonicGenerator;
import io.github.novacrypto.bip39.Words;
import io.github.novacrypto.bip39.wordlists.English;
import org.aion.api.log.LogEnum;
import org.aion.base.util.TypeConverter;
import org.aion.crypto.ECKey;
import org.aion.crypto.ECKeyFac;
import org.aion.mcf.account.KeystoreFormat;
import org.aion.mcf.account.KeystoreItem;
import org.aion.wallet.connector.dto.BlockDTO;
import org.aion.wallet.connector.dto.SendTransactionDTO;
import org.aion.wallet.connector.dto.TransactionDTO;
import org.aion.wallet.console.ConsoleManager;
import org.aion.wallet.crypto.MasterKey;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.dto.AccountType;
import org.aion.wallet.events.EventPublisher;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.hardware.AionAccountDetails;
import org.aion.wallet.hardware.HardwareWallet;
import org.aion.wallet.hardware.HardwareWalletException;
import org.aion.wallet.hardware.HardwareWalletFactory;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.storage.LocalKeystore;
import org.aion.wallet.storage.WalletStorage;
import org.aion.wallet.util.AddressUtils;
import org.aion.wallet.util.CryptoUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AccountManager {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private final WalletStorage walletStorage = WalletStorage.getInstance();

    private final Map<String, AccountDTO> addressToAccount = new HashMap<>();

    private final Map<String, byte[]> addressToKeystoreContent = Collections.synchronizedMap(new HashMap<>());

    private final KeystoreFormat keystoreFormat = new KeystoreFormat();

    private final Function<String, BigInteger> balanceProvider;

    private final Supplier<String> currencySupplier;

    private MasterKey root;

    private boolean isWalletLocked = false;

    public AccountManager(final Function<String, BigInteger> balanceProvider, final Supplier<String> currencySupplier) {
        this.balanceProvider = balanceProvider;
        this.currencySupplier = currencySupplier;
        for (String address : LocalKeystore.list()) {
            addressToAccount.put(address, getNewImportedAccount(address));
        }
        CryptoUtils.preloadNatives();
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

    private AccountDTO processMasterAccount(final String mnemonic, final String password) throws ValidationException {
        final ECKey rootEcKey = CryptoUtils.getBip39ECKey(mnemonic);

        root = new MasterKey(rootEcKey.getPrivKeyBytes());
        walletStorage.setMasterAccountMnemonic(mnemonic, password);
        final AccountDTO accountDTO = addInternalAccount();
        EventPublisher.fireAccountAdded(accountDTO);
        return accountDTO;
    }

    public void unlockMasterAccount(final String password) throws ValidationException {
        if (!walletStorage.hasMasterAccount()) {
            return;
        }
        isWalletLocked = false;

        final ECKey rootEcKey = CryptoUtils.getBip39ECKey(walletStorage.getMasterAccountMnemonic(password));
        root = new MasterKey(rootEcKey.getPrivKeyBytes());

        final int accountDerivations = walletStorage.getMasterAccountDerivations();
        Set<String> recoveredAddresses = new LinkedHashSet<>(accountDerivations);
        for (int i = 0; i < accountDerivations; i++) {
            final String address = unlockInternalAccount(i);
            if (address != null) {
                recoveredAddresses.add(address);
            }
        }
        EventPublisher.fireAccountsRecovered(recoveredAddresses);
        final String firstDerivationAddress = recoveredAddresses.iterator().next();
        EventPublisher.fireAccountChanged(getAccount(firstDerivationAddress));
    }

    public boolean isMasterAccountUnlocked() {
        return root != null;
    }

    public void createAccount() throws ValidationException {
        EventPublisher.fireAccountAdded(addInternalAccount());
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

    public AccountDTO importHardwareWallet(final AccountType accountType, final int derivationIndex, final String address) throws ValidationException {
        AccountDTO account;
        if (!addressToAccount.keySet().contains(address)) {
            account = createHardwareWalletAccount(accountType, derivationIndex, address);
            if (account == null) return null;
        } else {
            throw new ValidationException("Account already exists!");
        }
        EventPublisher.fireAccountAdded(account);
        return account;
    }

    private String unlockInternalAccount(final int derivationIndex) throws ValidationException {
        if (root == null) {
            return null;
        }
        final ECKey derivedKey = getEcKeyFromRoot(derivationIndex);
        final String address = TypeConverter.toJsonHex(derivedKey.computeAddress(derivedKey.getPubKey()));
        AccountDTO recoveredAccount = addressToAccount.get(address);
        if (recoveredAccount != null) {
            recoveredAccount.setPrivateKey(derivedKey.getPrivKeyBytes());
        } else {
            recoveredAccount = createAccountWithPrivateKey(address, derivedKey.getPrivKeyBytes(), AccountType.LOCAL, derivationIndex);
        }
        return recoveredAccount == null ? null : address;
    }

    private AccountDTO addInternalAccount(final int derivationIndex) throws ValidationException {
        if (root == null) {
            return null;
        }
        final ECKey derivedKey = getEcKeyFromRoot(derivationIndex);
        final String address = TypeConverter.toJsonHex(derivedKey.computeAddress(derivedKey.getPubKey()));
        return createAccountWithPrivateKey(address, derivedKey.getPrivKeyBytes(), AccountType.LOCAL, derivationIndex);
    }

    private ECKey getEcKeyFromRoot(final int derivationIndex) throws ValidationException {
        return root.deriveHardened(new int[]{44, 425, 0, 0, derivationIndex});
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
            if (!LocalKeystore.exist(address)) {
                address = LocalKeystore.create(password, key);
                if (AddressUtils.isValid(address)) {
                    accountDTO = createExternalAccountFromPrivateKey(address, key.getPrivKeyBytes());
                } else {
                    throw new ValidationException("Failed to save keystore file");
                }
            } else {
                throw new ValidationException("Account already exists!");
            }
        } else {
            if (!addressToAccount.keySet().contains(address)) {
                accountDTO = createExternalAccountFromPrivateKey(address, key.getPrivKeyBytes());
            } else {
                throw new ValidationException("Account already exists!");
            }
        }
        if (accountDTO == null) {
            throw new ValidationException("Failed to create account");
        }
        processExternalAccountAdded(accountDTO, fileContent);
        return accountDTO;
    }

    public void exportAccount(final AccountDTO account, final String password, final String destinationDir) throws ValidationException {
        final ECKey ecKey = CryptoUtils.getECKey(account.getPrivateKey());
        final boolean remembered = account.isImported() && LocalKeystore.exist(account.getPublicAddress());
        if (!remembered) {
            LocalKeystore.create(password, ecKey);
        }
        if (Files.isDirectory(WalletStorage.KEYSTORE_PATH)) {
            final String fileNameRegex = getExportedFileNameRegex(account.getPublicAddress());
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(WalletStorage.KEYSTORE_PATH, fileNameRegex)) {
                for (Path keystoreFile : stream) {
                    final String fileName = keystoreFile.getFileName().toString();
                    if (remembered) {
                        Files.copy(keystoreFile, Paths.get(destinationDir + File.separator + fileName), StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        try {
                            Files.move(keystoreFile, Paths.get(destinationDir + File.separator + fileName), StandardCopyOption.ATOMIC_MOVE);
                        } finally {
                            if (Files.exists(keystoreFile)) {
                                Files.delete(keystoreFile);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new ValidationException(e);
            }
        } else {
            log.error("Could not find Keystore directory: " + WalletStorage.KEYSTORE_PATH);
        }

    }

    private String getExportedFileNameRegex(final String publicAddress) {
        return "UTC--*--" + publicAddress.substring(2);
    }

    public Set<TransactionDTO> getTransactions(final String address) {
        return addressToAccount.get(address).getTransactionsSnapshot();
    }

    public void removeTransactions(final String address, final Collection<TransactionDTO> transactions) {
        addressToAccount.get(address).removeTransactions(transactions);
    }

    public void addTransactions(final String address, final Collection<TransactionDTO> transactions) {
        addressToAccount.get(address).addTransactions(transactions);
    }

    public BlockDTO getLastSafeBlock(final String address) {
        final AccountDTO accountDTO = addressToAccount.get(address);
        return accountDTO != null ? accountDTO.getLastSafeBlock() : null;
    }

    public void updateLastSafeBlock(final String address, final BlockDTO lastCheckedBlock) {
        addressToAccount.get(address).setLastSafeBlock(lastCheckedBlock);
    }

    public List<AccountDTO> getAccounts() {
        final Collection<AccountDTO> filteredAccounts = addressToAccount.values().stream().filter(account -> account.isImported() || account.isUnlocked()).collect(Collectors.toList());
        for (AccountDTO account : filteredAccounts) {
            account.setBalance(balanceProvider.apply(account.getPublicAddress()));
        }
        List<AccountDTO> accounts = new ArrayList<>(filteredAccounts);
        accounts.sort((AccountDTO o1, AccountDTO o2) -> {
            final int order = o1.getType().getOrder() - o2.getType().getOrder();
            return order == 0 ? o1.getDerivationIndex() - o2.getDerivationIndex() : order;
        });
        return accounts;
    }

    public Set<String> getAddresses() {
        return new HashSet<>(addressToAccount.keySet());
    }

    public AccountDTO getAccount(final String address) {
        return Optional.ofNullable(addressToAccount.get(address)).orElse(getNewImportedAccount(address));
    }

    public void updateAccount(final AccountDTO account) {
        storeAccountName(account.getPublicAddress(), account.getName());
    }

    public void unlockAccount(final AccountDTO account, final String password) throws ValidationException {
        isWalletLocked = false;
        if (EnumSet.of(AccountType.LOCAL, AccountType.EXTERNAL).contains(account.getType())) {
            final Optional<byte[]> fileContent = Optional.ofNullable(addressToKeystoreContent.get(account.getPublicAddress()));
            final ECKey storedKey;
            if (fileContent.isPresent()) {
                storedKey = KeystoreFormat.fromKeystore(fileContent.get(), password);
            } else {
                storedKey = LocalKeystore.getKey(account.getPublicAddress(), password);
            }

            if (storedKey != null) {
                account.setPrivateKey(storedKey.getPrivKeyBytes());
            } else {
                throw new ValidationException("The password is incorrect!");
            }
        } else {
            HardwareWallet hardwareWallet = HardwareWalletFactory.getHardwareWallet(account.getType());
            final AionAccountDetails accountDetails;
            final String accountType = account.getType().getDisplayString();
            try {
                accountDetails = hardwareWallet.getAccountDetails(account.getDerivationIndex());
            } catch (HardwareWalletException e) {
                throw new ValidationException("Can't unlock! " + accountType + " device disconnected.");
            }
            if (!accountDetails.getAddress().equals(account.getPublicAddress())) {
                throw new ValidationException("Wrong " + accountType + " device connected. Could not find account!");
            }
        }
        account.setActive(true);
        EventPublisher.fireAccountChanged(account);

    }

    public List<SendTransactionDTO> getTimedOutTransactions(final String accountAddress) {
        final AccountDTO accountDTO = addressToAccount.get(accountAddress);
        return accountDTO == null ? Collections.emptyList() : accountDTO.getTimedOutTransactions();
    }

    public void addTimedOutTransaction(final SendTransactionDTO transaction) {
        addressToAccount.get(transaction.getFrom().getPublicAddress()).addTimedOutTransaction(transaction);
    }

    public void removeTimedOutTransaction(final SendTransactionDTO transaction) {
        addressToAccount.get(transaction.getFrom().getPublicAddress()).removeTimedOutTransaction(transaction);
    }

    public void lockAll() {
        if (isWalletLocked) {
            return;
        }
        isWalletLocked = true;
        ConsoleManager.addLog("Wallet has been locked due to inactivity", ConsoleManager.LogType.ACCOUNT);
        root = null;
        for (AccountDTO account : addressToAccount.values()) {
            account.setPrivateKey(null);
            account.setActive(false);
            EventPublisher.fireAccountLocked(account);
        }
    }

    private AccountDTO createExternalAccountFromPrivateKey(final String address, final byte[] privateKeyBytes) {
        return createAccountWithPrivateKey(address, privateKeyBytes, AccountType.EXTERNAL, -1);
    }

    private AccountDTO createAccountWithPrivateKey(final String address, final byte[] privateKeyBytes, AccountType accountType, int derivation) {
        if (address == null) {
            log.error("Can't create account with null address");
            return null;
        }
        if (privateKeyBytes == null || privateKeyBytes.length == 0) {
            log.error("Can't create account without private key");
            return null;
        }
        AccountDTO account = getNewAccount(address, accountType, derivation);
        account.setPrivateKey(privateKeyBytes);
        account.setActive(true);
        addressToAccount.put(account.getPublicAddress(), account);
        return account;
    }

    private AccountDTO createHardwareWalletAccount(final AccountType accountType, final int derivationIndex, final String address) {
        final AccountDTO account;
        if (address == null) {
            log.error("Can't create account with null address");
            return null;
        }
        account = getNewAccount(address, accountType, derivationIndex);
        account.setActive(true);
        addressToAccount.put(account.getPublicAddress(), account);
        return account;
    }

    private void processExternalAccountAdded(final AccountDTO account, final byte[] keystoreContent) {
        if (account == null || keystoreContent == null) {
            throw new IllegalArgumentException(String.format("account %s ; keystoreContent: %s", account, Arrays.toString(keystoreContent)));
        }
        final String address = account.getPublicAddress();
        addressToKeystoreContent.put(address, keystoreContent);
        EventPublisher.fireAccountAdded(account);
    }

    private String getStoredAccountName(final String publicAddress) {
        return walletStorage.getAccountName(publicAddress);
    }

    private AccountDTO getNewAccount(final String publicAddress, AccountType accountType, int derivation) {
        return new AccountDTO(getStoredAccountName(publicAddress),
                publicAddress,
                balanceProvider.apply(publicAddress),
                currencySupplier.get(),
                accountType,
                derivation);
    }

    private AccountDTO getNewImportedAccount(final String publicAddress) {
        return getNewAccount(publicAddress, AccountType.EXTERNAL, -1);
    }

    private void storeAccountName(final String address, final String name) {
        if (name.equalsIgnoreCase(getStoredAccountName(address))) {
            return;
        }
        walletStorage.setAccountName(address, name);
    }
}
