package org.aion.wallet.ui.components.partials;

import javafx.scene.Node;
import javafx.scene.input.InputEvent;
import javafx.scene.input.MouseEvent;

public abstract class HardwareWalletDisconnectedDialog {

    public abstract void open(final MouseEvent mouseEvent, final Exception cause);

    public final void close(final InputEvent eventSource) {
        ((Node) eventSource.getSource()).getScene().getWindow().hide();
    }
}
