package org.aion.wallet.ui.components.account;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.ui.events.EventPublisher;
import org.aion.wallet.util.BalanceUtils;
import org.aion.wallet.util.UIUtils;

import java.io.IOException;
import java.io.InputStream;

public class AccountCellItem extends ListCell<AccountDTO> {

    private static final String ICON_CONNECTED = "/org/aion/wallet/ui/components/icons/icon-connected-50.png";

    private static final String ICON_DISCONNECTED = "/org/aion/wallet/ui/components/icons/icon-disconnected-50.png";

    private static final String EDIT_ICON = "/org/aion/wallet/ui/components/icons/pencil-edit-button.png";

    private static final String CONFIRM_ICON = "/org/aion/wallet/ui/components/icons/icons8-checkmark-50.png";

    @FXML
    private TextField name;
    @FXML
    private TextField publicAddress;
    @FXML
    private TextField balance;
    @FXML
    private ImageView accountSelectButton;
    @FXML
    private ImageView editNameButton;

    private boolean nameInEditMode;

    public AccountCellItem() {
        loadFXML();
        publicAddress.setPrefWidth(575);

        name.addEventFilter(MouseEvent.MOUSE_PRESSED, ev -> {
            if (ev.getButton() == MouseButton.SECONDARY) {
                ev.consume();
                onNameFieldClicked();
            }
        });
    }

    private void loadFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AccountListViewItem.fxml"));
            loader.setController(this);
            loader.setRoot(this);
            loader.load();
            name.setOnKeyPressed(this::submitNameOnEnterPressed);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void submitNameOnEnterPressed(final KeyEvent event) {
        if (event.getCode().equals(KeyCode.ENTER)) {
            submitName();
        }
    }

    private void submitName() {
        name.setEditable(false);
        final AccountDTO accountDTO = getItem();
        accountDTO.setName(name.getText());
        EventPublisher.fireAccountChanged(accountDTO);
        updateItem(accountDTO, false);
        accountDTO.setActive(true);
        updateItem(accountDTO, false);
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

            publicAddress.setText(item.getPublicAddress());
            balance.setText(item.getBalance() + BalanceUtils.CCY_SEPARATOR + item.getCurrency());
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
        if(!nameInEditMode) {
            name.setEditable(true);
            name.getStyleClass().clear();
            name.getStyleClass().add("name-input-fields-selected");

            final InputStream resource = getClass().getResourceAsStream(CONFIRM_ICON);
            editNameButton.setImage(new Image(resource));

            name.requestFocus();
            nameInEditMode = true;
        }
        else {
            updateNameFieldOnSave();
        }
    }

    private void updateNameFieldOnSave() {
        if(name.getText() != null && getItem() != null && getItem().getName() != null) {
            name.getStyleClass().clear();
            name.getStyleClass().add("name-input-fields");

            final InputStream resource = getClass().getResourceAsStream(EDIT_ICON);
            editNameButton.setImage(new Image(resource));

            name.setEditable(false);
            nameInEditMode = false;
            submitName();
        }
    }
}
