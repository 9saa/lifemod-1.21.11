package com.example.lifemod.network;

import com.example.lifemod.block.ModBlocks;
import com.example.lifemod.data.LifeState;
import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public final class ReviveHandler {

    private ReviveHandler() {}

    public static void handle(ServerPlayerEntity reviver, UUID targetUuid, BlockPos beaconPos) {
        MinecraftServer server = reviver.getEntityWorld().getServer();
        if (server == null) return;

        ServerWorld world = (ServerWorld) reviver.getEntityWorld();

        // 1. Confirm the beacon is still there
        BlockState state = world.getBlockState(beaconPos);
        if (!state.isOf(ModBlocks.REVIVE_BEACON_BLOCK)) return;

        // 2. Confirm the reviver is still near the beacon (anti-cheat / sanity)
        double dx = reviver.getX() - (beaconPos.getX() + 0.5);
        double dy = reviver.getY() - (beaconPos.getY() + 0.5);
        double dz = reviver.getZ() - (beaconPos.getZ() + 0.5);
        if (dx * dx + dy * dy + dz * dz > 64.0) return;

        // 3. Attempt the revive. Returns false if the target is not eligible.
        boolean success = LifeState.revivePlayerAtBeacon(server, targetUuid, world, beaconPos);
        if (!success) return;

        // 4. Effects: charge sound, then explosion + particles (no real explosion)
        double x = beaconPos.getX() + 0.5;
        double y = beaconPos.getY() + 0.5;
        double z = beaconPos.getZ() + 0.5;

        world.playSound(null, beaconPos,
                SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.0f);
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticles(ParticleTypes.EXPLOSION, x, y, z, 16, 1.5, 1.5, 1.5, 0.0);
        world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 32, 1.0, 1.0, 1.0, 0.15);

        // 5. Remove the block
        world.removeBlock(beaconPos, false);
    }
}
