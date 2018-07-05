package org.aion.wallet.ui.components.partials;

import javafx.scene.input.MouseEvent;

public class AboutPage {
    private final AboutDialog aboutDialog = new AboutDialog();

    public void openAboutDialog(MouseEvent mouseEvent) {
        aboutDialog.open(mouseEvent);
    }
}
