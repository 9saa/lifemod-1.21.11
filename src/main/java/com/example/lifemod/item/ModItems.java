package com.example.lifemod.item;

import com.example.lifemod.LifeMod;
import com.example.lifemod.block.ModBlocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.function.Function;

public class ModItems {

    public static final RegistryKey<Item> LIFE_ITEM_KEY =
            RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LifeMod.MOD_ID, "life_item"));

    public static final Item LIFE_ITEM = register(
            LIFE_ITEM_KEY,
            LifeItem::new,
            new Item.Settings()
                    .registryKey(LIFE_ITEM_KEY)
                    .maxCount(16)
                    .rarity(Rarity.EPIC)
                    .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
    );

    public static final RegistryKey<Item> REVIVE_BEACON_KEY =
            RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LifeMod.MOD_ID, "revive_beacon"));

    public static final Item REVIVE_BEACON = register(
            REVIVE_BEACON_KEY,
            settings -> new BlockItem(ModBlocks.REVIVE_BEACON_BLOCK, settings),
            new Item.Settings()
                    .registryKey(REVIVE_BEACON_KEY)
                    .maxCount(1)
                    .rarity(Rarity.RARE)
                    .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
    );

    private static Item register(RegistryKey<Item> key,
                                 Function<Item.Settings, Item> factory,
                                 Item.Settings settings) {
        return Registry.register(Registries.ITEM, key, factory.apply(settings));
    }

    public static void register() {
        LifeMod.LOGGER.info("Registering items for " + LifeMod.MOD_ID);
    }
}
