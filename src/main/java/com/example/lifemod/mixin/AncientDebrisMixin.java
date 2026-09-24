package com.example.lifemod.mixin;

import com.example.lifemod.config.ModConfig;
import net.minecraft.block.Blocks;
import net.minecraft.world.gen.feature.OreFeature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OreFeature.class)  // <-- changed from Feature.class
public class AncientDebrisMixin {

    @Inject(
        method = "generate(Lnet/minecraft/world/gen/feature/util/FeatureContext;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onGenerate(FeatureContext<OreFeatureConfig> context,
                           CallbackInfoReturnable<Boolean> cir) {
        // Your existing logic
        OreFeatureConfig config = context.getConfig();
        boolean isAncientDebris = config.targets.stream()
                .anyMatch(target -> target.state.isOf(Blocks.ANCIENT_DEBRIS));

        if (isAncientDebris) {
            ModConfig modConfig = ModConfig.get();
            if (context.getRandom().nextDouble() > modConfig.ancientDebrisRarityMultiplier) {
                cir.setReturnValue(false);
            }
        }
    }
}
