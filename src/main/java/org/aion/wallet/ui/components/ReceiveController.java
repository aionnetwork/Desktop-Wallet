package org.aion.wallet.ui.components;

import com.google.common.eventbus.Subscribe;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.EventPublisher;

import java.net.URL;
import java.util.ResourceBundle;

public class ReceiveController implements Initializable{

    @FXML
    private TextArea accountAddress;

    private Tooltip copiedTooltip;

    private AccountDTO accountDTO;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        registerEventBusConsumer();
        copiedTooltip = new Tooltip();
        copiedTooltip.setText("Copied");
        copiedTooltip.setAutoHide(true);
        accountAddress.setTooltip(copiedTooltip);
    }

    private void registerEventBusConsumer() {
        EventBusFactory.getBus(EventPublisher.ACCOUNT_CHANGE_EVENT_ID).register(this);
    }

    @Subscribe
    private void handleAccountChanged(AccountDTO accountDTO) {
        this.accountDTO = accountDTO;

        accountAddress.setText(accountDTO.getPublicAddress());
    }

    public void onCopyToClipBoard(MouseEvent mouseEvent) {
        if(accountDTO != null && accountDTO.getPublicAddress() != null) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(accountDTO.getPublicAddress());
            clipboard.setContent(content);

            copiedTooltip.show(accountAddress.getScene().getWindow());
        }
    }

}
