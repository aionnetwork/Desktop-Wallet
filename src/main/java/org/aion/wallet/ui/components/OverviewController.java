package org.aion.wallet.ui.components;

import com.google.common.eventbus.Subscribe;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.ui.components.partials.AddAccountDialog;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.EventPublisher;
import org.aion.wallet.ui.events.HeaderPaneButtonEvent;
import org.aion.wallet.ui.events.RefreshEvent;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class OverviewController extends AbstractController {

    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();
    @FXML
    private ListView<AccountDTO> accountListView;
    private AddAccountDialog addAccountDialog;
    private AccountDTO account;


    @Override
    public void internalInit(final URL location, final ResourceBundle resources) {
        addAccountDialog = new AddAccountDialog();
        reloadAccounts();
    }

    @Override
    protected void registerEventBusConsumer() {
        super.registerEventBusConsumer();
        EventBusFactory.getBus(HeaderPaneButtonEvent.ID).register(this);
        EventBusFactory.getBus(EventPublisher.ACCOUNT_CHANGE_EVENT_ID).register(this);
    }

    private void reloadAccounts() {
        final Task<List<AccountDTO>> getAccountsTask = getApiTask(o -> blockchainConnector.getAccounts(), null);
        runApiTask(
                getAccountsTask,
                evt -> reloadAccountObservableList(getAccountsTask.getValue()),
                getErrorEvent(throwable -> {}, getAccountsTask),
                getEmptyEvent()
        );
    }

    private void reloadAccountObservableList(List<AccountDTO> accounts) {
        for (AccountDTO account : accounts) {
            account.setActive(this.account != null && this.account.equals(account));
        }
        accountListView.setItems(FXCollections.observableArrayList(accounts));
    }

    @Subscribe
    private void handleAccountChanged(final AccountDTO account) {
        if (account.isActive()) {
            this.account = account;
        }
        reloadAccounts();
    }

    @Subscribe
    private void handleHeaderPaneButtonEvent(final HeaderPaneButtonEvent event) {
        if (event.getType().equals(HeaderPaneButtonEvent.Type.OVERVIEW)) {
            reloadAccounts();
            addAccountDialog.close();
        }
    }

    @Subscribe
    private void handleRefreshEvent(final RefreshEvent event){
        if (RefreshEvent.Type.OPERATION_FINISHED.equals(event.getType())){
            reloadAccounts();
        }
    }

    public void openAddAccountDialog(MouseEvent mouseEvent) {
        addAccountDialog.open(mouseEvent);
    }
}
