package org.aion.wallet.ui.components;

import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.dto.TransactionDTO;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.EventPublisher;
import org.aion.wallet.ui.events.HeaderPaneButtonEvent;
import org.aion.wallet.util.AddressUtils;
import org.aion.wallet.util.BalanceFormatter;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class HistoryController extends AbstractController {

    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();
    @FXML
    private TableView<TxRow> txListOverview;

    private AccountDTO account;


    protected void internalInit(final URL location, final ResourceBundle resources) {
        buildTableModel();
        refreshView();
    }

    @Subscribe
    private void handleHeaderPaneButtonEvent(final HeaderPaneButtonEvent event) {
        if (event.getType().equals(HeaderPaneButtonEvent.Type.HISTORY)) {
            refreshView();
        }
    }

    @Subscribe
    private void handleAccountChanged(final AccountDTO account) {
        this.account = account;
        refreshView();
    }

    protected void registerEventBusConsumer() {
        super.registerEventBusConsumer();
        EventBusFactory.getBus(HeaderPaneButtonEvent.ID).register(this);
        EventBusFactory.getBus(EventPublisher.ACCOUNT_CHANGE_EVENT_ID).register(this);
    }

    @Override
    protected void refreshView() {
        Platform.runLater(() -> {
            if (account == null) {
                return;
            }
            String me = account.getPublicAddress();
            List<TxRow> txs = blockchainConnector.getLatestTransactions(me).stream()
                    .map(t -> new TxRow(me, t))
                    .collect(Collectors.toList());
            txListOverview.setItems(FXCollections.observableList(txs));
        });
    }

    private void buildTableModel() {
        TableColumn<TxRow, String> typeCol = new TableColumn<>("Type");
        TableColumn<TxRow, String> addrCol = new TableColumn<>("Address");
        TableColumn<TxRow, String> valueCol = new TableColumn<>("Value");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        addrCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));

        txListOverview.getColumns().addAll(typeCol, addrCol, valueCol);
    }

    public static class TxRow {
        private final SimpleStringProperty type;
        private final SimpleStringProperty address;
        private final SimpleStringProperty value;

        private TxRow(final String requestingAddress, final TransactionDTO dto) {
            boolean fromRequestingAddress = AddressUtils.equals(requestingAddress, dto.getFrom());
            this.type = new SimpleStringProperty(fromRequestingAddress ? "to" : "from");
            this.address = new SimpleStringProperty(fromRequestingAddress ? dto.getTo() : dto.getFrom());
            this.value = new SimpleStringProperty(BalanceFormatter.formatBalance(dto.getValue()));
        }

        public String getType() {
            return type.get();
        }

        public void setType(final String type) {
            this.type.setValue(type);
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
