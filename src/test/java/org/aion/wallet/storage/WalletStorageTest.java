package org.aion.wallet.storage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class WalletStorageTest {

    private static final String HOME_DIR = System.getProperty("user.home");

    private static final String STORAGE_DIR = HOME_DIR + File.separator + ".aion";

    private static final String ACCOUNTS_FILE = STORAGE_DIR + File.separator + "accounts.properties";

    private static final String ADDRESS = "some_address";

    private Path file;
    private Path dir;
    private WalletStorage walletStorage;

    @Before
    public void setUp() {
        dir = Paths.get(STORAGE_DIR);
        file = Paths.get(ACCOUNTS_FILE);
        assertFalse(Files.exists(dir));
        assertFalse(Files.exists(file));
        walletStorage = WalletStorage.getInstance();
        assertTrue(Files.exists(dir));
        assertTrue(Files.exists(file));
    }

    @After
    public void tearDown() throws Exception {
        assertTrue(Files.exists(file));
        assertTrue(Files.exists(dir));
        Files.delete(file);
        Files.delete(dir);
        assertFalse(Files.exists(file));
        assertFalse(Files.exists(dir));
    }

    @Test
    public void getMissingAccountName() {
        assertNull(walletStorage.getAccountName(ADDRESS));
    }

    @Test
    public void setAccountName() {
        assertNull(walletStorage.getAccountName(ADDRESS));
        final String name = "some_name";
        walletStorage.setAccountName(ADDRESS, name);
        assertEquals(name, walletStorage.getAccountName(ADDRESS));
        final String newName = "other_name";
        walletStorage.setAccountName(ADDRESS, newName);
        assertEquals(newName, walletStorage.getAccountName(ADDRESS));
    }

    @Test
    public void saveAccountName() {
        assertNull(walletStorage.getAccountName(ADDRESS));
        final String name = "some_name";
        walletStorage.setAccountName(ADDRESS, name);
        assertEquals(name, walletStorage.getAccountName(ADDRESS));
        walletStorage.save();

        WalletStorage newStorage = WalletStorage.getInstance();
        assertEquals(name, newStorage.getAccountName(ADDRESS));

    }
}
