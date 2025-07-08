package com.github.antoj2.statthing;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = Statthing.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // string that shall be treated as JSON for custom stats
    private static final ForgeConfigSpec.ConfigValue<String> STATS_JSON = BUILDER.comment("Some JSON").define("stats", """
            {
              "deaths_per_hour": {
                "operation": "div",
                "operands": [
                  "minecraft:custom/minecraft:deaths",
                  {
                    "operation": "div",
                    "operands": [
                      "minecraft:custom/minecraft:play_time",
                      72000
                    ]
                  }
                ]
              }
            }""");

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static JsonObject customStats;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        customStats = new Gson().fromJson(STATS_JSON.get(), JsonObject.class);

    }
}
