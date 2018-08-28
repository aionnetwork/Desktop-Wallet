package org.aion.wallet.hardware.ledger;

import org.aion.api.log.LogEnum;
import org.aion.base.util.TypeConverter;
import org.aion.wallet.hardware.AionAccountDetails;
import org.aion.wallet.hardware.HardwareWallet;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.util.OSUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class LedgerWallet implements HardwareWallet {
    private static final String WINDOWS_DRIVER_PATH = "native\\win\\ledger\\Aion-HID\\npm.cmd";
    private static final String WINDOWS_DRIVER_PATH_HID = "\\native\\win\\ledger\\Aion-HID\\hid";
    private static final String WINDOWS_NPM_LOCATION = "\\native\\win\\ledger\\Aion-HID";
    private static final String WINDOWS_PREFIX_KEY = "--prefix";
    private static final String WINDOWS_AION_HID_KEY = "get:aion-hid";

    private static final String MAC_DRIVER_LOCATION = "/native/mac/ledger/hid";

    private static final String LINUX_DRIVER_PATH = "native/linux/ledger/Aion-HID";
    private static final String PARAM_KEY = "--param=";
    private static final byte PATH_LENGTH = 0x15;
    private static final String DEFAULT_DERIVATION_PATH = "058000002c800001a98000000080000000";

    private static final int DERIVATION_INDEX_FOR_CONNECTIVITY_CHECKS = 1;

    private static final String GET_PUBLIC_KEY_INDEX = "02";
    private static final String SIGN_TRANSACTION_INDEX = "04";
    private static final String DEFAULT_THIRD_AND_FORTH_BYTES_FOR_SIGNING = "0000";
    private static final String DEFAULT_THIRD_AND_FORTH_BYTES_FOR_KEY_GENERATION = "0100";

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());
    private static Map<Integer, AionAccountDetails> accountCache;
    private ProcessBuilder processBuilder;

    public LedgerWallet(){
        accountCache = new HashMap<>();
        processBuilder = createProcessBuilder();
        installNpmIfRequired();
    }

    private void installNpmIfRequired() {
        if(OSUtils.isWindows()){
            if(!Files.exists(Paths.get(System.getProperty("user.dir") + WINDOWS_DRIVER_PATH_HID + "\\node_modules"))) {
                String[] commands = new String[]{System.getProperty("user.dir") + WINDOWS_NPM_LOCATION + "\\npm.cmd", "install"};
                processBuilder.directory(new File(System.getProperty("user.dir") + WINDOWS_DRIVER_PATH_HID));
                try {
                    Process process = processBuilder.command(commands).start();
                    BufferedReader lineReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = lineReader.readLine()) != null) {
                        log.info(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                log.debug("The node_module folder already exists");
            }
        } else if(OSUtils.isMac()){
            if(!Files.exists(Paths.get(System.getProperty("user.dir") + MAC_DRIVER_LOCATION + "/node_modules"))){
                log.info("Driver not installed installing it now...");
                String[] commands = new String[]{ "bash", System.getProperty("user.dir") + MAC_DRIVER_LOCATION + "/setup.sh"};
                processBuilder.directory(new File(System.getProperty("user.dir") + MAC_DRIVER_LOCATION));
                try {
                    Process process = processBuilder.command(commands).start();
                    BufferedReader lineReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    String line;
                    while ((line = lineReader.readLine()) != null) {
                        log.info(line);
                    }
                    while ((line = errorReader.readLine()) != null) {
                        log.info(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private ProcessBuilder createProcessBuilder(){
        ProcessBuilder tmpBuilder = new ProcessBuilder();
        Map<String, String> envs = tmpBuilder.environment();
        if(OSUtils.isWindows()) {
            String path = envs.get("Path");
            envs.put("Path", path + File.pathSeparator + System.getProperty("user.dir") + WINDOWS_NPM_LOCATION);
        } else if(OSUtils.isMac()) {
            tmpBuilder.directory(new File(System.getProperty("user.dir") + MAC_DRIVER_LOCATION));
        }
        return tmpBuilder;
    }

    @Override
    public boolean isConnected() {
        try {
            getAccountDetails(DERIVATION_INDEX_FOR_CONNECTIVITY_CHECKS);
        } catch (LedgerException e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    @Override
    public AionAccountDetails getAccountDetails(final int derivationIndex) throws LedgerException {
        String[] commands = getCommandForAccountAddress(derivationIndex);
        Process process;
        try {
            process = createProcessBuilder().command(commands).start();
        } catch (IOException e) {
            throw new LedgerException(e);
        }

        BufferedReader lineReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        String line;
        StringBuilder output = new StringBuilder();

        try {
            while ((line = lineReader.readLine()) != null) {
                output.append(line);
            }

            while ((line = errorReader.readLine()) != null) {
                output.append(line);
            }
        } catch (IOException e) {
            throw new LedgerException(e);
        }


        log.debug("Ledger returned for getPublic address : " + output);
        String[] spaceSplitted = output.toString().split(" ");

        List<String> strings = Arrays.asList(spaceSplitted);
        int indexResponse = strings.indexOf("response");
        if (spaceSplitted[indexResponse].equals("response") && !spaceSplitted[indexResponse + 2].isEmpty()) {
            return new AionAccountDetails(spaceSplitted[indexResponse + 2].substring(0, 64), spaceSplitted[indexResponse + 2].substring(64, 128), derivationIndex);
        } else {
            throw new LedgerException("Error wile communicating with the ledger...");
        }
    }

    @Override
    public List<AionAccountDetails> getMultipleAccountDetails(int derivationIndexStart, int derivationIndexEnd) throws LedgerException {
        List<AionAccountDetails> accounts = new LinkedList<>();
        //check first derivation if we have the value in the cache
        if(accountCache.containsKey(derivationIndexStart)){
            //check if the fist key matches
            AionAccountDetails aionAccountDetails = getAccountDetails(derivationIndexStart);
            AionAccountDetails existingAionAccountDetails = accountCache.get(derivationIndexStart);
            accounts.add(aionAccountDetails);
            if(!aionAccountDetails.equals(existingAionAccountDetails)){
                //invalidate the cache
                accountCache = new HashMap<>();
            }
        } else {
            AionAccountDetails aionAccountDetails = getAccountDetails(derivationIndexStart);
            accounts.add(aionAccountDetails);
        }

        for(int i=derivationIndexStart+1;i<derivationIndexEnd;i++){
            if(accountCache.containsKey(i)){
                accounts.add(accountCache.get(i));
            } else {
                AionAccountDetails tmp = getAccountDetails(i);
                accounts.add(tmp);
                accountCache.put(i, tmp);
            }
        }

        return accounts;
    }

    @Override
    public String signMessage(final int derivationIndex, final byte[] message) throws LedgerException {
        String[] commands  = getCommandForTransactionSigning(derivationIndex, message);
        Process process = null;
        try {
            process = processBuilder.command(commands).start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert process != null;
        BufferedReader lineReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        String line;
        StringBuilder output = new StringBuilder();

        try {
            while ((line = lineReader.readLine()) != null) {
                output.append(line);
            }

            while ((line = errorReader.readLine()) != null) {
                output.append(line);
            }
        } catch (IOException e) {
            throw new LedgerException(e);
        }

        log.debug("Ledger returned for signing command : " + output);
        String[] spaceSplitted = output.toString().split(" ");

        List<String> strings = Arrays.asList(spaceSplitted);
        int indexResponse = strings.indexOf("response");
        if (spaceSplitted[indexResponse].equals("response") && !spaceSplitted[indexResponse + 2].isEmpty()) {
            return spaceSplitted[indexResponse + 2];
        } else {
            throw new LedgerException("Error wile communicating with the ledger...");
        }
    }

    private String getDerivationPathForIndex(int derivationIndex) {
        BigInteger defaultDerivation = new BigInteger("80000000", 16);
        BigInteger pathIndex = new BigInteger(String.valueOf(derivationIndex));
        return DEFAULT_DERIVATION_PATH + defaultDerivation.or(pathIndex).toString(16);
    }

    private String[] getCommandForAccountAddress(int derivationIndex) throws LedgerException {
        //Example of param : e002010015058000002c800001a9800000008000000080000000
        if (OSUtils.isUnix()) {
            return new String[]{LINUX_DRIVER_PATH, PARAM_KEY +
                    "e0" + GET_PUBLIC_KEY_INDEX +
                    DEFAULT_THIRD_AND_FORTH_BYTES_FOR_KEY_GENERATION +
                    PATH_LENGTH + getDerivationPathForIndex(derivationIndex)};

        } else if (OSUtils.isWindows()) {
            return new String[]{System.getProperty("user.dir") + File.separator + WINDOWS_DRIVER_PATH, "run",
                    WINDOWS_AION_HID_KEY, "e0" +
                    GET_PUBLIC_KEY_INDEX +
                    DEFAULT_THIRD_AND_FORTH_BYTES_FOR_KEY_GENERATION +
                    PATH_LENGTH + getDerivationPathForIndex(derivationIndex),
                    WINDOWS_PREFIX_KEY, System.getProperty("user.dir") + File.separator + WINDOWS_DRIVER_PATH_HID};
        } else {
            return new String[]{"npm", "run",
                    WINDOWS_AION_HID_KEY, "e0" +
                    GET_PUBLIC_KEY_INDEX +
                    DEFAULT_THIRD_AND_FORTH_BYTES_FOR_KEY_GENERATION +
                    PATH_LENGTH + getDerivationPathForIndex(derivationIndex)};
        }
    }

    private String[] getCommandForTransactionSigning(int derivationIndex, final byte[] message) throws LedgerException {
        int length = PATH_LENGTH + message.length;
        //Example of param : e0040000cf058000002c800001a9800000008000000080000000 + message
        if (OSUtils.isUnix()) {
            return new String[]{LINUX_DRIVER_PATH, PARAM_KEY +
                                "e0" + SIGN_TRANSACTION_INDEX +
                                DEFAULT_THIRD_AND_FORTH_BYTES_FOR_SIGNING +
                                Integer.toHexString(length) +
                                getDerivationPathForIndex(derivationIndex) + TypeConverter.toJsonHex(message).substring(2)};

        } else if(OSUtils.isWindows()){
            return new String[]{System.getProperty("user.dir") + File.separator + WINDOWS_DRIVER_PATH, "run", WINDOWS_AION_HID_KEY ,
                                "e0" + SIGN_TRANSACTION_INDEX + DEFAULT_THIRD_AND_FORTH_BYTES_FOR_SIGNING +
                                Integer.toHexString(length) + getDerivationPathForIndex(derivationIndex) +
                                TypeConverter.toJsonHex(message).substring(2), "--prefix", System.getProperty("user.dir") + File.separator + WINDOWS_DRIVER_PATH_HID};
        } else {
            return new String[]{"npm", "run", WINDOWS_AION_HID_KEY ,
                    "e0" + SIGN_TRANSACTION_INDEX + DEFAULT_THIRD_AND_FORTH_BYTES_FOR_SIGNING +
                            Integer.toHexString(length) + getDerivationPathForIndex(derivationIndex) +
                            TypeConverter.toJsonHex(message).substring(2)};
        }
    }
}
