package org.aion.wallet.dto;

public class ExtendedAccountDTO extends AccountDTO{
    private byte[] privateKey;

    public ExtendedAccountDTO(final String name, final String publicAddress, final String balance, final String currency) {
        super(name, publicAddress, balance, currency);
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(byte[] privateKey) {
        this.privateKey = privateKey;
    }
}
