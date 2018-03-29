package org.aion.wallet.ui.events;

import javafx.scene.Node;

public class AccountOverviewEvent extends AbstractUIEvent<AccountOverviewEvent.Type>{

    private final Node eventSource;

    public AccountOverviewEvent(Type eventType, Node nodeSource) {
        super(eventType);
        this.eventSource = nodeSource;
    }

    public Node getSource() {
        return eventSource;
    }

    public enum Type {
        ACCOUNT_CREATED
    }
}
