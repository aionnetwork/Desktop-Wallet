package org.aion.wallet.ui.components.partials;

import com.google.common.eventbus.Subscribe;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.HeaderPaneButtonEvent;
import org.aion.wallet.util.BalanceFormatter;

import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AccountOverview implements Initializable{
    @FXML
    private ListView<AccountDTO> accountListView;

    private AddAccountDialog addAccountDialog;

    private  final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();

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
        List<AccountDTO> accountListViewItems = new ArrayList<>();
        List<String> accounts = blockchainConnector.getAccounts();
        for(String account : accounts) {
            AccountDTO dto = new AccountDTO();
            dto.setPublicAddress(account);
            dto.setBalance(BalanceFormatter.formatBalance(getAccountBalance(account)));
            accountListViewItems.add(dto);
        }

        accountListViewItems.get(0).setActive(true);
        accountListView.setItems(FXCollections.observableArrayList(accountListViewItems));
    }
    private BigInteger getAccountBalance(String account) {
        BigInteger balance = null;
        try {
            balance = blockchainConnector.getBalance(account);
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
