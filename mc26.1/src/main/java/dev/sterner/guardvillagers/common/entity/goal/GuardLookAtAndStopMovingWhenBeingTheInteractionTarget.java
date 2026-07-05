package dev.sterner.guardvillagers.common.entity.goal;

import dev.sterner.guardvillagers.common.entity.GuardEntity;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;

public class GuardLookAtAndStopMovingWhenBeingTheInteractionTarget extends Goal {
    private final GuardEntity guard;
    private Villager villager;

    public GuardLookAtAndStopMovingWhenBeingTheInteractionTarget(GuardEntity guard) {
        this.guard = guard;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        List<Villager> list = this.guard.level().getEntitiesOfClass(Villager.class, guard.getBoundingBox().inflate(10.0D));
        if (!list.isEmpty()) {
            for (Villager villager : list) {
                if (villager.getBrain().hasMemoryValue(MemoryModuleType.INTERACTION_TARGET) && villager.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).get().equals(guard)) {
                    this.villager = villager;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        guard.getNavigation().stop();
        guard.lookAt(villager, 30.0F, 30.0F);
        guard.getLookControl().setLookAt(villager);
    }
}