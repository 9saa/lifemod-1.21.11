package com.example.lifemod.block;

import com.example.lifemod.LifeMod;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModBlocks {

    // --- REVIVE BEACON BLOCK ---
    public static final RegistryKey<Block> REVIVE_BEACON_BLOCK_KEY =
            RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(LifeMod.MOD_ID, "revive_beacon"));

    public static final Block REVIVE_BEACON_BLOCK = register(
            REVIVE_BEACON_BLOCK_KEY,
            ReviveBeaconBlock::new,
            AbstractBlock.Settings.create()
                    .registryKey(REVIVE_BEACON_BLOCK_KEY)
                    .mapColor(MapColor.GOLD)
                    .strength(5.0f, 1200.0f)
                    .sounds(BlockSoundGroup.METAL)
                    .luminance(state -> 15)
                    .nonOpaque()
    );

    private static Block register(RegistryKey<Block> key,
                                  Function<AbstractBlock.Settings, Block> factory,
                                  AbstractBlock.Settings settings) {
        return Registry.register(Registries.BLOCK, key, factory.apply(settings));
    }

    public static void register() {
        LifeMod.LOGGER.info("Registering blocks for " + LifeMod.MOD_ID);
    }
}
