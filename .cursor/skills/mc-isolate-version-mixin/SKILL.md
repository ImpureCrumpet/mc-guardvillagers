---
name: mc-isolate-version-mixin
description: Places Fabric mixins and mixin JSON only under version subprojects with correct Yarn vs Mojmap shadows.
triggers:
  - mixin
  - mixins.json
  - "@Shadow"
  - bytecode patch
  - multi-version mixin
  - Fabric mappings
  - Mojmap
dependencies: []
version: "1.1.0"
---

# Isolate Version Mixin

**Scope:** Java/Kotlin **Fabric** mods with per-version Gradle subprojects (e.g. `mc1.21.11/`, `mc26.1/`). Not for Forge mixins unless the user’s project uses Fabric-style layout.

## CRITICAL — never use the shared root for mixins

**Do not** place `@Mixin` classes or **`mixins.*.json`** under the repository’s **shared root** `src/main/java` or `src/main/resources`.

A config in the shared root is picked up by **every** subproject. On **1.21.x**, targets are resolved through that version’s mapping layer (e.g. Intermediary). On **26.x**, the game uses **Mojang names** directly and the old remapping story does not apply the same way. One shared mixin config **cannot** satisfy both; the wrong version **crashes on load**.

## When to use

- Adding or editing **`@Mixin`** classes.
- Creating or updating **`mixins.<modid>.<version>.json`** (or the project’s naming convention).
- Fixing mixin targets, `@Shadow` / descriptors, or injectors after a Minecraft/Fabric upgrade.

## Action: isolate under the requested subproject only

For the **requested** subproject (e.g. `mc1.21.11/`, `mc26.1/`):

1. **Java:** Put mixin sources under  
   `mc<VERSION>/src/main/java/<package>/...`  
   only — not under root `src/main/java/`.

2. **Resources:** Put the mixin config under  
   `mc<VERSION>/src/main/resources/`  
   and register every mixin class there.

3. **Naming (align with repo + guide):** Examples from the plan-of-record doc:
   - `mixins.yourmod.1_21.json` — all targets for that 1.21 line
   - `mixins.yourmod.26_x.json` — all targets for that 26.x line  
   Replace `yourmod` / segment style with the mod’s actual convention; keep **one config per version line** that owns **all** mixins for that line.

4. **`fabric.mod.json`:** Ensure this subproject’s build points `mixins` (or `custom` mixin entry) at **this** JSON file, not a path in the shared root.

## Duplication across versions

If a mixin would be **identical** in two Minecraft versions, **still duplicate** the `.java` file (and config entries) into **each** subproject folder. Duplication is cheap; a **mapping-induced production crash** is not. Shared mixin code creates hidden coupling that breaks when names diverge.

## Mappings vs `@Shadow` / targets

| Target MC line | Names for `@Mixin` targets, `@Shadow`, method descriptors |
|----------------|------------------------------------------------------------|
| **1.21.x** | **Standard Fabric / Loom mappings** for that subproject (Yarn/Intermediary as configured). |
| **26.x** | **Un-obfuscated Mojang names** — verify `@Shadow` fields/methods and injectors against Mojmap-style names for that release. |

Always compile and run against the **same** subproject you edited (`./gradlew :mc…:build` or gametest as the project uses).

### Loom `migrateMappings` (Yarn ↔ Mojang on one MC version)

When **renaming** existing mixin sources to a new mapping set on the **same** Minecraft version (typical: **Yarn → Mojang** on **1.21.11** before 26.x), use Fabric Loom’s **`migrateMappings`** task; **Loom 1.13+** can migrate mixins and access wideners as part of that flow. Follow **[Migrating Mappings using Loom](https://docs.fabricmc.net/develop/porting/mappings/loom)** (order of operations: run task **before** switching `mappings { }` in Gradle per docs; default output **`remappedSrc/`**). After migration, **still** keep mixin classes under **per-version** `mc*/` trees — do not move mixins to shared root.

## Checklist

- [ ] No mixin `.java` and no `mixins*.json` in **root** `src/`.
- [ ] Mixin classes live under **`mc<VERSION>/src/main/java/...`** only.
- [ ] Config lives under **`mc<VERSION>/src/main/resources/...`** and lists every mixin.
- [ ] Shadows and targets match **1.21.x Fabric mappings** vs **26.x Mojang** per row above.

## Examples

- "Add a mixin targeting `PlayerEntity` for 1.21.11" → place under `mc1.21.11/src/main/java/…`, add to `mc1.21.11/src/main/resources/mixins.yourmod.1_21.json`, use Yarn names.
- "Port that mixin to 26.1" → duplicate `.java` into `mc26.1/src/main/java/…`, create `mixins.yourmod.26_x.json`, use Mojmap names for `@Shadow`/targets.
- "Can I share a mixin between 1.21.11 and 1.21.8?" → still duplicate into each `mc*/` tree; names may diverge silently between patches.

## Reference

Full rationale and examples: **“Rule: Mixins Are Always Version-Specific”** in **`.cursor/fabric-mod-build-release-guide-v4.3.md`**.
