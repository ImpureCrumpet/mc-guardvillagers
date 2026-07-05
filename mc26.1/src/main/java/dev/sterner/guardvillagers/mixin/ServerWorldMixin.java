package dev.sterner.guardvillagers.mixin;

import dev.sterner.guardvillagers.GuardVillagersConfig;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import dev.sterner.guardvillagers.common.entity.goal.AttackEntityDaytimeGoal;
import dev.sterner.guardvillagers.common.entity.goal.HealGolemGoal;
import dev.sterner.guardvillagers.common.entity.goal.HealGuardAndPlayerGoal;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Mixin(ServerLevel.class)
public abstract class ServerWorldMixin extends Level implements WorldGenLevel {


    protected ServerWorldMixin(WritableLevelData properties, ResourceKey<Level> registryRef, RegistryAccess registryManager, Holder<DimensionType> dimensionEntry, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, seed, maxChainedNeighborUpdates);
    }

    @Inject(method = "addFreshEntity", at = @At("TAIL"))
    private void onSpawn(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (GuardVillagersConfig.raidAnimals) {
            if (entity instanceof Raider raiderEntity)
                if (raiderEntity.hasActiveRaid()) {
                    raiderEntity.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(raiderEntity, Animal.class, false));
                }
        }

        if (GuardVillagersConfig.attackAllMobs) {
            if (entity instanceof Monster && !(entity instanceof Spider)) {
                Mob mob = (Mob) entity;
                mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, GuardEntity.class, false));
            }
            if (entity instanceof Spider spider) {
                spider.targetSelector.addGoal(3, new AttackEntityDaytimeGoal<>(spider, GuardEntity.class));
            }
        }

        if (entity instanceof AbstractIllager illager) {
            if (GuardVillagersConfig.illagersRunFromPolarBears) {
                illager.goalSelector.addGoal(2, new AvoidEntityGoal<>(illager, PolarBear.class, 6.0F, 1.0D, 1.2D));
            }

            illager.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(illager, GuardEntity.class, false));
        }

        if (entity instanceof Villager villagerEntity) {
            if (GuardVillagersConfig.villagersRunFromPolarBears)
                villagerEntity.goalSelector.addGoal(2, new AvoidEntityGoal<>(villagerEntity, PolarBear.class, 6.0F, 1.0D, 1.2D));
            if (GuardVillagersConfig.witchesVillager)
                villagerEntity.goalSelector.addGoal(2, new AvoidEntityGoal<>(villagerEntity, Witch.class, 6.0F, 1.0D, 1.2D));

            if (GuardVillagersConfig.blackSmithHealing)
                villagerEntity.goalSelector.addGoal(1, new HealGolemGoal(villagerEntity));
            if (GuardVillagersConfig.clericHealing)
                villagerEntity.goalSelector.addGoal(1, new HealGuardAndPlayerGoal(villagerEntity, 1.0D, 100, 0, 10.0F));
        }

        if (entity instanceof IronGolem golem) {
            HurtByTargetGoal tolerateFriendlyFire = new HurtByTargetGoal(golem, GuardEntity.class).setAlertOthers();
            golem.targetSelector.getAvailableGoals().stream().map(WrappedGoal::getGoal).filter(it -> it instanceof HurtByTargetGoal).findFirst().ifPresent(angerGoal -> {
                golem.targetSelector.removeGoal(angerGoal);
                golem.targetSelector.addGoal(2, tolerateFriendlyFire);
            });
        }

        if (entity instanceof Zombie zombie) {
            zombie.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(zombie, GuardEntity.class, false));
        }

        if (entity instanceof Ravager ravager) {
            ravager.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(ravager, GuardEntity.class, false));
        }

        if (entity instanceof Witch witch) {
            if (GuardVillagersConfig.witchesVillager) {
                witch.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(witch, Villager.class, true));
                witch.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(witch, IronGolem.class, true));
                witch.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(witch, GuardEntity.class, true));
            }
        }

        if (entity instanceof Cat cat) {
            cat.goalSelector.addGoal(1, new AvoidEntityGoal<>(cat, AbstractIllager.class, 12.0F, 1.0D, 1.2D));
        }
    }
}
