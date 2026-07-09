package dev.sterner.guardvillagers.integration;

import dev.sterner.guardvillagers.GuardVillagers;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optional Mob Civil War coexistence — no compile-time dependency on {@code civil-war}.
 *
 * <p>Guards are village defenders ({@link dev.sterner.guardvillagers.common.entity.GuardEntity})
 * and intentionally stay outside Civil War faction AI. This class detects CW at runtime and
 * documents policy; it does not call {@code CivilWarIntegration.applyFactionGoals} on guards.
 *
 * @see docs/civil-war-compat.md
 */
public final class CivilWarCompat {

    private static final String CIVIL_WAR_MOD_ID = "civil-war";
    private static final Logger LOGGER = LoggerFactory.getLogger(GuardVillagers.MODID);

    private static Boolean loaded;

    private CivilWarCompat() {
    }

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = FabricLoader.getInstance().isModLoaded(CIVIL_WAR_MOD_ID);
        }
        return loaded;
    }

    public static void onGuardVillagersInitialized() {
        if (isLoaded()) {
            LOGGER.info(
                    "Mob Civil War ({}) detected — guards remain village defenders outside faction AI; see docs/civil-war-compat.md",
                    CIVIL_WAR_MOD_ID);
        }
    }
}
