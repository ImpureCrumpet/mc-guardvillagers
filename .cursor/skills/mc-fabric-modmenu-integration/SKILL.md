---
name: mc-fabric-modmenu-integration
description: "Ensures Fabric mods present Mod Menu metadata and config screens correctly: fabric.mod.json custom.modmenu block, translation keys, ModMenuApi entrypoints, Cloth Config and MidnightLib wiring, and multi-version Gradle dependency conventions."
triggers:
  - mod menu
  - modmenu
  - Mod Menu
  - mod config screen
  - configure button
  - Edit Config
  - ModMenuApi
  - ConfigScreenFactory
  - modmenu entrypoint
  - custom.modmenu
  - modmenu.descriptionTranslation
  - cloth config mod menu
  - midnightconfig
  - midnightlib
  - MidnightConfig
  - midnightconfig tooltip
  - mod menu integration
  - mod menu metadata
  - mod menu badge
  - library badge modmenu
dependencies: []
version: "1.1.0"
---

# Fabric Mod Menu integration

**Source:** [TerraformersMC/ModMenu developer guide](https://github.com/TerraformersMC/ModMenu#developers) (Fabric Metadata API, Translation API, Java API).

**Scope:** Client-side presentation and config-button wiring. Mod Menu is **client-only** — never ship `modmenu` JARs on dedicated servers; mark API/library modules with the `library` badge when they are not end-user mods.

## 1. Route the work

| Goal | Mechanism | Java required? |
|---|---|---|
| Better name / summary / description in the list | Translation API keys | No |
| Links, badges, parent grouping, update-checker opt-out | `fabric.mod.json` → `custom.modmenu` | No |
| **Configure…** button opens your settings | `ModMenuApi` + `entrypoints.modmenu` | Yes |
| Settings built with Cloth Config | `ModMenuApi.getModConfigScreenFactory` returning a Cloth `Screen` | Yes |
| Settings via `MidnightConfig` | `MidnightConfig.init(modid, Config.class)` — MidnightLib registers the button | Init only |
| API module that should stay hidden | `"badges": ["library"]` + optional `"environment": "client"` | Metadata only |

If the mod has **no** user-facing config, skip the Java API — still add metadata when the mod is client-facing or split into modules.

## 2. Presentation — `fabric.mod.json` metadata

Add a `custom.modmenu` block (Quilt: top-level `"modmenu"` instead of under `custom`).

```json
"custom": {
  "modmenu": {
    "links": {
      "modmenu.discord": "https://discord.gg/example",
      "modmenu.modrinth": "https://modrinth.com/mod/your-mod"
    },
    "badges": ["library"],
    "parent": "parent-mod-id",
    "update_checker": true
  }
}
```

### Badges

| Value | When to use |
|---|---|
| *(auto)* **Client** | Set `"environment": "client"` on the mod container |
| `library` | Pure dependency / API — hidden unless user enables libraries |
| `deprecated` | Legacy shim kept for compatibility |

Mod Menu does **not** support custom badge strings.

### Links

- Keys are **translation keys**; link text comes from your lang file or Mod Menu defaults (`modmenu.discord`, `modmenu.modrinth`, `modmenu.github_releases`, … — see [Mod Menu `en_us.json`](https://github.com/TerraformersMC/ModMenu/blob/26.3/src/main/resources/assets/modmenu/lang/en_us.json)).
- Use **your own namespace** for custom link keys (not `modmenu.*` unless you intend Mod Menu’s default label).
- A `sources` contact in standard `fabric.mod.json` metadata is also surfaced as a link.

### Parents (multi-module mods)

- String parent: `"parent": "flamingo"` — child groups under an installed mod with that id.
- Dummy parent object — use when the parent is not a real mod; repeat the same dummy metadata in **every** child `fabric.mod.json`:

```json
"parent": {
  "id": "mymod-suite",
  "name": "My Mod Suite",
  "description": "Grouped modules",
  "icon": "assets/mymod/icon.png",
  "badges": ["library"]
}
```

### Update checker

Default: Mod Menu hashes the JAR and checks Modrinth. Set `"update_checker": false` for dev builds, unpublished mods, or when you supply a custom checker via `ModMenuApi.getUpdateChecker()`.

## 3. Presentation — Translation API

Add keys to `assets/<namespace>/lang/en_us.json` (and other locales):

```json
{
  "modmenu.nameTranslation.mymod": "My Mod",
  "modmenu.summaryTranslation.mymod": "One-line summary for the list.",
  "modmenu.descriptionTranslation.mymod": "Longer description shown in the detail pane."
}
```

Replace `mymod` with your mod id. Summary can differ from description; if they match, summary is optional.

## 4. Config screens — Gradle dependency

Add the Terraformers Maven repo and Mod Menu as a **compile/dev** dependency (Mod Menu is optional at runtime for players, but needed to test the button):

```gradle
repositories {
  maven { name = "Terraformers"; url = "https://maven.terraformersmc.com/" }
}

dependencies {
  // 26.x (Mojmap, no remapJar): plain implementation
  implementation("com.terraformersmc:modmenu:${project.modmenu_version}")

  // ≤ 1.21.11 (Yarn): use modImplementation or modCompileOnly
  // modCompileOnly("com.terraformersmc:modmenu:${project.modmenu_version}")
}
```

Pin `modmenu_version` in `gradle.properties`. Match the Mod Menu release to the Minecraft line ([Modrinth versions](https://modrinth.com/mod/modmenu/versions) or [GitHub releases](https://github.com/TerraformersMC/ModMenu/releases)).

| Minecraft line | Typical Mod Menu release (verify before pin) |
|---|---|
| 26.2 | e.g. `20.0.1` |
| 1.21.11 | e.g. `17.x` |

Use **`modCompileOnly`** when you only need the API at compile time and will test with a manually installed Mod Menu JAR.

## 5. Config screens — `ModMenuApi` entrypoint

1. Implement `com.terraformersmc.modmenu.api.ModMenuApi` in a **client** class.
2. Register it under `entrypoints.modmenu` in the **same** `fabric.mod.json` as the mod id that should get the button.

```json
"entrypoints": {
  "modmenu": ["com.example.mymod.MymodModMenu"]
}
```

Mod Menu resolves the **mod id from the entrypoint’s mod container** — the `modmenu` entrypoint must live in the JAR whose id should show **Configure…**, not in a shared root-only stub unless that stub is the container.

```java
public class MymodModMenu implements ModMenuApi {
  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return parent -> new MymodConfigScreen(parent);
  }
}
```

`ConfigScreenFactory` is `Screen create(Screen parent)` — always accept `parent` and pass it when constructing sub-screens so **Done** returns to Mod Menu.

### Do not use `getProvidedConfigScreenFactories` for your own mod

That map is for **libraries** (e.g. Cloth Config, **MidnightLib**) registering screens for **other** mod ids. Your mod’s button comes from `getModConfigScreenFactory` on **your** entrypoint — or, for MidnightLib, from MidnightLib’s own provider after `MidnightConfig.init(...)`. Do **not** add a custom `ModMenuApi` just to re-register your MidnightConfig screen.

## 6. Cloth Config path

When settings use [Cloth Config](https://shedaniel.gitbook.io/cloth-config/):

1. Build the screen with `ClothConfigScreenBuilder` / `AutoConfig` (per your Cloth Config version).
2. Return it from `getModConfigScreenFactory`:

```java
@Override
public ConfigScreenFactory<?> getModConfigScreenFactory() {
  return parent -> AutoConfig.getConfigScreen(MyConfig.class, parent).get();
}
```

3. Keep Cloth Config on `modApi` / `modImplementation` as your Cloth docs require; Mod Menu stays `implementation` / `modCompileOnly`.
4. If the config class or screen builder is version-specific, place the `ModMenuApi` class in the **client** source set of each `mc*` subproject (same pattern as other client entrypoints).

## 6b. MidnightLib path

When settings extend `MidnightConfig` ([MidnightLib](https://modrinth.com/mod/midnightlib), bundled or declared as a dependency), **no `ModMenuApi` entrypoint is required** — MidnightLib registers **Configure…** via `getProvidedConfigScreenFactories` for mods that call `MidnightConfig.init(...)` during mod init.

```java
MidnightConfig.init(MODID, MyModConfig.class);
```

`MyModConfig` extends `MidnightConfig` with `@Entry` fields (and optional `@Comment` intros, `@Condition` on 1.9.x+).

### Translation keys (config screen)

Prefix: `<modid>.midnightconfig.` (e.g. `guardvillagers.midnightconfig.`).

| Key | Used for |
|---|---|
| `<modid>.midnightconfig.title` | Screen title |
| `<modid>.midnightconfig.category.<name>` | Tab label (`@Entry(category = "…")`) |
| `<modid>.midnightconfig.<fieldName>` | Option label — **must match** the `@Entry` / `@Comment` Java field name |
| `<modid>.midnightconfig.<fieldName>.label.tooltip` | Hover on the **label** (`EntryInfo.getTooltip(false)`) |
| `<modid>.midnightconfig.<fieldName>.tooltip` | Hover on the **control** (toggle, slider, text field; `getTooltip(true)`) |

`EntryInfo.getTooltip(boolean isButton)` builds the key as `translationKey + (isButton ? "" : ".label") + ".tooltip"`. Missing keys show no tooltip — labels do **not** fall back to `.tooltip` when only `.label.tooltip` is absent (set **both** when label and control should share the same help text).

`@Comment` intro lines use the field name as the translation key (e.g. `guardsIntro` → `guardvillagers.midnightconfig.guardsIntro`).

Example (`en_us.json`):

```json
{
  "mymod.midnightconfig.title": "My Mod",
  "mymod.midnightconfig.category.general": "General",
  "mymod.midnightconfig.generalIntro": "Short intro shown at the top of the tab.",
  "mymod.midnightconfig.enableFeature": "Enable Feature",
  "mymod.midnightconfig.enableFeature.label.tooltip": "Shown when hovering the option name.",
  "mymod.midnightconfig.enableFeature.tooltip": "Shown when hovering the toggle."
}
```

### MidnightLib version notes

| Feature | 1.5.7 (1.21.x) | 1.9.x (26.x) |
|---|---|---|
| `@Comment` tab intros | yes | yes |
| `.label.tooltip` / `.tooltip` split | yes (`MidnightConfig$EntryInfo`) | yes (`EntryInfo`) |
| `@Condition` (show/hide fields) | **no** | yes |

Source: MidnightLib `EntryInfo.getTooltip()` (1.9.x) and `MidnightConfig$EntryInfo` (1.5.7). Validated on mc-guardvillagers (1.5.7 on 1.21.11, 1.9.x on 26.2).

## 7. Multi-version subprojects

- **Metadata** (`custom.modmenu`, lang keys): usually identical in every subproject’s `fabric.mod.json`; duplicate or merge from root `src/main/resources` per your repo’s resource strategy.
- **Java API**: client-only; under `src/client/java` (or per-subproject client sources). Do not register `modmenu` entrypoints on server-only artifacts.
- **Dependency coordinate style:** `implementation` on **26.x**; `modImplementation` / `modCompileOnly` on **1.21.x** Yarn lines (see §4).
- **Mappings:** Mod Menu artifacts are named for the game version — pin **per subproject** `modmenu_version` in each `gradle.properties` or version catalog row.

## 8. Verification checklist

Run in a **client** dev env with Mod Menu on the runtime classpath (or in `mods/`):

1. Open Mod Menu → find the mod by name; search **configurable** filter should list it when a config factory is registered.
2. Select the mod → description, links, badges, and credits look correct (not raw translation keys).
3. **Configure…** appears when a config factory is registered (your `ModMenuApi`, Cloth Config, or MidnightLib after `MidnightConfig.init`); click opens your screen; **Done** returns to Mod Menu without error.
4. If using Cloth Config, confirm `AutoConfig` / builder matches the serialized config file on disk after save.
5. If using MidnightLib, confirm every `@Entry` field has a matching `<fieldName>` label key; tooltips use `.label.tooltip` and `.tooltip` as needed (§6b).
6. Library/API modules: hidden by default with `library` badge; visible when **Libraries → Shown**.
7. Multi-module: children nest under the parent; dummy parent shows once with shared icon/description.
8. Dedicated-server pack audit: `modmenu` not in server `mods/` (client-only).

### Common failures

| Symptom | Likely cause |
|---|---|
| No **Configure…** button | Missing `entrypoints.modmenu`, factory returns null, entrypoint in wrong mod container, or `MidnightConfig.init` not called |
| Raw `mymod.midnightconfig.fieldName` in config UI | Lang key missing or Java `@Entry` field name does not match the key suffix |
| Label has no tooltip but control does | Only `.tooltip` set — labels need `.label.tooltip` (no fallback) |
| `Failed to load config screen for '…'` | Factory throws; fix stack trace in log — Mod Menu shows this for the **target mod id**, not Mod Menu itself |
| Raw `modmenu.descriptionTranslation.foo` in UI | Missing lang entry for that mod id |
| Config works in dev, not for players | Forgot to ship the `modmenu` entrypoint class in the release JAR (client source set / split not wired) |
| Button on wrong mod | `modmenu` entrypoint registered in another module’s `fabric.mod.json` |

## 9. Optional APIs (rare)

- `getUpdateChecker()` — override Modrinth hash checking for your mod.
- `getProvidedConfigScreenFactories()` / `getProvidedUpdateCheckers()` — **library mods only**.
- `attachModpackBadges(Consumer<String>)` — mark bundled mods with the Modpack badge.
- `ModMenuApi.createModsScreen(parent)` / `createModsButtonText()` — embed Mod Menu from your own UI.

## Examples

- "Add Mod Menu config for our Cloth settings" → §4–6: `ModMenuApi`, `entrypoints.modmenu`, `AutoConfig.getConfigScreen`.
- "Polish MidnightLib config labels and tooltips" → §6b: `midnightconfig.*` keys, `.label.tooltip` vs `.tooltip`.
- "Hide our API module from the default list" → §2: `"badges": ["library"]`.
- "Group `mymod-core` and `mymod-client` under one parent" → §2 parents + consistent dummy parent metadata in both JARs.
- "Port Mod Menu wiring to mc26.2" → §4 `implementation` dependency + verify Mod Menu release supports 26.2.
