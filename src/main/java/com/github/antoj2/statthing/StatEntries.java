package com.github.antoj2.statthing;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.StatType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StatEntries {
    public record StatEntry(String name, Double value) {
        @Override
        public @NotNull String toString() {
            return name + ": " + value;
        }
    }

    public List<StatEntry> entries;
    public StatEntry playerStatEntry;

    private final ResourceLocation statType;
    private final ResourceLocation statName;

    public StatEntries(List<StatEntry> entries, StatEntry playerStatEntry, ResourceLocation statType, ResourceLocation statName) {
        this.entries = entries;
        this.playerStatEntry = playerStatEntry;

        this.statType = statType;
        this.statName = statName;
    }

    public static StatEntries fromStat(ResourceKey<StatType<?>> statType, ResourceKey<StatType<?>> statName, UUID executorUuid, MinecraftServer server) {
        List<StatEntry> result = new ArrayList<>();
        StatEntry playerStatEntry = null;

        File[] statsDir = server.getWorldPath(LevelResource.PLAYER_STATS_DIR).toFile().listFiles((dir, name) -> name.endsWith(".json"));
        for (File file : statsDir) {
            UUID userUuid = UUID.fromString(file.getName().substring(0, file.getName().length() - 5));
            try (FileReader reader = new FileReader(file)) {
                JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

                JsonObject stats = jsonObject.getAsJsonObject("stats");
                JsonObject type = stats.getAsJsonObject(String.valueOf(statType.location()));

                if (type == null || !type.has(String.valueOf(statName.location()))) {
                    continue; // Skip if the stat type or stat name does not exist
                }

                double value = type.getAsJsonPrimitive(String.valueOf(statName.location())).getAsDouble();

                String playerName = server.getProfileCache().get(userUuid).map(GameProfile::getName).orElse(userUuid.toString());
                StatEntry entry = new StatEntry(playerName, value);
                if (executorUuid.equals(userUuid)) {
                    playerStatEntry = entry;
                }
                result.add(new StatEntry(playerName, value));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (playerStatEntry == null) {
            playerStatEntry = new StatEntry(server.getProfileCache().get(executorUuid).map(GameProfile::getName).orElse(executorUuid.toString()), 0.0); // Default to 0 if the player has no stats
        }
        return new StatEntries(result, playerStatEntry, statType.location(), statName.location());
    }

    public static StatEntries fromCustom(ResourceLocation statName, JsonObject object, UUID executorUuid, MinecraftServer server, ServerLevel level) {
        List<StatEntry> result = new ArrayList<>();
        StatEntry playerStatEntry = null;

        File[] statsDir = server.getWorldPath(LevelResource.PLAYER_STATS_DIR).toFile().listFiles((dir, name) -> name.endsWith(".json"));
        for (File file : statsDir) {
            UUID userUuid = UUID.fromString(file.getName().substring(0, file.getName().length() - 5));
            String userName = server.getProfileCache().get(userUuid).map(GameProfile::getName).orElse(userUuid.toString());
            GameProfile gameProfile = new GameProfile(userUuid, userName);

            ServerPlayer player = new ServerPlayer(server, level, gameProfile);
            double value = evaluate(Config.customStats.get(statName.getPath()), player);

            StatEntry entry = new StatEntry(userName, value);
            if (executorUuid.equals(userUuid)) {
                playerStatEntry = entry;
            }
            result.add(new StatEntry(userName, value));
        }

        if (playerStatEntry == null) {
            playerStatEntry = new StatEntry(server.getProfileCache().get(executorUuid).map(GameProfile::getName).orElse(executorUuid.toString()), 0.0); // Default to 0 if the player has no stats
        }

        return new StatEntries(result, playerStatEntry, ResourceLocation.fromNamespaceAndPath("statthing", "custom"), statName);
    }

    public static double evaluate(JsonElement element, ServerPlayer player) {
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                return primitive.getAsDouble();
            } else if (primitive.isString()) {
                String key = primitive.getAsString();
                String[] split = key.split("/");

                ResourceLocation first = ResourceLocation.parse(split[0]);
                ResourceLocation second = ResourceLocation.parse(split[1]);

                StatType<Object> statType = (StatType<Object>) ForgeRegistries.STAT_TYPES.getValue(first);
                Object value = statType.getRegistry().get(second);

                return player.getStats().getValue(statType.get(value));
            }
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            String op = obj.get("operation").getAsString();
            JsonArray operands = obj.getAsJsonArray("operands");

            List<Double> args = new ArrayList<>();
            for (JsonElement operand : operands) {
                args.add(evaluate(operand, player));
            }

            return switch (op) {
                case "add" -> args.stream().mapToDouble(Double::doubleValue).sum();
                case "sub" -> args.get(0) - args.stream().skip(1).mapToDouble(Double::doubleValue).sum();
                case "mul" -> args.stream().reduce(1.0, (a, b) -> a * b);
                case "div" -> {
                    double result = args.get(0);
                    for (int i = 1; i < args.size(); i++) {
                        double divisor = args.get(i);
                        result = (divisor == 0) ? 0 : result / divisor;
                    }
                    yield result;
                }
                default -> throw new IllegalArgumentException("blehhh");
            };
        }

        throw new IllegalArgumentException("Invalid JSON element: " + element);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("§l").append(statType).append("§r§7/").append("§r§l").append(statName).append("§r").append("§r:\n");
        int max = Math.min(getEntries().size(), 10);
        for (int i = 0; i < max; i++) {
            StatEntry statEntry = getEntries().get(i);
            String userName = statEntry.name();
            double value = statEntry.value();
            final int rank = i + 1;
            sb.append("#").append(rank).append(". ").append(userName).append(": ").append(value).append("\n");
        }
        sb.append("-------------------------\n");
        int playerIndex = getEntries().indexOf(playerStatEntry);
        playerIndex = playerIndex == -1 ? getEntries().size() + 1 : playerIndex + 1; // if not found default to last
        sb.append("§7").append("#").append(playerIndex).append(". ").append(playerStatEntry.name()).append(": ").append(playerStatEntry.value()).append("§r");

        return sb.toString();
    }

    public List<StatEntry> getEntries() {
        return entries;
    }
}
