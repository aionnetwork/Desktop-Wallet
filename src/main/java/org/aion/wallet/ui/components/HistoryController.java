package org.aion.wallet.ui.components;

import com.google.common.eventbus.Subscribe;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.aion.base.util.TypeConverter;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.dto.TransactionDTO;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.EventPublisher;
import org.aion.wallet.ui.events.HeaderPaneButtonEvent;
import org.aion.wallet.util.AddressUtils;
import org.aion.wallet.util.BalanceUtils;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class HistoryController extends AbstractController {

    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();
    @FXML
    private TableView<TxRow> txTable;

    private AccountDTO account;


    protected void internalInit(final URL location, final ResourceBundle resources) {
        buildTableModel();
        reloadWalletView();
    }

    @Subscribe
    private void handleHeaderPaneButtonEvent(final HeaderPaneButtonEvent event) {
        if (event.getType().equals(HeaderPaneButtonEvent.Type.HISTORY)) {
            reloadWalletView();
        }
    }

    @Subscribe
    private void handleAccountChanged(final AccountDTO account) {
        this.account = account;
        if (isInView()) {
            reloadWalletView();
        }
    }

    protected void registerEventBusConsumer() {
        super.registerEventBusConsumer();
        EventBusFactory.getBus(HeaderPaneButtonEvent.ID).register(this);
        EventBusFactory.getBus(EventPublisher.ACCOUNT_CHANGE_EVENT_ID).register(this);
    }

    private void reloadWalletView() {
        if (account == null) {
            return;
        }
        final Task<List<TxRow>> getTransactionsTask = getApiTask(
                address -> blockchainConnector.getLatestTransactions(address).stream()
                        .map(t -> new TxRow(address, t))
                        .collect(Collectors.toList()), account.getPublicAddress()
        );

        runApiTask(
                getTransactionsTask,
                event -> txTable.setItems(FXCollections.observableList(getTransactionsTask.getValue())),
                getEmptyEvent(),
                getEmptyEvent()
        );
    }

    private void buildTableModel() {
        TableColumn<TxRow, String> typeCol = new TableColumn<>("Type");
        TableColumn<TxRow, String> nameCol = new TableColumn<>("Name");
        TableColumn<TxRow, String> addrCol = new TableColumn<>("Address");
        TableColumn<TxRow, String> valueCol = new TableColumn<>("Value");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        addrCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        typeCol.prefWidthProperty().bind(txTable.widthProperty().multiply(0.08));
        nameCol.prefWidthProperty().bind(txTable.widthProperty().multiply(0.15));
        addrCol.prefWidthProperty().bind(txTable.widthProperty().multiply(0.63));
        valueCol.prefWidthProperty().bind(txTable.widthProperty().multiply(0.14));

        txTable.getColumns().addAll(typeCol, nameCol, addrCol, valueCol);
    }

    public class TxRow {

        private static final String TO = "to";
        private static final String FROM = "from";

        private final SimpleStringProperty type;
        private final SimpleStringProperty name;
        private final SimpleStringProperty address;
        private final SimpleStringProperty value;

        private TxRow(final String requestingAddress, final TransactionDTO dto) {
            final AccountDTO fromAccount = blockchainConnector.getAccount(dto.getFrom());
            final AccountDTO toAccount = blockchainConnector.getAccount(dto.getTo());
            final String balance = BalanceUtils.formatBalance(dto.getValue());
            boolean isFromTx = AddressUtils.equals(requestingAddress, fromAccount.getPublicAddress());
            this.type = new SimpleStringProperty(isFromTx ? TO : FROM);
            this.name = new SimpleStringProperty(isFromTx ? toAccount.getName() : fromAccount.getName());
            this.address = new SimpleStringProperty(isFromTx ? TypeConverter.toJsonHex(toAccount.getPublicAddress())
                    : TypeConverter.toJsonHex(fromAccount.getPublicAddress()));
            this.value = new SimpleStringProperty(balance);
        }

        public String getType() {
            return type.get();
        }

        public void setType(final String type) {
            this.type.setValue(type);
        }

        public String getName() {
            return name.get();
        }

        public void setName(final String name) {
            this.name.setValue(name);
        }

        public String getAddress() {
            return address.get();
        }

        public void setAddress(final String address) {
            this.address.setValue(address);
        }

        public String getValue() {
            return value.get();
        }

        public void setValue(final String value) {
            this.value.setValue(value);
        }

    }
}
