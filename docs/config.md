# Configuration (Mod Menu / MidnightLib)

Open **Mods → Guard Villagers → Configure** (requires [Mod Menu](https://modrinth.com/mod/modmenu); [MidnightLib](https://modrinth.com/mod/midnightlib) is bundled).

Settings are stored in `config/guardvillagers.json`.

The in-game screen has six tabs. Hover any option name or control for a short description.

## Guards

| Option | Default | What it does |
|--------|---------|--------------|
| **Open Doors** | on | Guards open doors while patrolling. |
| **Shield Formation** | on | Shield guards group and hold formation in combat. |
| **Flee Polar Bears** | off | Guards run from polar bears. |
| **Follow Hero of the Village** | on | Guards can follow a player with Hero of the Village. |
| **Seek Cleric Healing** | on | Injured guards path to clerics. |
| **Armorer Armor Repair** | on | Guards visit armorers to repair armor. |
| **Passive Regeneration** | `1` | Health restored periodically while injured. |
| **Equipment Drop Chance** | `1` | Death drop chance from 0–1 (`1` = always, unless prevent-drop enchants apply). Legacy configs with `100` still always drop. |
| **Use Player Model** | off | Classic player model instead of the guard model. |

## Combat

| Option | Default | What it does |
|--------|---------|--------------|
| **Always Raise Shield** | off | Keep shields up whenever possible. |
| **Avoid Friendly Fire** | on | Crossbow guards reposition when allies are in the line of fire. |
| **Projectiles Hurt Villagers** | on | When off, guard projectiles do not hurt villagers or golems. |
| **Attack All Hostile Mobs** | off | Target any hostile mob, not only raid-related enemies. |
| **Attack Blacklist** | empty | Entity IDs to never attack (e.g. `minecraft:creeper`). |
| **Ally Help Range** | `50` | Blocks within which allies join a fight. |

## Interaction

| Option | Default | What it does |
|--------|---------|--------------|
| **Reputation to Interact** | `15` | Minimum reputation to open guard inventory / give items. |
| **Reputation to Anger** | `-100` | At or below this, guards may attack you. |
| **Hero Required to Equip** | off | Need Hero of the Village to give items. |
| **Hero Required for Patrol** | off | Need Hero of the Village to set patrol points. |
| **Hero Required to Convert** | off | Need Hero of the Village to convert villagers with sword/crossbow. |

## Village

| Option | Default | What it does |
|--------|---------|--------------|
| **Raiders Attack Animals** | off | Raiders also target animals during raids. |
| **Witches Attack Villagers** | on | Witches target villagers. |
| **Smiths Repair Golems** | on | Toolsmiths / weaponsmiths repair iron golems. |
| **Illagers Flee Polar Bears** | on | Illagers run from polar bears. |
| **Villagers Flee Polar Bears** | on | Villagers run from polar bears. |

## Spawning

Natural villagers can spawn guards on chunk load. Each villager is processed at most once (`guardvillagers.natural_spawn_processed` tag). See [Natural guard spawning](../README.md#natural-guard-spawning).

| Option | Default | What it does |
|--------|---------|--------------|
| **Spawn Chance** | `0.2` | One-time 0–1 roll per natural villager. `0` = minimum-fill only. |
| **Minimum per Cluster** | `1` | Fill sparse clusters up to this many guards. |
| **Cluster Radius** | `64` | Block radius for spawn counting. |

## Stats

Applied to newly spawned guards only.

| Option | Default | What it does |
|--------|---------|--------------|
| **Max Health** | `20` | Base maximum health. |
| **Movement Speed** | `0.5` | Base movement speed. |
| **Detection Range** | `20` | How far guards detect and pursue targets. |

## Notes

- **Hero of the Village** is the vanilla effect from winning a raid.
- For Mob Civil War coexistence, see [civil-war-compat.md](civil-war-compat.md).
