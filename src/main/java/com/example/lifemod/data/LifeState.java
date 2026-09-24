package com.example.lifemod.data;

import com.example.lifemod.LifeMod;
import com.example.lifemod.config.ModConfig;
import com.example.lifemod.network.ModNetworking;
import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LifeState extends PersistentState {

    private final Map<UUID, PlayerLifeData> playerData = new HashMap<>();

    private static final Codec<LifeState> CODEC = NbtCompound.CODEC.xmap(
            LifeState::fromNbt,
            LifeState::toNbt
    );

    private static final PersistentStateType<LifeState> TYPE =
            new PersistentStateType<>(
                    LifeMod.MOD_ID,
                    LifeState::new,
                    CODEC,
                    null
            );

    public LifeState() {}

    public static LifeState getServerState(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) return new LifeState();
        return overworld.getPersistentStateManager().getOrCreate(TYPE);
    }

    // =========================================================
    // Player lifecycle
    // =========================================================

    public static void onPlayerJoin(MinecraftServer server, ServerPlayerEntity player) {
        LifeState state = getServerState(server);
        UUID uuid = player.getUuid();

        PlayerLifeData data = state.playerData.get(uuid);
        if (data == null) {
            data = new PlayerLifeData(uuid, player.getName().getString(),
                    ModConfig.get().startingLives);
            state.playerData.put(uuid, data);
            state.markDirty();
        }

        if (data.isEliminated()) {
            player.networkHandler.disconnect(
                    Text.literal("You have been eliminated! (0 lives)")
                            .formatted(Formatting.DARK_RED, Formatting.BOLD));
            return;
        }

        if (data.hasPendingRespawn()) {
            RegistryKey<World> worldKey = data.getPendingRespawnWorldKey();
            BlockPos pos = data.getPendingRespawnPos();
            ServerWorld targetWorld = worldKey != null ? server.getWorld(worldKey) : null;
            if (targetWorld != null && pos != null) {
                player.teleport(targetWorld,
                        pos.getX() + 0.5,
                        pos.getY() + 1.0,
                        pos.getZ() + 0.5,
                        Set.of(),
                        player.getYaw(),
                        player.getPitch(),
                        true);
            }
            data.clearPendingRespawn();
            state.markDirty();
        }

        // Sync this player's own lives to their HUD
        sendLivesToPlayer(server, player);

        // Sync EVERYONE's lives to ALL clients (for tab list)
        ModNetworking.sendAllLivesToAll(server);

    }

    // =========================================================
    // Data access
    // =========================================================

    public static PlayerLifeData getPlayerData(MinecraftServer server, ServerPlayerEntity player) {
        LifeState state = getServerState(server);
        return state.playerData.computeIfAbsent(player.getUuid(),
                k -> new PlayerLifeData(k, player.getName().getString(),
                        ModConfig.get().startingLives));
    }

    public static PlayerLifeData getPlayerDataByUuid(MinecraftServer server, UUID uuid) {
        return getServerState(server).playerData.get(uuid);
    }

    // =========================================================
    // Life operations
    // =========================================================

    public static void loseLife(MinecraftServer server, ServerPlayerEntity player) {
        LifeState state = getServerState(server);
        PlayerLifeData data = getPlayerData(server, player);
        data.loseLife();
        state.markDirty();

        sendLivesToPlayer(server, player);
        ModNetworking.sendAllLivesToAll(server);

        if (data.getLives() <= 0) {
            eliminatePlayer(server, player, data);
        }
    }

    public static void gainLife(MinecraftServer server, ServerPlayerEntity player) {
        LifeState state = getServerState(server);
        PlayerLifeData data = getPlayerData(server, player);
        data.gainLife(ModConfig.get().maxLives);
        state.markDirty();

        sendLivesToPlayer(server, player);
        ModNetworking.sendAllLivesToAll(server);
    }

    public static void eliminatePlayer(MinecraftServer server,
                                       ServerPlayerEntity player,
                                       PlayerLifeData data) {
        data.loseLife();
        getServerState(server).markDirty();
        ModNetworking.sendAllLivesToAll(server);

        player.networkHandler.disconnect(
                Text.literal("You have been eliminated! (0 lives)")
                        .formatted(Formatting.DARK_RED, Formatting.BOLD));

        LifeMod.LOGGER.info("Player {} eliminated", player.getName().getString());
    }

    // =========================================================
    // Revive
    // =========================================================

    public static boolean revivePlayerAtBeacon(MinecraftServer server, UUID targetUuid,
                                               ServerWorld world, BlockPos beaconPos) {
        LifeState state = getServerState(server);
        PlayerLifeData data = state.playerData.get(targetUuid);

        if (data == null || !data.isEliminated()) return false;

        ModConfig config = ModConfig.get();
        if (config.maxRevivals >= 0 && data.getRevivalCount() >= config.maxRevivals) {
            return false;
        }

        data.revive(config.livesGrantedByRevival);
        data.setPendingRespawn(world.getRegistryKey(), beaconPos);
        state.markDirty();

        ServerPlayerEntity online = server.getPlayerManager().getPlayer(targetUuid);
        if (online != null) {
            online.teleport(world,
                    beaconPos.getX() + 0.5,
                    beaconPos.getY() + 1.0,
                    beaconPos.getZ() + 0.5,
                    Set.of(),
                    online.getYaw(),
                    online.getPitch(),
                    true);
            data.clearPendingRespawn();
            state.markDirty();
            sendLivesToPlayer(server, online);
        }

        ModNetworking.sendAllLivesToAll(server);

        LifeMod.LOGGER.info("Player {} revived at beacon {} with {} lives",
                data.getPlayerName(), beaconPos, data.getLives());
        return true;
    }

    public static void revivePlayer(MinecraftServer server, UUID targetUuid) {
        PlayerLifeData data = getPlayerDataByUuid(server, targetUuid);
        if (data == null || !data.isEliminated()) return;

        ModConfig config = ModConfig.get();
        if (config.maxRevivals >= 0 && data.getRevivalCount() >= config.maxRevivals) return;

        data.revive(config.livesGrantedByRevival);
        getServerState(server).markDirty();
        ModNetworking.sendAllLivesToAll(server);
    }

    // =========================================================
    // Network sync
    // =========================================================

    public static void sendLivesToPlayer(MinecraftServer server, ServerPlayerEntity player) {
        PlayerLifeData data = getPlayerData(server, player);
        if (data == null) return;
        ServerPlayNetworking.send(player, new ModNetworking.LifeDataPayload(data.getLives()));
    }

    // =========================================================
    // Persistence
    // =========================================================

    public static void load(MinecraftServer server) {}
    public static void save(MinecraftServer server) { getServerState(server).markDirty(); }

    public NbtCompound toNbt() {
        NbtCompound root = new NbtCompound();
        NbtList list = new NbtList();
        for (PlayerLifeData data : playerData.values()) list.add(data.toNbt());
        root.put("Players", list);
        return root;
    }

    public static LifeState fromNbt(NbtCompound nbt) {
        LifeState state = new LifeState();
        NbtList list = nbt.getList("Players").orElseGet(NbtList::new);
        for (int i = 0; i < list.size(); i++) {
            NbtElement el = list.get(i);
            if (el instanceof NbtCompound c) {
                PlayerLifeData data = PlayerLifeData.fromNbt(c);
                state.playerData.put(data.getUuid(), data);
            }
        }
        return state;
    }

    // =========================================================
    // GUI queries
    // =========================================================

    public static Map<UUID, PlayerLifeData> getEliminatedPlayers(MinecraftServer server) {
        LifeState state = getServerState(server);
        Map<UUID, PlayerLifeData> eliminated = new HashMap<>();
        for (Map.Entry<UUID, PlayerLifeData> entry : state.playerData.entrySet()) {
            if (entry.getValue().isEliminated()) {
                eliminated.put(entry.getKey(), entry.getValue());
            }
        }
        return eliminated;
    }
}
