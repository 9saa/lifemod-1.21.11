package com.example.lifemod.block;

import com.example.lifemod.data.LifeState;
import com.example.lifemod.data.PlayerLifeData;
import com.example.lifemod.network.ModNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReviveBeaconBlock extends Block {

    public ReviveBeaconBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) return ActionResult.SUCCESS;
        if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;

        MinecraftServer server = sp.getEntityWorld().getServer();
        if (server == null) return ActionResult.FAIL;

        Map<UUID, PlayerLifeData> eliminated = LifeState.getEliminatedPlayers(server);

        List<ModNetworking.PlayerEntry> entries = new ArrayList<>();
        for (Map.Entry<UUID, PlayerLifeData> e : eliminated.entrySet()) {
            entries.add(new ModNetworking.PlayerEntry(
                    e.getKey(),
                    e.getValue().getPlayerName(),
                    e.getValue().getLives()));
        }

        ServerPlayNetworking.send(sp,
                ModNetworking.OpenReviveScreenPayload.from(pos, entries));
        return ActionResult.CONSUME;
    }
}
