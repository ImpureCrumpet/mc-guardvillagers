---
name: mc-fabric-mojmap-migration-26x
description: "Routes and checklists the 1.21.x → 26.x migration: de-obfuscation, Mojmap-only sources, Loom plugin/Gradle config swap, Fabric API renames, removed deprecated APIs, and new 26.1 vanilla/Fabric subsystems."
triggers:
  - port to 26.1
  - port to 26.x
  - 1.21 to 26 migration
  - Mojmap migration
  - removing obfuscation
  - de-obfuscation
  - de-obfuscated Minecraft
  - migrateMappings
  - Yarn to Mojmap
  - ItemStackTemplate
  - ChunkSectionLayer
  - BlockColorRegistry
  - FluidModel
  - villager trade datapack
  - DimensionEvents
  - BlockEvents
  - ItemEvents
  - HudRenderCallback removed
  - fabric-convention-tags-v1
  - fabric-loot-api-v2
  - net.fabricmc.fabric-loom
  - remapJar gone
  - modImplementation removed
  - ItemGroupEvents rename
  - CreativeModeTabEvents
  - freeze 1.21.x
  - protect 1.21.x build
  - don't break 1.21.x
  - port without migrateMappings
  - 26.x regression check
dependencies:
  - mc-gradle-daemon-hygiene
  - mc-scaffold-new-fabric-subproject
  - mc-extract-fabric-version-adapter
  - mc-isolate-version-mixin
  - mc-fabric-minecraft-recipe-datapack
version: "1.1.0"
---

# Fabric 1.21.x → 26.x migration (Mojmap, de-obfuscation)

**Scope:** Fabric mods moving from a **Yarn or mixed-mapping 1.21.x** code base onto **26.x**, where Minecraft ships **un-obfuscated** at runtime, **Yarn is no longer published**, and several Fabric APIs were renamed or removed. This skill is the **router and checklist** — it owns items that have no other home, and points to deeper skills for the rest.

**Sources of truth** (read these when you need authoritative detail):

- [Fabric for Minecraft 26.1 — release blog](https://fabricmc.net/2026/03/14/261.html)
- [Mojang: Removing obfuscation in Java Edition](https://www.minecraft.net/en-us/article/removing-obfuscation-in-java-edition)
- [Migrating Mappings using Loom](https://docs.fabricmc.net/develop/porting/mappings/loom)
- `.cursor/fabric-mod-build-release-guide-v4.3.md` **§7 — Minecraft 26.1 — De-obfuscation & New Versioning** and **§8 — 26.1 Migration & Environment Checklist** (plan of record; this skill aligns with it)

## When to use

- A user says "port my mod to 26.1" / "26.x" / "remove obfuscation from my mod".
- An existing 1.21.x Yarn-based mod needs to ship a 26.x JAR.
- You are debugging "this used to compile on 1.21.11 and the same code does not compile under `mc26.1/`".
- You need a one-stop checklist that crosses Gradle, mappings, mixins, adapters, recipes, and removed/new APIs.

## Routing — what other skills own what

This skill owns the **migration narrative** and the items below. Defer the deep workflow to:

| Concern | Skill |
|---------|-------|
| JDK 25 daemon, `./gradlew --stop`, wrapper hygiene | `mc-gradle-daemon-hygiene` |
| Creating `mc26.x/` (and `mc1.21.x/`) subprojects, `settings.gradle.kts`, `fabric.mod.json` per subproject | `mc-scaffold-new-fabric-subproject` |
| Per-subproject mixins (Yarn vs Mojmap), `migrateMappings` for mixins, no shared-root mixins | `mc-isolate-version-mixin` |
| Shared interfaces in root `src/`, version-specific impls (e.g. `ItemGroupEvents` ↔ `CreativeModeTabEvents`) | `mc-extract-fabric-version-adapter` |
| Recipe JSON datapack moves (≤1.21.1 object → 1.21.2+ string), result `item` → `id`, custom `CraftingRecipe` pitfalls | `mc-fabric-minecraft-recipe-datapack` |

Load those when their specific work is up next.

## Protecting stable 1.21.x during a 26.x port

The per-`mc*/` subproject layout architecturally isolates **most** version-specific code (Java sources, mixins, `build.gradle.kts`, `fabric.mod.json`, recipe JSON via override). That's strong protection — a change in `mc26.1/build.gradle.kts` cannot affect a `mc1.21.11` compile, and a mixin under `mc26.1/src/main/resources/` cannot poison 1.21.x mixin loading.

**But four bleed surfaces remain.** The default assumption when porting a stable mod is *"do not regress 1.21.x"* — these are the rules:

### 1. Shared root `src/main/java/` is mapping-neutral

Any class in shared root must compile under **both** mapping vocabularies your subprojects use. Concretely:

- **No 26.x-only names** (e.g. `CreativeModeTabEvents`, `ItemStackTemplate`, `BlockColorRegistry`, `FluidModel`, `ChunkSectionLayer`). These exist only on 26.x and won't resolve under `mc1.21.x/` mappings.
- **No 1.21.x-only Yarn names** (e.g. `ItemGroupEvents` if you've adopted Mode A and `mc1.21.x/` is still on Yarn). They won't resolve in 26.x mappings.
- **If shared code needs to interact with a divergent API:** define a `*Adapter` interface in shared root, implement per-subproject. Load `mc-extract-fabric-version-adapter` for the pattern.
- **`ItemStackTemplate` in particular:** if shared root ever does `new ItemStack(item, n)` at a static-init or registration callsite, that callsite is a porting hazard — it works on 1.21.x but throws on 26.1 because no world is loaded yet. Adapt: put early-creation behind an interface that returns an `ItemStack` (1.21.x impl) or `ItemStackTemplate` (26.x impl), or — easier — defer the creation to runtime and keep shared code mapping-neutral.

### 2. Shared root `src/main/resources/` rarely needs edits during a port

If you find yourself editing a shared resource for the 26.x port (a model JSON, a tag, a localization), ask whether it should be a per-subproject override under `mc26.1/src/main/resources/<same path>` instead. Recipe JSON has a documented override flow already (`mc-fabric-minecraft-recipe-datapack`). Apply the same instinct to other resources: the safe default is "26.x-only resource lives under `mc26.1/`".

### 3. Root `build.gradle.kts` and `gradle.properties` are shared — scope edits

- **Plugin versions** at root affect all subprojects. Bumping Loom or Fabric Loom version because 26.x needs a newer Loom can break 1.21.x configure phases. If a bump is required, run `./gradlew :mc1.21.11:build` immediately after to confirm; if it regresses, push the version override down into `mc26.1/buildscript { }` instead of root.
- **`subprojects { }` blocks** in root: when you must add 26.x-specific config there, scope it by name:

  ```kotlin
  subprojects.matching { it.name.startsWith("mc26.") }.configureEach {
      // 26.x-only config here
  }
  ```

  Never put unconditional `subprojects { }` config that only makes sense for one mapping era.
- **`gradle.properties`:** `org.gradle.jvmargs` is shared and a heap bump for 26.x builds is fine; just verify 1.21.x still configures cleanly afterwards.

### 4. Regression check after every 26.x change set

The migration is iterative. After each meaningful 26.x edit (especially anything that touched shared root or root buildscript), run:

```bash
./gradlew :mc1.21.11:build :mc1.21.11:runGametest
```

If 1.21.x is red, the most recent edit is almost certainly the cause. Don't proceed with more 26.x work until 1.21.x is green again. This is the rule the **Phase 9** checklist enforces; it's worth applying continuously, not just at the end.

> **Quick mental model:** *Every commit during the port should leave both `:mc1.21.11:build` and the in-progress `:mc26.1` work compilable (the latter possibly with TODOs). If a commit breaks 1.21.x, revert and re-do via an adapter or per-subproject override.*

---

## Phased migration checklist

Mirrors guide §8 with the items the guide doesn't fully list.

### Phase 0 — Environment

- [ ] **JDK 25** is on disk (Temurin or equivalent). Switch to it via your tool of choice (e.g. `j25` if SDKMAN). 1.21.x subprojects still build under JDK 21 via toolchain.
- [ ] **Gradle wrapper:** `./gradlew wrapper --gradle-version 9.4.0` (full three-part version).
- [ ] **Loom:** `fabric-loom` **1.15** (or newer compatible patch). On 26.x, see **Phase 2** for the plugin-id rename.
- [ ] **IDE:** IntelliJ **2025.3+** required for Mixins to work correctly under 26.1 per the [Fabric 26.1 blog](https://fabricmc.net/2026/03/14/261.html). Cursor/VS Code users: set the Gradle JVM to **JDK 25** for any project that contains a 26.x subproject.
- [ ] Daemon hygiene: load **`mc-gradle-daemon-hygiene`** before any `./gradlew` invocation, especially after switching JDKs.

### Phase 1 — Decide the layout

26.x **adds** a subproject; it does not replace 1.21.x. The repo lands on the **default anchor** (`mc1.21.11` + `mc26.1`) per guide §3 unless there is a specific reason to backfill older 1.21.x lines. Older 1.21.x → opt-in only.

- [ ] Create or confirm `mc26.1/` exists (load **`mc-scaffold-new-fabric-subproject`** if not).
- [ ] Confirm shared root `src/` only holds **version-agnostic** code. Mixins in root are forbidden — load **`mc-isolate-version-mixin`** to verify and split.
- [ ] Update the mod repo `README.md` to list **exactly** which Minecraft versions ship JARs (guide §3 README rule). Don't claim "all of 1.21.x".

### Phase 2 — Mappings strategy (**ask the user first**)

There are **two valid paths** across the Mojmap divide. Default to **Mode A** unless the user explicitly chooses Mode B. **Ask once** before touching mappings if it isn't already clear.

#### Mode A — Freeze and port (default, recommended for stable 1.21.x mods)

Use when 1.21.x is **stable and shipping** and the goal is "add 26.x support without disturbing 1.21.x". This is the **most common case** for porting work.

- [ ] **Do not** run `migrateMappings`. Leave `mc1.21.x/src/main/java/` on its current mappings (Yarn or whatever it's on).
- [ ] In `mc1.21.x/build.gradle.kts`, keep `mappings(...)` as-is (e.g. Yarn, or `loom.layered { officialMojangMappings(); parchment(...) }`).
- [ ] In `mc26.1/build.gradle.kts`, mappings are layered Mojmap by Loom (no `mappings(...)` declaration needed beyond what guide §2 shows for 26.x). 26.1 has no Yarn published; do not try to use it.
- [ ] Write **fresh Mojmap-named impl classes directly** in `mc26.1/src/main/java/...`. They are not copies of `mc1.21.x/` impls; they share only the **interface contract** from shared root `src/`.
- [ ] **Shared root `src/` must stay mapping-neutral** — see "Protecting stable 1.21.x" below. Anything that references a name only valid in one mapping vocabulary belongs in a per-`mc*/` impl behind a shared interface.
- [ ] Per-version `@Shadow` names: keep Yarn-style in `mc1.21.x/`, write Mojmap-style in `mc26.1/`. Duplication is required (see `mc-isolate-version-mixin`).

**Cost:** You write the 26.x impl by hand against Mojmap names rather than letting Loom rewrite for you. **Benefit:** Your stable 1.21.x sources are not modified. Zero regression risk on the 1.21.x build.

#### Mode B — Migrate then port (opt-in)

Use only when the user **explicitly wants** to bring `mc1.21.x/` sources onto Mojmap too — e.g. they plan to keep actively developing 1.21.x, want one mapping vocabulary across the codebase, or are willing to re-stabilize 1.21.x on Mojmap before adding 26.x.

- [ ] Confirm with the user that rewriting `mc1.21.x/src/main/java/` is acceptable.
- [ ] Ensure Loom is at **1.13+** so `migrateMappings` covers **mixins** and **access wideners** (newer Loom also covers split client source sets, class tweakers).
- [ ] Run on the current MC version, not 26.x: `./gradlew :mc1.21.11:migrateMappings --mappings "net.minecraft:mappings:1.21.11"` (scope to the relevant subproject).
- [ ] Default output is `remappedSrc/`. Merge into `mc1.21.11/src/main/java` (or use `--overrideInputsIHaveABackup` on Loom 1.13+ — only if you have a real backup; commit before running).
- [ ] Switch `dependencies { mappings … }` for `mc1.21.x` to `loom.officialMojangMappings()` **after** the migration task completes, per the [Fabric Loom mappings docs](https://docs.fabricmc.net/develop/porting/mappings/loom).
- [ ] Manually review mixin targets, `@Shadow` names, and code the task missed. (Resources: mappings.dev, Linkie.)
- [ ] Re-run `./gradlew :mc1.21.11:build :mc1.21.11:runGametest` and confirm 1.21.x is back to green **before** writing any `mc26.1/` code. Mode B is a two-step stabilization, not a single push to 26.x.

### Phase 3 — `mc26.1/build.gradle.kts` (the de-obfuscation buildscript swap)

Per Fabric blog: 26.x uses a **different Loom plugin id** that does not remap. The old `net.fabricmc.fabric-loom-remap` (and the legacy `fabric-loom` id when it resolved to the remapping plugin) is replaced by the new `net.fabricmc.fabric-loom`.

- [ ] `plugins { id("net.fabricmc.fabric-loom") version "1.15" }` for the **26.x** subproject (or use the version catalog). Do **not** rely on the bare `id("fabric-loom")` shorthand for 26.x without verifying which plugin it resolves to.
- [ ] Replace **`modImplementation`** with **`implementation`** (and `modCompileOnly` → `compileOnly`, etc.) for `fabric-loader` and `fabric-api` in `mc26.x/`. **Keep** `modImplementation` in `mc1.21.x/`.
- [ ] Remove `remapJar`. Configure the standard `jar` task instead.
- [ ] Drop `mappings(loom.officialMojangMappings())` is **not** required for 26.x runtime, but Loom still layers Mojmap metadata for IDE/parameter names; follow the plan-of-record example in guide §2 — `mc26.1` block.
- [ ] Use a current Fabric Loader version (`0.18.x` line at time of writing).

Reference example: guide §2 — *Version `build.gradle.kts` (`mc26.1/`)*.

### Phase 4 — `fabric.mod.json` cleanup

- [ ] **Remove** any `depends` entry for the legacy `fabric` mod-id. Fabric API no longer publishes it (deprecated for 3+ years, [removed in 26.1](https://fabricmc.net/2026/03/14/261.html)). Use `fabric-api` instead.
- [ ] Update `depends.minecraft` to `>=26.1` (or whatever 26.x line this subproject targets).
- [ ] Update `depends.fabric-api` to the exact 26.x API version you put in Gradle.
- [ ] Add the Mojmap `entrypoints` and `fabric-gametest` block per guide §4 (the gametest helper class typically stays in shared `src/test/`).

### Phase 5 — Translate Fabric API renames

26.1 renamed many API classes to match Mojang's official names. **Do not rename in shared code** — instead use the adapter pattern so 1.21.x and 26.x can both compile. Load **`mc-extract-fabric-version-adapter`** for the full pattern.

The well-known examples (non-exhaustive):

| 1.21.x (Yarn-style) | 26.1 (Mojmap-style) |
|---------------------|----------------------|
| `ItemGroupEvents.modifyEntriesEvent` | `CreativeModeTabEvents.modifyEntriesEvent` |
| `ColorProviderRegistry.BLOCK.register(…)` | `BlockColorRegistry.register(List.of(BlockTintSource …), block)` |
| `FluidRenderHandler` (registered via `FluidRenderHandlerRegistry`) | `FluidModel` (registered via `FluidRenderingRegistry.register`) |
| `TradeOfferHelper` (Java registration) | **Datapack:** `data/<ns>/villager_trade/*.json` + `data/<ns>/trade_set/*.json` + `data/minecraft/tags/villager_trade/<profession>` |
| `HudRenderCallback` | `HudElementRegistry` |

For the full rename table and the **IntelliJ migration map** that automates the bulk of these, follow the rename appendix linked from the [Fabric 26.1 blog](https://fabricmc.net/2026/03/14/261.html). Run the IntelliJ map **inside the `mc26.1/` source tree only** so 1.21.x sources are untouched.

### Phase 6 — Removed Fabric APIs (no replacement, must rewrite)

These were removed in 26.1 ([blog](https://fabricmc.net/2026/03/14/261.html)). If the mod uses them on 1.21.x, the 26.x port must rewrite or drop the feature:

- [ ] `fabric-convention-tags-v1` — convention tags labeled for 1.22 removal are gone (ticket #5056). Audit `tags/items/c/…` and `tags/blocks/c/…` references and migrate to current convention tags.
- [ ] `fabric-loot-api-v2` — removed. Use the current loot API surface.
- [ ] `HudRenderCallback` — removed in favor of `HudElementRegistry`.
- [ ] Bare `fabric` mod-id dependency — drop, depend on `fabric-api`.

### Phase 7 — New 26.1 vanilla subsystems (likely to bite)

These are vanilla-side changes, not Fabric. Code that compiled cleanly on 1.21.11 may need rework on 26.1 even after the rename pass:

- [ ] **`ItemStackTemplate`:** `ItemStack` can no longer be created **before a world is loaded**. Static initializers, registration code, mod menu icons, and similar early callsites that did `new ItemStack(item, n)` must produce an **`ItemStackTemplate`** instead. Components on a template are unbound until the world launches — accessing them too early throws.
- [ ] **`ChunkSectionLayer`** replaces `RenderType` / `RenderLayer` for terrain. Block render-layer assignment is now **automatic** based on sprite transparency (translucent / cutout / solid). Drop manual `BlockRenderLayerMap` calls. Override only via block model JSON or `MutableQuadView` + Model Loading API when needed.
- [ ] **Recipe serializers** simplified: each recipe class no longer needs an inner `RecipeSerializer`. Register `new RecipeSerializer<>(ModRecipe.CODEC, ModRecipe.STREAM_CODEC)` directly. Existing custom recipes need their serializer collapsed to a `MapCodec` + `StreamCodec` pair. (Datapack-side JSON shifts are covered by **`mc-fabric-minecraft-recipe-datapack`**.)
- [ ] **Villager trading is data-driven.** Move any `TradeOfferHelper` Java registration into:
  - `data/<namespace>/villager_trade/*.json` (one trade)
  - `data/<namespace>/trade_set/*.json` (a set of trades)
  - `data/minecraft/tags/villager_trade/<profession>/*.json` (attach to profession)
- [ ] **Fluids:** Replace `FluidRenderHandler` / `FluidRenderHandlerRegistry` registration with `FluidModel.Unbaked` + `FluidRenderingRegistry.register`.
- [ ] **Block colors:** `ColorProviderRegistry.BLOCK` → `BlockColorRegistry.register(List.of(BlockTintSource …), block)`.
- [ ] **New event surfaces** (use only inside `mc26.1/` impls):
  - `DimensionEvents.MODIFY_ATTRIBUTES` — replace per-biome iteration when the change is dimension-wide.
  - `BlockEvents#USE_ITEM_ON`, `BlockEvents#USE_WITHOUT_ITEM`, `ItemEvents#USE_ON`, `ItemEvents#USE` — finer-grained than the older `UseBlockCallback` / `UseItemCallback`.

### Phase 8 — OpenGL / Vulkan note (forward-looking)

26.1 is expected to be the **last** release that solely supports OpenGL ([blog](https://fabricmc.net/2026/03/14/261.html)). Mods making **raw OpenGL** calls (rather than going through the Blaze3D API) should plan a Blaze3D migration before 26.2 lands. If your mod uses Blaze3D only, no action.

### Phase 9 — Test, tag, publish

- [ ] **1.21.x regression gate (mandatory):** Before tagging, `./gradlew :mc1.21.11:build :mc1.21.11:runGametest` (and the same for any other shipped 1.21.x line) must be green. If 26.x work has touched shared root or root buildscript, this is the only way to confirm 1.21.x didn't silently regress. **Do not** proceed past this checkbox if 1.21.x is red.
- [ ] **26.x verification:** `./gradlew :mc26.1:build :mc26.1:runGametest` is green. Load **`mc-scaffold-new-fabric-subproject`** for `FabricGameTestHelper` wiring.
- [ ] **Smoke-test in-game:** boot the **oldest** and **newest** shipped MC lines, exercise the mod's main features. Compile-green ≠ runtime-green; the new 26.1 vanilla subsystems (Phase 7) bite at runtime, not at compile.
- [ ] **Tag** the release with the per-version JAR naming from guide §5.
- [ ] **Publish.** If uploading to Modrinth, the registry-driven flow lives in `mm-modrinth-upload` (Minder-only, not part of this portable skill).

## Common pitfalls

- **Renaming in shared `src/`:** Tempting after the IntelliJ migration map, but it breaks the 1.21.x build. Always go through an interface in shared code; impls per `mc*/`. (`mc-extract-fabric-version-adapter`.)
- **Forgetting the daemon JDK:** Switching from `mc1.21.11` work (JDK 21 daemon) to `mc26.1` work (JDK 25 daemon) without `./gradlew --stop` produces cryptic Loom configure-time errors. (`mc-gradle-daemon-hygiene`.)
- **Putting mixins in shared root because "they look identical":** A shared mixin config crashes one of the two versions because Yarn vs Mojmap descriptors differ. Duplicate per `mc*/`. (`mc-isolate-version-mixin`.)
- **Using `id("fabric-loom")` shorthand on 26.x:** May resolve to the legacy remap plugin depending on Loom version. Use the explicit `id("net.fabricmc.fabric-loom")` for 26.x subprojects.
- **Static `new ItemStack(...)` in registration code:** Will throw on 26.1 because no world is loaded. Switch to `ItemStackTemplate` for early references; defer real `ItemStack` construction to runtime.
- **Recipe JSON moved but not updated:** `data/<ns>/recipes/` (plural) → `data/<ns>/recipe/` (singular) at 1.21+; ingredient object → string at 1.21.2+; result `item` → `id`. (`mc-fabric-minecraft-recipe-datapack`.)

## Examples

- "Port my Yarn-based 1.21.11 mod to 26.1" → Phase 0 env, Phase 2 `migrateMappings` on 1.21.11, scaffold `mc26.1/` (`mc-scaffold-new-fabric-subproject`), Phase 3 buildscript swap, Phase 5 adapters, Phase 6/7 rewrite removed/changed APIs, Phase 9 gametest.
- "My 26.1 build fails because `ItemGroupEvents` is unresolved" → expected: that class is renamed to `CreativeModeTabEvents` in 26.1. Use the adapter pattern (`mc-extract-fabric-version-adapter`) so 1.21.x keeps the old name.
- "My static `ItemStack` icon constants throw at startup on 26.1" → switch to `ItemStackTemplate` for early references; build the real `ItemStack` lazily.
- "Where do villager trades go now?" → Phase 7 — three datapack folders (`villager_trade`, `trade_set`, `tags/villager_trade/<profession>`).
- "Why is my mixin config wrong on 26.1 even after `migrateMappings`?" → load `mc-isolate-version-mixin`; mixin descriptors must match per-version mappings, and shared-root mixin configs are forbidden.

## Reference

- `.cursor/fabric-mod-build-release-guide-v4.3.md` — §7 De-obfuscation & New Versioning, §8 26.1 Migration & Environment Checklist, §2 Gradle config (1.21.x and 26.1 examples).
- [Fabric for Minecraft 26.1](https://fabricmc.net/2026/03/14/261.html) — full rename map link, removed APIs, new event/registry surfaces.
- [Removing obfuscation in Java Edition](https://www.minecraft.net/en-us/article/removing-obfuscation-in-java-edition) — Mojang's own announcement and rationale.
- [Migrating Mappings using Loom](https://docs.fabricmc.net/develop/porting/mappings/loom) — `migrateMappings` task, mixin/AW handling, output flow.
