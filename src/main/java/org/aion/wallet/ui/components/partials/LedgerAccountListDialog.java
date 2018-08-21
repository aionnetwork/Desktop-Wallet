package org.aion.wallet.ui.components.partials;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.InputEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.aion.api.log.LogEnum;
import org.aion.wallet.log.WalletLoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LedgerAccountListDialog implements Initializable {
    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    @FXML
    private VBox ledgerAccountList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        for(int i = 0; i < 10; i++) {
            RadioButton r = new RadioButton("test"+i);
            ledgerAccountList.getChildren().add(r);
        }
    }

    public void open(MouseEvent mouseEvent) {
        StackPane pane = new StackPane();
        Pane ledgerAccountListDialog;
        try {
            ledgerAccountListDialog = FXMLLoader.load(getClass().getResource("LedgerAccountListDialog.fxml"));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return;
        }
        pane.getChildren().add(ledgerAccountListDialog);
        Scene secondScene = new Scene(pane, ledgerAccountListDialog.getPrefWidth(), ledgerAccountListDialog.getPrefHeight());
        secondScene.setFill(Color.TRANSPARENT);

        Stage popup = new Stage();
        popup.setTitle("Ledger accounts");
        popup.setScene(secondScene);

        Node eventSource = (Node) mouseEvent.getSource();
        popup.setX(eventSource.getScene().getWindow().getX() + eventSource.getScene().getWidth() / 2 - ledgerAccountListDialog.getPrefWidth() / 2);
        popup.setY(eventSource.getScene().getWindow().getY() + eventSource.getScene().getHeight() / 2 - ledgerAccountListDialog.getPrefHeight() / 2);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.TRANSPARENT);

        popup.show();
    }

    public void close(InputEvent eventSource) {
        ((Node) eventSource.getSource()).getScene().getWindow().hide();
    }
}
