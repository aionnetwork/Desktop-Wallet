package org.aion.wallet.storage;

import org.aion.wallet.dto.LightAppSettings;
import org.aion.wallet.exception.ValidationException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class WalletStorageTest {

    private static final String LOCAL_STORAGE_DIR = "local.storage.dir";

    static {
        System.setProperty(LOCAL_STORAGE_DIR, System.getProperty("user.home") + File.separator + "tmp" + File.separator + ".aion");
    }

    private static final String STORAGE_DIR = System.getProperty(LOCAL_STORAGE_DIR);

    private static final String ACCOUNTS_FILE = STORAGE_DIR + File.separator + "accounts.properties";

    private static final String WALLET_FILE = STORAGE_DIR + File.separator + "wallet.properties";

    private static final String ADDRESS = "some_address";

    private static final String DEFAULT_ADDRESS = "aion-main.bdnodes.net";

    private static final String DEFAULT_PORT = "8547";

    private static final String DEFAULT_PROTOCOL = "tcp";

    private static final Integer DEFAULT_LOCK_TIMEOUT = 1;

    private static final String DEFAULT_LOCK_TIMEOUT_MEASUREMENT_UNIT = "minutes";

    private WalletStorage walletStorage;

    @Before
    public void setUp() {
        walletStorage = WalletStorage.getInstance();
    }

    @BeforeClass
    public static void setUpClass() {
        final Path dir = Paths.get(STORAGE_DIR);
        final Path wallet = Paths.get(WALLET_FILE);
        final Path account = Paths.get(ACCOUNTS_FILE);
        assertFalse(Files.exists(dir));
        assertFalse(Files.exists(wallet));
        assertFalse(Files.exists(account));
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        final Path dir = Paths.get(STORAGE_DIR);
        final Path wallet = Paths.get(WALLET_FILE);
        final Path account = Paths.get(ACCOUNTS_FILE);
        assertTrue(Files.exists(account));
        assertTrue(Files.exists(wallet));
        assertTrue(Files.exists(dir));
        Files.delete(account);
        Files.delete(wallet);
        Files.delete(dir);
        assertFalse(Files.exists(account));
        assertFalse(Files.exists(wallet));
        assertFalse(Files.exists(dir));
    }

    @Test
    public void setAccountName() {
        assertEquals("", walletStorage.getAccountName(ADDRESS));
        final String name = "some_name";
        walletStorage.setAccountName(ADDRESS, name);
        assertEquals(name, walletStorage.getAccountName(ADDRESS));
        final String newName = "other_name";
        walletStorage.setAccountName(ADDRESS, newName);
        assertEquals(newName, walletStorage.getAccountName(ADDRESS));
    }

    @Test
    public void setLightAppSettings() {
        final LightAppSettings lightAppSettings = walletStorage.getLightAppSettings(ApiType.JAVA);
        assertEquals(ApiType.JAVA, lightAppSettings.getType());
//        assertEquals(DEFAULT_ADDRESS, lightAppSettings.getAddress());
//        assertEquals(DEFAULT_PORT, lightAppSettings.getPort());
//        assertEquals(DEFAULT_PROTOCOL, lightAppSettings.getProtocol());
        assertEquals(DEFAULT_LOCK_TIMEOUT, java.util.Optional.of(lightAppSettings.getLockTimeout()).get());
        assertEquals(DEFAULT_LOCK_TIMEOUT_MEASUREMENT_UNIT, lightAppSettings.getLockTimeoutMeasurementUnit());

        final String address = "ADDRESS";
        final String port = "port";
        final String proto = "proto";
        final String seconds = "seconds";
//        LightAppSettings newSettings = new LightAppSettings(name, address, port, proto, ApiType.JAVA, 2, seconds);
//        walletStorage.saveLightAppSettings(newSettings);

        final LightAppSettings reloadedSettings = walletStorage.getLightAppSettings(ApiType.JAVA);
        assertEquals(ApiType.JAVA, reloadedSettings.getType());
//        assertEquals(address, reloadedSettings.getAddress());
//        assertEquals(port, reloadedSettings.getPort());
//        assertEquals(proto, reloadedSettings.getProtocol());
        assertEquals(Integer.valueOf(2), java.util.Optional.of(reloadedSettings.getLockTimeout()).get());
        assertEquals(seconds, reloadedSettings.getLockTimeoutMeasurementUnit());
    }

    @Test
    public void testMasterAccountProperties() {
        assertEquals(0, walletStorage.getMasterAccountDerivations());
        try {
            walletStorage.incrementMasterAccountDerivations();
            fail();
        } catch (ValidationException e) {
            assertEquals("Cannot increment derivation when master account is missing", e.getMessage());
        }
        assertEquals(0, walletStorage.getMasterAccountDerivations());

        final String password = "password";
        try {
            walletStorage.getMasterAccountMnemonic(password);
        } catch (ValidationException e) {
            assertEquals("No master account present", e.getMessage());
        }

        final String mnemonic = "mnemonic";
        try {
            walletStorage.setMasterAccountMnemonic(mnemonic, password);
        } catch (ValidationException e) {
            fail();
        }

        try {
            final String invalidPassword = "";
            walletStorage.getMasterAccountMnemonic(invalidPassword);
        } catch (ValidationException e) {
            assertEquals("Password is not valid", e.getMessage());
        }

        final String wrong_password = "wrong_password";
        try {
            walletStorage.getMasterAccountMnemonic(wrong_password);
        } catch (ValidationException e) {
            assertEquals("Cannot decrypt your seed", e.getMessage());
        }

        String recoveredMnemonic = null;
        try {
            recoveredMnemonic = walletStorage.getMasterAccountMnemonic(password);
        } catch (ValidationException e) {
            fail();
        }
        assertEquals(recoveredMnemonic, mnemonic);
    }
}
