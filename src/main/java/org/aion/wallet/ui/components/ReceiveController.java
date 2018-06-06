package org.aion.wallet.ui.components;

import com.google.common.eventbus.Subscribe;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.events.AccountEvent;
import org.aion.wallet.events.EventBusFactory;

import javax.imageio.ImageIO;
import java.net.URL;
import java.util.ResourceBundle;

public class ReceiveController implements Initializable{
    @FXML
    public ImageView qrCode;

    @FXML
    private TextArea accountAddress;

    private Tooltip copiedTooltip;

    private AccountDTO account;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        registerEventBusConsumer();
        copiedTooltip = new Tooltip();
        copiedTooltip.setText("Copied");
        copiedTooltip.setAutoHide(true);
        accountAddress.setTooltip(copiedTooltip);
    }

    private void registerEventBusConsumer() {
        EventBusFactory.getBus(AccountEvent.ID).register(this);
    }

    @Subscribe
    private void handleAccountChanged(final AccountEvent event) {
        if (AccountEvent.Type.CHANGED.equals(event.getType())) {
            account = event.getAccount();
            accountAddress.setText(account.getPublicAddress());

            Image image = SwingFXUtils.toFXImage(account.getQrCode(), null);
            qrCode.setImage(image);
            qrCode.setFitHeight(0);
            qrCode.setFitWidth(0);
            qrCode.setSmooth(true);

        } else if (AccountEvent.Type.LOCKED.equals(event.getType())) {
            if (event.getAccount().equals(account)) {
                account = null;
                accountAddress.setText("");
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
