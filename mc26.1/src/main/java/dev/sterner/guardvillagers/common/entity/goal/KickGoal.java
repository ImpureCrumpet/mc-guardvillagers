package dev.sterner.guardvillagers.common.entity.goal;

import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

public class KickGoal extends Goal {

    public final GuardEntity guard;

    public KickGoal(GuardEntity guard) {
        this.guard = guard;
    }

    @Override
    public boolean canUse() {
        return guard.getTarget() != null && guard.getTarget().distanceTo(guard) <= 2.5D && guard.getMainHandItem().getItem().useOnRelease(guard.getMainHandItem()) && !guard.isBlocking() && guard.kickCoolDown == 0;
    }

    @Override
    public void start() {
        guard.setKicking(true);
        if (guard.kickTicks <= 0) {
            guard.kickTicks = 10;
        }
        if (guard.level() instanceof ServerLevel serverWorld){

            guard.doHurtTarget(serverWorld, guard.getTarget());
        }
    }

    @Override
    public void stop() {
        guard.setKicking(false);
        guard.kickCoolDown = 50;
    }
}