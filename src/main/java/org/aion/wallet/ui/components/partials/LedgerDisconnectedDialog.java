package org.aion.wallet.ui.components.partials;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import org.aion.api.log.LogEnum;
import org.aion.wallet.console.ConsoleManager;
import org.aion.wallet.log.WalletLoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;

public class LedgerDisconnectedDialog extends HardwareWalletDisconnectedDialog {
    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());
    public static final String ERROR_TEXT = "errorText";

    public void open(final MouseEvent mouseEvent, final Exception cause) {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setAutoFix(true);

        Pane ledgerDisconnectedDialog;
        try {
            ledgerDisconnectedDialog = FXMLLoader.load(getClass().getResource("LedgerDisconnectedDialog.fxml"));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            ConsoleManager.addLog(e.getMessage(), ConsoleManager.LogType.ACCOUNT, ConsoleManager.LogLevel.ERROR);
            return;
        }

        ((VBox) ledgerDisconnectedDialog.getChildren().get(0)).getChildren().stream()
                .filter(node -> ERROR_TEXT.equals(node.getId())).findFirst()
                .ifPresent(node -> ((Text) node).setText(cause.getMessage()));

        Node eventSource = (Node) mouseEvent.getSource();
        final double windowX = eventSource.getScene().getWindow().getX();
        final double windowY = eventSource.getScene().getWindow().getY();
        popup.setX(windowX + eventSource.getScene().getWidth() / 2 - ledgerDisconnectedDialog.getPrefWidth() / 2);
        popup.setY(windowY + eventSource.getScene().getHeight() / 2 - ledgerDisconnectedDialog.getPrefHeight() / 2);
        popup.getContent().addAll(ledgerDisconnectedDialog);
        popup.show(eventSource.getScene().getWindow());
    }
}
