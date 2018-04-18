package org.aion.wallet.ui.components.account;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import org.aion.base.util.TypeConverter;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.ui.events.EventPublisher;
import org.aion.wallet.util.UIUtils;

import java.io.IOException;
import java.io.InputStream;

public class AccountCellItem extends ListCell<AccountDTO> {

    private static final String ICON_CONNECTED = "/org/aion/wallet/ui/components/icons/icon-connected-50.png";

    private static final String ICON_DISCONNECTED = "/org/aion/wallet/ui/components/icons/icon-disconnected-50.png";

    @FXML
    private TextField name;
    @FXML
    private TextField publicAddress;
    @FXML
    private TextField balance;
    @FXML
    private ImageView accountSelectButton;
    @FXML
    private ImageView enterNameForSave;

    public AccountCellItem() {
        loadFXML();
        publicAddress.setPrefWidth(575);
    }

    private void loadFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AccountListViewItem.fxml"));
            loader.setController(this);
            loader.setRoot(this);
            loader.load();
            name.setOnKeyPressed(this::submitName);
            name.setOnMouseExited(this::submitName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void submitName(final KeyEvent event) {
        if (event.getCode().equals(KeyCode.ENTER)) {
            name.setEditable(false);
            final AccountDTO accountDTO = getItem();
            accountDTO.setName(name.getText());
            EventPublisher.fireAccountChanged(accountDTO);
            updateItem(accountDTO, false);
            enterNameForSave.setVisible(false);
            accountDTO.setActive(true);
            updateItem(accountDTO, false);
        }
    }

    private void submitName(MouseEvent event) {
        if(name.getText() != null && getItem() != null && getItem().getName() != null
        && name.getText().equals(getItem().getName())) {
            enterNameForSave.setVisible(false);
            name.getStyleClass().clear();
            name.getStyleClass().add("name-input-fields");
        }
    }

    @Override
    protected void updateItem(AccountDTO item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setText(null);
            setContentDisplay(ContentDisplay.TEXT_ONLY);
        } else {
            name.setText(item.getName());
            UIUtils.setWidth(name);
            ContextMenu contextMenu = new ContextMenu();
            MenuItem edit = new MenuItem("Edit");
            contextMenu.getItems().add(edit);
            name.setContextMenu(contextMenu);
            edit.setOnAction(event -> name.setEditable(true));

            publicAddress.setText(TypeConverter.toJsonHex(item.getPublicAddress()));
            balance.setText(item.getBalance() + item.getCurrency());
            UIUtils.setWidth(balance);

            if (item.isActive()) {
                final InputStream resource = getClass().getResourceAsStream(ICON_CONNECTED);
                accountSelectButton.setImage(new Image(resource));
            } else {
                final InputStream resource = getClass().getResourceAsStream(ICON_DISCONNECTED);
                accountSelectButton.setImage(new Image(resource));
            }

            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }
    }

    public void onDisconnectedClicked() {
        EventPublisher.fireAccountChanged(this.getItem());
    }

    public void onNameFieldClicked() {
        name.setEditable(true);
        name.getStyleClass().clear();
        name.getStyleClass().add("name-input-fields-selected");
        enterNameForSave.setVisible(true);
    }
}
