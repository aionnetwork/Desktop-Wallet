module aion.wallet{

    requires aion.apiserver;
    requires aion.api.client;
    requires aion.crypto;
    requires aion.zero;
    requires aion.zero.impl;
    requires aion.log;
    requires aion.base;
    requires aion.mcf;
    requires aion.evtmgr.impl;
    requires javafx.fxml;
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.swing;
    requires java.desktop;
    requires slf4j.api;
    requires guava;

    exports org.aion.wallet;
    exports org.aion.wallet.ui;
}
