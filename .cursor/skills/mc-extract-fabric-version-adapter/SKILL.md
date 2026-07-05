---
name: mc-extract-fabric-version-adapter
description: Defines version-agnostic Java/Kotlin APIs and implements them per Minecraft/Fabric subproject.
triggers:
  - adapter
  - multi-version API
  - Fabric API rename
  - ModInitializer
  - Minecraft registry abstraction
  - shared src
dependencies: []
version: "1.2.0"
---

# Extract Fabric Version Adapter

**Scope:** Java/Kotlin **Fabric** mods with a **multi-version Gradle layout** (shared root `src/` + per-version subprojects like `mc1.21.11/`, `mc26.1/`). Not for Forge-only or single-version projects unless the user is introducing this layout.

## When to use

- Shared code calls APIs that **differ by Minecraft/Fabric version** (e.g. `ItemGroupEvents` vs `CreativeModeTabEvents` on 26.x).
- You need one place for **business logic** and separate places for **mapping-specific** calls.

## Do not mix with shared Mixins

**Never** put mixin configs or `@Mixin` classes in the shared root. Version-specific bytecode targets belong under each subproject’s `src/main/resources/` and `src/main/java/`. Adapters solve **Java API** differences; mixins stay isolated per version.

## Workflow

### 1. Define the contract (shared root)

Add a **version-agnostic interface** (or small API surface) under:

`src/main/java/<your base package>/...`

- Use **stable names** in the interface (`registerCreativeTabs`, not `ItemGroupEvents` names).
- Keep it minimal: only what shared code needs.

### 2. Implement per active subproject

Add the **implementing class** under the **active** version folder only, e.g.:

- `mc1.21.11/src/main/java/<package>/...Impl12111.java` (example suffix; match repo conventions)

**Mappings & names (critical):**

| Line | Mapping style | Implementation detail |
|------|----------------|------------------------|
| **1.21.x** | **Yarn / Intermediary** (per subproject Loom setup) | Use names and calls valid for that toolchain (e.g. `ItemGroupEvents` where that is correct for the mod’s 1.21 setup). |
| **26.x** | **Mojmap** (un-obfuscated Mojang names at runtime) | Use Mojang/Fabric API names for that release (e.g. `CreativeModeTabEvents`). Loom still layers Mojmap for IDE/metadata. |

Do not assume one implementation compiles in another subproject; **duplicate** adapter classes per version when APIs diverge.

### Optional: bulk mapping rename on one Minecraft version first

If the whole mod still uses **Yarn** (or non-Mojmap) sources on **1.21.x** and you plan **Mojmap-only** 26.x subprojects, run Loom’s **`migrateMappings`** on the **current** MC version **before** relying on hand-renames. Fabric documents the Gradle task, `remappedSrc` vs `--overrideInputsIHaveABackup`, and updating `mappings` in `build.gradle` **after** the task — see **[Migrating Mappings using Loom](https://docs.fabricmc.net/develop/porting/mappings/loom)** and **`.cursor/fabric-mod-build-release-guide-v4.3.md`** (Gradle § *Migrating mappings with Loom*). Adapters in this skill still apply for **API** differences between lines (e.g. Fabric event renames), not only mapping IDs.

### 3. Wire from the subproject entrypoint

In the **subproject’s** `ModInitializer` (or Kotlin entrypoint):

- **Construct** the concrete adapter for **that** Minecraft version.
- **Inject** or **register** it with shared/core code (constructor, static holder, small registry, or DI pattern already used in the mod).

Shared initialization code should depend on the **interface** only and receive the implementation **once at startup** from the versioned entrypoint.

## Minimal pattern (from “Handling API Changes”)

**Shared:**

```java
public interface CreativeTabAdapter {
    void registerItems();
}
```

**1.21.x subproject:** implement using APIs valid under that subproject’s mappings.

**26.x subproject:** implement using Mojmap-aligned Fabric/vanilla names (e.g. `CreativeModeTabEvents` instead of `ItemGroupEvents`).

## Checklist

- [ ] Interface lives in **root** `src/main/java/`.
- [ ] Implementation lives only under **`mcX.Y.Z/src/main/java/`** (or `mc26.x/...`) for the version being built.
- [ ] Subproject **ModInitializer** creates the impl and passes it into shared code.
- [ ] No version-specific class names leaked into shared code beyond the interface.
- [ ] Mixins remain **out** of shared root.

## Common adapter targets (beyond creative tabs)

The adapter pattern applies to **any** API that diverges across versions, not only Fabric event renames. Common real-world cases:

| API area | What diverges | Adapter shape |
|----------|--------------|---------------|
| **Recipe serializers** | `CustomRecipe` registration changed between **1.21/1.21.1** and **1.21.2+** (older `RecipeSerializer.register()` vs newer codec-based flow). | Interface: `RecipeRegistrationAdapter.registerRecipes()`. Per-version impl calls the correct registration API. |
| **Entity/item registration** | `EntityType.Builder.build(...)` signature, `Item.Properties.setId()` — changed across 1.21.x patches. | Interface: `RegistrationAdapter.registerEntities()` / `registerItems()`. Each `mc*` impl uses the builder API valid for that patch. |
| **Particle options** | `SpellParticleOption` vs alternatives across **1.21.4** / **1.21.5+**. | Interface: `ParticleAdapter.createSpellParticle(...)`. |
| **Components / data** | `POTION_DURATION_SCALE`, `Unit` stream codec — differ around **1.21.5**. | Interface or helper method in a `ComponentAdapter`. |

These sit alongside the **26.x** adapter layer (Mojmap renames). When building a full 1.21.x ladder, you may need **two** adapter layers: one for 1.21.x API bands and one for the 26.x boundary.

## Examples

- "Creative tab registration changed between 1.21 and 26.x" → define `CreativeTabAdapter` interface in root `src/`, implement with `ItemGroupEvents` under `mc1.21.11/`, implement with `CreativeModeTabEvents` under `mc26.1/`.
- "Shared code needs to call a renamed Fabric API method" → add a one-method interface in root `src/`, implement per subproject, inject via `ModInitializer`.
- "Recipe registration differs between 1.21.1 and 1.21.2" → define `RecipeRegistrationAdapter` in root `src/`, implement old serializer API under `mc1.21.1/`, implement codec-based API under `mc1.21.2/` and newer modules.
- "Entity builder changed signature around 1.21.5" → define `EntityRegistrationAdapter`, implement per API band, wire from each `ModInitializer`.

## Optional reference

For full toolchain context (Gradle, Loom, Java 21 vs 25, `modImplementation` vs `implementation` on 26.x), read **“Handling API Changes (Adapters)”** and **“Project Structure & The Adapter Pattern”** in **`.cursor/fabric-mod-build-release-guide-v4.3.md`** (same path as this skill when you copy `.cursor/` into a mod repo).
