---
name: mc-gradle-daemon-hygiene
description: Manages Gradle daemon lifecycle, JDK selection, and wrapper hygiene for multi-version Fabric Minecraft builds.
triggers:
  - gradle daemon
  - gradlew
  - JAVA_HOME
  - Loom error
  - toolchain
  - j21
  - j25
  - offline build
  - stale daemon
  - configure time
  - OutOfMemoryError
  - metaspace
  - heap size
  - build hangs
dependencies: []
version: "1.3.2"
---

# Gradle Daemon Hygiene

**Scope:** Multi-version **Fabric** mod repos with 1.21.x and 26.x subprojects. JDK switching examples use **SDKMAN!** with `j21` / `j25` aliases when available; substitute your environment's equivalent (`sdk use java <ver>`, `JAVA_HOME=$(/usr/libexec/java_home -v <ver>)`, `mise use java@<ver>`, etc.) — the principles are the same. Not for single-version projects or non-Gradle build systems.

**Minder workspace:** If **`mods.yaml`** is at the repo root and **`mm-*`** skills exist under **`.cursor/skills/`**, defer **disk/cache cleanup** to **`mm-gradle-user-home-cleanup`** or **`mm-mod-repo-build-cleanup`** — not this skill. This skill still applies for JDK, daemon, Loom, heap, and wrapper hygiene (including **`./gradlew --stop`** before those cleanups). In mod clones without **`mm-*`**, this skill is authoritative for all Gradle hygiene here.

## When to use

- Before running **any** `./gradlew` command in a multi-version Fabric mod repo.
- After switching JDK versions (e.g. `j21` ↔ `j25`).
- When debugging cryptic Loom configure-time failures or asset resolution errors.
- When setting up a CI image or IDE Gradle JVM.

## The Daemon Trap

`java { toolchain { } }` in Gradle controls **compile targets** but does **not** control the JDK that launches the Gradle daemon. Loom resolves Minecraft assets and mappings at **configure time** inside the daemon process — if the daemon's JDK is wrong, toolchain settings are irrelevant.

**Key facts:**

- The daemon inherits `JAVA_HOME` from the shell that **first spawned it**.
- A daemon stays alive (default 3 hours idle timeout). Switching Java with `j25` **after** a JDK 21 daemon is already running has **no effect** on that daemon.
- 26.x subprojects require **JDK 25** at daemon level. A stale JDK 21 daemon causes cryptic Loom configure-time failures.

## Before any `./gradlew` command

### 1. Set the JDK

| Target subproject | Required JDK | SDKMAN alias (optional) |
|-------------------|--------------|-------------------------|
| **1.21.x** (`mc1.21.*`) | Temurin 21 | `j21` (`sdk use java 21`) |
| **26.x** (`mc26.*`) | Temurin 25 | `j25` (`sdk use java 25`) |

For a **full multi-version build** that includes any 26.x subproject, use **JDK 25** — JDK 25 can compile Java 21 targets via toolchain, but JDK 21 **cannot** satisfy Loom's 26.x configure-time requirements. If you don't use SDKMAN, switch JDKs however your environment supports it (mise, asdf, jenv, raw `JAVA_HOME` export, IntelliJ project SDK, etc.).

### 2. Kill the stale daemon (when switching JDKs)

If you previously ran Gradle under a different JDK in this terminal (or any terminal), stop the old daemon before proceeding:

```bash
./gradlew --stop
```

Then switch JDKs (e.g. `j21` / `j25` or your environment's equivalent) and invoke Gradle. A fresh daemon will spawn with the correct `JAVA_HOME`.

### 3. Verify (when debugging)

To confirm which JDK the running daemon is using:

```bash
./gradlew --status
```

Cross-reference the listed PID's Java version. If it doesn't match expectations, `--stop` and re-launch.

## Asset Fetching & Security

Gradle downloads wrappers, dependencies, and Minecraft assets. Protect the pipeline:

### Strict wrapper URLs

Always use the **full three-part version** in the Gradle wrapper filename:

```
gradle-9.4.0-bin.zip    ✅
gradle-9.4-bin.zip      ❌  (may resolve to unexpected patch)
```

When downloading or verifying the wrapper manually, use `curl -fL` to **fail fast** on HTTP errors instead of silently saving an XML error page:

```bash
curl -fL https://services.gradle.org/distributions/gradle-9.4.0-bin.zip -o gradle.zip
```

### Offline caching

After the initial dependency sync succeeds, subsequent builds on the same machine can skip network verification:

```bash
./gradlew build --offline
```

Useful when building on multiple Macs with a shared cache, or when working without reliable internet. Remove `--offline` when dependencies change.

## Heap & Metaspace for Multi-`mc*` Builds

Building **many** Loom subprojects in a single Gradle invocation increases daemon memory pressure. Default `org.gradle.jvmargs` (typically `-Xmx1G`) is often insufficient.

### Symptoms

- Daemon killed mid-build or silently restarted.
- `GC overhead limit exceeded` or `OutOfMemoryError`.
- Loom asset resolution stalling (long pauses during configure phase).
- `Metaspace` exhaustion (`java.lang.OutOfMemoryError: Metaspace`) when many plugins and subproject configurations are loaded simultaneously.

### Mitigation

Set heap and metaspace in **`gradle.properties`** at the project root:

```properties
org.gradle.jvmargs=-Xmx3G -XX:MaxMetaspaceSize=512m
```

Scale `-Xmx` with the number of `mc*` subprojects. A full 1.21.x ladder (10+ modules) may need **4G+**. CI images should mirror these settings — container memory limits must exceed `-Xmx` to leave room for off-heap/native memory.

### Interaction with daemon lifecycle

Changing `org.gradle.jvmargs` causes Gradle to **spawn a new daemon** (the old one does not adopt new JVM args). After editing `gradle.properties`, run `./gradlew --stop` to avoid two daemons competing for resources.

## Wrapper & Disk Hygiene

### Execution order in root `build.gradle.kts`

Defer shared Java and source-set wiring using `afterEvaluate { }` **before** touching `JavaPluginExtension` on subprojects. Eagerly configuring Java targets before Loom has finished its own configuration causes resolution failures.

### Dist folder bloat

Multi-version building accumulates old wrapper distributions in `~/.gradle/wrapper/dists/`. Periodically prune distributions older than 30 days, or add a cleanup step to update scripts.

## CI & IDE Configuration

- **IDE (Cursor / IntelliJ):** Set the Gradle JVM to **JDK 25** when the project includes any 26.x subproject. Toolchain handles compile-level targeting; the IDE Gradle JVM controls the daemon.
- **CI images:** Use a JDK 25 base image for full multi-version builds. Matrix jobs for 1.21.x-only can use JDK 21.

## Checklist

- [ ] Correct JDK selected (e.g. `j21` / `j25`) **before** `./gradlew`.
- [ ] Stale daemon killed (`./gradlew --stop`) after switching JDK versions.
- [ ] Wrapper URL uses full three-part version.
- [ ] IDE Gradle JVM matches the highest required JDK (25 if any 26.x subproject exists).
- [ ] `org.gradle.jvmargs` sized for multi-`mc*` builds when many Loom subprojects configure in one invocation (see **Heap & Metaspace** above).
- [ ] `afterEvaluate { }` used for shared Java wiring in root build script.

## Examples

- "My 26.x build fails with a Loom configure error" → run `./gradlew --stop`, switch to JDK 25 (`j25` or equivalent), then rebuild.
- "I just switched from mc1.21.11 to mc26.1 work" → kill daemon and switch JDK: `./gradlew --stop && j25` (or equivalent).
- "Is my daemon using the right Java?" → `./gradlew --status`, cross-reference PID.

## Reference

- **`.cursor/fabric-mod-build-release-guide-v4.3.md`** — Gradle configuration, Loom plugin pairing, and multi-version project architecture. (Daemon trap, JDK vs toolchain, and wrapper notes are covered in this skill’s sections above.)
