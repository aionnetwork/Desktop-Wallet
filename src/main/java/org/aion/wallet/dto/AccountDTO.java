package org.aion.wallet.dto;

import org.aion.base.util.TypeConverter;
import org.aion.wallet.util.QRCodeUtils;

import java.awt.image.BufferedImage;
import java.util.Objects;

public class AccountDTO {
    private final String currency;
    private final String publicAddress;
    private byte[] privateKey;
    private String balance;  //TODO this has to be BigInteger
    private String name;
    private boolean active;
    private boolean isImported;
    private int derivationIndex = -1;
    private final BufferedImage qrCode;

    public AccountDTO(final String name, final String publicAddress, final String balance, final String currency) {
        this.name = name;
        this.publicAddress = TypeConverter.toJsonHex(publicAddress);
        this.balance = balance;
        this.currency = currency;
        this.qrCode = QRCodeUtils.writeQRCode(publicAddress);
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

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isImported() {
        return isImported;
    }

    public void setImported(boolean imported) {
        isImported = imported;
    }

    public int getDerivationIndex() {
        return derivationIndex;
    }

    public void setDerivationIndex(int derivationIndex) {
        this.derivationIndex = derivationIndex;
    }

    public BufferedImage getQrCode() {
        return qrCode;
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
        return privateKey != null;
    }
}
