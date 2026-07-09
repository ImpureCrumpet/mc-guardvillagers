package dev.sterner.guardvillagers.mixin;

import dev.sterner.guardvillagers.GuardEntityTypes;
import dev.sterner.guardvillagers.GuardVillagersConfig;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Mob.class)
public abstract class MobEntityMixin {

    @Inject(method = "setTarget", at = @At("TAIL"))
    private void onSetTarget(@Nullable LivingEntity target, CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (target == null || self instanceof GuardEntity) {
            return;
        }
        boolean isVillager = target.getType() == GuardEntityTypes.VILLAGER || target instanceof GuardEntity;
        if (isVillager) {
            List<Mob> list = self.level().getEntitiesOfClass(Mob.class, self.getBoundingBox().inflate(GuardVillagersConfig.guardVillagerHelpRange, 5.0D, GuardVillagersConfig.guardVillagerHelpRange));
            for (Mob mobEntity : list) {
                if ((mobEntity instanceof GuardEntity || mobEntity.getType() == GuardEntityTypes.IRON_GOLEM) && mobEntity.getTarget() == null) {
                    mobEntity.setTarget(self);
                }
            }
        }

        if (self instanceof IronGolem golem && target instanceof GuardEntity) {
            golem.setTarget(null);
        }
    }
}
