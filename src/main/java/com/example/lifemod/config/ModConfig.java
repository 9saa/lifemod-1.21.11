package com.example.lifemod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.example.lifemod.LifeMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("lifemod.json");

    public int startingLives = 3;
    public int maxLives = 10;
    public int tpaDelaySeconds = 5;
    public double ancientDebrisRarityMultiplier = 0.3;
    public int combatTimeoutSeconds = 40;
    public double combatEscapeDistanceBlocks = 25.0;
    public boolean deathHeadDropsEnabled = true;
    public boolean combatLogDeathEnabled = true;
    public int livesGrantedByRevival = 1;
    public int maxRevivals = -1;

    private static ModConfig instance;

    public static ModConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                instance = GSON.fromJson(Files.readString(CONFIG_PATH), ModConfig.class);
            } else {
                instance = new ModConfig();
                save();
            }
        } catch (Exception e) {
            LifeMod.LOGGER.error("Failed to load config, using defaults", e);
            instance = new ModConfig();
        }
        instance.validate();
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(get()));
        } catch (IOException e) {
            LifeMod.LOGGER.error("Failed to save config", e);
        }
    }

    private void validate() {
        if (startingLives < 0) startingLives = 3;
        if (maxLives < startingLives) maxLives = startingLives;
        if (tpaDelaySeconds < 0) tpaDelaySeconds = 5;
        if (ancientDebrisRarityMultiplier < 0) ancientDebrisRarityMultiplier = 0.3;
        if (ancientDebrisRarityMultiplier > 10.0) ancientDebrisRarityMultiplier = 10.0;
        if (combatTimeoutSeconds < 1) combatTimeoutSeconds = 40;
        if (combatEscapeDistanceBlocks < 0) combatEscapeDistanceBlocks = 25.0;
        if (livesGrantedByRevival < 1) livesGrantedByRevival = 1;
    }
}
