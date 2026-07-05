package dev.sterner.guardvillagers.common.entity.task;

import com.google.common.collect.ImmutableMap;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;

public class ShareGossipWithGuard extends MultiTickTask<VillagerEntity> {
    public ShareGossipWithGuard() {
        super(ImmutableMap.of(MemoryModuleType.INTERACTION_TARGET, MemoryModuleState.VALUE_PRESENT, MemoryModuleType.VISIBLE_MOBS, MemoryModuleState.VALUE_PRESENT));
    }

    @Override
    protected boolean shouldRun(ServerWorld serverWorld, VillagerEntity villagerEntity) {
        return villagerEntity.getBrain()
                .getOptionalRegisteredMemory(MemoryModuleType.INTERACTION_TARGET)
                .filter(e -> e instanceof GuardEntity)
                .isPresent();
    }

    @Override
    protected boolean shouldKeepRunning(ServerWorld serverWorld, VillagerEntity villagerEntity, long time) {
        return this.shouldRun(serverWorld, villagerEntity);
    }

    @Override
    protected void run(ServerWorld serverWorld, VillagerEntity villagerEntity, long time) {
        GuardEntity guard = (GuardEntity) villagerEntity.getBrain()
                .getOptionalRegisteredMemory(MemoryModuleType.INTERACTION_TARGET).get();
        walkTowardsEachOther(villagerEntity, guard, 0.5F, 2);
    }

    @Override
    protected void keepRunning(ServerWorld serverWorld, VillagerEntity villagerEntity, long time) {
        GuardEntity guard = (GuardEntity) villagerEntity.getBrain()
                .getOptionalRegisteredMemory(MemoryModuleType.INTERACTION_TARGET).get();
        if (villagerEntity.squaredDistanceTo(guard) < 25.0D) {
            walkTowardsEachOther(villagerEntity, guard, 0.5F, 2);
            guard.gossip(villagerEntity, time);
        }
    }

    @Override
    protected void finishRunning(ServerWorld serverWorld, VillagerEntity villagerEntity, long time) {
        villagerEntity.getBrain().forget(MemoryModuleType.INTERACTION_TARGET);
    }

    private void walkTowardsEachOther(VillagerEntity villager, GuardEntity guard, float speed, int completionRange) {
        villager.getLookControl().lookAt(guard, 30.0f, 30.0f);
        guard.getLookControl().lookAt(villager, 30.0f, 30.0f);
        villager.getNavigation().startMovingTo(guard, speed);
    }
}
