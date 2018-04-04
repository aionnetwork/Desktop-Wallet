package org.aion.wallet.ui.components.account;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.ui.events.EventPublisher;

import java.io.IOException;

public class AccountCellItem extends ListCell<AccountDTO> {
    @FXML
    private Label publicAddress;
    @FXML
    private Label balance;
    @FXML
    private ImageView connectedImage;
    @FXML
    private ImageView disconnectedImage;

    public AccountCellItem() {
        loadFXML();
    }

    private void loadFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../../components/account/AccountListViewItem.fxml"));
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
            balance.setText(item.getBalance() + " AION");
            connectedImage.setVisible(item.getActive());
            disconnectedImage.setVisible(!item.getActive());
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }
    }

    public void onDisconnectedClicked() {
        EventPublisher.fireAccountChanged(this.getItem());
    }
}
