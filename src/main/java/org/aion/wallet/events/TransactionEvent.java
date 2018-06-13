package org.aion.wallet.events;

import org.aion.wallet.connector.dto.SendTransactionDTO;
import org.aion.wallet.dto.AccountDTO;

public class TransactionEvent extends AbstractEvent<TransactionEvent.Type> {

    public static final String ID = "transaction.resubmit";

    private final SendTransactionDTO transaction;

    protected TransactionEvent(final TransactionEvent.Type eventType, final SendTransactionDTO transaction) {
        super(eventType);
        this.transaction = transaction;
    }

    public SendTransactionDTO getTransaction() {
        return transaction;
    }

    public enum Type {
        RESUBMIT
    }
}
