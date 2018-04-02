package org.aion.wallet.ui.components.partials;

import com.google.common.eventbus.Subscribe;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import org.aion.wallet.connector.WalletBlockchainConnector;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.HeaderPaneButtonEvent;
import org.aion.wallet.util.BalanceFormatter;

import java.math.BigInteger;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AccountOverview implements Initializable{
    @FXML
    private ListView<String> accountListView;

    private AddAccountDialog addAccountDialog;

    private WalletBlockchainConnector walletBlockchainConnector = new WalletBlockchainConnector();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        addAccountDialog = new AddAccountDialog();
        registerEventBusConsumer();
        reloadWalletView();
    }

    private void registerEventBusConsumer() {
        EventBusFactory eventBusFactory = EventBusFactory.getInstance();
        eventBusFactory.getBus(HeaderPaneButtonEvent.ID).register(this);
    }

    @Subscribe
    private void handleHeaderPaneButtonEvent(HeaderPaneButtonEvent event) {
        reloadWalletView();
        addAccountDialog.close();
    }

    private void reloadWalletView() {
        ObservableList<String> accountListViewItems = accountListView.getItems();
        accountListViewItems.clear();
        List<String> accounts = walletBlockchainConnector.getAccounts();
        for(String account : accounts) {
            accountListViewItems.add(account + " - " + BalanceFormatter.formatBalance(getAccountBalance(account)));
        }
    }
    private BigInteger getAccountBalance(String account) {
        BigInteger balance = null;
        try {
            balance = walletBlockchainConnector.getBalance(account);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(balance != null) {
            return balance;
        }
        return BigInteger.ZERO;
    }

    public void openAddAccountDialog(MouseEvent mouseEvent) {
        addAccountDialog.open(mouseEvent);
    }
}
