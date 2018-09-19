package org.aion.wallet.storage;

import org.aion.api.log.LogEnum;
import org.aion.wallet.dto.ConnectionDetails;
import org.aion.wallet.dto.ConnectionProvider;
import org.aion.wallet.dto.LightAppSettings;
import org.aion.wallet.exception.ValidationException;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.util.CryptoUtils;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;

public class WalletStorage {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    public static final Path KEYSTORE_PATH;

    private static final String BLANK = "";

    private static final String ACCOUNT_NAME_PROP = ".name";

    private static final String MASTER_DERIVATIONS_PROP = "master.derivations";

    private static final String MASTER_MNEMONIC_PROP = "master.mnemonic";

    private static final String LEGACY_ENCRYPTION_ALGORITHM = "Blowfish";

    private static final WalletStorage INST;

    private static final String STORAGE_DIR;

    private static final String ACCOUNTS_FILE;

    private static final String CONNECTIONS_FILE;

    private static final String WALLET_FILE;

    static {
        String storageDir = System.getProperty("local.storage.dir");
        if (storageDir == null || storageDir.equalsIgnoreCase("")) {
            storageDir = System.getProperty("user.home") + File.separator + ".aion";
        }
        STORAGE_DIR = storageDir;

        KEYSTORE_PATH = Paths.get(STORAGE_DIR + File.separator + "keystore");

        ACCOUNTS_FILE = STORAGE_DIR + File.separator + "accounts.properties";

        CONNECTIONS_FILE = STORAGE_DIR + File.separator + "connections.properties";

        WALLET_FILE = STORAGE_DIR + File.separator + "wallet.properties";
    }

    static {
        try {
            INST = new WalletStorage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final Properties accountsProperties;

    private final Properties connectionProperties;

    private final Properties lightAppProperties;

    private WalletStorage() throws IOException {
        final Path dir = Paths.get(STORAGE_DIR);
        ensureExistence(dir, true);
        ensureExistence(KEYSTORE_PATH, true);
        accountsProperties = getPropertiesFomFIle(ACCOUNTS_FILE);
        connectionProperties = getPropertiesFomFIle(CONNECTIONS_FILE);
        lightAppProperties = getPropertiesFomFIle(WALLET_FILE);
    }

    public static WalletStorage getInstance() {
        return INST;
    }

    private Properties getPropertiesFomFIle(final String fullPath) throws IOException {
        final Path filePath = Paths.get(fullPath);
        ensureExistence(filePath, false);
        final InputStream reader = Files.newInputStream(filePath);
        Properties properties = new Properties();
        properties.load(reader);
        return properties;
    }

    private void ensureExistence(final Path path, final boolean isDir) throws IOException {
        if (!Files.exists(path)) {
            if (isDir) {
                Files.createDirectories(path);
            } else {
                Files.createFile(path);
            }
        }
    }

    public void save() {
        saveAccounts();
        saveSettings();
    }

    private void saveAccounts() {
        savePropertiesToFile(accountsProperties, ACCOUNTS_FILE);
    }

    private void saveSettings() {
        savePropertiesToFile(lightAppProperties, WALLET_FILE);
        savePropertiesToFile(connectionProperties, CONNECTIONS_FILE);
    }

    private void savePropertiesToFile(Properties lightAppProperties, String connectionsFile) {
        try (final OutputStream writer = Files.newOutputStream(Paths.get(connectionsFile))) {
            lightAppProperties.store(writer, LocalDateTime.now().toString());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public String getAccountName(final String address) {
        return Optional.ofNullable(accountsProperties.get(address + ACCOUNT_NAME_PROP)).map(Object::toString).orElse(BLANK);
    }

    public void setAccountName(final String address, final String accountName) {
        if (address != null && accountName != null) {
            accountsProperties.setProperty(address + ACCOUNT_NAME_PROP, accountName);
            saveAccounts();
        }
    }

    public String getMasterAccountMnemonic(final String password) throws ValidationException {
        if (password == null || password.equalsIgnoreCase("")) {
            throw new ValidationException("Password is not valid");
        }
        String encodedMnemonic = accountsProperties.getProperty(MASTER_MNEMONIC_PROP);
        if (encodedMnemonic == null) {
            throw new ValidationException("No master account present");
        }

        try {
            return decryptMnemonic(encodedMnemonic.getBytes(StandardCharsets.ISO_8859_1), password);
        } catch (Exception e) {
            throw new ValidationException("Cannot decrypt your seed!");
        }
    }

    public void setMasterAccountMnemonic(final String mnemonic, final String password) throws ValidationException {
        try {
            if (mnemonic != null) {
                accountsProperties.setProperty(MASTER_MNEMONIC_PROP, encryptMnemonic(mnemonic, password));
                saveAccounts();
            }
        } catch (Exception e) {
            throw new ValidationException("Cannot encode master account key");
        }
    }

    public boolean hasMasterAccount() {
        String mnemonic = accountsProperties.getProperty(MASTER_MNEMONIC_PROP);
        return mnemonic != null && !mnemonic.equalsIgnoreCase("");
    }

    public int getMasterAccountDerivations() {
        return Optional.ofNullable(accountsProperties.getProperty(MASTER_DERIVATIONS_PROP)).map(Integer::parseInt).orElse(0);
    }

    public void incrementMasterAccountDerivations() throws ValidationException {
        if (hasMasterAccount()) {
            accountsProperties.setProperty(MASTER_DERIVATIONS_PROP, String.valueOf(getMasterAccountDerivations() + 1));
            saveAccounts();
        } else {
            throw new ValidationException("Cannot increment derivation when master account is missing");
        }
    }

    public final LightAppSettings getLightAppSettings(final ApiType type) {
        return new LightAppSettings(lightAppProperties, type, getConnectionProvider());
    }

    public final void saveLightAppSettings(@Nonnull final LightAppSettings lightAppSettings) {
        lightAppProperties.putAll(lightAppSettings.getSettingsProperties());
        saveSettings();
    }

    public final ConnectionProvider getConnectionProvider() {
        return new ConnectionProvider(connectionProperties);
    }

    public final void saveConnectionProperties(@Nonnull final ConnectionProvider connectionProvider) {
        connectionProperties.clear();
        for(ConnectionDetails connectionDetails : connectionProvider.getAllConnections()) {
            connectionProperties.put(connectionDetails.serialized(), connectionDetails.getSecureKey());
        }
        saveSettings();
    }

    private String encryptMnemonic(final String mnemonic, final String password) throws Exception {
        return CryptoUtils.getEncryptedText(mnemonic, password);
    }

    private String decryptMnemonic(final byte[] encryptedMnemonicBytes, final String password) throws ValidationException {
        String result;
        boolean isLegacy = false;
        try {
            result = new String(decryptLegacyMnemonic(encryptedMnemonicBytes, password));
            isLegacy = true;
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException | NoSuchPaddingException e) {
            result = CryptoUtils.decryptMnemonic(encryptedMnemonicBytes, password);
        }
        if (isLegacy) {
            setMasterAccountMnemonic(result, password);
        }
        return result;
    }

    private byte[] decryptLegacyMnemonic(final byte[] encryptedMnemonicBytes, final String password) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
        SecretKeySpec keySpec = new SecretKeySpec(password.getBytes(), LEGACY_ENCRYPTION_ALGORITHM);
        Cipher cipher = Cipher.getInstance(LEGACY_ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        return cipher.doFinal(encryptedMnemonicBytes);
    }
}
