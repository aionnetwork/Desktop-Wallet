package org.aion.wallet.hardware.ledger;

import org.aion.api.log.LogEnum;
import org.aion.wallet.hardware.AionAccountDetails;
import org.aion.wallet.hardware.HardwareWallet;
import org.aion.wallet.log.WalletLoggerFactory;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.stream.Collectors;

public class LedgerWallet implements HardwareWallet {
    private static final String WINDOWS_DRIVER_PATH = "native/win/ledger/Aion-HID/npm run get:aion-hid";

    private static final String LINUX_DRIVER_PATH = "native/linux/ledger/Aion-HID";
    private static final String PARAM_KEY = "--param=";
    private static final byte PATH_LENGTH = 0x15;
    private static final String DEFAULT_DERIVATION_PATH = "058000002c800001a98000000080000000";
    private static final String ERROR_AION_APP_NOT_OPEN = "error invalid status 6700";
    private static final String ERROR_LEDGER_NOT_CONNECTED = "err no ledger connected";
    private static final String ERROR_CANNOT_SIGN_WITH_ACCOUNT = "error invalid status 6a80";
    private static final String ERROR = "error invalid status 6b00";

    private static final int DERIVATION_INDEX_FOR_CONNECTIVITY_CHECKS = 1;

    private static final String GET_PUBLIC_KEY_INDEX = "02";
    private static final String SIGN_TRANSACTION_INDEX = "04";

    private static String OS = System.getProperty("os.name").toLowerCase();
    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    @Override
    public boolean isConnected() {
        try {
            getPublicKey(DERIVATION_INDEX_FOR_CONNECTIVITY_CHECKS);
        } catch (LedgerException e) {
            log.info("Ledger is not connected...");
            return false;
        }
        return true;
    }

    @Override
    public AionAccountDetails getPublicKey(final int derivationIndex) throws LedgerException {

        Runtime runtime = Runtime.getRuntime();
        String[] commands = getCommandForAccountAddress(derivationIndex);
        Process process = null;

        try {
            process = runtime.exec(commands);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert process != null;
        BufferedReader lineReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String output = lineReader.lines().collect(Collectors.joining());
        log.debug("Ledger returned for getPublic address : " + output);
        String[] spaceSplitted = output.split(" ");

        if(spaceSplitted.length == 3){
            return new AionAccountDetails(spaceSplitted[2].substring(64, 128).getBytes(), spaceSplitted[2].substring(0, 64).getBytes());
        } else {
            throw new LedgerException("Error wile communicating with the ledger...");
        }
    }

    @Override
    public byte[] signMessage(final int derivationIndex, final byte[] message) throws LedgerException {
        Runtime runtime = Runtime.getRuntime();
        String[] commands  = getCommandForTransactionSigning(derivationIndex, message);
        Process process = null;
        try {
            process = runtime.exec(commands);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert process != null;
        BufferedReader lineReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String output = lineReader.lines().collect(Collectors.joining());
        log.debug("Ledger returned for signing command : " + output);
        String[] spaceSplitted = output.split(" ");

        if(spaceSplitted.length == 3){
            return spaceSplitted[2].getBytes();
        } else {
            throw new LedgerException("Error wile communicating with the ledger...");
        }
    }

    private String getDerivationPathForIndex(int derivationIndex){
        BigInteger defaultDerivation = new BigInteger("80000000", 16);
        BigInteger pathIndex = new BigInteger(String.valueOf(derivationIndex));
        return DEFAULT_DERIVATION_PATH + defaultDerivation.or(pathIndex).toString(16);
    }

    private String[] getCommandForAccountAddress(int derivationIndex) throws LedgerException {
        //Example of param : e002010015058000002c800001a9800000008000000080000000
        if(isUnix()) {
            return new String[]{LINUX_DRIVER_PATH, PARAM_KEY + "e0" + GET_PUBLIC_KEY_INDEX + "0100" + PATH_LENGTH + getDerivationPathForIndex(derivationIndex)};
        } else if(isWindows()){
            return new String[]{WINDOWS_DRIVER_PATH, "e0" + GET_PUBLIC_KEY_INDEX + "0100" + PATH_LENGTH + getDerivationPathForIndex(derivationIndex)};
        } else {
            throw new LedgerException("Platform not yet supported");
        }
    }

    private String[] getCommandForTransactionSigning(int derivationIndex, final byte[] message) throws LedgerException {
        int length = PATH_LENGTH + message.length/2;
        //Example of param : e0040000cf058000002c800001a9800000008000000080000000 + message
        if(isUnix()){
            return new String[]{LINUX_DRIVER_PATH, PARAM_KEY + "e0" + SIGN_TRANSACTION_INDEX + "0000" + Integer.toHexString(length) + getDerivationPathForIndex(derivationIndex) + new String(message)};
        } else if(isWindows()){
            return new String[]{WINDOWS_DRIVER_PATH, "e0" + SIGN_TRANSACTION_INDEX + "0000" + Integer.toHexString(length) + getDerivationPathForIndex(derivationIndex) + new String(message)};
        } else {
            throw new LedgerException("Platform not yet supported");
        }
    }

    public static boolean isWindows() {

        return (OS.contains("win"));

    }

    public static boolean isMac() {

        return (OS.contains("mac"));

    }

    public static boolean isUnix() {

        return (OS.contains("nix") || OS.contains("nux") || OS.indexOf("aix") > 0 );

    }
}
