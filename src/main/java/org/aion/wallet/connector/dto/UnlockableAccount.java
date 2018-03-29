package org.aion.wallet.connector.dto;

import org.aion.base.type.Address;

public interface UnlockableAccount {
    Address getAddress();
    String getPassword();
}
