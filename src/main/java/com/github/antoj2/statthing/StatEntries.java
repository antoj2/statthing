package com.github.antoj2.statthing;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatType;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StatEntries {
    public record StatEntry(String name, Integer value) {
        @Override
        public @NotNull String toString() {
            return name + ": " + value;
        }
    }

    public List<StatEntry> entries;
    public StatEntry playerStatEntry;

    private final ResourceKey<StatType<?>> statType;
    private final ResourceKey<StatType<?>> statName;

    public StatEntries(List<StatEntry> entries, StatEntry playerStatEntry, ResourceKey<StatType<?>> statType, ResourceKey<StatType<?>> statName) {
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

                int value = type.getAsJsonPrimitive(String.valueOf(statName.location())).getAsInt();

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
            playerStatEntry = new StatEntry(server.getProfileCache().get(executorUuid).map(GameProfile::getName).orElse(executorUuid.toString()), 0); // Default to 0 if the player has no stats
        }
        return new StatEntries(result, playerStatEntry, statType, statName);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("§l").append(statType.location()).append("§r§7/").append("§r§l").append(statName.location()).append("§r").append("§r:\n");
        int max = Math.min(getEntries().size(), 10);
        for (int i = 0; i < max; i++) {
            StatEntry statEntry = getEntries().get(i);
            String userName = statEntry.name();
            int value = statEntry.value();
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
