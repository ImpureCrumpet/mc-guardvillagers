package dev.sterner.guardvillagers.common.entity.goal;

import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.phys.Vec3;

public class WalkBackToCheckPointGoal extends Goal {
    private final GuardEntity guard;
    private final double speed;

    public WalkBackToCheckPointGoal(GuardEntity guard, double speedIn) {
        this.guard = guard;
        this.speed = speedIn;

    }

    @Override
    public boolean canUse() {
        return guard.getPatrolPos() != null && !this.guard.getPatrolPos().closerThan(this.guard.blockPosition(), 1.0D) && !guard.isFollowing() && guard.isPatrolling();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void tick() {
        BlockPos blockpos = this.guard.getPatrolPos();
        if (blockpos != null) {
            Vec3 vector3d = Vec3.atBottomCenterOf(blockpos);
            Vec3 vector3d1 = DefaultRandomPos.getPosTowards(this.guard, 16, 3, vector3d, (float) Math.PI / 10F);
            this.guard.getPatrolPos().closerThan(this.guard.blockPosition(), 1.0D);
            if (guard.getMainHandItem().getItem() instanceof ProjectileWeaponItem) {
                this.guard.getNavigation().moveTo(blockpos.getX(), blockpos.getY(), blockpos.getZ(), this.speed);
            } else if (vector3d1 != null && guard.getTarget() == null) {
                this.guard.getNavigation().moveTo(vector3d1.x, vector3d1.y, vector3d1.z, this.speed);
            }
        }
    }
}