# Fabric Mod Build, Release & Modernization Guide

*v4.3.1 — **Portable bundle:** this file plus **`.cursor/skills/`** copied into a mod repo; optional machine-readable version matrix **`.cursor/fabric-loader-yarn-fabric-api.yaml`** (locked through **26.2**). Toolchain: **SDKMAN!** (**`j21` / `j25`**), **no Foojay resolver plugin**, Gradle daemon vs toolchain hygiene, IDE/CI notes. Older guide snapshots: **git history** or local archive only.*

*Covers: multi-version **`mc*`** layout, **§3 default = anchor only** (`mc1.21.11` + `mc26.1`); optional wider 1.21.x ladder with **API band caveats**; 26.x transition (de-obfuscation + versioning).*

---

## Cursor skills (portable bundle)

These skills complement this guide when you copy **`.cursor/`** into a mod repo:

| Focus | Path |
|-------|------|
| Gradle daemon lifecycle, `JAVA_HOME`, assets, wrapper, **heap** | `.cursor/skills/mc-gradle-daemon-hygiene/SKILL.md` |
| New `mc*` subproject scaffolding | `.cursor/skills/mc-scaffold-new-fabric-subproject/SKILL.md` |
| Version-isolated mixins (`mc*` only) | `.cursor/skills/mc-isolate-version-mixin/SKILL.md` |
| Version adapters (interfaces in root, implementations per `mc*`) | `.cursor/skills/mc-extract-fabric-version-adapter/SKILL.md` |
| Cross-version crafting-recipe JSON (datapack paths, ingredient/result format shifts, `CraftingRecipe` pitfalls) | `.cursor/skills/mc-fabric-minecraft-recipe-datapack/SKILL.md` |
| **1.21.x → 26.x migration router** (Mojmap, Loom plugin/Gradle swap, Fabric API renames, removed APIs, new 26.1 vanilla/Fabric subsystems) | `.cursor/skills/mc-fabric-mojmap-migration-26x/SKILL.md` |
| GitLab mirror push (SSH default, or HTTPS + PAT from `.env.gitlab-mirror`) | `.cursor/skills/mc-gitlab-mod-mirror-push/SKILL.md` |

**Optional companion artifact:** **`.cursor/fabric-loader-yarn-fabric-api.yaml`** — machine-readable Fabric coordinates (Minecraft version, loader, Loom, Fabric API, mapping type) for scripted or matrix builds. The YAML includes a `mappings` field (`yarn` vs `mojmap`) and optional `notes` per row. It is **not** required — the tables in **§3** are authoritative. **Rename note:** older copies used **`Fabric_Loader_Yarn_Fabric API.yaml`** (space in filename); use the kebab-case name above in scripts and deploy flows.

**Minecraft Minder registry:** Skills prefixed **`mm-*`** (for example **`mm-github-issues-mod-registry`**) stay in the **Minder** repo only. Deployable Fabric/build skills use **`mc-*`** in both Minder and mod repos (see **`mm-deploy-fabric-skills-to-repo`**).

**`mc-gradle-daemon-hygiene`** is the checklist-style companion to **§ Prerequisites — Gradle daemon, wrapper & hygiene** below.

**Build-script DSL:** Examples in this guide use **Kotlin DSL** (`build.gradle.kts`, `settings.gradle.kts`). The same patterns apply to **Groovy DSL** (`build.gradle`, `settings.gradle`) — translate syntax as needed (single quotes, no `val`, `=` instead of `.set()`). Most Fabric mod templates ship Kotlin DSL; use whichever the existing repo already uses.

---

## Core Philosophy

Avoid "version branches" and manual cherry-picking. Use a single `main` branch with Gradle subprojects for each Minecraft version. Shared code lives in the root `src/`; version-specific code and dependencies live in sub-folders. This same architecture carries forward into the 26.x era with minimal adjustment.

---

## Prerequisites & Setup

- **Java 21** for 1.21.x subprojects and **Java 25** for 26.x subprojects (26.1 Snapshot 1 onward), installed and selected via **SDKMAN!** (Temurin builds recommended). Use the **`j21`** and **`j25`** shell aliases before running Gradle so the correct JDK is on your `PATH` / `JAVA_HOME`.
- **Do not** add the Gradle **`foojay-resolver-convention`** plugin. It can provision JDKs independently of SDKMAN! and duplicates or bypasses the JDKs you manage. With Foojay removed, rely on JDKs installed through SDKMAN! (and your aliases) so one source of truth controls versions.
- **Gradle toolchain resolution:** The root build still declares Java **21** vs **25** per subproject via `java { toolchain { ... } }`. Gradle resolves those toolchains from **installed** JDKs. If resolution fails, confirm both versions are installed (`sdk list java` / `sdk install`) and run `j21` or `j25` in the shell you use for `./gradlew`, or set `org.gradle.java.home` when needed.
- **Gradle 9.4.0** (required as of 26.1 Snapshot 1; may change with future updates). Run `./gradlew wrapper --gradle-version 9.4.0`.
- **Loom 1.15+** minimum for 26.1; **1.17.12** is the locked default for **26.x** builds in this workspace (see **`.cursor/fabric-loader-yarn-fabric-api.yaml`**).
- **Fabric Loader:** **`0.18.6`** on **1.21.x** subprojects; **`0.19.3`** on **26.x** (locked through **26.2**). Loader is forwards-compatible within a major line — do not mix older 0.18.x on 26.x modules.
- **IDE:** **Cursor** or **VS Code** (with **`.cursorrules`** in the **mod repo** root so assistants use `j21`/`j25`), or **IntelliJ IDEA 2025.3+** (strong choice for Mixin-heavy 26.1 work).
- **Node / Python:** Core Fabric builds do not require them. When a mod or script needs them, use **fnm** (Node) and **uv** (Python) per your environment.
- **Optional housekeeping:** If stray **`package-lock.json`** under **`~`** causes tooling confusion, remove it (see v2 notes).

### .gitignore Essentials

```text
.gradle/
build/
.DS_Store
*.jar
*.class
.cursorrules
```

### Gradle daemon, wrapper & hygiene

`java { toolchain { } }` controls **compile targets**; it does **not** replace the JDK that **launches the Gradle daemon**. **Fabric Loom** resolves Minecraft assets and mappings at **configure time** inside that process. If the daemon’s JDK is wrong, toolchain lines in Gradle do not fix it.

- **Before `./gradlew`:** run **`j21`** when you only build **1.21.x** subprojects, and **`j25`** when you build **any 26.x** subproject or a **full multi-version build** that includes 26.x. JDK **25** can compile Java **21** targets via toolchain; JDK **21** cannot satisfy Loom’s **26.x** configure-time requirements.
- **Switching JDKs:** stop stale daemons so a new one picks up `JAVA_HOME`: `./gradlew --stop`. To debug which JDK a daemon uses: `./gradlew --status`.
- **Wrapper URLs:** use the **full three-part** distribution name (e.g. `gradle-9.4.0-bin.zip`). When fetching manually, use `curl -fL` so HTTP errors fail fast instead of saving an error page as the zip.
- **Supply chain:** [Aikido Safe Chain](https://github.com/AikidoSec/safe-chain) wraps **npm, pnpm, uv, pip** at the machine level — it does **not** cover Gradle/Maven. It protects Python tooling in this workspace (e.g. `uv run mm-modrinth`). Managed via your dev-environment stack; do not run `safe-chain setup` as a per-clone step.
- **Offline:** after a successful sync, `./gradlew build --offline` can skip network re-verification; drop `--offline` when dependencies change.
- **Heap sizing for multi-`mc*` builds:** Building **many** Loom subprojects in one Gradle invocation can **OOM or GC-thrash** with default `org.gradle.jvmargs` (typically `-Xmx1G`). Set **`-Xmx3G`** (or higher) in **`gradle.properties`** at the project root: `org.gradle.jvmargs=-Xmx3G -XX:MaxMetaspaceSize=512m`. Symptoms of insufficient heap: daemon killed mid-build, `GC overhead limit exceeded`, or Loom asset resolution stalling. CI images should also size memory for the full subproject count.
- **Root `build.gradle.kts`:** defer shared Java / source-set wiring with `afterEvaluate { }` **before** applying custom configuration to subprojects’ `JavaPluginExtension`, so Loom can finish its own configuration first.
- **IDE:** set the **Gradle JVM** to **JDK 25** when the project includes **any** **26.x** subproject (toolchain still selects per-subproject compile levels).
- **CI:** use a **JDK 25** base image for **full** multi-version builds; matrix jobs that only build **1.21.x** may use JDK **21**.

Full step-by-step checklist: **`.cursor/skills/mc-gradle-daemon-hygiene/SKILL.md`** (self-contained; no separate narrative file required).

---

## 1. Project Structure & The Adapter Pattern

### Folder Layout

| Location | What lives there |
|----------|-----------------|
| `src/main/java` | ~90% shared code |
| `mcX.Y.Z/src/main/java` | ~10% version-specific adapters & entrypoints |
| `mc26.x/src/main/java` | 26.x-specific adapters (post-deobfuscation) |
| `mcX.Y.Z/src/main/resources/` | Version-specific `mixins.yourmod.X_Y.json` and `@Mixin` classes **only** |

### Rule: Mixins Are Always Version-Specific

**Never put mixin configs or `@Mixin` classes in the shared root `src/`.** This is not a preference — it is a hard architectural constraint.

A mixin config placed in the shared root will be applied by every subproject. In 1.21.11, the target class name may be resolved through Intermediary. In 26.x, that remapping step no longer exists and the game ships with Mojang's names directly. A single shared mixin config cannot satisfy both, and whichever version it gets wrong will crash on load.

**The rule:**

- `mc1.21.11/src/main/resources/mixins.yourmod.1_21.json` — owns all 1.21.11 mixin targets
- `mc26.x/src/main/resources/mixins.yourmod.26_x.json` — owns all 26.x mixin targets
- If a mixin class is identical across versions, **duplicate it into both subproject folders.** The duplication is cheap; a mapping-induced crash in production is not. Treating it as shared creates a hidden coupling that will break the moment one version's names diverge.

### Handling API Changes (Adapters)

When Minecraft introduces breaking changes, use the **Adapter Pattern** so shared code doesn't crash on older versions. This is critical for 26.1, where Fabric API explicitly renames classes (e.g., `ItemGroupEvents` became `CreativeModeTabEvents`).

1. **Interface (shared root `src/`):**

    ```java
    public interface CreativeTabAdapter {
        void registerItems();
    }
    ```

2. **Version implementation (subproject `src/`):**

    ```java
    // mc1.21.11/src/main/java/.../CreativeTabImpl12111.java
    public class CreativeTabImpl12111 implements CreativeTabAdapter {
        @Override
        public void registerItems() {
            ItemGroupEvents.modifyEntriesEvent(...); // Uses 1.21.x mapping
        }
    }
    ```

    ```java
    // mc26.1/src/main/java/.../CreativeTabImpl261.java
    public class CreativeTabImpl261 implements CreativeTabAdapter {
        @Override
        public void registerItems() {
            CreativeModeTabEvents.modifyEntriesEvent(...); // Uses 26.1 Mojang mapping
        }
    }
    ```

---

## 2. Gradle Configuration

### `settings.gradle.kts`

No Foojay plugin — JDKs come from SDKMAN! (see Prerequisites).

```kotlin
pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
    }
}
rootProject.name = "your-mod"

// Default (§3): anchor only — do not add older 1.21.x modules unless you need them.
include("mc1.21.11", "mc26.1")

// Optional — full 1.21.x ladder + gap-fill (see §3); add to include(...) only when required:
// "mc1.21.10", "mc1.21.8", "mc1.21.5", "mc1.21.1", "mc1.21", …
```

### Root `build.gradle.kts`

The root `subprojects` block handles the Java toolchain split automatically based on folder naming convention.

```kotlin
plugins {
    id("fabric-loom") version "1.15" apply false // 1.15 minimum for 26.1; use 1.17.12 for 26.x (see YAML matrix)
}

subprojects {
    apply(plugin = "fabric-loom")
    repositories { maven("https://maven.fabricmc.net/") }

    // Dynamic Java version routing.
    // Convention: folders containing "26" → Java 25, everything else → Java 21.
    val targetJavaVersion = if (project.name.contains("26")) 25 else 21

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(targetJavaVersion)
    }

    (the<SourceSetContainer>())["main"].apply {
        java.srcDirs(rootProject.file("src/main/java"))
        resources.srcDirs(rootProject.file("src/main/resources"))
    }
    (the<SourceSetContainer>())["test"].apply {
        java.srcDirs(rootProject.file("src/test/java"))
    }
}
```

### Version `build.gradle.kts` (`mc1.21.11/`)

```kotlin
plugins { id("fabric-loom") }

dependencies {
    minecraft("com.mojang:minecraft:1.21.11")
    mappings(loom.officialMojangMappings())

    // modImplementation is strictly required for pre-26.x projects.
    modImplementation("net.fabricmc:fabric-loader:0.18.6")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.141.3+1.21.11")
}
```

### Version `build.gradle.kts` (`mc26.1/`)

For 26.1, `modImplementation` is retired in favor of standard Gradle configurations. Note: Obfuscation is gone at runtime, but mappings (Mojmap) are still layered internally by Loom for IDE support and parameter names.

```kotlin
plugins { id("fabric-loom") }

dependencies {
    minecraft("com.mojang:minecraft:26.1")

    // Yarn is dropped. Use standard Gradle configurations.
    // Note: Fabric Loader is conceptually still a mod dependency at runtime,
    // but the 26.x toolchain requires it to be declared as standard implementation.
    implementation("net.fabricmc:fabric-loader:0.19.3")
    implementation("net.fabricmc.fabric-api:fabric-api:0.145.1+26.1") // See YAML matrix per 26.x line
}

// remapJar is gone. Configure the standard jar task instead.
tasks.named<Jar>("jar") {
    // standard jar configs
}
```

### Migrating mappings with Loom (`migrateMappings`)

Official reference: **[Migrating Mappings using Loom](https://docs.fabricmc.net/develop/porting/mappings/loom)** (Fabric Documentation).

Use this when converting a mod’s **Java sources** between mapping sets on the **same** Minecraft version you already target (e.g. Yarn → official Mojang mappings), **before** or alongside adopting the multi-version layout. Loom exposes Gradle tasks; there is no separate plugin beyond **Fabric Loom** (upgrade Loom in `build.gradle` / `settings.gradle` as needed).

| Topic | Summary |
|-------|---------|
| **Loom version** | **Loom 1.13+** recommended: can migrate **Mixins**, **access wideners**, and **split client** source sets; newer Loom adds `migrateClientMappings` and `migrateClassTweakerMappings`. |
| **Mojang mappings** | Example: `./gradlew migrateMappings --mappings "net.minecraft:mappings:1.21.11"` — use the **same** Minecraft version the project is currently on. |
| **Before Gradle edits** | Per upstream docs: **do not** change `gradle.properties` or `build.gradle` mappings **until** you have run the migration flow as documented — then switch `dependencies { mappings ... }` to `loom.officialMojangMappings()` (or Yarn) and refresh the IDE. |
| **Output** | Default output is **`remappedSrc/`**; merge into `src/main/java` (or use **`--overrideInputsIHaveABackup`** on Loom 1.13+ to overwrite in place — backup first). |
| **Yarn → 26.x path** | **1.21.11** is the last release where Yarn is published; mods targeting **26.1+** should be on **Mojang mappings**. Use `migrateMappings` to move sources to Mojmap on 1.21.11, then port to 26.x with adapters/mixins per version subproject. |
| **After automation** | Manually review **mixin targets**, **@Shadow** names, and any code the task missed; tools like mappings.dev or Linkie help (as noted in Fabric docs). |

Additional flags (custom `--input` / `--output`, client-only trees) are documented on the same page.

---

## 3. 1.21.x Version Coverage Strategy

### Default: anchor only (`mc1.21.11` + `mc26.1`)

For **Minecraft Minder** workflows and **many mod repos** using this guide, **stop here**: **`mc1.21.11`** (latest obfuscated 1.21.x line) and **`mc26.1`** (current 26.x anchor). Do **not** schedule, scaffold, or plan **Phase 2 / Phase 3** (older 1.21.x patch lines) unless the **user asks** or a mod has a **documented need** (e.g. pack pins to 1.21.8).

That keeps copies of **`.cursor/`** lean, avoids duplicate `mc*` churn across repos, and matches “newest 1.21.x + 26.x” support. README **Supported Minecraft versions** should list **only what you ship** — two lines is fine.

### Optional: full 1.21.x ladder — phased rollout

Some mods **do** want JARs across more 1.21.x pins. The architecture is the same; you add `mc*` modules when needed.

| Phase | What to build | When |
|-------|--------------|------|
| **Phase 1 — Anchor** | `mc1.21.11` + `mc26.1` | Always the first milestone; validate shared code, adapters, mixins across the 26.x boundary. |
| **Phase 2 — Backfill** | `mc1.21.10`, `mc1.21.8`, `mc1.21.5`, `mc1.21.1` | **Opt-in** — e.g. modpack coverage, maintainer explicitly wants a wide matrix. |
| **Phase 3 — Gap fill** | `mc1.21`, `mc1.21.9`, `mc1.21.7`, `mc1.21.6`, `mc1.21.4` | **Opt-in**; lower traffic. **`mc1.21`** = `minecraft("1.21")` before 1.21.1. |

**Agents:** after Phase 1 is green, **do not** proactively propose Phase 2 unless the user or project policy calls for a broader ladder (see **`mc-scaffold-new-fabric-subproject`**).

### Which 1.21.x versions to skip (or treat as optional gap-fill)

- **1.21** (Gradle **`minecraft("1.21")`**, folder e.g. **`mc1.21`**) — **optional Phase 3** for the first **1.21** release before **1.21.1**. Fabric publishes **`fabric-api`** artifacts with a **`+1.21`** suffix (e.g. **`0.102.0+1.21`** — confirm the latest on [Fabric Maven](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/)). Include **`mc1.21`** when you want a full ladder from the initial **1.21** through **1.21.11** without a one-off prompt.
- **1.21.2 / 1.21.3** — short-lived; Fabric API support stopped early. Skip unless a player specifically asks.

### README: list supported Minecraft versions (don’t claim “all of 1.21.x”)

Phased rollout **intentionally** omits some patch releases (e.g. **1.21.2** / **1.21.3**) and may omit Phase 3 gap-fill lines until scaffolded. Saying only **“supports 1.21.x”** or **“all active 1.21.x”** in the mod repo **README** is **misleading** for players and pack makers: they will assume every 1.21 patch is covered.

**In the mod repository (not Minecraft Minder):** add a short **Supported Minecraft versions** section to **`README.md`** that matches reality:

- List the **exact** game versions you **build and ship** (one line per version or a compact range with gaps spelled out).
- Include **26.x** lines (e.g. **26.1**) when those subprojects exist.
- When this guide’s policy **skips** a version (e.g. 1.21.2 / 1.21.3), either **omit** them from the list or add a one-line note: *not shipped — short-lived releases; see build guide §3.*
- **Update this README section** whenever you add/remove an `mc*` subproject or change what you publish to Modrinth.

**Agents:** when scaffolding or removing `mc*` modules, **propose the same edit** to **`README.md`** in the same change set so marketing text stays aligned with `settings.gradle.kts`.

### API band caveats (1.21.x is not one API surface)

While the multi-`mc*` layout keeps Gradle wiring uniform, **Minecraft's Java API is not stable across all 1.21.x patches**. Real-world splits include:

- **1.21 / 1.21.1** vs **1.21.2+:** `CustomRecipe` serializer registration, `EntityType.Builder.build(...)` signature, early item/entity registration helpers. These patches may require a **separate compat source tree** or branched adapter implementations.
- **1.21.4 and earlier** vs **1.21.5+:** Component changes (`POTION_DURATION_SCALE`), particle classes (`SpellParticleOption` vs alternatives), `Unit` stream codec differences.

When porting across these boundaries, expect **extra `src/compat/...` trees** or per-version adapter implementations beyond the `build.gradle.kts` + `fabric.mod.json` changes. The **`mc-extract-fabric-version-adapter`** skill covers patterns for factoring splits into shared interfaces with per-subproject implementations. Do not assume a straight copy from the anchor (`mc1.21.11/`) compiles on all 1.21.x targets without review.

### Fabric API Version Matrix (1.21.x & 26.x) — reference for optional modules

Use when you **add** a row beyond the anchor. For **default** work you only need **1.21.11** and **26.1** scaffold rows; **ship** lines through **26.2** use the locked coordinates in **`.cursor/fabric-loader-yarn-fabric-api.yaml`**. Set `depends.fabric-api` to `>=` the exact coordinate. Check [Fabric Develop](https://fabricmc.net/develop) or [Maven](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/) before rebumps.

| Minecraft | Latest Fabric API (locked Jul 2026) | Phase | Notes |
|-----------|-------------------------------------|-------|-------|
| **1.21.11** | `0.141.3+1.21.11` | 1 (Anchor) | Latest 1.21.x; primary dev target |
| **1.21.10** | `0.138.4+1.21.10` | 2 | Active backport line |
| **1.21.9** | `0.134.1+1.21.9` | 3 | Gap fill |
| **1.21.8** | `0.136.1+1.21.8` | 2 | Active backport line |
| **1.21.7** | `0.128.2+1.21.7` | 3 | Gap fill |
| **1.21.6** | `0.128.2+1.21.6` | 3 | Gap fill |
| **1.21.5** | `0.128.2+1.21.5` | 2 | Active backport line |
| **1.21.4** | `0.119.4+1.21.4` | 3 | Gap fill |
| **1.21.1** | `0.116.9+1.21.1` | 2 | LTS-like; still receiving API updates in 2026 |
| **1.21** | `0.102.0+1.21` | 3 | Initial **1.21** (before 1.21.1). **Optional** gap-fill; verify latest `+1.21` on [Maven](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/). |
| **26.1** | `0.145.1+26.1` | 1 (Anchor) | 26.x scaffold anchor; loader **0.19.3**, Loom **1.17.12** |
| **26.1.1** | `0.145.2+26.1.1` | Ship | 26.1 patch line |
| **26.1.2** | `0.152.1+26.1.2` | Ship | Common LOREal ship target; often `mc26.1.2/` |
| **26.2** | `0.152.2+26.2` | Ship | Current stable 26.x line; `mc26.2/` |

**Loader version:** **`0.18.6`** on **1.21.x**; **`0.19.3`** on **26.x** (locked through **26.2**). Do not use 0.18.x loader coordinates on 26.x subprojects.

**26.x beyond 26.2:** Preview/snapshot lines (e.g. **26.3-snapshot-***) are **not** in the locked matrix — add a YAML row only when you intentionally scaffold a preview module.

---

## 4. Multi-Version Automated Testing

Add an empty class implementing `FabricGameTestHelper` in root `src/test/` and add the `"fabric-gametest"` entrypoint to each version's `fabric.mod.json`.

```bash
# Test ALL versions simultaneously
./gradlew runGametest
```

A successful boot to the entrypoint without crashing validates mixins and initialization across both obfuscated and un-obfuscated environments.

---

## 5. Build & Release Flow

### Step 1: Build

```bash
./gradlew build
```

Output JARs land in `mcX.Y.Z/build/libs/`.

#### Recommended JAR file naming (multi-`mc*` layouts)

**Optional but recommended** when several `mc*` subprojects build **side-by-side**: Gradle’s defaults name artifacts after the **subproject** (e.g. `mc1.21.11-1.0.0.jar`), which is easy to confuse when copying JARs to Modrinth or `release/`. **Single-target** mods (one shippable module) may keep defaults.

**Convention (use your own placeholders — do not copy another mod’s slug):**

| Piece | Role | Example placeholder |
|-------|------|---------------------|
| **Distribution slug** | `archiveBaseName` — human-facing name for the file (Modrinth project slug, GitHub repo name, or a dedicated publish name) | `{distribution_slug}` |
| **Mod version** | From root `gradle.properties` (e.g. `mod_version`) — SemVer release part only | `1.0.0` |
| **Game line** | SemVer **build metadata** per subproject: `+` + Minecraft version string matching that module’s `minecraft(...)` coordinate | `+1.21.11`, `+26.1` |

**Resulting filenames** (illustrative): `{distribution_slug}-1.0.0+1.21.11.jar`, `{distribution_slug}-1.0.0+26.1.jar`. Repos may use other naming (e.g. `release/{slug}-{semver}-mc{minecraft}.jar`); pick one scheme per project and document it — do not assume a single repo-specific pattern.

**Important distinctions:**

- **`archiveBaseName`** is **not** required to match `fabric.mod.json` **`id`** or Maven **`group`** — it only identifies the **artifact file** for humans and upload scripts.
- **Git tags** in this guide (e.g. `v1.0.1+mc26.1` in **Step 3** below) are separate; you can keep tag style and JAR metadata style aligned or not, as long as the team agrees.

**Gradle / Loom (aligned with §2 and §7.2):**

- **1.21.x (obfuscated line, Loom remap):** Configure **`remapJar`** with `archiveBaseName` and `archiveVersion` (e.g. `archiveVersion.set("$modVersion+$minecraftGameVersion")`). Also set the same on the plain **`jar`** task so intermediates under `build/libs/` stay consistent and unambiguous.
- **26.x (no `remapJar`):** Configure **`jar`** only — same `archiveBaseName` / `archiveVersion` pattern.

Use a fully qualified task type for remap where your Loom version requires it, e.g. `tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") { ... }` alongside `tasks.named<Jar>("jar") { ... }`. Centralize `mod_version` from the root; pass or define the per-subproject game version string next to that module’s `minecraft(...)` dependency.

### Step 2: Validation Checklist

- [ ] `./gradlew build` exits 0 for all target versions
- [ ] `fabric.mod.json` inside built JARs has correct `depends.minecraft` bounds.
- [ ] `runGametest` passes for all targets.

### Step 3: Versioning & Tagging

- **Mod version:** `MAJOR.MINOR.PATCH` (e.g. `1.0.1`)
- **Full version:** `<mod_version>+mc<mc_version>` (e.g. `1.0.1+mc26.1`)

```bash
git tag -a v1.0.1-mc26.1 -m "Release 1.0.1 for MC 26.1"
git push origin v1.0.1-mc26.1
```

---

## 6. Migrating from Version Branches to Subprojects

Use an isolated **Refactor Branch** — do not attempt `git merge` across old version branches.

```bash
git checkout main && git pull
git checkout -b refactor/multi-version-setup
```

1. **Foundation (Phase 1):** Add root `settings.gradle.kts` and `build.gradle.kts`. Create `mc1.21.11/` and `mc26.1/`. Verify `./gradlew :mc1.21.11:runGametest` and `./gradlew :mc26.1:runGametest`.
2. **Backfill (Phase 2, optional):** Only if you need older 1.21.x JARs: scaffold `mc1.21.10/`, `mc1.21.8/`, `mc1.21.5/`, `mc1.21.1/` (scaffold skill). Per-module `build.gradle.kts` and `fabric.mod.json` always differ; adapter code and mixins **may also diverge** across API bands (see §3 — API band caveats). Build and gametest each.
3. **Extract (if migrating from branches):** Pull files directly from old branches (e.g., `git checkout 1.20.x -- src/...`) and map them to their respective subproject folders.
4. **Merge & Clean:** Once all gametests pass, merge back to `main` and delete the legacy version branches.

---

## 7. Minecraft 26.1 — De-obfuscation & New Versioning

> **Companion skill:** `.cursor/skills/mc-fabric-mojmap-migration-26x/SKILL.md` is the agent-facing **router and checklist** for this migration. It owns items not enumerated below (`ItemStackTemplate`, `ChunkSectionLayer`, `BlockColorRegistry`, `FluidModel`, datapack-driven villager trades, `HudRenderCallback` removal, `fabric-convention-tags-v1` / `fabric-loot-api-v2` removal, new `DimensionEvents`/`BlockEvents`/`ItemEvents` surfaces) and routes to `mc-gradle-daemon-hygiene`, `mc-scaffold-new-fabric-subproject`, `mc-isolate-version-mixin`, `mc-extract-fabric-version-adapter`, and `mc-fabric-minecraft-recipe-datapack` for deeper workflows.

### 7.1 New Version Numbering

The format is `YEAR.DROP.HOTFIX`.

- **`1.21.11` → `26.1`**
- Snapshot `25w41a` → `26.1-snapshot-1`

### 7.2 Removal of Obfuscation & Fabric Impacts

Starting in 26.1, Java Edition ships **un-obfuscated** at runtime using Mojang's names.

| Component | Impact |
|-----------|--------|
| **Yarn mappings** | **Officially dropped.** Mojmap is strictly required. |
| **Intermediary** | No longer needed at runtime. The remapping layer is gone. |
| **Gradle Plugins** | `modImplementation` → `implementation` (For 26.x projects only; 1.21.x still requires `modImplementation`). `remapJar` → `jar`. |
| **Tooling & IDE** | While the runtime is un-obfuscated, Loom still layers Mojmap metadata internally for parameter names and IDE support. |
| **Fabric API** | APIs updated to match Mojang's official names (e.g. `ItemGroupEvents` → `CreativeModeTabEvents`). |
| **Vanilla Code Changes** | `ItemStackTemplate` replaces early `ItemStack` creation. Villager trading (`TradeOfferHelper`) is entirely data-driven now. Fluid rendering is handled by `FluidModel`. |

### 7.3 Branch Strategy for 26.x

**`main` is the only branch you need.** The `mc26.1/` folder *is* the version isolation mechanism. A bug found in `mc1.21.11` gets fixed in the `mc1.21.11/` folder on `main`, built, and tagged.

1. On `main`, add `mc26.1/` subproject folder.
2. Register it in `settings.gradle.kts`.
3. Write the `build.gradle.kts` (Uses standard `implementation` and target Java 25).
4. Write the Adapters using Mojang's un-obfuscated names.
5. Build `:mc26.1`, tag, and publish.

---

## 8. 26.1 Migration & Environment Checklist

> Agents working a real port should load **`mc-fabric-mojmap-migration-26x`** for the full phased checklist (env → mappings → buildscript swap → API renames → removed APIs → new 26.1 subsystems → test). The lists below remain the canonical short form.

**Environment (SDKMAN! / editors)**

- [ ] **Java aliases:** Run `j21` then `java -version` (and repeat with `j25`) so SDKMAN! JDKs are what you expect before builds.
- [ ] **Daemon vs toolchain:** If you switch between `j21` and `j25`, run `./gradlew --stop` before the next build so a new daemon inherits the correct `JAVA_HOME` (see **§ Prerequisites — Gradle daemon, wrapper & hygiene**).
- [ ] **Gradle wrapper:** Run `./gradlew wrapper --gradle-version 9.4.0` (use a **full three-part** distribution version in wrapper properties).
- [ ] **Loom:** Root `build.gradle.kts` uses `fabric-loom` version **`1.17.12`** for **26.x** work (minimum **1.15** for 26.1); see YAML matrix.
- [ ] **IDE:** IntelliJ **2025.3+** for heavy Mixin work, or Cursor/VS Code with **`.cursorrules`** in the **mod** repo mentioning `j21`/`j25`. If any **26.x** subproject exists, set the **Gradle JVM** to **JDK 25**.
- [ ] **Optional:** Remove stray `package-lock.json` from `~` if it pollutes tooling paths.
- [ ] **Optional:** Verify [Aikido Safe Chain](https://github.com/AikidoSec/safe-chain) is active on this machine (`uv safe-chain-verify`); use `./gradlew build --offline` only when Gradle dependencies are already resolved.

**26.1 project work**

- [ ] If the mod still uses **Yarn** on **1.21.x**, plan a **`migrateMappings`** pass to **Mojang mappings** on that line first (see **§2 — Migrating mappings with Loom** and [Fabric: Loom](https://docs.fabricmc.net/develop/porting/mappings/loom)).
- [ ] Create `mc26.1/` subproject
- [ ] Ensure 26.1 `build.gradle.kts` uses `implementation` instead of `modImplementation`
- [ ] Verify all `@Shadow` field names against Mojang's original un-obfuscated names
- [ ] Translate Fabric API event calls to their renamed Mojmap equivalents (e.g., `CreativeModeTabEvents`)
- [ ] Run `./gradlew :mc26.1:runGametest`
- [ ] Publish to Modrinth with game version `26.1`

---

## Document history

- **v4.3.1 (2026-07-04):** Locked **26.x** coordinates through **26.2** in **`.cursor/fabric-loader-yarn-fabric-api.yaml`** and §3 matrix — **26.1.1**, **26.1.2**, **26.2** rows added; **26.x** loader **`0.19.3`**, Loom **`1.17.12`**, Fabric API pins aligned with LOREal shipped mods. Split loader guidance: **`0.18.6`** (1.21.x) vs **`0.19.3`** (26.x). Preview lines beyond **26.2** (e.g. **26.3-snapshot-***) explicitly out of scope until scaffolded. Repo version **3.21.0**.
- **v4.3 (filename):** Canonical file is **`fabric-mod-build-release-guide-v4.3.md`**. Driven by [Issue #3](https://github.com/ImpureCrumpet/Minecraft-Minder/issues/3) field experience from **mc-just-the-tips** multi-version port. **Changes:** Fabric Loader aligned to **`0.18.6`** across all examples and prose (was 0.15.11 / 0.18.4 / 0.18.5 in various places). **§3** adds **API band caveats** — documents that 1.21.x is not one API surface (1.21/1.21.1 vs 1.21.2+, 1.21.4 vs 1.21.5+ splits); qualifies §6 Phase 2 language. **Heap sizing** for multi-`mc*` builds added to Gradle daemon/hygiene section (`-Xmx3G`). **Build-script DSL note:** Kotlin DSL examples apply to Groovy with syntax translation. **Portable bundle list** now mentions optional **`fabric-loader-yarn-fabric-api.yaml`** companion artifact (renamed from space-containing filename). Companion skills updated: `mc-scaffold-new-fabric-subproject` v1.4.0, `mc-gradle-daemon-hygiene` v1.3.0, `mc-extract-fabric-version-adapter` v1.2.0. Issue #3 closed in 3.16.2.
- **v4.2:** Superseded by **v4.3**.  Canonical file is **`fabric-mod-build-release-guide-v4.2.md`**. **§3** default is **anchor only** (`mc1.21.11` + `mc26.1`); Phase 2/3 and older 1.21.x lines are **opt-in**. **`settings.gradle.kts`** example shows minimal `include`. **§3** adds **README: list supported Minecraft versions** — explicit list in the mod repo README (not vague “1.21.x”); align with phased rollout and intentional skips (e.g. 1.21.2 / 1.21.3); agents patch README when changing `mc*` modules. Removed references to untracked **`ref/`** narratives and obsolete **`Build + 26/archive`** paths; older snapshots are **git history** / **local** only.
- **v4.1:** Superseded by **v4.2**. **§3** added **`mc1.21`** / **`minecraft("1.21")`** as optional Phase 3 gap-fill (Fabric API **`+1.21`** line); documented **§ Cursor skills** note on **`mm-*`** Minder-only skills.
- **v4 (incremental):** **§5 — Recommended JAR file naming** for multi-`mc*` repos (`archiveBaseName` / `archiveVersion`, `jar` + `remapJar` vs `jar` only on 26.x). Scaffold skill cross-link updated in-repo.
- **v4:** Adds **§3 — 1.21.x Version Coverage Strategy** (phased rollout: anchor → backfill → gap fill), complete **Fabric API Version Matrix** for all active 1.21.x game versions plus 26.1, phase-labelled `settings.gradle.kts` example, updated **§6 Migration** steps to include Phase 2 backfill. Replaces previous §3 (single-row matrix). Bundled with **`.cursor/skills/`** for portability into mod repos. Prefer **v4** content for day-to-day work.
- **v3.1:** Added Gradle daemon vs toolchain guidance, wrapper/security/offline notes, IDE/CI JDK alignment, Cursor skills table, expanded §8 checklist. Superseded by **v4**.
- **v3:** Merges v1 (complete guide) with v2 (SDKMAN!, no Foojay, editor/alias checklist). First published as **`.cursor/fabric-mod-build-release-guide-v3.md`**; content continued in **v3.1** (this file). Older **v1** / **v2** snapshots may exist only in **git history** or a **local** archive — not in this portable bundle. **Loom `migrateMappings`:** see §2 subsection *Migrating mappings with Loom* and [Fabric Documentation](https://docs.fabricmc.net/develop/porting/mappings/loom).
- **v1 / v2:** Kept as snapshots for diff archaeology where archived; prefer **v4** for day-to-day work.
