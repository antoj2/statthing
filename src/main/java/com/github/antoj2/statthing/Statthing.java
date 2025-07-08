package com.github.antoj2.statthing;

import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.UUID;

@Mod(Statthing.MODID)
public class Statthing {
    public static final String MODID = "statthing";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Statthing() {
        FMLJavaModLoadingContext context = FMLJavaModLoadingContext.get();
        IEventBus modEventBus = context.getModEventBus();

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        System.out.println("Registering commands...");
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("score");

        ForgeRegistries.STAT_TYPES.getEntries().forEach(entry -> {
            ResourceKey<StatType<?>> statType = entry.getKey();
            StatType<?> statName = entry.getValue();

            builder.then(Commands.literal(statType.location().toString()).then(Commands.argument("name", ResourceKeyArgument.key(statName.getRegistry().key())).executes(context -> {
                ResourceKey<StatType<?>> name = context.getArgument("name", ResourceKey.class);
                MinecraftServer server = context.getSource().getServer();
                UUID playerUuid = context.getSource().getPlayerOrException().getUUID();

                StatEntries statEntries = StatEntries.fromStat(statType, name, playerUuid, server);
                statEntries.getEntries().sort(Comparator.comparingDouble(StatEntries.StatEntry::value).reversed());

                context.getSource().sendSuccess(() -> Component.literal(statEntries.toString()), false);
                return 1;
            })));
        });

        builder.then(Commands.literal("statthing:custom").then(Commands.argument("name", ResourceLocationArgument.id()).suggests((context, builder1) -> {
            Config.customStats.keySet().forEach((name) -> builder1.suggest("statthing:" + name));
            return builder1.buildFuture();
        }).executes(context -> {
            ResourceLocation statName = context.getArgument("name", ResourceLocation.class);
            MinecraftServer server = context.getSource().getServer();
            JsonObject stat = Config.customStats.getAsJsonObject(statName.getPath());
            UUID playerUuid = context.getSource().getPlayerOrException().getUUID();

            StatEntries statEntries = StatEntries.fromCustom(statName, stat, playerUuid, server, context.getSource().getLevel());
            statEntries.getEntries().sort(Comparator.comparingDouble(StatEntries.StatEntry::value).reversed());

            context.getSource().sendSuccess(() -> Component.literal(statEntries.toString()), false);

            return 1;
        })));

        event.getDispatcher().register(builder);
    }


    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
    }
}
