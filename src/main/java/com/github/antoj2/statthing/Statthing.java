package com.github.antoj2.statthing;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.Comparator;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Statthing.MODID)
public class Statthing {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "statthing";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    //private static final DeferredRegister<StatType<?>> STAT_TYPES = DeferredRegister.create(ForgeRegistries.STAT_TYPES, MODID);

    //public static final RegistryObject<StatType<Block>> MY_STAT = STAT_TYPES.register("my_stat", () -> new StatType<>(BuiltInRegistries.BLOCK));

    public Statthing() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the deferred register for stat types
        //STAT_TYPES.register(modEventBus);

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
            ResourceKey<StatType<?>> statType = entry.getKey();
            StatType<?> statName = entry.getValue();

            builder.then(Commands.literal(statType.location().toString())
                    .then(Commands.argument("name", ResourceKeyArgument.key(statName.getRegistry().key()))
                            .executes(context -> {
                                ResourceKey<StatType<?>> name = context.getArgument("name", ResourceKey.class);
                                MinecraftServer server = context.getSource().getServer();

                                StatEntries statEntries = StatEntries.fromStat(statType, name, context.getSource().getPlayerOrException().getUUID(), server);
                                statEntries.getEntries().sort(Comparator.comparingInt(StatEntries.StatEntry::value).reversed());

                                context.getSource().sendSuccess(() -> Component.literal(statEntries.toString()), false);
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
    }
}
