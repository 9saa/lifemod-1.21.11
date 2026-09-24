package com.example.lifemod.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

public class PlayerLifeData {
    private final UUID uuid;
    private final String playerName;
    private int lives;
    private boolean eliminated;
    private int revivalCount;

    // Pending respawn (set when a player is revived while offline)
    private boolean hasPendingRespawn = false;
    private RegistryKey<World> pendingRespawnWorld = null;
    private BlockPos pendingRespawnPos = null;

    public PlayerLifeData(UUID uuid, String playerName, int lives) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.lives = lives;
        this.eliminated = false;
        this.revivalCount = 0;
    }

    public UUID getUuid() { return uuid; }
    public String getPlayerName() { return playerName; }
    public int getLives() { return lives; }
    public boolean isEliminated() { return eliminated; }
    public int getRevivalCount() { return revivalCount; }

    public void loseLife() {
        if (lives > 0) {
            lives--;
            if (lives == 0) eliminated = true;
        }
    }

    public void gainLife(int maxLives) {
        if (lives < maxLives) lives++;
    }

    public void revive(int livesGranted) {
        lives = livesGranted;
        eliminated = false;
        revivalCount++;
    }

    // --- Pending respawn ---
    public boolean hasPendingRespawn() { return hasPendingRespawn; }
    public RegistryKey<World> getPendingRespawnWorldKey() { return pendingRespawnWorld; }
    public BlockPos getPendingRespawnPos() { return pendingRespawnPos; }

    public void setPendingRespawn(RegistryKey<World> world, BlockPos pos) {
        this.hasPendingRespawn = true;
        this.pendingRespawnWorld = world;
        this.pendingRespawnPos = pos.toImmutable();
    }

    public void clearPendingRespawn() {
        this.hasPendingRespawn = false;
        this.pendingRespawnWorld = null;
        this.pendingRespawnPos = null;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("UUID", uuid.toString());
        nbt.putString("Name", playerName);
        nbt.putInt("Lives", lives);
        nbt.putBoolean("Eliminated", eliminated);
        nbt.putInt("RevivalCount", revivalCount);
        nbt.putBoolean("HasPendingRespawn", hasPendingRespawn);
        if (hasPendingRespawn && pendingRespawnWorld != null && pendingRespawnPos != null) {
            nbt.putString("PendingWorld", pendingRespawnWorld.getValue().toString());
            nbt.putInt("PendingX", pendingRespawnPos.getX());
            nbt.putInt("PendingY", pendingRespawnPos.getY());
            nbt.putInt("PendingZ", pendingRespawnPos.getZ());
        }
        return nbt;
    }

    public static PlayerLifeData fromNbt(NbtCompound nbt) {
        UUID uuid = UUID.fromString(nbt.getString("UUID").orElse(""));
        String name = nbt.getString("Name").orElse("Unknown");
        int lives = nbt.getInt("Lives").orElse(3);
        boolean eliminated = nbt.getBoolean("Eliminated").orElse(false);
        int revivals = nbt.getInt("RevivalCount").orElse(0);

        PlayerLifeData data = new PlayerLifeData(uuid, name, lives);
        data.eliminated = eliminated;
        data.revivalCount = revivals;

        if (nbt.getBoolean("HasPendingRespawn").orElse(false)) {
            String worldId = nbt.getString("PendingWorld").orElse("");
            if (!worldId.isEmpty()) {
                RegistryKey<World> worldKey = RegistryKey.of(
                        RegistryKeys.WORLD, Identifier.of(worldId));
                BlockPos pos = new BlockPos(
                        nbt.getInt("PendingX").orElse(0),
                        nbt.getInt("PendingY").orElse(64),
                        nbt.getInt("PendingZ").orElse(0));
                data.setPendingRespawn(worldKey, pos);
            }
        }
        return data;
    }
}
