package com.github.antoj2.statthing;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Statthing.MODID)
public class Statthing {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "statthing";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<StatType<?>> STAT_TYPES = DeferredRegister.create(ForgeRegistries.STAT_TYPES, MODID);

    public static final RegistryObject<StatType<Block>> MY_STAT = STAT_TYPES.register("my_stat", () -> new StatType<>(BuiltInRegistries.BLOCK));

    public Statthing() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the deferred register for stat types
        STAT_TYPES.register(modEventBus);

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        System.out.println("Registering commands...");
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("score");

        ForgeRegistries.STAT_TYPES.getEntries().forEach(entry -> {
            ResourceKey<StatType<?>> statKey = entry.getKey();
            StatType<?> statType = entry.getValue();

            builder.then(Commands.literal(statKey.location().toString())
                    .then(Commands.argument("type", ResourceKeyArgument.key(statType.getRegistry().key()))
                            .executes(context -> {
                                ResourceKey<StatType<?>> key = context.getArgument("type", ResourceKey.class);
                                MinecraftServer server = context.getSource().getServer();
                                File[] statsDir = server.getWorldPath(LevelResource.PLAYER_STATS_DIR).toFile().listFiles((dir, name) -> name.endsWith(".json"));
                                for (File file : statsDir) {
                                    context.getSource().sendSuccess(() -> Component.literal("Found stats file: " + file.getName()), false);
                                    String uuid = file.getName().substring(0, file.getName().length() - 5);
                                    try (FileReader reader = new FileReader(file)) {
                                        JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

                                        context.getSource().sendSuccess(() -> Component.literal(jsonObject.toString()), false);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                // context.getSource().getServer().getProfileCache().get(XXXX);
                                context.getSource().sendSuccess(() -> Component.literal("AHHHH " + key.location()), false);
                                return 1;
                            })));
        });

        event.getDispatcher().register(builder);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        if (Config.logDirtBlock) LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }


    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
