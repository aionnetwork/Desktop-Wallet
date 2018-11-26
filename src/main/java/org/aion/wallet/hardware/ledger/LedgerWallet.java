package org.aion.wallet.hardware.ledger;

import org.aion.api.log.LogEnum;
import org.aion.base.util.TypeConverter;
import org.aion.ledger.KeyAddress;
import org.aion.ledger.LedgerDevice;
import org.aion.ledger.LedgerUtilities;
import org.aion.ledger.application.AionApp;
import org.aion.ledger.exceptions.CommsException;
import org.aion.wallet.events.EventPublisher;
import org.aion.wallet.hardware.AionAccountDetails;
import org.aion.wallet.hardware.HardwareWallet;
import org.aion.wallet.hardware.HardwareWalletException;
import org.aion.wallet.log.WalletLoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class LedgerWallet implements HardwareWallet {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private static final int FIRST_INDEX = 0;

    private final Map<Integer, AionAccountDetails> accountCache = new ConcurrentHashMap<>();

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private ReentrantLock lock = new ReentrantLock();

    @Override
    public boolean isConnected() {
        try {
            AionAccountDetails firstAccount = getAccountDetails(FIRST_INDEX);
            if (accountCache.containsKey(FIRST_INDEX) && !firstAccount.equals(accountCache.get(FIRST_INDEX))) {
                accountCache.clear();
            }
            return true;
        } catch (LedgerException e) {
            e.printStackTrace();
            return false;
        }

    }

    @Override
    public AionAccountDetails getAccountDetails(final int derivationIndex) throws LedgerException {
        LedgerDevice ledgerDevice = null;
        try {
            lock.lock();
            ledgerDevice = LedgerUtilities.findLedgerDevice();
            AionApp aionApp = new AionApp(ledgerDevice);
            KeyAddress publicKey = aionApp.getPublicKey(derivationIndex);
            if(publicKey != null) {
                return new AionAccountDetails(TypeConverter.toJsonHex(publicKey.getPublicKey()), TypeConverter.toJsonHex(publicKey.getAddress()), derivationIndex);
            }
            else {
                throw new LedgerException("Could not retrieve public key!");
            }
        } catch (IOException | CommsException | NullPointerException e) {
            throw new LedgerException(e);
        }
        finally {
            if(ledgerDevice != null) {
                ledgerDevice.close();
            }
            lock.unlock();
        }
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
                accountCache.put(i, getAccountDetails(i));
            }
            accounts.add(accountCache.get(i));
        }
        backgroundExecutor.submit(() -> {
            try {
                for (int i = derivationIndexEnd; i < derivationIndexEnd + (derivationIndexEnd - derivationIndexStart); i++) {
                    if (!accountCache.containsKey(i)) {
                        accountCache.put(i, getAccountDetails(i));
                    }
                }
                EventPublisher.fireAccountsRecovered(accountCache.values().stream().map(AionAccountDetails::getAddress).collect(Collectors.toSet()));
            } catch (HardwareWalletException e) {
                log.error("Could not preload next accounts: " + e.getMessage(), e);
            }
        });
        return accounts;
    }

    @Override
    public byte[] signMessage(final int derivationIndex, final byte[] message) throws LedgerException {
        LedgerDevice ledgerDevice = null;
        try {
            lock.lock();
            ledgerDevice = LedgerUtilities.findLedgerDevice();
            AionApp aionApp = new AionApp(ledgerDevice);
            byte[] result = aionApp.signPayload(derivationIndex, message);
            return result;
        } catch (IOException | CommsException e) {
            throw new LedgerException(e);
        }
        finally {
            if(ledgerDevice != null) {
                ledgerDevice.close();
            }
            lock.unlock();
        }
    }
}
