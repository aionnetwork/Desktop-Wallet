package org.aion.wallet.ui.components;

import com.google.common.eventbus.Subscribe;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import org.aion.api.log.AionLoggerFactory;
import org.aion.api.log.LogEnum;
import org.aion.wallet.log.WalletLoggerFactory;
import org.aion.wallet.ui.events.EventBusFactory;
import org.aion.wallet.ui.events.TimerEvent;
import org.aion.wallet.util.DataUpdater;
import org.slf4j.Logger;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractController implements Initializable {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    @FXML
    private Node parent;

    private ExecutorService apiExecutor = Executors.newSingleThreadExecutor();

    @Override
    public final void initialize(final URL location, final ResourceBundle resources) {
        registerEventBusConsumer();
        internalInit(location, resources);
    }

    protected void registerEventBusConsumer() {
        EventBusFactory.getBus(DataUpdater.UI_DATA_REFRESH).register(this);
    }

    @Subscribe
    private void handleConnectivityStatusEvent(final TimerEvent event) {
        if (isInView()) {
            refreshView();
        }
    }

    protected final <T, R> Task<R> getApiTask(final Function<T, R> consumer, T param) {
        return new Task<>() {
            @Override
            protected R call() {
                return consumer.apply(param);
            }
        };
    }

    protected final <T> void runApiTask(final Task<T> executeAppTask, final EventHandler<WorkerStateEvent> successHandler, final EventHandler<WorkerStateEvent> errorHandler, final EventHandler<WorkerStateEvent> cancelledHandler) {
        executeAppTask.setOnSucceeded(successHandler);
        executeAppTask.setOnFailed(errorHandler);
        executeAppTask.setOnCancelled(cancelledHandler);

        apiExecutor.submit(executeAppTask);
    }

    protected final EventHandler<WorkerStateEvent> getEmptyEvent() {
        return event -> {
        };
    }

    protected final EventHandler<WorkerStateEvent> getErrorEvent(Consumer<Throwable> consumer, Throwable e) {
        return event -> {
            log.error(e.getMessage(), e);
            consumer.accept(e);
        };
    }

    protected final boolean isInView() {
        return parent.isVisible();
    }

    protected void refreshView() {}

    protected abstract void internalInit(final URL location, final ResourceBundle resources);
}
