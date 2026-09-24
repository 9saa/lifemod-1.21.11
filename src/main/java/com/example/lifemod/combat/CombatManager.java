package com.example.lifemod.combat;

import com.example.lifemod.LifeMod;
import com.example.lifemod.config.ModConfig;
import com.example.lifemod.data.LifeState;
import com.example.lifemod.tpa.TpaManager;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatManager {
    private static final Map<UUID, CombatTag> combatTags = new HashMap<>();

    private static class CombatTag {
        UUID attackerId;
        long tagEndTime;

        CombatTag(UUID playerId, UUID attackerId, int timeoutSeconds) {
            this.attackerId = attackerId;
            refresh(timeoutSeconds);
        }

        void refresh(int timeoutSeconds) {
            this.tagEndTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        }

        void updateAttacker(UUID newAttackerId, int timeoutSeconds) {
            this.attackerId = newAttackerId;
            refresh(timeoutSeconds);
        }

        boolean isExpired() {
            return System.currentTimeMillis() >= tagEndTime;
        }

        long getRemainingSeconds() {
            return Math.max(0, (tagEndTime - System.currentTimeMillis()) / 1000);
        }
    }

    public static void onPlayerDamaged(ServerPlayerEntity victim, DamageSource source) {
        if (victim.getEntityWorld().isClient()) return;

        // Check if attacker is a player
        if (source.getAttacker() instanceof ServerPlayerEntity attacker) {
            ModConfig config = ModConfig.get();
            CombatTag existingTag = combatTags.get(victim.getUuid());

            if (existingTag != null) {
                existingTag.updateAttacker(attacker.getUuid(),
                        config.combatTimeoutSeconds);
            } else {
                combatTags.put(victim.getUuid(),
                        new CombatTag(victim.getUuid(), attacker.getUuid(),
                                config.combatTimeoutSeconds));
            }

            victim.sendMessage(Text.literal("You are now in combat!")
                    .formatted(Formatting.RED, Formatting.BOLD), true);

            TpaManager.onPlayerDamage(victim);
        }
    }

    public static void tick(ServerPlayerEntity player) {
        CombatTag tag = combatTags.get(player.getUuid());
        if (tag == null) return;

        ModConfig config = ModConfig.get();
        World world = player.getEntityWorld();

        if (!(world instanceof ServerWorld serverWorld)) return;

        if (tag.isExpired()) {
            // Check distance requirement
            ServerPlayerEntity attacker = serverWorld.getServer()
                    .getPlayerManager().getPlayer(tag.attackerId);

            if (attacker == null || !attacker.isAlive()
                    || player.distanceTo(attacker) >= config.combatEscapeDistanceBlocks) {
                combatTags.remove(player.getUuid());
                player.sendMessage(Text.literal("You are no longer in combat.")
                        .formatted(Formatting.GREEN), true);
            }
        } else {
            // Still in combat - show timer
            player.sendMessage(Text.literal("Combat: ")
                    .formatted(Formatting.RED)
                    .append(Text.literal(tag.getRemainingSeconds() + "s")
                            .formatted(Formatting.YELLOW, Formatting.BOLD))
                    .append(Text.literal(" remaining").formatted(Formatting.RED)),
                    true);
        }
    }

    public static boolean isInCombat(ServerPlayerEntity player) {
        CombatTag tag = combatTags.get(player.getUuid());
        return tag != null && !tag.isExpired();
    }

    public static void onPlayerDisconnect(ServerPlayerEntity player) {
        CombatTag tag = combatTags.get(player.getUuid());
        if (tag != null) {
            LifeMod.LOGGER.info("Player {} combat logged!",
                    player.getName().getString());

            if (ModConfig.get().combatLogDeathEnabled) {
                handleCombatLogDeath(player);
            }
            combatTags.remove(player.getUuid());
        }
    }

    private static void handleCombatLogDeath(ServerPlayerEntity player) {
        World world = player.getEntityWorld();
        if (!(world instanceof ServerWorld serverWorld)) return;

        var pos = player.getBlockPos();

        // Drop inventory
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                player.dropStack(serverWorld, stack.copy());
                inventory.setStack(i, ItemStack.EMPTY);
            }
        }

        // Drop head
        com.example.lifemod.death.DeathHandler.dropPlayerHead(player, world, pos);

        // Lose life
        LifeState.loseLife(serverWorld.getServer(), player);
    }

    public static void onPlayerDeath(ServerPlayerEntity player) {
        combatTags.remove(player.getUuid());
    }
}
