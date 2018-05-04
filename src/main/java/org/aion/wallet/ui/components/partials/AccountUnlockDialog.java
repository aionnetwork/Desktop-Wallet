package org.aion.wallet.ui.components.partials;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.aion.api.log.AionLoggerFactory;
import org.aion.api.log.LogEnum;
import org.slf4j.Logger;

import java.io.IOException;

public class AccountUnlockDialog {
    private static final Logger log = AionLoggerFactory.getLogger(LogEnum.WLT.name());

    private final Popup popup = new Popup();

    public void open(MouseEvent mouseEvent) {
        StackPane pane = new StackPane();
        Pane accountUnlockDialog;
        try {
            accountUnlockDialog = FXMLLoader.load(getClass().getResource("AccountUnlockDialog.fxml"));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return;
        }
        pane.getChildren().add(accountUnlockDialog);
        Scene secondScene = new Scene(pane, accountUnlockDialog.getPrefWidth(), accountUnlockDialog.getPrefHeight());
        secondScene.setFill(Color.TRANSPARENT);

        Stage popup = new Stage();
        popup.setTitle("Import account");
        popup.setScene(secondScene);

        Node eventSource = (Node) mouseEvent.getSource();
        popup.setX(eventSource.getScene().getWindow().getX() + eventSource.getScene().getWidth() / 2 - accountUnlockDialog.getPrefWidth() / 2);
        popup.setY(eventSource.getScene().getWindow().getY() + eventSource.getScene().getHeight() / 2 - accountUnlockDialog.getPrefHeight() / 2);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.TRANSPARENT);

        popup.show();
    }
}
