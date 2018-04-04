package org.aion.wallet.ui.components.partials;

import com.google.common.eventbus.Subscribe;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.EventPublisher;
import org.aion.wallet.ui.events.HeaderPaneButtonEvent;

import java.math.BigInteger;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AccountOverview implements Initializable{
    @FXML
    private ListView<AccountDTO> accountListView;

    private AddAccountDialog addAccountDialog;

    private  final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();

    private AccountDTO account;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        addAccountDialog = new AddAccountDialog();
        registerEventBusConsumer();
        reloadWalletView();
    }

    private void registerEventBusConsumer() {
        EventBusFactory eventBusFactory = EventBusFactory.getInstance();
        eventBusFactory.getBus(HeaderPaneButtonEvent.ID).register(this);
        EventBusFactory.getInstance().getBus(EventPublisher.ACCOUNT_CHANGE_EVENT_ID).register(this);
    }

    @Subscribe
    private void handleAccountChanged(AccountDTO account) {
        this.account = account;
        // todo: don't reload the account list from blockchain connector
        reloadWalletView();
    }

    @Subscribe
    private void handleHeaderPaneButtonEvent(HeaderPaneButtonEvent event) {
        reloadWalletView();
        addAccountDialog.close();
    }

    private void reloadWalletView() {
        List<AccountDTO> accounts = blockchainConnector.getAccounts();
        for (AccountDTO account : accounts) {
            account.setActive(this.account != null && this.account.getPublicAddress().equals(account.getPublicAddress()));
        }
        if (account == null && accounts.size() > 0) {
            EventPublisher.fireAccountChanged(accounts.get(0));
        }
        accountListView.setItems(FXCollections.observableArrayList(accounts));
    }

    private BigInteger getAccountBalance(String account) {
        BigInteger balance = null;
        try {
            balance = blockchainConnector.getBalance(account);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (balance != null) {
            return balance;
        }
        return BigInteger.ZERO;
    }

    public void openAddAccountDialog(MouseEvent mouseEvent) {
        addAccountDialog.open(mouseEvent);
    }
}
