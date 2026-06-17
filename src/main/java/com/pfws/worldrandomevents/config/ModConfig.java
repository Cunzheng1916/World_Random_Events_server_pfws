package com.pfws.worldrandomevents.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.pfws.worldrandomevents.WorldRandomEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @SerializedName("global_enabled")
    public boolean globalEnabled = true;

    @SerializedName("structure_mode")
    public String structureMode = "BUILTIN"; // "BUILTIN" or "STRUCTURE"

    @SerializedName("events")
    public Map<String, EventConfig> events = new LinkedHashMap<>();

    public ModConfig() {
        events.put("blood_moon", new EventConfig(true, EventConfig.EventType.DISASTER));
        events.put("piglin_invasion", new EventConfig(true, EventConfig.EventType.DISASTER));
        events.put("fish_rain", new EventConfig(true, EventConfig.EventType.DISASTER));
        events.put("meteor_shower", new EventConfig(true, EventConfig.EventType.BLESSING));
        events.put("harvest_festival", new EventConfig(true, EventConfig.EventType.BLESSING));
        events.put("caravan_expedition", new EventConfig(true, EventConfig.EventType.NEUTRAL));
    }

    public static ModConfig load() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("world-random-events");
        Path configPath = configDir.resolve("config.json");

        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                ModConfig loaded = GSON.fromJson(json, ModConfig.class);
                if (loaded != null && loaded.events != null) {
                    return loaded;
                }
            } catch (IOException e) {
                WorldRandomEvents.LOGGER.error("[WorldRandomEvents] Failed to load config, using defaults", e);
            }
        }

        ModConfig defaultConfig = new ModConfig();
        defaultConfig.save();
        return defaultConfig;
    }

    public void save() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("world-random-events");
        Path configPath = configDir.resolve("config.json");
        try {
            Files.createDirectories(configDir);
            Files.writeString(configPath, GSON.toJson(this));
        } catch (IOException e) {
            WorldRandomEvents.LOGGER.error("[WorldRandomEvents] Failed to save config", e);
        }
    }

    public boolean isEventEnabled(String eventId) {
        if (!globalEnabled) return false;
        EventConfig ec = events.get(eventId);
        return ec != null && ec.enabled;
    }
}