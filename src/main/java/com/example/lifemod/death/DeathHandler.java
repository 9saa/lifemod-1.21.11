package com.example.lifemod.death;

import com.example.lifemod.LifeMod;
import com.example.lifemod.config.ModConfig;
import com.example.lifemod.data.LifeState;
import com.example.lifemod.combat.CombatManager;
import com.example.lifemod.tpa.TpaManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DeathHandler {
    private static final Set<UUID> processedDeaths = new HashSet<>();

    public static void onPlayerDeath(ServerPlayerEntity player,
                                     DamageSource damageSource) {
        UUID playerId = player.getUuid();

        // Prevent duplicate processing
        if (processedDeaths.contains(playerId)) {
            return;
        }
        processedDeaths.add(playerId);

        try {
            LifeMod.LOGGER.info("Processing death for {}",
                    player.getName().getString());

            TpaManager.onPlayerDeath(player);
            CombatManager.onPlayerDeath(player);

            if (ModConfig.get().deathHeadDropsEnabled) {
                dropPlayerHead(player, player.getEntityWorld(), player.getBlockPos());
            }

            LifeState.loseLife(player.getEntityWorld().getServer(), player);

            var lifeData = LifeState.getPlayerData(
                    player.getEntityWorld().getServer(), player);
            if (lifeData.isEliminated()) {
                player.sendMessage(Text.literal("You have been eliminated!")
                        .formatted(Formatting.DARK_RED, Formatting.BOLD), false);
            } else {
                player.sendMessage(Text.literal("You died! Lives remaining: ")
                        .formatted(Formatting.RED)
                        .append(Text.literal(String.valueOf(lifeData.getLives()))
                                .formatted(Formatting.GOLD, Formatting.BOLD)), false);
            }
        } finally {
            // Clean up after 5 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                processedDeaths.remove(playerId);
            }).start();
        }
    }

    public static void dropPlayerHead(PlayerEntity player,
                                       World world, BlockPos pos) {
        if (world.isClient()) return;

        ItemStack headStack = new ItemStack(Items.PLAYER_HEAD);
        // Use ProfileComponent.ofStatic() - verified to exist in 1.21.11
        headStack.set(DataComponentTypes.PROFILE,
                ProfileComponent.ofStatic(player.getGameProfile()));

        var itemEntity = new net.minecraft.entity.ItemEntity(
                world,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                headStack
        );
        world.spawnEntity(itemEntity);

        LifeMod.LOGGER.info("Dropped head for {} at {}",
                player.getName().getString(), pos.toShortString());
    }
}
