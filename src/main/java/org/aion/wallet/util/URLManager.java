package org.aion.wallet.util;

import org.aion.api.log.LogEnum;
import org.aion.log.AionLoggerFactory;
import org.slf4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class URLManager {
    private static final Logger log = AionLoggerFactory.getLogger(LogEnum.WLT.name());

    public static void openURL(String URL) {
        if(URL != null) {
            final String OS = System.getProperty("os.name").toLowerCase();
            if(OS.indexOf("win") >= 0) {
                try {
                    Desktop.getDesktop().browse(new URI(URL));
                } catch (IOException | URISyntaxException e) {
                    log.error("Exception occurred trying to open website: %s", e.getMessage(), e);
                }
            }
            else if(OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0 )
            try {
                if (Runtime.getRuntime().exec(new String[]{"which", "xdg-open"}).getInputStream().read() != -1) {
                    Runtime.getRuntime().exec(new String[]{"xdg-open", URL});
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
