package dev.sterner.guardvillagers.common.entity;

import dev.sterner.guardvillagers.GuardVillagers;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

public class GuardEntityLootTables {

    public static final ResourceKey<LootTable> GUARD_MAIN_HAND = registerLootTable("entities/guard_main_hand");
    public static final ResourceKey<LootTable> GUARD_OFF_HAND = registerLootTable("entities/guard_off_hand");
    public static final ResourceKey<LootTable> GUARD_HELMET = registerLootTable("entities/guard_helmet");
    public static final ResourceKey<LootTable> GUARD_CHEST = registerLootTable("entities/guard_chestplate");
    public static final ResourceKey<LootTable> GUARD_LEGGINGS = registerLootTable("entities/guard_legs");
    public static final ResourceKey<LootTable> GUARD_FEET = registerLootTable("entities/guard_feet");

    public static ResourceKey<LootTable> registerLootTable(String id) {
        return ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(GuardVillagers.MODID, id));
    }
}