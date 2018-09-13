package org.aion.wallet.util;

import org.aion.api.log.LogEnum;
import org.aion.wallet.log.WalletLoggerFactory;
import org.slf4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;

public class URLManager {

    private static final Logger log = WalletLoggerFactory.getLogger(LogEnum.WLT.name());

    private static final String TRANSACTION_URL = "/#/transaction/";

    private static final String CENTRYS_URL = "https://www.centrys.io";


    public static void openDashboard() {
        openURL(AionConstants.AION_URL);
    }

    public static void openCentrys() {
        openURL(CENTRYS_URL);
    }

    public static void openTransaction(final String transactionHash) {
        if (checkTransactionHash(transactionHash)) {
            openURL(AionConstants.AION_URL + TRANSACTION_URL + transactionHash);
        }
    }

    private static boolean checkTransactionHash(final String transactionHash) {
        return transactionHash != null &&
               !transactionHash.isEmpty() &&
               transactionHash.length() == 64 &&
               transactionHash.matches("[0-9a-fA-F]+");
    }

    private static void openURL(final String url) {
        if (url != null) {
            final LinkOpener genericLinkOpener = getGenericLinkOpener();
            final LinkOpener linuxLinkOpener = getLinuxLinkOpener();

            OSUtils.executeForOs(genericLinkOpener, genericLinkOpener, linuxLinkOpener, url);
        }
    }

    private static LinkOpener getLinuxLinkOpener() {
        return new LinkOpener() {
            @Override
            protected void openLink(String link) throws IOException {
                if (Runtime.getRuntime().exec(new String[]{"which", "xdg-open"}).getInputStream().read() != -1) {
                    Runtime.getRuntime().exec(new String[]{"xdg-open", link});
                }
            }
        };
    }

    private static LinkOpener getGenericLinkOpener() {
        return new LinkOpener() {
            @Override
            protected void openLink(String link) throws URISyntaxException, IOException {
                Desktop.getDesktop().browse(new URI(link));
            }
        };
    }

    private static abstract class LinkOpener implements Consumer<String> {
        @Override
        public final void accept(final String link) {
            try {
                openLink(link);
            } catch (Exception e) {
                log.error("Exception occurred trying to open website: %s", e.getMessage(), e);
            }
        }

        protected abstract void openLink(String link) throws URISyntaxException, IOException;
    }
}
