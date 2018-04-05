package org.aion.wallet.ui.components.account;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.ui.events.EventPublisher;
import org.aion.wallet.util.UIUtils;

import java.io.IOException;

public class AccountCellItem extends ListCell<AccountDTO> {
    @FXML
    private TextField publicAddress;
    @FXML
    private TextField balance;
    @FXML
    private ImageView connectedImage;
    @FXML
    private ImageView disconnectedImage;

    public AccountCellItem() {
        loadFXML();
    }

    private void loadFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AccountListViewItem.fxml"));
            loader.setController(this);
            loader.setRoot(this);
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void updateItem(AccountDTO item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
            setContentDisplay(ContentDisplay.TEXT_ONLY);
        } else {
            publicAddress.setText(item.getPublicAddress());
            UIUtils.setWidth(publicAddress);
            balance.setText(item.getBalance() + item.getCurrency());
            UIUtils.setWidth(balance);
            connectedImage.setVisible(item.getActive());
            disconnectedImage.setVisible(!item.getActive());
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }
    }

    public void doNothing(){}

    public void onDisconnectedClicked() {
        EventPublisher.fireAccountChanged(this.getItem());
    }
}
