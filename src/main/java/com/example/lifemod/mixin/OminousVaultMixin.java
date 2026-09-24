package com.example.lifemod.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ModifiableWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModifiableWorld.class)
public interface OminousVaultMixin {
    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z",
            at = @At("HEAD"), cancellable = true)
    default void preventOminousVaultPlacement(BlockPos pos, BlockState state,
                                               int flags,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (state.isOf(Blocks.VAULT)
                && state.contains(Properties.OMINOUS)
                && state.get(Properties.OMINOUS)) {
            cir.setReturnValue(false);
        }
    }
}
