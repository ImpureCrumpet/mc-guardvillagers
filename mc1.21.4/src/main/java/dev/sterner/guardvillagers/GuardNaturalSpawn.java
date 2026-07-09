package dev.sterner.guardvillagers;

import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.Map;
import java.util.WeakHashMap;

public final class GuardNaturalSpawn {

    private static final String SPAWN_PROCESSED_TAG = "guardvillagers.natural_spawn_processed";
    private static final Map<ServerWorld, Map<Long, Object>> CLUSTER_LOCKS = new WeakHashMap<>();

    private GuardNaturalSpawn() {
    }

    public static void trySpawnWithVillager(VillagerEntity villager, ServerWorld world) {
        if (!villager.isNatural() || villager.getCommandTags().contains(SPAWN_PROCESSED_TAG)) {
            return;
        }

        int radius = Math.max(1, GuardVillagersConfig.villageGuardClusterRadius);
        synchronized (clusterLock(world, villager.getBlockPos(), radius)) {
            if (villager.getCommandTags().contains(SPAWN_PROCESSED_TAG)) {
                return;
            }

            float spawnChance = MathHelper.clamp(GuardVillagersConfig.spawnChancePerVillager, 0f, 1f);
            int minimum = Math.max(0, GuardVillagersConfig.minimumGuardsPerVillage);
            int guardCount = countGuardsNearby(world, villager, radius);
            boolean needsMinimum = minimum > 0 && guardCount < minimum;
            boolean shouldSpawn = needsMinimum || world.getRandom().nextFloat() < spawnChance;

            if (shouldSpawn && (!needsMinimum || guardCount < minimum)) {
                spawnGuardAt(villager, world);
            }

            villager.addCommandTag(SPAWN_PROCESSED_TAG);
        }
    }

    private static Object clusterLock(ServerWorld world, BlockPos pos, int radius) {
        long clusterKey = clusterKey(pos, radius);
        synchronized (CLUSTER_LOCKS) {
            return CLUSTER_LOCKS.computeIfAbsent(world, ignored -> new WeakHashMap<>())
                    .computeIfAbsent(clusterKey, ignored -> new Object());
        }
    }

    private static long clusterKey(BlockPos pos, int radius) {
        int cell = Math.max(radius, 16);
        return BlockPos.asLong(
                Math.floorDiv(pos.getX(), cell),
                0,
                Math.floorDiv(pos.getZ(), cell)
        );
    }

    private static int countGuardsNearby(ServerWorld world, VillagerEntity villager, int radius) {
        return world.getEntitiesByType(
                GuardVillagers.GUARD_VILLAGER,
                villager.getBoundingBox().expand(radius),
                Entity::isAlive
        ).size();
    }

    private static void spawnGuardAt(VillagerEntity villager, ServerWorld world) {
        GuardEntity guardEntity = GuardVillagers.GUARD_VILLAGER.create(world, SpawnReason.NATURAL);
        if (guardEntity == null) {
            return;
        }
        guardEntity.spawnWithArmor = true;
        guardEntity.initialize(world, world.getLocalDifficulty(villager.getBlockPos()), SpawnReason.NATURAL, null);
        guardEntity.refreshPositionAndAngles(villager.getBlockPos(), 0.0f, 0.0f);

        int variant = GuardEntity.getRandomTypeForBiome(guardEntity.getWorld(), guardEntity.getBlockPos());
        guardEntity.setGuardVariant(variant);
        guardEntity.setPersistent();
        guardEntity.setCustomName(villager.getCustomName());
        guardEntity.setCustomNameVisible(villager.isCustomNameVisible());
        guardEntity.setEquipmentDropChance(EquipmentSlot.HEAD, 100.0F);
        guardEntity.setEquipmentDropChance(EquipmentSlot.CHEST, 100.0F);
        guardEntity.setEquipmentDropChance(EquipmentSlot.FEET, 100.0F);
        guardEntity.setEquipmentDropChance(EquipmentSlot.LEGS, 100.0F);
        guardEntity.setEquipmentDropChance(EquipmentSlot.MAINHAND, 100.0F);
        guardEntity.setEquipmentDropChance(EquipmentSlot.OFFHAND, 100.0F);

        world.spawnEntityAndPassengers(guardEntity);
    }
}
