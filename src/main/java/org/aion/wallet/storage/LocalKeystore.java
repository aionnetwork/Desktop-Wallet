package org.aion.wallet.storage;

import org.aion.api.type.core.account.KeystoreFormat;
import org.aion.base.type.Address;
import org.aion.base.util.ByteArrayWrapper;
import org.aion.base.util.ByteUtil;
import org.aion.base.util.Hex;
import org.aion.base.util.TypeConverter;
import org.aion.crypto.ECKey;
import org.aion.crypto.ECKeyFac;
import org.aion.log.LogEnum;
import org.aion.mcf.account.FileDateTimeComparator;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.util.OSUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LocalKeystore {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.API.name());
    private static final FileDateTimeComparator COMPARE = new FileDateTimeComparator();
    private static final Pattern HEX_64 = Pattern.compile("^[\\p{XDigit}]{64}$");
    private static final String ADDR_PREFIX = "0x";
    private static final String AION_PREFIX = "a0";
    private static final int IMPORT_LIMIT = 100;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSS'Z'");
    private static final String SEPARATOR = "--";
    private static final String PERMISSIONS = "rwxr-----";

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static List<File> getFiles() {
        final File[] files = WalletStorage.KEYSTORE_PATH.toFile().listFiles();
        return files != null ? Arrays.asList(files) : Collections.emptyList();
    }

    public static String create(final String password) {
        return create(password, ECKeyFac.inst().create());
    }

    public static String create(final String password, final ECKey key) {

        final Set<PosixFilePermission> perms = PosixFilePermissions.fromString(PERMISSIONS);
        final FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);

        if (!Files.exists(WalletStorage.KEYSTORE_PATH)) {
            try {
                final FileCreator posixDirCreator = getPosixFileCreator(true, attr);
                final FileCreator winDirCreator = getWinFileCreator(true);
                OSUtils.executeForOs(winDirCreator, posixDirCreator, posixDirCreator, WalletStorage.KEYSTORE_PATH);
                Files.createDirectory(WalletStorage.KEYSTORE_PATH, attr);
            } catch (IOException e) {
                log.error("keystore folder create failed!");
                return "";
            }
        }

        String address = ByteUtil.toHexString(key.getAddress());
        if (exist(address)) {
            return ADDR_PREFIX;
        } else {
            byte[] content = new KeystoreFormat().toKeystore(key, password);
            String iso_date = DATE_FORMAT.format(new Date(System.currentTimeMillis()));
            String fileName = "UTC" + SEPARATOR + iso_date + SEPARATOR + address;
            try {
                final Path keyFile = WalletStorage.KEYSTORE_PATH.resolve(fileName);
                if (!Files.exists(keyFile)) {
                    final FileCreator posixFileCreator = getPosixFileCreator(false, attr);
                    final FileCreator winFileCreator = getWinFileCreator(false);
                    OSUtils.executeForOs(winFileCreator, posixFileCreator, posixFileCreator, keyFile);
                }
                String path = keyFile.toString();
                FileOutputStream fos = new FileOutputStream(path);
                fos.write(content);
                fos.close();
                return TypeConverter.toJsonHex(address);
            } catch (IOException e) {
                log.error("failed to create keystore");
                return ADDR_PREFIX;
            }
        }
    }

    public static Map<Address, ByteArrayWrapper> exportAccount(final Map<Address, String> account) {
        if (account == null) {
            throw new NullPointerException();
        }

        final Map<Address, ByteArrayWrapper> res = new HashMap<>();
        for (final Map.Entry<Address, String> entry : account.entrySet()) {
            final ECKey eckey = LocalKeystore.getKey(entry.getKey().toString(), entry.getValue());
            if (eckey != null) {
                res.put(entry.getKey(), ByteArrayWrapper.wrap(eckey.getPrivKeyBytes()));
            }
        }

        return res;
    }

    public static Map<Address, ByteArrayWrapper> backupAccount(final Map<Address, String> account) {
        if (account == null) {
            throw new NullPointerException();
        }

        final List<File> files = getFiles();
        if (files == null) {
            if (log.isWarnEnabled()) {
                log.warn("No key file been stored in the kernel.");
            }
            return new java.util.HashMap<>();
        }

        final List<File> matchedFile = files.parallelStream()
                .filter(file -> account.entrySet().parallelStream()
                        .anyMatch(ac -> file.getName()
                                .contains(ac.getKey().toString())
                        )
                ).collect(Collectors.toList());

        final Map<Address, ByteArrayWrapper> res = new HashMap<>();
        for (final File file : matchedFile) {
            try {
                String[] frags = file.getName().split(SEPARATOR);
                if (frags.length == 3) {
                    if (frags[2].startsWith(AION_PREFIX)) {
                        Address addr = Address.wrap(frags[2]);
                        byte[] content = Files.readAllBytes(file.toPath());

                        final String pw = account.get(addr);
                        if (pw != null) {
                            ECKey key = KeystoreFormat.fromKeystore(content, pw);
                            if (key != null) {
                                res.put(addr, ByteArrayWrapper.wrap(content));
                            }
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Wrong address format: {}", frags[2]);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return res;
    }

    public static String[] list() {
        return addAddrs(getFiles()).toArray(new String[0]);
    }

    private static List<String> addAddrs(final List<File> files) {
        final List<String> addresses = new ArrayList<>();
        files.forEach(
                (file) -> {
                    String[] frags = file.getName().split(SEPARATOR);
                    if (frags.length == 3) {
                        if (frags[2].startsWith(AION_PREFIX)) {
                            addresses.add(TypeConverter.toJsonHex(frags[2]));
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("Wrong address format: {}", frags[2]);
                            }
                        }
                    }
                });
        return addresses;
    }

    /**
     * Returns a sorted list of account addresses
     *
     * @return address represent by String as a List
     */
    public static List<String> accountsSorted() {
        final List<File> files = getFiles();
        files.sort(COMPARE);
        return addAddrs(files);
    }

    public static ECKey getKey(final String address, final String password) {
        final String strippedAddress;
        if (address.startsWith(ADDR_PREFIX)) {
            strippedAddress = address.substring(2);
        } else {
            strippedAddress = address;
        }

        ECKey key = null;
        if (strippedAddress.startsWith(AION_PREFIX)) {
            List<File> files = getFiles();
            for (final File file : files) {
                if (HEX_64.matcher(strippedAddress).find() && file.getName().contains(strippedAddress)) {
                    try {
                        byte[] content = Files.readAllBytes(file.toPath());
                        key = KeystoreFormat.fromKeystore(content, password);
                    } catch (IOException e) {
                        log.error("getKey exception! {}", e);
                    }
                    break;
                }
            }
        }
        return key;
    }

    /**
     * Returns true if the address _address exists, false otherwise.
     *
     * @param address the address whose existence is to be tested.
     * @return true only if _address exists.
     */
    public static boolean exist(final String address) {
        final String strippedAddress;
        if (address.startsWith(ADDR_PREFIX)) {
            strippedAddress = address.substring(2);
        } else {
            strippedAddress = address;
        }

        boolean flag = false;
        if (strippedAddress.startsWith(AION_PREFIX)) {
            List<File> files = getFiles();
            for (final File file : files) {
                if (HEX_64.matcher(strippedAddress).find() && file.getName().contains(strippedAddress)) {
                    flag = true;
                    break;
                }
            }
        }
        return flag;
    }

    public static Set<String> importAccount(final Map<String, String> importKey) {
        if (importKey == null) {
            throw new NullPointerException();
        }

        final Set<String> rtn = new HashSet<>();
        int count = 0;
        for (final Map.Entry<String, String> keySet : importKey.entrySet()) {
            if (count < IMPORT_LIMIT) {
                final ECKey key = KeystoreFormat.fromKeystore(Hex.decode(keySet.getKey()), keySet.getValue());
                if (key != null) {
                    final String address = LocalKeystore.create(keySet.getValue(), key);
                    if (!address.equals(ADDR_PREFIX)) {
                        if (log.isDebugEnabled()) {
                            log.debug("The private key was imported, the address is {}", keySet.getKey());
                        }
                    } else {
                        log.error("Failed to import the private key {}. Already exists?", keySet.getKey());
                        // only return the failed import privateKey.
                        rtn.add(keySet.getKey());
                    }
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("The account import limit was reached, the address didn't import into keystore {}", keySet.getKey());
                }
                rtn.add(keySet.getKey());
            }
            count++;
        }

        return rtn;
    }

    /*
     * Test method. Don't use it for the code dev.
     */
    protected static File getAccountFile(final String address, final String password) {
        final List<File> files = getFiles();
        if (files == null) {
            if (log.isWarnEnabled()) {
                log.warn("No key file been stored in the kernel.");
            }
            return null;
        }

        final Optional<File> matchedFile = files.parallelStream().filter(file -> file.getName().contains(address)).findFirst();

        if (matchedFile.isPresent()) {
            byte[] content = new byte[0];
            try {
                content = Files.readAllBytes(matchedFile.get().toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (null != KeystoreFormat.fromKeystore(content, password)) {
                return matchedFile.get();
            }
        }

        return null;
    }

    private static FileCreator getWinFileCreator(final boolean isDir) {
        return new FileCreator(isDir) {
            @Override
            protected void createDir(Path dir) throws IOException {
                Files.createDirectory(dir);
            }

            @Override
            protected void createFile(Path file) throws IOException {
                Files.createFile(file);
            }
        };
    }

    private static FileCreator getPosixFileCreator(final boolean isDir, final FileAttribute<Set<PosixFilePermission>> attr) {
        return new FileCreator(isDir) {
            @Override
            protected void createDir(Path dir) throws IOException {
                Files.createDirectory(dir, attr);
            }

            @Override
            protected void createFile(Path file) throws IOException {
                Files.createFile(file, attr);
            }
        };
    }


    private static abstract class FileCreator implements Consumer<Path> {

        private final boolean isDirectory;

        public FileCreator(final boolean isDirectory) {
            this.isDirectory = isDirectory;
        }

        @Override
        public void accept(final Path path) {
            try {
                if (isDirectory) {
                    createDir(path);
                } else {
                    createFile(path);
                }
            } catch (IOException e) {
                log.error("failed to create keystore");
                log.error(e.getMessage(), e);
            }
        }

        protected abstract void createDir(final Path dir) throws IOException;

        protected abstract void createFile(final Path file) throws IOException;
    }
}
