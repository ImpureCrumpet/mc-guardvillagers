---
name: mc-scaffold-new-fabric-subproject
description: Scaffolds a new mc* Gradle Fabric subproject and wires settings, dependencies, fabric.mod.json, and gametest.
triggers:
  - new Minecraft version
  - add mc subproject
  - scaffold fabric
  - opt-in backfill
  - version matrix
  - mc1.21
  - mc26
  - FabricGameTestHelper
dependencies:
  - mc-gradle-daemon-hygiene
version: "1.4.0"
---

# Scaffold New Fabric Subproject

**Scope:** **Gradle-based Fabric** repositories that already use (or are moving to) **multi-version subprojects** with shared root `src/`. Not for Maven-only or non-Fabric loaders unless the user specifies otherwise.

**Build-script DSL:** Examples use **Kotlin DSL** (`build.gradle.kts`, `settings.gradle.kts`). The same patterns apply to **Groovy DSL** (`build.gradle`, `settings.gradle`) — translate syntax as needed. Use whichever DSL the existing repo already uses.

## When to use

- User asks to **add a Minecraft version** (e.g. "Add **26.2**", "target **1.21.12**").
- You need a new **`mc<VERSION>/`** tree consistent with the rest of the repo and with **`.cursor/fabric-mod-build-release-guide-v4.3.md`**.
- **Default stop:** After **Phase 1** (`mc1.21.11` + `mc26.1`) is green, **do not** propose **Phase 2** or older **1.21.x** scaffolds unless the **user asks** or states a need (pack pins, wide compatibility). The guide **§3** treats the full ladder as **opt-in**.

## 1.21.x version coverage (phased rollout)

**Default:** anchor only — **`mc1.21.11`** + **`mc26.1`**. Optional wider matrix: **§3 — 1.21.x Version Coverage Strategy** in the guide.

| Phase | Subprojects | When |
|-------|-------------|------|
| **1 — Anchor** | `mc1.21.11` + `mc26.1` | First scaffold; usual completion point for bulk / Minder workflows |
| **2 — Backfill** | `mc1.21.10`, `mc1.21.8`, `mc1.21.5`, `mc1.21.1` | **Opt-in** — user or project explicitly wants older 1.21.x JARs |
| **3 — Gap fill** | `mc1.21`, `mc1.21.9`, … | **Opt-in**; see guide §3 |

**Phase 2+:** Scaffold each `mc*` like Phase 1. `build.gradle.kts` (Fabric API coordinate) and `fabric.mod.json` (`depends.minecraft` / `depends.fabric-api`) **always** differ per module. Adapters, mixins, and compat source trees **may also differ** when APIs diverge within 1.21.x — see **API band caveats** in the guide §3. Key splits:

- **1.21 / 1.21.1** vs **1.21.2+:** `CustomRecipe` serializer registration, `EntityType.Builder` signatures, early registration helpers — may require a **separate compat tree**.
- **1.21.4 and earlier** vs **1.21.5+:** Component changes, particle classes, stream codec differences.

Do not assume a straight copy from the anchor (`mc1.21.11/`) compiles on all 1.21.x targets without review. Use the **`mc-extract-fabric-version-adapter`** skill when adapters need per-version implementations. **Phase 3** initial **1.21**: guide §3 matrix **`+1.21`**. Build + gametest per subproject.

## Read first

- **§3 — 1.21.x Version Coverage Strategy** — phased rollout, which versions to target, which to skip; **README: list supported Minecraft versions** (explicit list in the mod repo README, not vague “1.21.x”).
- **§3 — Fabric API Version Matrix** — coordinates for each line you add (anchor needs 1.21.11 + 26.1 only).
- **§ Gradle Configuration** — `settings.gradle.kts`, root `subprojects`, per-version `build.gradle.kts`.
- **§ Gradle — Migrating mappings with Loom** — `migrateMappings` / Mojmap vs Yarn; use when a **single** subproject still needs a mapping migration before or after adding new `mc*` modules. Official: [Migrating Mappings using Loom](https://docs.fabricmc.net/develop/porting/mappings/loom).
- **§ Minecraft 26.1** (and 26.x follow-ons) — de-obfuscation, `implementation` vs `modImplementation`, `remapJar` → `jar` on 26.x lines.
- **§ Multi-Version Automated Testing** — `FabricGameTestHelper` + `fabric-gametest` entrypoint.
- **§5 — Recommended JAR file naming** — when adding a subproject to a multi-`mc*` repo, set `archiveBaseName` (distribution slug) and `archiveVersion` (`mod_version` + `+{minecraft_game_version}`) on **`remapJar` + `jar`** (1.21.x) or **`jar`** only (26.x). See **`.cursor/fabric-mod-build-release-guide-v4.3.md`**.

## Actions (in order)

### 1. Create the subproject root

- Add folder **`mc<VERSION>/`** at the repo root (match existing naming: e.g. `mc26.2`, `mc1.21.12`).
- Add **`mc<VERSION>/build.gradle.kts`** (see dependency rules below).
- Add **`mc<VERSION>/src/main/java/`** and **`mc<VERSION>/src/main/resources/`** as needed (often minimal until adapters/mixins are added).
- **Do not** add the **Foojay** resolver plugin anywhere — **JDKs come from SDKMAN!** and Gradle toolchains (see guide Prerequisites).

### 2. Register the subproject

- In **root `settings.gradle.kts`**, append the new module to **`include(...)`** (comma-separated string matching the folder name).

### 3. Dependencies in `mc<VERSION>/build.gradle.kts`

Apply **`fabric-loom`** in that file (same pattern as sibling `mc*` modules).

| Minecraft line | Loader / Fabric API declarations |
|----------------|----------------------------------|
| **≥ 26.1** (26.x game versions) | **`implementation`** for `fabric-loader` and `fabric-api` — not `modImplementation`. Yarn is dropped; runtime is Mojmap-oriented per guide. |
| **≤ 1.21.x** (pre-26) | **`modImplementation`** for loader and Fabric API (required for those toolchains). |

- Set **`minecraft("com.mojang:minecraft:<VERSION>")`** to the exact target (e.g. `26.2`, `1.21.11`) per project policy.
- **Mappings:** Follow the repo's existing pattern for that line (guide shows Mojang mappings for 1.21.11 example; 26.x uses Mojmap layered by Loom).
- **Versions:** Resolve **current** loader and Fabric API coordinates from [Fabric Develop](https://fabricmc.net/develop) for that Minecraft version — do not copy stale placeholders.
- For **26.x**, **`remapJar` is gone** — use the normal **`jar`** task configuration like other 26.x subprojects in the repo.
- **Shippable JAR names:** If the repo ships multiple `mc*` artifacts, add `archiveBaseName` / `archiveVersion` per guide **§5 — Recommended JAR file naming** (1.21.x: `jar` + `remapJar`; 26.x: `jar` only). Use a `{distribution_slug}` placeholder in docs; do not hard-code another mod’s slug.

### 4. Root Gradle expectations

- **Do not** introduce **`foojay-resolver-convention`** (or similar Foojay JDK provisioning).
- If the root already uses the **"`26` in name → Java 25, else Java 21"** convention, a folder like **`mc26.2`** should pick up **Java 25** automatically; **`mc1.21.x`** → **Java 21**. Align with existing `subprojects { ... }` in root `build.gradle.kts`.

### 5. `fabric.mod.json` (inside `mc<VERSION>/src/main/resources/`)

Generate or extend **`fabric.mod.json`** for **this** subproject's artifact:

- **`depends.minecraft`:** Set lower/upper bounds that match the intended support window for this module (tighten to the exact `minecraft(...)` version when appropriate).
- **`depends.fabric-api`:** Use **`>=`** the **exact** Fabric API version string you put in Gradle (see matrix in guide §3).

### 6. Testing scaffold — `FabricGameTestHelper`

Per guide §4:

- Add an **empty** (or minimal) class **`implements FabricGameTestHelper`** — typically under **root `src/test/java/`** so all versions share one helper **unless** the repo already places tests elsewhere; follow **existing** repo layout if it differs.
- In **this** subproject's **`fabric.mod.json`**, add the **`"fabric-gametest"`** entrypoint pointing at that helper class so **`./gradlew runGametest`** can boot this version.

If a gametest class already exists, only add the **entrypoint** for the new subproject.

### 7. README in the mod repo (required for honest messaging)

Per guide **§3 — README: list supported Minecraft versions**:

- Add or update a **Supported Minecraft versions** (or **Built / shipped versions**) section in the mod repository’s **`README.md`**.
- List the **exact** game versions that have `mc*` subprojects and that you ship (bullet list or compact enumeration with gaps explained).
- Do **not** leave only vague “supports 1.21.x” / “all active 1.21.x” wording unless every patch line is actually built—this guide intentionally skips some patches (e.g. **1.21.2** / **1.21.3**).
- When **removing** a subproject, remove or adjust that version from the README in the same change.

## After scaffolding — daemon hygiene (required)

After edits, follow the **`mc-gradle-daemon-hygiene`** skill before running any Gradle commands. In particular:

1. Run `./gradlew --stop` if the daemon may be running under a different JDK.
2. Switch to the JDK that matches the new subproject (**JDK 21** for `mc1.21.x`, **JDK 25** for `mc26.x`) **before** invoking `./gradlew`. Use whichever switching mechanism your environment supports (SDKMAN aliases like `j21`/`j25`, mise, asdf, raw `JAVA_HOME`, IDE project SDK, etc.).
3. Tell the user which JDK is required and the exact switch command for their setup if known.

Example: *"Run `./gradlew --stop`, switch to JDK 25 (e.g. `j25` if you use SDKMAN), then `./gradlew :mc26.2:build` — the 26.x module requires a JDK 25 daemon."*

See **`.cursor/skills/mc-gradle-daemon-hygiene/SKILL.md`** for the full daemon lifecycle, asset fetching, and wrapper hygiene workflow.

## Checklist

- [ ] `mc<VERSION>/` exists with `build.gradle.kts` and resources layout.
- [ ] `settings.gradle.kts` includes the new module.
- [ ] **No Foojay** plugin added.
- [ ] **26.x:** `implementation` for loader/API; **pre-26:** `modImplementation`.
- [ ] `fabric.mod.json` has sensible **`depends.minecraft`** and **`depends.fabric-api`**.
- [ ] `FabricGameTestHelper` + **`fabric-gametest`** entrypoint wired per §4.
- [ ] If multi-`mc*` releases are expected: JAR naming (`archiveBaseName` / `archiveVersion`) per guide §5 — `jar`+`remapJar` (1.21.x) or `jar` only (26.x).
- [ ] Daemon hygiene followed per **`mc-gradle-daemon-hygiene`** skill.
- [ ] **`README.md`** lists supported Minecraft versions explicitly (guide §3 README subsection); no misleading “all of 1.21.x” if the matrix has gaps.

## Examples

- "Add 26.2 support" → create `mc26.2/`, wire `settings.gradle.kts`, use `implementation` for loader/API, set `depends.minecraft` in `fabric.mod.json`, switch to JDK 25 (e.g. `j25` if SDKMAN) before building.
- "Phase 1 builds are green — what's next?" → default answer: nothing required; optional **Phase 2** only if they ask for older 1.21.x lines.
- "Add mc1.21.8" → scaffold from anchor pattern; update `build.gradle.kts` Fabric API coordinate and `fabric.mod.json` bounds; **review API band caveats** (§3) before assuming adapters/mixins copy verbatim from `mc1.21.11/`; verify with gametest.

## Reference

**`.cursor/fabric-mod-build-release-guide-v4.3.md`** — **1.21.x Version Coverage Strategy** (including README supported-versions), **Fabric API Version Matrix**, **Gradle Configuration**, **Minecraft 26.1**, **Multi-Version Automated Testing**.
