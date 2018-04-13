package org.aion.wallet.util;

public class AionConstants {

    private AionConstants() {}

    public final static String CCY = "AION";

    public static final String DEFAULT_NRG = "100000";

    public static final String DEFAULT_NRG_PRICE = "1000000";

    public static final int BLOCK_MINING_TIME_SECONDS = 10;

    public static final Long BLOCK_MINING_TIME_MILLIS = BLOCK_MINING_TIME_SECONDS * 1000L;

    public static final Integer MAX_BLOCKS_FOR_LATEST_TRANSACTIONS_QUERY = 5000;

    // todo: will we be able to access this from AccountManager?

    public static final Integer DEFAULT_WALLET_UNLOCK_DURATION = 1000;
}
