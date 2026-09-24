package com.example.lifemod;

import com.example.lifemod.command.ModCommands;
import com.example.lifemod.config.ModConfig;
import com.example.lifemod.data.LifeState;
import com.example.lifemod.item.ModItems;
import com.example.lifemod.block.ModBlocks;
import com.example.lifemod.network.ModNetworking;
import com.example.lifemod.combat.CombatManager;
import com.example.lifemod.death.DeathHandler;
import com.example.lifemod.tpa.TpaManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LifeMod implements ModInitializer {
    public static final String MOD_ID = "lifemod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing LifeMod for Minecraft 1.21.11");

        ModConfig.load();
        ModItems.register();
        ModBlocks.register();
        ModNetworking.register();

        // Commands - registered via Fabric API
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> {
                    ModCommands.register(dispatcher);
                });

        // Death events
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayerEntity player) {
                DeathHandler.onPlayerDeath(player, damageSource);
            }
        });

        // Damage events for combat
        ServerLivingEntityEvents.AFTER_DAMAGE.register(
                (entity, source, baseDamageTaken, damageTaken, blocked) -> {
                    if (entity instanceof ServerPlayerEntity victim) {
                        CombatManager.onPlayerDamaged(victim, source);
                    }
                });

        // Player connection events
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            LifeState.onPlayerJoin(server, player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.player;
            if (player != null) {
                CombatManager.onPlayerDisconnect(player);
                TpaManager.onPlayerDisconnect(player);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                CombatManager.tick(player);
            }

            // --- NEW: re-broadcast lives once per second ---
            if (server.getTicks() % 20 == 0) {
                ModNetworking.sendAllLivesToAll(server);
            }
        });

        // Server lifecycle for persistence
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            LifeState.load(server);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LifeState.save(server);
        });

        LOGGER.info("LifeMod initialization complete");
    }
}
