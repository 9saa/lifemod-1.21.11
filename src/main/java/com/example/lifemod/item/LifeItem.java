package com.example.lifemod.item;

import com.example.lifemod.config.ModConfig;
import com.example.lifemod.data.LifeState;
import com.example.lifemod.data.PlayerLifeData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class LifeItem extends Item {

    public LifeItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        // Play the swing animation on the client, do nothing else
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (!(user instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }

        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) {
            return ActionResult.PASS;
        }

        ModConfig config = ModConfig.get();
        PlayerLifeData data = LifeState.getPlayerData(server, player);

        // Already at max — bail without consuming
        if (data.getLives() >= config.maxLives) {
            player.sendMessage(Text.literal("You already have the maximum number of lives!")
                    .formatted(Formatting.RED), false);
            return ActionResult.SUCCESS;
        }

        // Grant the life
        LifeState.gainLife(server, player);

        // Read the new count AFTER the gain so the message is accurate
        int newLives = LifeState.getPlayerData(server, player).getLives();

        player.sendMessage(Text.literal("You gained 1 life! Total lives: ")
                .formatted(Formatting.GREEN)
                .append(Text.literal(String.valueOf(newLives))
                        .formatted(Formatting.GOLD, Formatting.BOLD)), false);

        // Consume one item (skips if the player is in creative)
        ItemStack stack = player.getStackInHand(hand);
        stack.decrementUnlessCreative(1, player);

        return ActionResult.SUCCESS;
    }
}
