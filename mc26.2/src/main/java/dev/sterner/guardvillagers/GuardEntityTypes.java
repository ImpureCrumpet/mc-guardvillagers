package dev.sterner.guardvillagers;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;

/** 26.2 overlay — entity constants moved to {@link EntityTypes}. */
public final class GuardEntityTypes {

    public static final EntityType<Villager> VILLAGER = EntityTypes.VILLAGER;
    public static final EntityType<IronGolem> IRON_GOLEM = EntityTypes.IRON_GOLEM;
    public static final EntityType<ZombieVillager> ZOMBIE_VILLAGER = EntityTypes.ZOMBIE_VILLAGER;
    public static final EntityType<Witch> WITCH = EntityTypes.WITCH;

    private GuardEntityTypes() {
    }
}
