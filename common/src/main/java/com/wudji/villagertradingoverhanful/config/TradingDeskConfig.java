package com.wudji.villagertradingoverhanful.config;

import com.wudji.villagertradingoverhanful.Constants;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class TradingDeskConfig {
    public static final String CLIENT_CONFIG_FILE = Constants.MOD_ID + "-client.toml";

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue BATCH_LIMIT;
    public static final ModConfigSpec.BooleanValue HIDE_UNAVAILABLE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Villager Trading Overhaul client settings.").push("general");
        BATCH_LIMIT = builder
                .comment("Maximum number of trades performed by the batch trade button.")
                .defineInRange("batchLimit", 32, 1, 128);
        HIDE_UNAVAILABLE = builder
                .comment("Hide offers that are out of stock.")
                .define("hideUnavailable", false);
        builder.pop();
        SPEC = builder.build();
    }

    private TradingDeskConfig() {
    }
}
