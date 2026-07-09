package dev.sterner.guardvillagers;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * Vanilla entity type constants for the 26.1.x API band ({@link EntityType} fields).
 * Overridden in {@code mc26.2} for {@code EntityTypes}.
 */
public final class GuardEntityTypes {

    public static final EntityType<Villager> VILLAGER = EntityType.VILLAGER;
    public static final EntityType<IronGolem> IRON_GOLEM = EntityType.IRON_GOLEM;
    public static final EntityType<ZombieVillager> ZOMBIE_VILLAGER = EntityType.ZOMBIE_VILLAGER;
    public static final EntityType<Witch> WITCH = EntityType.WITCH;

    private GuardEntityTypes() {
    }
}
