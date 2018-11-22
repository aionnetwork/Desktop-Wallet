package org.aion.wallet.ui.components;

import com.google.common.eventbus.Subscribe;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.events.AccountEvent;
import org.aion.wallet.events.EventBusFactory;

import java.net.URL;
import java.util.EnumSet;
import java.util.ResourceBundle;

public class ReceiveController implements Initializable{

    private final Tooltip copiedTooltip = new Tooltip("Copied");

    @FXML
    public ImageView qrCode;

    @FXML
    private TextArea accountAddress;

    private AccountDTO account;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        registerEventBusConsumer();
        copiedTooltip.setAutoHide(true);
        accountAddress.setTooltip(copiedTooltip);
    }

    private void registerEventBusConsumer() {
        EventBusFactory.getBus(AccountEvent.ID).register(this);
    }

    @Subscribe
    private void handleAccountChanged(final AccountEvent event) {
        if (EnumSet.of(AccountEvent.Type.CHANGED, AccountEvent.Type.ADDED).contains(event.getType())) {
            AccountDTO receivedAccount = event.getPayload();
            if (receivedAccount.isActive()) {
                account = receivedAccount;
                accountAddress.setText(account.getPublicAddress());
                qrCode.setImage(SwingFXUtils.toFXImage(account.getQrCode(), null));
            }
        } else if (AccountEvent.Type.LOCKED.equals(event.getType())) {
            if (event.getPayload().equals(account)) {
                account = null;
                accountAddress.setText("");
                qrCode.setImage(null);
            }
        }
    }

    public void onCopyToClipBoard() {
        if (account != null && account.getPublicAddress() != null) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(account.getPublicAddress());
            clipboard.setContent(content);
            copiedTooltip.show(accountAddress.getScene().getWindow());
        }
    }

}
