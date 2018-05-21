package org.aion.wallet.ui.components.partials;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.InputEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.aion.api.log.AionLoggerFactory;
import org.aion.api.log.LogEnum;
import org.slf4j.Logger;

import java.io.IOException;

public class MnemonicDialog {
    private static final Logger log = AionLoggerFactory.getLogger(LogEnum.WLT.name());

    public void open(InputEvent mouseEvent) {
        StackPane pane = new StackPane();
        Pane mnemonicDialog;
        try {
            mnemonicDialog = FXMLLoader.load(getClass().getResource("MnemonicDialog.fxml"));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return;
        }
        pane.getChildren().add(mnemonicDialog);
        Scene secondScene = new Scene(pane, mnemonicDialog.getPrefWidth(), mnemonicDialog.getPrefHeight());
        secondScene.setFill(Color.TRANSPARENT);

        Stage popup = new Stage();
        popup.setTitle("Mnemonic");
        popup.setScene(secondScene);

        Node eventSource = (Node) mouseEvent.getSource();
        popup.setX(eventSource.getScene().getWindow().getX() + eventSource.getScene().getWidth() / 2 - mnemonicDialog.getPrefWidth() / 2);
        popup.setY(eventSource.getScene().getWindow().getY() + eventSource.getScene().getHeight() / 2 - mnemonicDialog.getPrefHeight() / 2);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.TRANSPARENT);

        popup.show();
    }
}
