package org.aion.wallet.ui;

import com.google.common.eventbus.Subscribe;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.aion.log.AionLoggerFactory;
import org.aion.log.LogEnum;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.HeaderPaneButtonEvent;
import org.aion.wallet.ui.events.WindowControlsEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class MainWindow extends Application {

    private static final Logger log = AionLoggerFactory.getLogger(LogEnum.WLT.name());

    private static final String TITLE = "Aion Wallet";
    private static final String MAIN_WINDOW_FXML = "MainWindow.fxml";
    private static final String AION_LOGO = "components/icons/aion_logo.png";

    private final Map<HeaderPaneButtonEvent.Type, Node> panes = new HashMap<>();

    private double xOffset;
    private double yOffset;
    private Stage stage;

    @Override
    public void start(final Stage stage) throws IOException {
        this.stage = stage;
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.getIcons().add(new Image(getClass().getResourceAsStream(AION_LOGO)));

        registerEventBusConsumer();

        Parent root = FXMLLoader.load(getClass().getResource(MAIN_WINDOW_FXML));
        root.setOnMousePressed(this::handleMousePressed);
        root.setOnMouseDragged(this::handleMouseDragged);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);

        stage.setOnCloseRequest(t -> shutDown());

        stage.setTitle(TITLE);
        stage.setScene(scene);
        stage.show();

        panes.put(HeaderPaneButtonEvent.Type.HOME, scene.lookup("#homePane"));
        panes.put(HeaderPaneButtonEvent.Type.SEND, scene.lookup("#sendPane"));
        panes.put(HeaderPaneButtonEvent.Type.HISTORY, scene.lookup("#historyPane"));
    }

    private void registerEventBusConsumer() {
        final EventBusFactory eventBusFactory = EventBusFactory.getInstance();
        eventBusFactory.getBus(WindowControlsEvent.ID).register(this);
        eventBusFactory.getBus(HeaderPaneButtonEvent.ID).register(this);
    }

    @Subscribe
    private void handleWindowControlsEvent(final WindowControlsEvent event) {
        switch (event.getType()) {
            case MINIMIZE:
                minimize(event);
                break;
            case CLOSE:
                shutDown();
                break;
        }
    }

    @Subscribe
    private void handleHeaderPaneButtonEvent(final HeaderPaneButtonEvent event) {
        if(stage.getScene() == null) {
            return;
        }
        log.debug(event.getType().toString());
        // todo: refactor by adding a view controller
        for(Map.Entry<HeaderPaneButtonEvent.Type, Node> entry: panes.entrySet()) {
            if(event.getType().equals(entry.getKey())) {
                entry.getValue().setVisible(true);
            } else {
                entry.getValue().setVisible(false);
            }
        }
    }

    private void minimize(final WindowControlsEvent event) {
        ((Stage) event.getSource().getScene().getWindow()).setIconified(true);
    }

    private void shutDown() {
        Platform.exit();
        Executors.newSingleThreadExecutor().submit(() -> System.exit(0));
    }

    private void handleMousePressed(final MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    private void handleMouseDragged(final MouseEvent event) {
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }
}
