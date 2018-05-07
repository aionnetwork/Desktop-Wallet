package org.aion.wallet.storage;

import org.aion.api.log.LogEnum;
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

    private static final String HOME_DIR = System.getProperty("user.home");

    private static final String STORAGE_DIR = HOME_DIR + File.separator + ".aion";

    private static final String ACCOUNTS_FILE = STORAGE_DIR + File.separator + "accounts.properties";

    private static final String ACCOUNT_NAME_PROP = ".name";

    private static final String ACCOUNT_TX_COUNT = ".tx.count";

    private static final String ACCOUNT_TX_LATEST_BLOCK = ".tx.latest_block";

    private static final WalletStorage INST;

    static {
        try {
            INST = new WalletStorage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final Properties accountProperties;

    private WalletStorage() throws IOException {
        final Path dir = Paths.get(STORAGE_DIR);
        final Path path = Paths.get(ACCOUNTS_FILE);
        if (!Files.exists(dir)) {
            Files.createDirectory(dir);
        }
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
        final InputStream reader = Files.newInputStream(path);
        accountProperties = new Properties();
        accountProperties.load(reader);

    }

    public static WalletStorage getInstance() {
        return INST;
    }

    public void save() {
        try (final OutputStream writer = Files.newOutputStream(Paths.get(ACCOUNTS_FILE))) {
            accountProperties.store(writer, LocalDateTime.now().toString());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public String getAccountName(final String address) {
        return Optional.ofNullable(accountProperties.get(address + ACCOUNT_NAME_PROP)).map(Object::toString).orElse("");
    }

    public void setAccountName(final String address, final String accountName) {
        accountProperties.setProperty(address + ACCOUNT_NAME_PROP, accountName);
    }
}
