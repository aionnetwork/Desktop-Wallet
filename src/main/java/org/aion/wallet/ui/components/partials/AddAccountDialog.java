package org.aion.wallet.ui.components.partials;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.aion.mcf.account.Keystore;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.HeaderPaneButtonEvent;

public class AddAccountDialog {

    @FXML
    private TextField newPassword;

    @FXML
    private TextField retypedPassword;

    public void createAccount() {
        if (newPassword.getText() != null && retypedPassword.getText() != null && newPassword.getText().equals(retypedPassword.getText())) {
            Keystore.create(newPassword.getText());
        }
        EventBusFactory.getInstance().getBus(HeaderPaneButtonEvent.ID).post(new HeaderPaneButtonEvent(HeaderPaneButtonEvent.Type.HOME));
    }

    public void unlockAccount() {

    }
}
