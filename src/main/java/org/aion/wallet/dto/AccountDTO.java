package org.aion.wallet.dto;

import org.aion.base.util.TypeConverter;
import org.aion.wallet.connector.dto.BlockDTO;
import org.aion.wallet.connector.dto.SendTransactionDTO;
import org.aion.wallet.connector.dto.TransactionDTO;
import org.aion.wallet.util.BalanceUtils;
import org.aion.wallet.util.QRCodeUtils;

import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.*;

public class AccountDTO {

    private final String currency;
    private final String publicAddress;
    private final AccountType type;
    private final int derivationIndex;
    private final BufferedImage qrCode;
    private final SortedSet<TransactionDTO> transactions = new TreeSet<>();
    private final List<SendTransactionDTO> timedOutTransactions = new ArrayList<>();
    private byte[] privateKey;
    private BigInteger balance;
    private String name;
    private boolean active;
    private BlockDTO lastSafeBlock = null;

    public AccountDTO(final String name, final String publicAddress, final BigInteger balance, final String currency, AccountType type, int derivationIndex) {
        this.name = name;
        this.publicAddress = TypeConverter.toJsonHex(publicAddress);
        this.balance = balance;
        this.currency = currency;
        this.qrCode = QRCodeUtils.writeQRCode(publicAddress);
        this.type = type;
        this.derivationIndex = derivationIndex;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getCurrency() {
        return currency;
    }

    public String getPublicAddress() {
        return publicAddress;
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(byte[] privateKey) {
        this.privateKey = privateKey;
    }

    public String getFormattedBalance() {
        return BalanceUtils.formatBalance(balance);
    }

    public void setBalance(BigInteger balance) {
        this.balance = balance;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isImported() {
        return !AccountType.LOCAL.equals(type);
    }

    public AccountType getType() {
        return type;
    }

    public int getDerivationIndex() {
        return derivationIndex;
    }

    public BufferedImage getQrCode() {
        return qrCode;
    }

    public SortedSet<TransactionDTO> getTransactionsSnapshot() {
        return Collections.unmodifiableSortedSet(new TreeSet<>(transactions));
    }

    public void addTransactions(final Collection<TransactionDTO> transactions) {
        this.transactions.addAll(transactions);
    }

    public void removeTransactions(final Collection<TransactionDTO> transactions) {this.transactions.removeAll(transactions);
    }

    public BlockDTO getLastSafeBlock() {
        return lastSafeBlock;
    }

    public void setLastSafeBlock(final BlockDTO lastSafeBlock) {
        this.lastSafeBlock = lastSafeBlock;
    }

    public List<SendTransactionDTO> getTimedOutTransactions() {
        return timedOutTransactions;
    }

    public void addTimedOutTransaction(SendTransactionDTO transaction) {
        if (transaction == null) {
            return;
        }
        this.timedOutTransactions.add(transaction);
    }

    public void removeTimedOutTransaction(SendTransactionDTO transaction) {
        if (transaction == null) {
            return;
        }
        this.timedOutTransactions.remove(transaction);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AccountDTO that = (AccountDTO) o;
        return Objects.equals(currency, that.currency) &&
                Objects.equals(publicAddress, that.publicAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currency, publicAddress);
    }

    public boolean isUnlocked() {
        return !EnumSet.of(AccountType.LOCAL, AccountType.EXTERNAL).contains(type) || privateKey != null;
    }

}
