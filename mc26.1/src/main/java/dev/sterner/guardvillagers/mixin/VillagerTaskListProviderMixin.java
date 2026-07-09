package dev.sterner.guardvillagers.mixin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.GuardVillagersConfig;
import dev.sterner.guardvillagers.common.entity.task.RepairGolemTask;
import dev.sterner.guardvillagers.common.entity.task.ShareGossipWithGuard;
import net.minecraft.core.Holder;
import dev.sterner.guardvillagers.GuardEntityTypes;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.GateBehavior;
import net.minecraft.world.entity.ai.behavior.InteractWith;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.TradeWithVillager;
import net.minecraft.world.entity.ai.behavior.VillagerGoalPackages;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(VillagerGoalPackages.class)
public class VillagerTaskListProviderMixin {

    @Inject(method = "getMeetPackage", cancellable = true, at = @At("RETURN"))
    private static void createMeetTasks(
            float speed,
            CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir) {
        List<Pair<Integer, ? extends BehaviorControl<? super Villager>>> villagerList = new ArrayList<>(cir.getReturnValue());
        villagerList.add(Pair.of(2, new GateBehavior<>(ImmutableMap.of(), ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET), GateBehavior.OrderPolicy.ORDERED, GateBehavior.RunningPolicy.RUN_ONE, ImmutableList.of(Pair.of(new ShareGossipWithGuard(), 1), Pair.of(new TradeWithVillager(), 1)))));
        cir.setReturnValue(ImmutableList.copyOf(villagerList));
    }

    @Inject(method = "getIdlePackage", cancellable = true, at = @At("RETURN"))
    private static void createIdleTasks(
            float speed,
            CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir) {
        List<Pair<Integer, ? extends BehaviorControl<? super Villager>>> villagerList = new ArrayList<>(cir.getReturnValue());
        villagerList.add(Pair.of(2, new RunOne<>(ImmutableList.of(Pair.of(InteractWith.of(GuardVillagers.GUARD_VILLAGER, 8, MemoryModuleType.INTERACTION_TARGET, speed, 2), 3), Pair.of(InteractWith.of(GuardEntityTypes.VILLAGER, 8, MemoryModuleType.INTERACTION_TARGET, speed, 2), 3), Pair.of(new DoNothing(30, 60), 1)))));
        villagerList.add(Pair.of(2, new GateBehavior<>(ImmutableMap.of(), ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET), GateBehavior.OrderPolicy.ORDERED, GateBehavior.RunningPolicy.RUN_ONE, ImmutableList.of(Pair.of(new ShareGossipWithGuard(), 1), Pair.of(new TradeWithVillager(), 1)))));
        cir.setReturnValue(ImmutableList.copyOf(villagerList));
    }

    @Inject(method = "getWorkPackage", cancellable = true, at = @At("RETURN"))
    private static void createWorkTasks(
            Holder<VillagerProfession> profession,
            float speed,
            CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir) {
        if (profession.is(VillagerProfession.TOOLSMITH) || profession.is(VillagerProfession.WEAPONSMITH) && GuardVillagersConfig.blackSmithHealing) {
            List<Pair<Integer, ? extends BehaviorControl<? super Villager>>> villagerList = new ArrayList<>(cir.getReturnValue());
            villagerList.add(Pair.of(2, new GateBehavior<>(ImmutableMap.of(), ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET), GateBehavior.OrderPolicy.ORDERED, GateBehavior.RunningPolicy.RUN_ONE, ImmutableList.of(Pair.of(new RepairGolemTask(), 1), Pair.of(new TradeWithVillager(), 1)))));
            cir.setReturnValue(ImmutableList.copyOf(villagerList));
        }
    }
}