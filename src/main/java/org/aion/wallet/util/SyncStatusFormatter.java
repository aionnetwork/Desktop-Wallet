package org.aion.wallet.util;

import org.aion.api.server.types.SyncInfo;

public class SyncStatusFormatter {

    public static final int SECONDS_IN_A_MINUTE = 60;
    public static final int SECONDS_IN_A_HOUR = 3600;
    public static final int SECONDS_IN_A_DAY = 86400;
    public static final String UNDEFINED = "Undefined";
    public static final String UP_TO_DATE = "Up to date";

    public static String formatSyncStatus(SyncInfo syncInfo) {
        if(syncInfo != null) {
            if(syncInfo.networkBestBlkNumber > 0) {
                long seconds = (syncInfo.networkBestBlkNumber - syncInfo.chainBestBlkNumber) * 10;
                if((int) seconds < 60) {
                    return UP_TO_DATE;
                }
                int minutes = (int) seconds / SECONDS_IN_A_MINUTE;
                int hours = (int) seconds / SECONDS_IN_A_HOUR;
                int days = (int) seconds / SECONDS_IN_A_DAY;
                String syncStatus = "";
                if(days > 0) {
                    syncStatus += days + " days ";
                }
                if(hours > 0) {
                    syncStatus += (hours - days*24) + " hours ";
                }
                if(minutes > 0) {
                    syncStatus += (minutes - hours*60) + " minutes ";
                }
                if((int) seconds > 0) {
                    syncStatus += (seconds - minutes*60) + " seconds";
                }
                return syncStatus;
            }
            return UNDEFINED;
        }
        return UNDEFINED;
    }
}
