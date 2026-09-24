package com.example.lifemod.client;

import com.example.lifemod.LifeMod;
import com.example.lifemod.client.screen.ReviveScreen;
import com.example.lifemod.network.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class LifeModClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger(LifeMod.MOD_ID + "_client");

    public static int clientLives = 0;
    public static final Map<UUID, Integer> CACHED_LIVES = new HashMap<>();

    public static int getCachedLives(UUID uuid) {
        return CACHED_LIVES.getOrDefault(uuid, 0);
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing LifeMod client");

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.HEALTH_BAR,
                Identifier.of(LifeMod.MOD_ID, "lives_hearts"),
                (context, tickCounter) -> renderLivesHearts(context)
        );

        ClientPlayNetworking.registerGlobalReceiver(
                ModNetworking.LifeDataPayload.PACKET_ID,
                (payload, context) -> context.client().execute(() -> {
                    clientLives = payload.lives();
                }));

        ClientPlayNetworking.registerGlobalReceiver(
                ModNetworking.AllLivesPayload.PACKET_ID,
                (payload, context) -> context.client().execute(() -> {
                    CACHED_LIVES.clear();
                    CACHED_LIVES.putAll(payload.decode());
                }));

        ClientPlayNetworking.registerGlobalReceiver(
                ModNetworking.OpenReviveScreenPayload.PACKET_ID,
                (payload, context) -> context.client().execute(() ->
                        context.client().setScreen(new ReviveScreen(
                                payload.beaconPos(), payload.players()))
                ));

        LOGGER.info("LifeMod client initialization complete");
    }

    private void renderLivesHearts(net.minecraft.client.gui.DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        int heartsToShow = clientLives;
        if (heartsToShow <= 0) return;

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        int x = screenWidth / 2 - 91;
        int y = screenHeight - 49;

        Identifier heartTexture = Identifier.of(LifeMod.MOD_ID, "textures/gui/lives_heart.png");

        for (int i = 0; i < heartsToShow; i++) {
            context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    heartTexture,
                    x + (i * 8), y,
                    0.0f, 0.0f,
                    9, 9,
                    9, 9
            );
        }
    }
}
