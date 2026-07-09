# Guard Villagers × Mob Civil War — compatibility notes

**Date:** 2026-07-04 (matrix) · **2026-07-07** (GV coexistence layer)  
**Mods:** Guard Villagers (GV) · [Mob Civil War](https://modrinth.com/mod/mob-civil-war) (CW)  
**CW repo:** `~/Documents/Cursor/Github/mc-civil-war` (`ImpureCrumpet/mc-civil-war`)  
**CW integration reference:** Stranded Soldiers (`CivilWarCompat` + `stranded_defender` tag)  
**GV code:** `dev.sterner.guardvillagers.integration.CivilWarCompat` (detect only — no faction goals on guards)  
**Minder skill track:** [Minecraft-Minder#6](https://github.com/ImpureCrumpet/Minecraft-Minder/issues/6)  
**Minder dependency registry:** [Minecraft-Minder#7](https://github.com/ImpureCrumpet/Minecraft-Minder/issues/7)

Neither mod hard-depends on the other. This doc captures expected runtime behavior when both are installed on matching MC lines (1.21.11, 26.x).

---

## GV coexistence policy (implemented)

| Rule | Rationale |
|------|-----------|
| **`fabric.mod.json` → `suggests.civil-war`** | Soft pairing on PandaServer; no hard dependency or release coupling |
| **`CivilWarCompat` detects CW only** | Logs once at startup when both mods load; **does not** call `CivilWarIntegration.applyFactionGoals` |
| **Guards stay `PathAwareEntity`** | CW faction install runs on `HostileEntity` only — guards remain outside factions |
| **No CW repo changes for GV features** | Companion owns bridge; CW changes only for new generic hooks |
| **Config defaults stay conservative** | `attackAllMobs = false`, `raidAnimals = false` — less goal crowding when CW is active |
| **Re-check on major AI rewrites** | Do not replace hostile `initGoals` wholesale; keep mixin targets separate from CW's `initGoals` TAIL |

**Agents:** load Minder skill `mc-fabric-civil-war-compat` when implemented ([#6](https://github.com/ImpureCrumpet/Minecraft-Minder/issues/6)); until then, this file is the contract.

---

## Summary

| Verdict | Detail |
|---------|--------|
| **Safe to run together** | No known crash/mixin clash; complementary goals, not replacements |
| **GV coexistence layer** | `suggests` + `CivilWarCompat` (detect/log); guards **not** in CW factions |
| **Guards outside CW factions** | Guards never receive or participate in undead / illager / arthropod faction AI |
| **Playtest still wise** | Raid + ambient CW battles — goal priority crowding possible, not predicted to break |

---

## How Civil War works (relevant bits)

- **`GlobalFactionMixin`** injects at `MobEntity.initGoals` TAIL.
- **`FactionGoalInstaller.installDefaultFactionGoals`** runs only for **`HostileEntity`**.
- Faction targeting uses **`EntityTypeTags`**: `SENSITIVE_TO_SMITE` (undead), `RAIDERS` / `ILLAGER`, `ARTHROPOD` (minus silverfish/bee/endermite).
- Faction `ActiveTargetGoal`s are added at **priority 2**.
- Optional companion hook: **`CivilWarIntegration.applyFactionGoals(mob)`** — today only for mobs tagged **`stranded_defender`** (Stranded Soldiers).

## How Guard Villagers works (relevant bits)

- **`GuardEntity`** extends **`PathAwareEntity`** — not `HostileEntity`, not `Monster`.
- Custom entity type **`guardvillagers:guard`** — not in CW faction tags.
- **`ServerWorldMixin.onSpawn`** adds guard-targeting goals when hostiles/illagers/etc. spawn (config-gated in places).
- **`MobEntityMixin.onSetTarget`** — when something targets a villager/guard, nearby guards/golems assist; iron golems ignore guard revenge.
- Guards target raiders, monsters (config), zombies, witches, players (when angered), etc. via their own `targetSelector` in `GuardEntity.initGoals`.

---

## Interaction matrix

| Scenario | GV | CW | Expected result |
|----------|----|----|-----------------|
| Guard spawned | Village defense AI | No-op (not hostile) | Guard defends; no faction membership |
| Zombie / skeleton spawned | May target guard (zombie @ 3; `attackAllMobs` → all hostiles @ 2) | Faction goals @ 2 vs illagers/spiders | Mob fights guards **and** faction enemies — additive |
| Illager / raider spawned | Targets guard @ 2 | Faction goals @ 2 vs undead/spiders | Raid + faction war overlap — busier AI |
| Spider spawned | Daytime guard attack goal @ 3 | Faction goals @ 2 | Spider may fight guards and CW factions |
| Witch spawned | May target villager/golem/guard (config) | No default faction install (not `HostileEntity`) | GV-only witch behavior |
| Iron golem | GV revenge tolerance for guards | — | Unrelated to CW |
| Ambient world | — | Undead vs illager vs arthropod skirmishes | Guards engage monsters already fighting each other |

---

## Mixin / technical overlap

| Mod | `MobEntity` mixin | Injection |
|-----|-------------------|-----------|
| **CW** | `GlobalFactionMixin` | `initGoals` @ TAIL |
| **GV** | `MobEntityMixin` | `setTarget` @ TAIL |

Different methods → should coexist. No shared accessor conflict with CW's `MobTargetSelectorAccessor` (GV uses access widener on `targetSelector` / `goalSelector` instead).

**Goal priority stacking:** Both mods add `ActiveTargetGoal` at priority **2** on several hostiles (e.g. illagers, ravagers). Minecraft runs all goals that can start; closest valid target wins. Unlikely to crash; edge-case weirdness possible during dense raids.

---

## What is *not* a conflict

- Guards are **not** CW faction members and are **not** auto-targeted by CW faction tags.
- CW does **not** remove or replace existing mob goals — only appends faction targets.
- GV does **not** replace `initGoals`; it adds spawn-time goals and one `setTarget` hook.
- Both mods ship 1.21.11 and 26.x lines independently; version-match the JARs, not each other's release tags.

---

## Risks to watch in playtesting

1. **Raid density** — illagers juggling player, guards, and CW faction targets at the same priority band.
2. **`attackAllMobs` (GV config)** — if enabled, every hostile gets guard-targeting on top of CW faction goals.
3. **No designed pairing** — unlike SS↔CW, guards do not get faction-aware retaliation rules via `CivilWarIntegration`.
4. **Future GV redo** — if AI is rewritten heavily, re-check against CW's `initGoals` TAIL inject (CW README warns about mods that fully replace mob AI).

---

## Optional integration (not implemented — by design)

GV intentionally does **not** use SS-style `applyFactionGoals` on guards. Faction membership would change gameplay (undead vs illager wars), not improve safety.

If a future design explicitly wants guard faction behavior, CW would need a **new generic hook** or tag — not a GV-only change:

| Piece | SS pattern | GV analogue (idea) |
|-------|------------|-------------------|
| Tag | `stranded_defender` | e.g. `guard_villager` or reuse none (call hook for all guards) |
| Caller | `CivilWarCompat.applyFactionGoals(mob)` after spawn AI | After `GuardEntity` goal init |
| CW side | `CivilWarIntegration.applyFactionGoals` → `installStrandedDefenderFactionGoals` | Would need CW API extension or new integration method |
| Target rules | `DefenderCombatRules` allows CW faction targets | Guard revenge / assist rules when CW loaded |

**Note:** Current CW hook only applies faction goals to **`stranded_defender`** tagged mobs. Guards would need either a new CW integration entry point or a tag CW recognizes — not a GV-only change.

Implemented soft coupling (no faction goals):

- GV: `fabric.mod.json` → `"suggests": { "civil-war": "*" }`
- GV: `integration/CivilWarCompat.java` — detect + startup log only (see **GV coexistence policy** above)

---

## References

- CW README — Stranded Soldiers companion section
- CW: `FactionGoalInstaller.java`, `GlobalFactionMixin.java`, `CivilWarIntegration.java`, `IntegrationTags.java`
- GV: `GuardEntity.java`, `ServerWorldMixin.java`, `MobEntityMixin.java`
- SS reference impl: `mc-stranded-soldiers/.../integration/CivilWarCompat.java`
