# Guard Villagers (Fabric)

Better village pest control — armed guards that defend villages, plus villager AI tweaks.

This repository is a **Fabric-only** fork maintained for continued development and integration with other Fabric mods. All work happens on **`main`** via Gradle subprojects — legacy version branches (1.20.x and older) are **not maintained** in this fork.

## Lineage

This mod has two upstreams:

| Role | Project | Links |
|------|---------|-------|
| **Fork parent** (Fabric port) | MrSterner's GuardVillagers | [GitHub — mrsterner/GuardVillagers](https://github.com/mrsterner/GuardVillagers) |
| **Original mod** | Guard Villagers by seymourimadeit | [Modrinth — guard-villagers](https://modrinth.com/mod/guard-villagers) · [GitHub — seymourimadeit/guardvillagers](https://github.com/seymourimadeit/guardvillagers) |

**This fork:** [ImpureCrumpet/mc-guardvillagers](https://github.com/ImpureCrumpet/mc-guardvillagers)

Gameplay and design credit belong to the original mod. This fork uses the Fabric port as a foundation and carries it forward on Fabric Loader only.

## Supported Minecraft versions

Built from `mc*` Gradle subprojects on **`main`**:

| Line | Versions shipped |
|------|------------------|
| **1.21.x** | 1.21, 1.21.1, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11 |
| **26.x** | 26.1, 26.1.1, 26.1.2, 26.2 |

**Not shipped:** 1.21.2, 1.21.3 (short-lived releases with limited Fabric API support).

**Out of scope:** anything before **1.21**.

### Build status

| Module | Status |
|--------|--------|
| `mc1.21`, `mc1.21.1` | Builds (early 1.21.x API band) |
| `mc1.21.4` | Builds (1.21.4+ API band shared tree) |
| `mc1.21.5` | Builds (`mc1.21.4` tree + `mc1.21.5` mid-band overlay) |
| `mc1.21.6` – `mc1.21.10` | WIP (shared overlays; per-version mixin API sync pending) |
| `mc1.21.11` | Builds (`mc1.21.4` + `mc1.21.5` overlays + `mc1.21.11` late-band overrides) |
| `mc26.1` – `mc26.2` | Builds (Mojmap; `mc26.1` base + `mc26.2` late-band overlays) |

## Requirements

- [Fabric Loader](https://fabricmc.net/use/)
- [Fabric API](https://modrinth.com/mod/fabric-api) (matched to your Minecraft release)
- [MidnightLib](https://modrinth.com/mod/midnightlib) (config UI)

**Java:** JDK **21** for 1.21.x modules. JDK **25** for 26.x modules (or when running a full build that includes 26.x).

## Building

```bash
# 1.21.x (JDK 21 — e.g. sdk use java 21.0.2-tem)
./gradlew :mc1.21:build

# 26.x (JDK 25 — run ./gradlew --stop after switching JDKs)
INCLUDE_26=true ./gradlew :mc26.2:build

# All 1.21.x modules (JDK 21)
./gradlew build

# Full matrix including 26.x (JDK 25)
INCLUDE_26=true ./gradlew build
```

26.x subprojects are included only when the Gradle JVM is **25+** or `INCLUDE_26=true` is set (see `settings.gradle`).

Output JARs: `mc<version>/build/libs/guardvillagers-<mod_version>+<minecraft>.jar`

## Project layout

| Location | Contents |
|----------|----------|
| `src/main/java/` | Shared mod logic (1.21.x Yarn mappings) |
| `src/main/resources/` | Shared assets, datapacks, lang |
| `mc1.21.*/` | Per-version module, mixins, `fabric.mod.json` |
| `mc26.1/` | 26.x Mojmap Java overrides (shared by all 26.x modules) |
| `mc26.*/` | 26.x modules (deps + resources) |

See [`.cursor/fabric-mod-build-release-guide-v4.3.md`](.cursor/fabric-mod-build-release-guide-v4.3.md) for the multi-version workflow.

## Natural guard spawning

Natural villagers can spawn guards on chunk load based on config (`spawnChancePerVillager`, `minimumGuardsPerVillage`, `villageGuardClusterRadius`). Each villager is processed at most once via the `guardvillagers.natural_spawn_processed` command tag.

**Upgrading existing worlds:** Natural villagers without that tag are processed once on their next chunk load. That can add guards from the spawn chance (default 20%) and fill empty clusters up to the minimum. This is a one-time migration, not ongoing duplication. Set `spawnChancePerVillager` to `0` if you only want minimum-fill on upgrade.

See [docs/civil-war-compat.md](docs/civil-war-compat.md) for Mob Civil War coexistence notes.

## License

This Fabric fork is released under [CC0 1.0 Universal](LICENSE) (public domain dedication), matching `fabric.mod.json`.

The original [Guard Villagers](https://modrinth.com/mod/guard-villagers) mod uses a separate custom license on its Forge/NeoForge distribution — see [seymourimadeit/guardvillagers](https://github.com/seymourimadeit/guardvillagers) for upstream terms.
