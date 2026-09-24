package com.example.lifemod.client.mixin;

import com.example.lifemod.client.LifeModClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

    private static final Identifier LIVES_FONT = Identifier.of("lifemod", "lives");

    @Inject(
            method = "getPlayerName(Lnet/minecraft/client/network/PlayerListEntry;)Lnet/minecraft/text/Text;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void addLivesHearts(PlayerListEntry entry,
                                CallbackInfoReturnable<Text> cir) {
        Text original = cir.getReturnValue();
        if (original == null) return;

        UUID uuid = entry.getProfile().id();
        int lives = LifeModClient.getCachedLives(uuid);

        if (lives <= 0) return;

        MutableText result = original.copy();
        result.append(Text.literal(" "));

        for (int i = 0; i < lives; i++) {
            result.append(Text.literal("\uE000")
                    .styled(style -> style.withFont(
                            new StyleSpriteSource.Font(LIVES_FONT))));
        }

        cir.setReturnValue(result);
    }
}
