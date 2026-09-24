package com.example.lifemod.tpa;


import com.example.lifemod.config.ModConfig;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TpaManager {
    private static final Map<UUID, UUID> pendingRequests = new HashMap<>();
    private static final Map<UUID, TeleportTask> activeTeleports = new HashMap<>();

    private static class TeleportTask {
        final ServerPlayerEntity player;
        final ServerPlayerEntity target;
        final long endTime;

        TeleportTask(ServerPlayerEntity player, ServerPlayerEntity target, int delaySeconds) {
            this.player = player;
            this.target = target;
            this.endTime = System.currentTimeMillis() + (delaySeconds * 1000L);
        }

        long getRemainingSeconds() {
            return Math.max(0, (endTime - System.currentTimeMillis()) / 1000);
        }
    }

    private static MinecraftServer getServerFrom(ServerPlayerEntity player) {
        World world = player.getEntityWorld();
        if (world instanceof ServerWorld serverWorld) {
            return serverWorld.getServer();
        }
        return null;
    }

    public static void sendRequest(ServerPlayerEntity requester, ServerPlayerEntity target) {
        UUID existingRequester = pendingRequests.get(target.getUuid());
        if (existingRequester != null && existingRequester.equals(requester.getUuid())) {
            requester.sendMessage(Text.literal("You already have a pending request to this player.")
                    .formatted(Formatting.RED), false);
            return;
        }

        if (activeTeleports.containsKey(requester.getUuid())) {
            requester.sendMessage(Text.literal("You are already teleporting.")
                    .formatted(Formatting.RED), false);
            return;
        }

        pendingRequests.put(target.getUuid(), requester.getUuid());

        requester.sendMessage(Text.literal("Teleport request sent to ")
                .formatted(Formatting.GREEN)
                .append(Text.literal(target.getName().getString())
                        .formatted(Formatting.YELLOW)), false);

        target.sendMessage(Text.literal(requester.getName().getString())
                .formatted(Formatting.YELLOW)
                .append(Text.literal(" has requested to teleport to you. Use /tpaccept or /tpdeny.")
                        .formatted(Formatting.GREEN)), false);

        // Correct 1.21.11 API: 3 parameters
        target.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    public static boolean acceptRequest(ServerPlayerEntity target) {
        UUID requesterUuid = pendingRequests.remove(target.getUuid());
        if (requesterUuid == null) {
            target.sendMessage(Text.literal("You have no pending teleport requests.")
                    .formatted(Formatting.RED), false);
            return false;
        }

        MinecraftServer server = getServerFrom(target);
        if (server == null) return false;

        ServerPlayerEntity requester = server.getPlayerManager().getPlayer(requesterUuid);
        if (requester == null) {
            target.sendMessage(Text.literal("The requester is no longer online.")
                    .formatted(Formatting.RED), false);
            return false;
        }

        if (activeTeleports.containsKey(requesterUuid)) {
            target.sendMessage(Text.literal("The requester is already teleporting.")
                    .formatted(Formatting.RED), false);
            return false;
        }

        ModConfig config = ModConfig.get();
        int delaySeconds = config.tpaDelaySeconds;

        TeleportTask task = new TeleportTask(requester, target, delaySeconds);
        activeTeleports.put(requesterUuid, task);

        requester.sendMessage(Text.literal("Teleport accepted! Teleporting in ")
                .formatted(Formatting.GREEN)
                .append(Text.literal(String.valueOf(delaySeconds))
                        .formatted(Formatting.YELLOW, Formatting.BOLD))
                .append(Text.literal(" seconds...").formatted(Formatting.GREEN)), false);

        startCountdown(task);
        return true;
    }

    public static boolean denyRequest(ServerPlayerEntity target) {
        UUID requesterUuid = pendingRequests.remove(target.getUuid());
        if (requesterUuid == null) {
            target.sendMessage(Text.literal("You have no pending teleport requests.")
                    .formatted(Formatting.RED), false);
            return false;
        }

        MinecraftServer server = getServerFrom(target);
        if (server != null) {
            ServerPlayerEntity requester = server.getPlayerManager().getPlayer(requesterUuid);
            if (requester != null) {
                requester.sendMessage(Text.literal(target.getName().getString())
                        .formatted(Formatting.YELLOW)
                        .append(Text.literal(" has denied your teleport request.")
                                .formatted(Formatting.RED)), false);
            }
        }

        target.sendMessage(Text.literal("Teleport request denied.")
                .formatted(Formatting.GREEN), false);
        return true;
    }

    private static void startCountdown(TeleportTask task) {
        MinecraftServer server = getServerFrom(task.player);
        if (server == null) return;

        server.execute(() -> {
            if (!activeTeleports.containsKey(task.player.getUuid())) {
                return;
            }

            long remainingSeconds = task.getRemainingSeconds();

            if (remainingSeconds <= 0) {
                executeTeleport(task);
                return;
            }

            task.player.sendMessage(Text.literal("Teleporting in ")
                    .formatted(Formatting.GOLD)
                    .append(Text.literal(String.valueOf(remainingSeconds))
                            .formatted(Formatting.YELLOW, Formatting.BOLD))
                    .append(Text.literal(" seconds...").formatted(Formatting.GOLD)), true);

            task.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 0.5f, 1.0f);

            // Schedule next second
            scheduleNextCountdown(task);
        });
    }

    private static void scheduleNextCountdown(TeleportTask task) {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            MinecraftServer server = getServerFrom(task.player);
            if (server != null) {
                server.execute(() -> startCountdown(task));
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private static void executeTeleport(TeleportTask task) {
        activeTeleports.remove(task.player.getUuid());

        if (!task.target.isAlive()) {
            task.player.sendMessage(Text.literal("Teleport cancelled: target is dead.")
                    .formatted(Formatting.RED), false);
            return;
        }

        World targetWorld = task.target.getEntityWorld();
        if (!(targetWorld instanceof ServerWorld serverWorld)) return;

        BlockPos targetPos = task.target.getBlockPos();

        task.player.teleport(
                serverWorld,
                targetPos.getX() + 0.5,
                targetPos.getY(),
                targetPos.getZ() + 0.5,
                java.util.Set.of(),
                task.target.getYaw(),
                task.target.getPitch(),
                false
        );

        task.player.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        task.target.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        serverWorld.spawnParticles(
                ParticleTypes.PORTAL,
                targetPos.getX() + 0.5, targetPos.getY() + 1, targetPos.getZ() + 0.5,
                50, 0.5, 1.0, 0.5, 0.5
        );

        task.player.sendMessage(Text.literal("Teleported successfully!")
                .formatted(Formatting.GREEN), false);
        task.target.sendMessage(Text.literal(task.player.getName().getString())
                .formatted(Formatting.YELLOW)
                .append(Text.literal(" has teleported to you.")
                        .formatted(Formatting.GREEN)), false);
    }

    public static void cancelTeleport(ServerPlayerEntity player, String reason) {
        TeleportTask task = activeTeleports.remove(player.getUuid());
        if (task != null) {
            player.sendMessage(Text.literal("Teleport cancelled: ")
                    .formatted(Formatting.RED)
                    .append(Text.literal(reason).formatted(Formatting.YELLOW)), false);
        }
    }

    public static void onPlayerDamage(ServerPlayerEntity player) {
        cancelTeleport(player, "You took damage!");
    }

    public static void onPlayerDisconnect(ServerPlayerEntity player) {
        pendingRequests.remove(player.getUuid());
        pendingRequests.entrySet().removeIf(entry ->
                entry.getValue().equals(player.getUuid()));
        activeTeleports.remove(player.getUuid());
    }

    public static void onPlayerDeath(ServerPlayerEntity player) {
        cancelTeleport(player, "Died");
        pendingRequests.remove(player.getUuid());
        pendingRequests.entrySet().removeIf(entry ->
                entry.getValue().equals(player.getUuid()));
    }
}
