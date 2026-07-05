package dev.sterner.guardvillagers.mixin;

import dev.sterner.guardvillagers.GuardVillagersConfig;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.world.entity.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.Level;

@Mixin(Mob.class)
public abstract class MobEntityMixin extends LivingEntity implements Targeting {
    protected MobEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "setTarget", at = @At("TAIL"))
    private void onSetTarget(@Nullable LivingEntity target, CallbackInfo ci) {
        if (target == null || ((Mob)(Object)this) instanceof GuardEntity) {
            return;
        }
        boolean isVillager = target.getType() == EntityTypes.VILLAGER || target instanceof GuardEntity;
        if (isVillager) {
            List<Mob> list = ((Mob)(Object)this).level().getEntitiesOfClass(Mob.class, ((Mob)(Object)this).getBoundingBox().inflate(GuardVillagersConfig.guardVillagerHelpRange, 5.0D, GuardVillagersConfig.guardVillagerHelpRange));
            for (Mob mobEntity : list) {
                if ((mobEntity instanceof GuardEntity || ((Mob)(Object)this).getType() == EntityTypes.IRON_GOLEM) && mobEntity.getTarget() == null) {
                    mobEntity.setTarget(((Mob)(Object)this));
                }
            }
        }

        if (((Mob)(Object)this) instanceof IronGolem golem && target instanceof GuardEntity) {
            golem.setTarget(null);
        }
    }
}
