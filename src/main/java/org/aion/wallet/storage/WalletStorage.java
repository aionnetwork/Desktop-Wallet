package org.aion.wallet.storage;

import org.aion.api.log.LogEnum;
import org.aion.wallet.dto.LightAppSettings;
import org.aion.wallet.log.WalletLoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;

public class WalletStorage {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private static final String USER_DIR = "user.dir";

    public static final Path KEYSTORE_PATH = Paths.get(System.getProperty(USER_DIR) + File.separator + "keystore");

    private static final String BLANK = "";

    private static final String HOME_DIR = System.getProperty("user.home");

    private static final String STORAGE_DIR = HOME_DIR + File.separator + ".aion";

    private static final String ACCOUNTS_FILE = STORAGE_DIR + File.separator + "accounts.properties";

    private static final String WALLET_FILE = STORAGE_DIR + File.separator + "wallet.properties";

    private static final String ACCOUNT_NAME_PROP = ".name";

    private static final String MASTER_ACCOUNT_PROP = "accounts.master.address";

    private static final String MASTER_DERIVATIONS_PROP = "accounts.master.derivations";

    private static final WalletStorage INST;

    static {
        try {
            INST = new WalletStorage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final Properties accountsProperties;

    private final Properties lightAppProperties;

    private WalletStorage() throws IOException {
        final Path dir = Paths.get(STORAGE_DIR);
        ensureExistence(dir, true);
        accountsProperties = getPropertiesFomFIle(ACCOUNTS_FILE);
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
                Files.createDirectory(path);
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
        try (final OutputStream writer = Files.newOutputStream(Paths.get(ACCOUNTS_FILE))) {
            accountsProperties.store(writer, LocalDateTime.now().toString());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void saveSettings() {
        try (final OutputStream writer = Files.newOutputStream(Paths.get(WALLET_FILE))) {
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

    public String getMasterAccount() {
        return Optional.ofNullable(accountsProperties.getProperty(MASTER_ACCOUNT_PROP)).orElse(BLANK);
    }

    public void setMasterAccount(final String address) {
        if (address != null) {
            accountsProperties.setProperty(MASTER_ACCOUNT_PROP, address);
            saveSettings();
        }
    }

    public boolean hasMasterAccount() {
        return !getMasterAccount().equalsIgnoreCase(BLANK);
    }

    public int getMasterAccountDerivations() {
        return Optional.ofNullable(accountsProperties.getProperty(MASTER_DERIVATIONS_PROP)).map(Integer::parseInt).orElse(0);
    }

    public void incrementMasterAccountDerivations() {
        accountsProperties.setProperty(MASTER_DERIVATIONS_PROP, getMasterAccountDerivations() + 1 + "");
        saveSettings();
    }

    public final LightAppSettings getLightAppSettings(final ApiType type) {
        return new LightAppSettings(lightAppProperties, type);
    }

    public final void saveLightAppSettings(final LightAppSettings lightAppSettings) {
        if (lightAppSettings != null) {
            lightAppProperties.putAll(lightAppSettings.getSettingsProperties());
            saveSettings();
        }
    }
}
