package org.aion.wallet.ui;

import com.google.common.eventbus.Subscribe;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.aion.api.log.LogEnum;
import org.aion.wallet.connector.BlockchainConnector;
import org.aion.wallet.console.ConsoleManager;
import org.aion.wallet.dto.LightAppSettings;
import org.aion.wallet.events.*;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.ui.components.partials.FatalErrorDialog;
import org.aion.wallet.util.AionConstants;
import org.aion.wallet.util.DataUpdater;
import org.aion.wallet.util.OSUtils;
import org.slf4j.Logger;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainWindow extends Application {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private static final String TITLE = "Aion Wallet";
    private static final String MAIN_WINDOW_FXML = "MainWindow.fxml";
    private static final String AION_LOGO = "components/icons/aion-icon.png";
    private static final String AION_UI_DIR = System.getProperty("user.dir");
    private static final String AION_EXECUTABLE = "aion_ui.sh";
    private static final BlockchainConnector CONNECTOR = BlockchainConnector.getInstance();

    private final Map<HeaderPaneButtonEvent.Type, Node> panes = new HashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private FatalErrorDialog fatalErrorDialog;
    private double xOffset;
    private double yOffset;
    private Stage stage;
    private Scene scene;
    private IdleMonitor idleMonitor;
    private Duration lockDelayDuration;

    @Override
    public void start(final Stage stage) throws IOException {
        ConsoleManager.addLog("Welcome!", ConsoleManager.LogType.SETTINGS);
        this.stage = stage;
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.getIcons().add(new Image(getClass().getResourceAsStream(AION_LOGO)));

        if(OSUtils.isMac()) {
            java.awt.Image image = new ImageIcon(MainWindow.class.getResource(AION_LOGO)).getImage();
            try {
                Class<?> appClass = Class.forName("com.apple.eawt.Application");
                Object app = appClass.getMethod("getApplication").invoke(appClass);
                Method setter = app.getClass().getMethod("setDockIconImage", java.awt.Image.class);
                setter.invoke(app, image);
            } catch (Throwable t) {
                System.err.println("Cannot set Mac tray icon due to error: " + t);
            }
        }

        registerEventBusConsumer();

        Parent root = FXMLLoader.load(getClass().getResource(MAIN_WINDOW_FXML));
        root.setOnMousePressed(this::handleMousePressed);
        root.setOnMouseDragged(this::handleMouseDragged);

        scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setOnCloseRequest(t -> shutDown(false));

        stage.setTitle(TITLE);
        stage.setScene(scene);
        stage.show();

        panes.put(HeaderPaneButtonEvent.Type.OVERVIEW, scene.lookup("#overviewPane"));
        panes.put(HeaderPaneButtonEvent.Type.SEND, scene.lookup("#sendPane"));
        panes.put(HeaderPaneButtonEvent.Type.RECEIVE, scene.lookup("#receivePane"));
        panes.put(HeaderPaneButtonEvent.Type.CONTRACTS, scene.lookup("#contractsPane"));
        panes.put(HeaderPaneButtonEvent.Type.HISTORY, scene.lookup("#historyPane"));
        panes.put(HeaderPaneButtonEvent.Type.SETTINGS, scene.lookup("#settingsPane"));

        scheduler.scheduleAtFixedRate(
                new DataUpdater(),
                AionConstants.BLOCK_MINING_TIME_MILLIS,
                3 * AionConstants.BLOCK_MINING_TIME_MILLIS,
                TimeUnit.MILLISECONDS
        );
        final LightAppSettings settings = CONNECTOR.getSettings();
        lockDelayDuration = Duration.seconds(
                computeDelay(settings.getLockTimeout(),
                        settings.getLockTimeoutMeasurementUnit())
        );
        registerIdleMonitor();

        fatalErrorDialog = new FatalErrorDialog();
    }


    private long computeDelay(final int lockTimeOut, final String lockTimeOutMeasurementUnit) {
        if (lockTimeOutMeasurementUnit == null) {
            return 60;
        }
        switch (lockTimeOutMeasurementUnit) {
            case "seconds":
                return lockTimeOut;
            case "minutes":
                return lockTimeOut * 60;
            case "hours":
                return lockTimeOut * 3600;
            default:
                return 60;
        }
    }

    private void registerIdleMonitor() {
        if (scene == null || lockDelayDuration == null) {
            return;
        }
        if (idleMonitor != null) {
            idleMonitor.stopMonitoring();
            idleMonitor = null;
        }
        idleMonitor = new IdleMonitor(lockDelayDuration, CONNECTOR::lockAll, CONNECTOR::unlockConnection);
        idleMonitor.register(scene, Event.ANY);
    }

    @Subscribe
    private void handleSettingsChanged(final SettingsEvent event) {
        if (SettingsEvent.Type.CHANGED.equals(event.getType())) {
            final LightAppSettings settings = event.getSettings();
            if (settings != null) {
                lockDelayDuration = Duration.seconds(computeDelay(settings.getLockTimeout(), settings.getLockTimeoutMeasurementUnit()));
                registerIdleMonitor();
            }
        }
    }

    private void registerEventBusConsumer() {
        EventBusFactory.getBus(WindowControlsEvent.ID).register(this);
        EventBusFactory.getBus(HeaderPaneButtonEvent.ID).register(this);
        EventBusFactory.getBus(SettingsEvent.ID).register(this);
        EventBusFactory.getBus(ErrorEvent.ID).register(this);
    }

    @Subscribe
    private void handleWindowControlsEvent(final WindowControlsEvent event) {
        switch (event.getType()) {
            case MINIMIZE:
                minimize(event);
                break;
            case RESTART:
                shutDown(true);
                break;
            case CLOSE:
                shutDown(false);
                break;
        }
    }

    @Subscribe
    private void handleHeaderPaneButtonEvent(final HeaderPaneButtonEvent event) {
        if (stage.getScene() == null) {
            return;
        }
        for (Map.Entry<HeaderPaneButtonEvent.Type, Node> entry : panes.entrySet()) {
            if (event.getType().equals(entry.getKey())) {
                entry.getValue().setVisible(true);
            } else {
                entry.getValue().setVisible(false);
            }
        }
    }

    @Subscribe
    private void handleErrorEvent(final ErrorEvent errorEvent) {
        Platform.runLater(() -> fatalErrorDialog.open(scene.getRoot()));
    }

    private void minimize(final WindowControlsEvent event) {
        ((Stage) event.getSource().getScene().getWindow()).setIconified(true);
    }

    private void shutDown(final boolean restart) {
        Platform.exit();
        BlockchainConnector.getInstance().close();
        scheduler.shutdown();
        if (restart) {
            restartApplication();
        }
        Executors.newSingleThreadExecutor().submit(() -> System.exit(0));
    }

    private void restartApplication() {
        final String executable = AION_UI_DIR + File.separator + AION_EXECUTABLE;

        final ArrayList<String> command = new ArrayList<>();
        command.add(executable);

        final ProcessBuilder builder = new ProcessBuilder(command);
        try {
            builder.start();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
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
