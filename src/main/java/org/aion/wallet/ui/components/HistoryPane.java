package org.aion.wallet.ui.components;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.aion.base.type.Address;
import org.aion.base.util.ByteUtil;
import org.aion.base.util.TypeConverter;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.connector.WalletBlockchainConnector;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static org.aion.wallet.util.BalanceFormatter.WEI_MULTIPLIER;

public class HistoryPane implements Initializable {

    private final BlockchainConnector blockchainConnector = new WalletBlockchainConnector();
    @FXML
    private TableView<TxRow> txListOverview;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TableColumn typeCol = new TableColumn("Type");
        TableColumn addrCol = new TableColumn("Addr");
        TableColumn valueCol = new TableColumn("Value");
        typeCol.setCellValueFactory(new PropertyValueFactory<TxRow, String>("type"));
        addrCol.setCellValueFactory(new PropertyValueFactory<TxRow, String>("address"));
        valueCol.setCellValueFactory(new PropertyValueFactory<TxRow, String>("value"));

        txListOverview.getColumns().addAll(typeCol, addrCol, valueCol);
        reloadHistory();
    }

    private void reloadHistory() {
        Address me = Address.wrap(ByteUtil.hexStringToBytes(blockchainConnector.getAccounts().get(0)));
        List<TxRow> txs = blockchainConnector.getTransactions(me).stream().map(t -> {
            TxRow row = new TxRow();
            row.setAddress(me.equals(t.getFrom()) ? t.getTo().toString() : t.getFrom().toString());
            row.setType(me.equals(t.getFrom()) ? "to" : "from");

            BigInteger intVal = TypeConverter.StringHexToBigInteger(TypeConverter.toJsonHex(t.getValue()));
            row.setValue(new BigDecimal(intVal).divide(WEI_MULTIPLIER, RoundingMode.HALF_UP).toString());
            return row;
        }).collect(Collectors.toList());
        txListOverview.setItems(FXCollections.observableList(txs));
    }

    public static class TxRow {
        private SimpleStringProperty type;
        private SimpleStringProperty address;
        private SimpleStringProperty value;

        private TxRow() {

        }

        public String getType() {
            return type.get();
        }

        public void setType(String type) {
            this.type = new SimpleStringProperty(type);
        }

        public String getAddress() {
            return address.get();
        }

        public void setAddress(String address) {
            this.address = new SimpleStringProperty(address);
        }

        public String getValue() {
            return value.get();
        }

        public void setValue(String value) {
            this.value = new SimpleStringProperty(value);
        }

    }
}