package com.example.lifemod.command;

import com.example.lifemod.data.LifeState;
import com.example.lifemod.data.PlayerLifeData;
import com.example.lifemod.tpa.TpaManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ModCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("tpa")
                .then(CommandManager.argument("player",
                        EntityArgumentType.player())
                        .executes(ModCommands::executeTpa)));

        dispatcher.register(CommandManager.literal("tpaccept")
                .executes(ModCommands::executeTpAccept));

        dispatcher.register(CommandManager.literal("tpdeny")
                .executes(ModCommands::executeTpDeny));

        dispatcher.register(CommandManager.literal("lives")
                .executes(ModCommands::executeLives));
    }

    private static int executeTpa(CommandContext<ServerCommandSource> context)
            throws CommandSyntaxException {
        ServerPlayerEntity source = context.getSource().getPlayerOrThrow();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

        if (source == target) {
            context.getSource().sendError(
                    Text.literal("You cannot TPA to yourself."));
            return 0;
        }

        TpaManager.sendRequest(source, target);
        return 1;
    }

    private static int executeTpAccept(CommandContext<ServerCommandSource> context)
            throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        return TpaManager.acceptRequest(player) ? 1 : 0;
    }

    private static int executeTpDeny(CommandContext<ServerCommandSource> context)
            throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        return TpaManager.denyRequest(player) ? 1 : 0;
    }

    private static int executeLives(CommandContext<ServerCommandSource> context)
            throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        PlayerLifeData data = LifeState.getPlayerData(
                context.getSource().getServer(), player);
        int lives = data.getLives();

        String message = (lives == 1) ? "You have 1 life." :
                "You have " + lives + " lives.";

        player.sendMessage(
                Text.literal(message).formatted(Formatting.GOLD), false);
        return 1;
    }
}
