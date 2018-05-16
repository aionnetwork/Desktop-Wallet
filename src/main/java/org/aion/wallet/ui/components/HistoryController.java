package org.aion.wallet.ui.components;

import com.google.common.eventbus.Subscribe;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.*;
import org.aion.api.log.LogEnum;
import org.aion.log.AionLoggerFactory;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.dto.TransactionDTO;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.EventPublisher;
import org.aion.wallet.ui.events.HeaderPaneButtonEvent;
import org.aion.wallet.util.AddressUtils;
import org.aion.wallet.util.BalanceUtils;
import org.aion.wallet.util.URLManager;
import org.slf4j.Logger;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class HistoryController extends AbstractController {

    private static final Logger log = AionLoggerFactory.getLogger(LogEnum.WLT.name());

    private static final String COPY_MENU = "Copy";

    private static final String LINK_STYLE = "link-style";

    private final BlockchainConnector blockchainConnector = BlockchainConnector.getInstance();
    @FXML
    private TableView<TxRow> txTable;

    private AccountDTO account;


    protected void internalInit(final URL location, final ResourceBundle resources) {
        buildTableModel();
        setEventHandlers();
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
                        .collect(Collectors.toList()),
                account.getPublicAddress()
        );

        runApiTask(
                getTransactionsTask,
                event -> txTable.setItems(FXCollections.observableList(getTransactionsTask.getValue())),
                getEmptyEvent(),
                getEmptyEvent()
        );
    }

    private void buildTableModel() {
        final TableColumn<TxRow, String> typeCol = getTableColumn("Type", "type", 0.08);
        final TableColumn<TxRow, String> nameCol = getTableColumn("Name", "name", 0.09);
        final TableColumn<TxRow, String> addressCol = getTableColumn("Address", "address", 0.36);
        final TableColumn<TxRow, String> hashCol = getTableColumn("Tx Hash", "txHash", 0.36);
        final TableColumn<TxRow, String> valueCol = getTableColumn("Value", "value", 0.11);

        hashCol.setCellFactory(column -> new TransactionHashCell());

        txTable.getColumns().addAll(Arrays.asList(typeCol, nameCol, addressCol, hashCol, valueCol));
    }

    private TableColumn<TxRow, String> getTableColumn(final String header, final String property, final double sizePercent) {
        final TableColumn<TxRow, String> valueCol = new TableColumn<>(header);
        valueCol.setCellValueFactory(new PropertyValueFactory<>(property));
        valueCol.prefWidthProperty().bind(txTable.widthProperty().multiply(sizePercent));
        return valueCol;
    }

    private void setEventHandlers() {
        txTable.setOnKeyPressed(new KeyTableCopyEventHandler());
        txTable.setOnMouseClicked(new MouseLinkEventHandler());

        ContextMenu menu = new ContextMenu();
        final MenuItem copyItem = new MenuItem(COPY_MENU);
        copyItem.setOnAction(new ContextMenuTableCopyEventHandler(txTable));
        menu.getItems().add(copyItem);
        txTable.setContextMenu(menu);
    }

    private static class KeyTableCopyEventHandler extends TableCopyEventHandler<KeyEvent> {
        private final KeyCodeCombination copyKeyCodeCombination = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY);

        public void handle(final KeyEvent keyEvent) {
            if (copyKeyCodeCombination.match(keyEvent)) {
                if (keyEvent.getSource() instanceof TableView) {
                    copySelectionToClipboard((TableView<?>) keyEvent.getSource());
                    keyEvent.consume();
                }
            }
        }
    }

    private static class ContextMenuTableCopyEventHandler extends TableCopyEventHandler<ActionEvent> {


        private final TableView<TxRow> txTable;

        public ContextMenuTableCopyEventHandler(final TableView<TxRow> txTable) {
            this.txTable = txTable;
        }

        public void handle(final ActionEvent keyEvent) {
            copySelectionToClipboard(txTable);
            keyEvent.consume();
        }
    }

    private static abstract class TableCopyEventHandler<T extends Event> implements EventHandler<T> {

        private static final char TAB = '\t';
        private static final char NEWLINE = '\n';

        protected final void copySelectionToClipboard(TableView<?> table) {
            StringBuilder clipboardString = new StringBuilder();
            ObservableList<TablePosition> positionList = table.getSelectionModel().getSelectedCells();
            int prevRow = -1;
            for (TablePosition position : positionList) {
                int row = position.getRow();
                int col = position.getColumn();
                Object cell = table.getColumns().get(col).getCellData(row);
                if (cell == null) {
                    cell = "";
                }
                if (prevRow == row) {
                    clipboardString.append(TAB);
                } else if (prevRow != -1) {
                    clipboardString.append(NEWLINE);
                }
                String text = cell.toString();
                clipboardString.append(text);
                prevRow = row;
            }
            final ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(clipboardString.toString());
            Clipboard.getSystemClipboard().setContent(clipboardContent);
        }
    }

    private static class MouseLinkEventHandler implements EventHandler<MouseEvent> {

        @Override
        public void handle(final MouseEvent mouseEvent) {
            if (MouseEvent.MOUSE_CLICKED.equals(mouseEvent.getEventType()) && MouseButton.PRIMARY.equals(mouseEvent.getButton())) {
                if (mouseEvent.getSource() instanceof TableView) {
                    redirect((TableView<?>) mouseEvent.getSource());
                    mouseEvent.consume();
                }
            }
        }

        private void redirect(final TableView<?> table) {
            ObservableList<TablePosition> positionList = table.getSelectionModel().getSelectedCells();
            for (TablePosition position : positionList) {
                int row = position.getRow();
                int col = position.getColumn();
                if (table.getColumns().get(col).getText().equals("Tx Hash")) {
                    Object cell = table.getColumns().get(col).getCellData(row);
                    URLManager.openTransaction(cell.toString());
                }
            }
        }
    }

    public class TxRow {

        private static final String TO = "to";
        private static final String FROM = "from";

        private final TransactionDTO transaction;
        private final SimpleStringProperty type;
        private final SimpleStringProperty name;
        private final SimpleStringProperty address;
        private final SimpleStringProperty value;

        private final SimpleStringProperty txHash;

        private TxRow(final String requestingAddress, final TransactionDTO dto) {
            transaction = dto;
            final AccountDTO fromAccount = blockchainConnector.getAccount(dto.getFrom());
            final AccountDTO toAccount = blockchainConnector.getAccount(dto.getTo());
            final String balance = BalanceUtils.formatBalance(dto.getValue());
            boolean isFromTx = AddressUtils.equals(requestingAddress, fromAccount.getPublicAddress());
            this.type = new SimpleStringProperty(isFromTx ? TO : FROM);
            this.name = new SimpleStringProperty(isFromTx ? toAccount.getName() : fromAccount.getName());
            this.address = new SimpleStringProperty(isFromTx ? toAccount.getPublicAddress() : fromAccount.getPublicAddress());
            this.value = new SimpleStringProperty(balance);
            this.txHash = new SimpleStringProperty(dto.getHash());
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

        public String getTxHash() {
            return txHash.get();
        }

        public void setHash(final String hash) {
            txHash.setValue(hash);
        }

        public TransactionDTO getTransaction() {
            return transaction;
        }
    }

    private class TransactionHashCell extends TableCell<TxRow, String> {
        @Override
        protected void updateItem(final String item, final boolean empty) {
            super.updateItem(item, empty);

            setText(empty ? "" : item);

            getStyleClass().clear();
            updateStyles(empty ? null : item);
        }

        private void updateStyles(final String item) {
            if (item == null) {
                return;
            }
            getStyleClass().add(LINK_STYLE);
        }
    }
}
