package org.aion.wallet.hardware.ledger;

import org.aion.api.log.LogEnum;
import org.aion.base.util.Hex;
import org.aion.wallet.events.EventPublisher;
import org.aion.wallet.hardware.AionAccountDetails;
import org.aion.wallet.hardware.HardwareWallet;
import org.aion.wallet.hardware.HardwareWalletException;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.util.CryptoUtils;
import org.aion.wallet.util.OSUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LedgerWallet implements HardwareWallet {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());
    private static final String USER_DIR = System.getProperty("user.dir");
    private static final String PATH = "Path";

    private static final String RESPONSE = "response";
    private static final String SPACE = " ";

    private static final String NPM_COMMAND = "npm";
    private static final String RUN_CMD = "run";
    private static final String NPM_AION_HID_KEY = "get:aion-hid";

    private static final String WINDOWS_DRIVER_PATH = USER_DIR + File.separator + "native\\win\\ledger\\Aion-HID\\npm.cmd";
    private static final String WINDOWS_DRIVER_PATH_HID = USER_DIR + File.separator + "native\\win\\ledger\\Aion-HID\\hid";
    private static final String WINDOWS_NPM_LOCATION = USER_DIR + File.separator + "native\\win\\ledger\\Aion-HID";
    private static final String WINDOWS_PREFIX_KEY = "--prefix";

    private static final String MAC_DRIVER_LOCATION = USER_DIR + File.separator + "native/mac/ledger/hid";
    private static final String MAC_NPM_LOCATION = MAC_DRIVER_LOCATION + File.separator + "node-v8.11.4-darwin-x64/bin/";

    private static final String LINUX_DRIVER_PATH = "native/linux/ledger/Aion-HID";
    private static final String LINUX_PARAM_KEY = "--param=";

    private static final int FIRST_INDEX = 0;
    private static final byte PATH_LENGTH = 0x15;
    private static final String AION_LEDGER_APP_PFX = "e0";
    private static final String GET_PUBLIC_KEY_INDEX = "02";
    private static final String SIGN_TRANSACTION_INDEX = "04";
    private static final String DEFAULT_THIRD_AND_FORTH_BYTES_FOR_KEY_GENERATION = "0100";
    private static final String DEFAULT_THIRD_AND_FORTH_BYTES_FOR_SIGNING = "0000";
    private static final String DEFAULT_DERIVATION_PATH = "058000002c800001a98000000080000000";

    private static final int HEX_KEY_SIZE = 64;

    private static final WindowsNpmInstaller WINDOWS_NPM_INSTALLER = new WindowsNpmInstaller();
    private static final String PREFIX = "--prefix";

    private static final int RETRIES = 3;

    private final Map<Integer, AionAccountDetails> accountCache = new ConcurrentHashMap<>();
    private final ProcessBuilder processBuilder = createProcessBuilder();

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    public LedgerWallet() {
        installNpmIfRequired();
    }

    private ProcessBuilder createProcessBuilder() {
        ProcessBuilder tmpBuilder = new ProcessBuilder();
        OSUtils.executeForOs(this::updateProcessBuilderForWindows, this::updateProcessBuilderForMac, p -> {
        }, tmpBuilder);
        return tmpBuilder;
    }

    private void updateProcessBuilderForMac(final ProcessBuilder p) {
        p.directory(new File(MAC_DRIVER_LOCATION));
        final Map<String, String> environment = p.environment();
        final String oldPath = environment.get("PATH");
        environment.put("PATH", oldPath + File.pathSeparator + MAC_NPM_LOCATION);
    }

    private void updateProcessBuilderForWindows(final ProcessBuilder p) {
        final Map<String, String> environment = p.environment();
        final String oldPath = environment.get(PATH);
        environment.put(PATH, oldPath + File.pathSeparator + WINDOWS_NPM_LOCATION);
    }

    private void installNpmIfRequired() {
        OSUtils.executeForOs(WINDOWS_NPM_INSTALLER, p -> {}, p -> {}, processBuilder);
    }

    @Override
    public boolean isConnected() {
        try {
            final AionAccountDetails firstAccount = getAccountDetails(FIRST_INDEX);
            if (accountCache.containsKey(FIRST_INDEX) && !firstAccount.equals(accountCache.get(FIRST_INDEX))) {
                accountCache.clear();
            }
            accountCache.put(FIRST_INDEX, firstAccount);
        } catch (HardwareWalletException e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    @Override
    public AionAccountDetails getAccountDetails(final int derivationIndex) throws LedgerException {
        final String[] commands = getCommandForAccountDetails(derivationIndex);
        final String output = getProcessOutputForCommand(commands);
        log.debug("Ledger returned for getPublic address : " + output);
        final String response = getResponse(output);
        final String pubKeyHex = response.substring(0, HEX_KEY_SIZE);
        final String addressHex = response.substring(HEX_KEY_SIZE, 2 * HEX_KEY_SIZE);
        return new AionAccountDetails(pubKeyHex, addressHex, derivationIndex);
    }

    @Override
    public List<AionAccountDetails> getMultipleAccountDetails(final int derivationIndexStart, final int derivationIndexEnd) throws LedgerException {
        final List<AionAccountDetails> accounts = new LinkedList<>();
        final AionAccountDetails newAccountDetails = getAccountDetails(derivationIndexStart);

        if (accountCache.containsKey(derivationIndexStart)) {
            final AionAccountDetails existingAionAccountDetails = accountCache.get(derivationIndexStart);
            if (!newAccountDetails.equals(existingAionAccountDetails)) {
                accountCache.clear();
                accountCache.put(derivationIndexStart, newAccountDetails);
            }
        }

        accounts.add(newAccountDetails);

        for (int i = derivationIndexStart + 1; i < derivationIndexEnd; i++) {
            if (!accountCache.containsKey(i)) {
                accountCache.put(i, getAccountDetailsWithRetries(i));
            }
            accounts.add(accountCache.get(i));
        }
        backgroundExecutor.submit(() -> {
            try {
                for (int i = derivationIndexEnd; i < derivationIndexEnd + (derivationIndexEnd - derivationIndexStart); i++) {
                    if (!accountCache.containsKey(i)) {
                        accountCache.put(i, getAccountDetailsWithRetries(i));
                    }
                }
                EventPublisher.fireAccountsRecovered(accountCache.values().stream().map(AionAccountDetails::getAddress).collect(Collectors.toSet()));
            } catch (HardwareWalletException e) {
                log.error("Could not preload next accounts: " + e.getMessage(), e);
            }
        });
        return accounts;
    }

    private AionAccountDetails getAccountDetailsWithRetries(final int index) throws LedgerException {
        AionAccountDetails accountDetails = null;
        int retry = 0;
        do {
            try {
                accountDetails = getAccountDetails(index);
                break;
            } catch (LedgerException e) {
                if (retry == RETRIES) {
                    throw e;
                }
            }
            retry++;
        } while (retry <= RETRIES);
        return accountDetails;
    }

    @Override
    public String signMessage(final int derivationIndex, final byte[] message) throws LedgerException {
        String[] commands = getCommandForTransactionSigning(derivationIndex, message);
        final String output = getProcessOutputForCommand(commands);
        log.debug("Ledger returned for signing command : " + output);
        return getResponse(output);
    }

    private String getResponse(final String output) throws LedgerException {
        String[] outputWords = output.split(SPACE);
        List<String> strings = Arrays.asList(outputWords);
        int indexResponse = OSUtils.getForOs(s -> s.indexOf(RESPONSE), s -> s.indexOf(RESPONSE), s -> 0, strings);
        final String result = outputWords[indexResponse + 2];
        if (!result.isEmpty() && result.length() == 2 * HEX_KEY_SIZE) {
            return result;
        } else {
            throw new LedgerException("Error wile communicating with the ledger..." + result);
        }
    }

    private String getProcessOutputForCommand(final String[] commands) throws LedgerException {
        Process process;
        try {
            process = processBuilder.command(commands).start();
        } catch (IOException e) {
            throw new LedgerException(e);
        }

        final BufferedReader lineReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        final BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        String line;
        final StringBuilder output = new StringBuilder();

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
        return output.toString();
    }

    private String[] getCommandForAccountDetails(final int derivationIndex) {
        //Example of command : e002010015058000002c800001a9800000008000000080000000
        return OSUtils.getForOs(this::getWinAccDetails, this::getMacAccDetails, this::getLinuxAccDetails,
                derivationIndex);
    }

    private String[] getWinAccDetails(final int derivationIndex) {
        return new String[]{
                WINDOWS_DRIVER_PATH,
                RUN_CMD,
                NPM_AION_HID_KEY,
                getAccountDetailsCommandAsHex(derivationIndex),
                WINDOWS_PREFIX_KEY,
                WINDOWS_DRIVER_PATH_HID
        };
    }

    private String[] getMacAccDetails(final int derivationIndex) {
        return new String[]{
                NPM_COMMAND,
                RUN_CMD,
                NPM_AION_HID_KEY,
                getAccountDetailsCommandAsHex(derivationIndex)
        };
    }

    private String[] getLinuxAccDetails(final int derivationIndex) {
        return new String[]{
                LINUX_DRIVER_PATH,
                LINUX_PARAM_KEY + getAccountDetailsCommandAsHex(derivationIndex)
        };
    }

    private String getAccountDetailsCommandAsHex(final int derivationIndex) {
        return AION_LEDGER_APP_PFX
                + GET_PUBLIC_KEY_INDEX
                + DEFAULT_THIRD_AND_FORTH_BYTES_FOR_KEY_GENERATION
                + PATH_LENGTH + getDerivationPathForIndex(derivationIndex);
    }

    private String getDerivationPathForIndex(final int derivationIndex) {
        final String hardenedIndex = Hex.toHexString(CryptoUtils.getHardenedNumber(derivationIndex));
        return DEFAULT_DERIVATION_PATH + hardenedIndex;
    }

    private String[] getCommandForTransactionSigning(final int derivationIndex, final byte[] message) {
        final MessageWrapper wrapper = new MessageWrapper(message, derivationIndex);
        //Example of param : e0040000cf058000002c800001a9800000008000000080000000 + message
        return OSUtils.getForOs(this::getWinSignedMessage, this::getMacSignedMessage, this::getLinuxSignedMessage,
                wrapper);
    }

    private String[] getWinSignedMessage(final MessageWrapper wrapper) {
        return new String[]{
                WINDOWS_DRIVER_PATH,
                RUN_CMD,
                NPM_AION_HID_KEY,
                getSignCommandAsHex(wrapper),
                WINDOWS_PREFIX_KEY,
                WINDOWS_DRIVER_PATH_HID
        };
    }

    private String[] getMacSignedMessage(final MessageWrapper wrapper) {
        return new String[]{
                NPM_COMMAND,
                RUN_CMD,
                NPM_AION_HID_KEY,
                getSignCommandAsHex(wrapper),
                PREFIX,
                MAC_DRIVER_LOCATION
        };
    }

    private String[] getLinuxSignedMessage(final MessageWrapper wrapper) {
        return new String[]{
                LINUX_DRIVER_PATH,
                LINUX_PARAM_KEY + getSignCommandAsHex(wrapper)
        };

    }

    private String getSignCommandAsHex(MessageWrapper wrapper) {
        return AION_LEDGER_APP_PFX
                + SIGN_TRANSACTION_INDEX
                + DEFAULT_THIRD_AND_FORTH_BYTES_FOR_SIGNING
                + Integer.toHexString(wrapper.length)
                + getDerivationPathForIndex(wrapper.derivationIndex)
                + Hex.toHexString(wrapper.message);
    }

    private static class MessageWrapper {

        private final byte[] message;
        private final int derivationIndex;
        private final int length;

        private MessageWrapper(byte[] message, int derivationIndex) {
            this.message = message;
            this.derivationIndex = derivationIndex;
            this.length = PATH_LENGTH + message.length;
        }
    }

    private static class WindowsNpmInstaller implements Consumer<ProcessBuilder> {

        @Override
        public void accept(final ProcessBuilder processBuilder) {
            if (!Files.exists(Paths.get(WINDOWS_DRIVER_PATH_HID + "\\node_modules"))) {
                String[] commands = new String[]{WINDOWS_NPM_LOCATION + "\\npm.cmd", "install"};
                processBuilder.directory(new File(WINDOWS_DRIVER_PATH_HID));
                try {
                    Process process = processBuilder.command(commands).start();
                    final BufferedReader lineReader = new BufferedReader(new InputStreamReader(process.getInputStream
                            ()));
                    String line;
                    while ((line = lineReader.readLine()) != null) {
                        log.info(line);
                    }
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            } else {
                log.debug("The node_modules folder already exists");
            }
        }
    }
}
