package dev.sterner.guardvillagers;

import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.Map;
import java.util.WeakHashMap;

public final class GuardNaturalSpawn {

    private static final String SPAWN_PROCESSED_TAG = "guardvillagers.natural_spawn_processed";
    private static final Map<ServerLevel, Map<Long, Object>> CLUSTER_LOCKS = new WeakHashMap<>();

    private GuardNaturalSpawn() {
    }

    public static void trySpawnWithVillager(Villager villager, ServerLevel world) {
        if (!villager.assignProfessionWhenSpawned() || villager.getTags().contains(SPAWN_PROCESSED_TAG)) {
            return;
        }

        int radius = Math.max(1, GuardVillagersConfig.villageGuardClusterRadius);
        synchronized (clusterLock(world, villager.blockPosition(), radius)) {
            if (villager.getTags().contains(SPAWN_PROCESSED_TAG)) {
                return;
            }

            float spawnChance = Mth.clamp(GuardVillagersConfig.spawnChancePerVillager, 0f, 1f);
            int minimum = Math.max(0, GuardVillagersConfig.minimumGuardsPerVillage);
            int guardCount = countGuardsNearby(world, villager, radius);
            boolean needsMinimum = minimum > 0 && guardCount < minimum;
            boolean shouldSpawn = needsMinimum || world.getRandom().nextFloat() < spawnChance;

            if (shouldSpawn && (!needsMinimum || guardCount < minimum)) {
                spawnGuardAt(villager, world);
            }

            villager.addTag(SPAWN_PROCESSED_TAG);
        }
    }

    private static Object clusterLock(ServerLevel world, BlockPos pos, int radius) {
        long clusterKey = clusterKey(pos, radius);
        synchronized (CLUSTER_LOCKS) {
            return CLUSTER_LOCKS.computeIfAbsent(world, ignored -> new WeakHashMap<>())
                    .computeIfAbsent(clusterKey, ignored -> new Object());
        }
    }

    private static long clusterKey(BlockPos pos, int radius) {
        int cell = Math.max(radius, 16);
        return BlockPos.asLong(
                Mth.floorDiv(pos.getX(), cell),
                0,
                Mth.floorDiv(pos.getZ(), cell)
        );
    }

    private static int countGuardsNearby(ServerLevel world, Villager villager, int radius) {
        return world.getEntities(
                GuardVillagers.GUARD_VILLAGER,
                villager.getBoundingBox().inflate(radius),
                Entity::isAlive
        ).size();
    }

    private static void spawnGuardAt(Villager villager, ServerLevel world) {
        GuardEntity guardEntity = GuardVillagers.GUARD_VILLAGER.create(world, EntitySpawnReason.NATURAL);
        if (guardEntity == null) {
            return;
        }
        guardEntity.spawnWithArmor = true;
        guardEntity.finalizeSpawn(world, world.getCurrentDifficultyAt(villager.blockPosition()), EntitySpawnReason.NATURAL, null);
        guardEntity.snapTo(villager.blockPosition(), 0.0f, 0.0f);

        int variant = GuardEntity.getRandomTypeForBiome(guardEntity.level(), guardEntity.blockPosition());
        guardEntity.setGuardVariant(variant);
        guardEntity.setPersistenceRequired();
        guardEntity.setCustomName(villager.getCustomName());
        guardEntity.setCustomNameVisible(villager.isCustomNameVisible());
        guardEntity.setDropChance(EquipmentSlot.HEAD, 100.0F);
        guardEntity.setDropChance(EquipmentSlot.CHEST, 100.0F);
        guardEntity.setDropChance(EquipmentSlot.FEET, 100.0F);
        guardEntity.setDropChance(EquipmentSlot.LEGS, 100.0F);
        guardEntity.setDropChance(EquipmentSlot.MAINHAND, 100.0F);
        guardEntity.setDropChance(EquipmentSlot.OFFHAND, 100.0F);

        world.addFreshEntityWithPassengers(guardEntity);
    }
}
