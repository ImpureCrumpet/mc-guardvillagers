package dev.sterner.guardvillagers;

import eu.midnightdust.lib.config.MidnightConfig;

import java.util.ArrayList;
import java.util.List;

public class GuardVillagersConfig extends MidnightConfig {

    @Comment(category = "guards")
    public static String guardsIntro;

    @Entry(category = "guards")
    public static boolean guardEntitysOpenDoors = true;
    @Entry(category = "guards")
    public static boolean guardEntityFormation = true;
    @Entry(category = "guards")
    public static boolean guardEntitysRunFromPolarBears = false;
    @Entry(category = "guards")
    public static boolean followHero = true;
    @Entry(category = "guards")
    public static boolean clericHealing = true;
    @Entry(category = "guards")
    public static boolean armorerRepairGuardEntityArmor = true;
    @Entry(category = "guards", isSlider = true, min = 0, max = 10, precision = 10)
    public static float amountOfHealthRegenerated = 1F;
    @Entry(category = "guards", isSlider = true, min = 0, max = 1, precision = 100)
    public static float chanceToDropEquipment = 1F;
    @Entry(category = "guards")
    public static boolean useSteveModel = false;

    @Comment(category = "combat")
    public static String combatIntro;

    @Entry(category = "combat")
    public static boolean guardAlwaysShield = false;
    @Entry(category = "combat")
    public static boolean friendlyFire = true;
    @Entry(category = "combat")
    public static boolean guardArrowsHurtVillagers = true;
    @Entry(category = "combat")
    public static boolean attackAllMobs = false;
    @Entry(category = "combat")
    public static List<String> mobBlackList = new ArrayList<>();
    @Entry(category = "combat", isSlider = true, min = 8, max = 128, precision = 1)
    public static double guardVillagerHelpRange = 50;

    @Comment(category = "interaction")
    public static String interactionIntro;

    @Entry(category = "interaction", min = -100, max = 100)
    public static int reputationRequirement = 15;
    @Entry(category = "interaction", min = -100, max = 100)
    public static int reputationRequirementToBeAttacked = -100;
    @Entry(category = "interaction")
    public static boolean giveGuardStuffHotv = false;
    @Entry(category = "interaction")
    public static boolean setGuardPatrolHotv = false;
    @Entry(category = "interaction")
    public static boolean convertVillagerIfHaveHotv = false;

    @Comment(category = "village")
    public static String villageIntro;

    @Entry(category = "village")
    public static boolean raidAnimals = false;
    @Entry(category = "village")
    public static boolean witchesVillager = true;
    @Entry(category = "village")
    public static boolean blackSmithHealing = true;
    @Entry(category = "village")
    public static boolean illagersRunFromPolarBears = true;
    @Entry(category = "village")
    public static boolean villagersRunFromPolarBears = true;

    @Comment(category = "spawning")
    public static String spawningIntro;

    // One-time roll when each natural villager is first processed (e.g. after upgrade); set 0 for minimum-fill only.
    @Entry(category = "spawning", isSlider = true, min = 0, max = 1, precision = 100)
    public static float spawnChancePerVillager = 0.2f;
    @Entry(category = "spawning", min = 0, max = 32)
    public static int minimumGuardsPerVillage = 1;
    @Entry(category = "spawning", isSlider = true, min = 16, max = 128, precision = 1)
    public static int villageGuardClusterRadius = 64;

    @Comment(category = "stats")
    public static String statsIntro;

    @Entry(category = "stats", isSlider = true, min = 1, max = 100, precision = 1)
    public static double healthModifier = 20D;
    @Entry(category = "stats", isSlider = true, min = 0.1, max = 2, precision = 100)
    public static double speedModifier = 0.5D;
    @Entry(category = "stats", isSlider = true, min = 8, max = 64, precision = 1)
    public static double followRangeModifier = 20D;
}
