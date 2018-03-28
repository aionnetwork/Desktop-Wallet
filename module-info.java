module aion.wallet{

    requires aion.apiserver;
    requires aion.zero.impl;
    requires aion.log;
    requires aion.base;
    requires aion.mcf;
    requires javafx.fxml;
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
    requires java.desktop;
    requires slf4j.api;

    exports org.aion.wallet;
    exports org.aion.wallet.ui;
}
