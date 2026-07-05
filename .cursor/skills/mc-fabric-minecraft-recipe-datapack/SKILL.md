---
name: mc-fabric-minecraft-recipe-datapack
description: "Covers Fabric/Minecraft crafting-recipe JSON across version boundaries: datapack path changes, ingredient format shifts, result format, and custom CraftingRecipe matching pitfalls. Use when adding, fixing, or debugging shaped/shapeless/smithing/datapack recipes or custom RecipeSerializers."
triggers:
  - crafting recipe
  - crafting_shaped
  - crafting_shapeless
  - data pack recipe
  - recipe json
  - ingredient format
  - CustomRecipe
  - RecipeSerializer
  - tipped arrow recipe
  - recipe folder
  - recipe parse error
dependencies: []
version: "2.0.0"
---

# Fabric / Minecraft crafting-recipe datapack reference

## 1. Datapack directory name

| Version range | Directory | Notes |
|---|---|---|
| **≤ 1.20.x** | `data/<namespace>/recipes/` | Plural |
| **1.21+** | `data/<namespace>/recipe/` | Singular — Mojang renamed all plural datapack folders |

If shaped recipes silently fail to load, check for a leftover **plural** `recipes/` path.

Custom recipe **type** JSON (e.g. `{ "type": "mymod:my_recipe", "category": "misc" }`) lives in the **same** directory as normal recipes.

## 2. Ingredient format

This is the most common cross-version breakage.

| Version range | Format | Example |
|---|---|---|
| **≤ 1.21.1** | Object | `{ "item": "minecraft:stick" }` or `{ "tag": "minecraft:planks" }` |
| **1.21.2+** | Plain string | `"minecraft:stick"` (item) or `"#minecraft:planks"` (tag) |

The old object format **will not parse** on 1.21.2+. Typical errors:

- `"Not a string: {\"item\":\"minecraft:...\"}"`
- `"No key fabric:type in MapLike[{\"item\":\"minecraft:...\"}]"`
- `"Failed to parse either. First: Not a string; Second: Not a json array"`

Arrays are supported in both eras for multiple alternatives:

- **≤ 1.21.1:** `[{ "item": "minecraft:a" }, { "item": "minecraft:b" }]`
- **1.21.2+:** `["minecraft:a", "minecraft:b"]`

## 3. Result format

| Version range | Format |
|---|---|
| **≤ 1.20.x** | `"result": { "item": "mymod:thing", "count": 4 }` |
| **1.21+** | `"result": { "id": "mymod:thing", "count": 4 }` |

Note the key change from `item` to `id` in the result block at 1.21.

## 4. Shaped recipe template (1.21.2+)

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [
    "NNN",
    " S ",
    " F "
  ],
  "key": {
    "N": "minecraft:iron_nugget",
    "S": "minecraft:stick",
    "F": "minecraft:feather"
  },
  "result": {
    "id": "mymod:iron_arrow",
    "count": 4
  }
}
```

For **≤ 1.21.1**, wrap each ingredient value in `{ "item": "..." }` and tags in `{ "tag": "..." }`.

## 5. Multi-version subproject strategy

When a single mod ships JARs for multiple Minecraft versions using per-version subprojects (e.g. `mc1.21/`, `mc1.21.4/`, etc.):

1. **Root recipes** use the **newest** supported format (currently 1.21.2+ strings).
2. **Older subprojects** override the same relative path with the format their version expects.
3. Resource merging (Loom `DuplicatesStrategy.EXCLUDE`, subproject `srcDirs` listed before root) ensures the override wins at build time — only subprojects that differ need a copy.

### Version-gated items

Some items only exist in certain minors (e.g. `minecraft:copper_nugget` appeared in **1.21.9**). A recipe referencing a missing item fails to load.

**Pattern:** root recipe uses the newest item; subprojects targeting older versions override with an available substitute (or remove the recipe entirely).

## 6. Custom `CraftingRecipe` (Java) pitfalls

- **Matching:** Prefer **`ItemStack.isSameItem`** when all slots must hold the "same" item. Stacks from crafting output, creative, commands, or trading may carry different component maps; **`isSameItemSameComponents`** rejects them.
- **Output:** Build results from a **representative input stack** (e.g. `arrowRef.copyWithCount(n)`) then set only the components the recipe changes. Avoid bare `new ItemStack(item, n)` when component parity with inputs matters.
- **`canCraftInDimensions`:** Must match vanilla grid-size expectations (typically `width >= 3 && height >= 3` for 3×3-only recipes).

## 7. Verification checklist

- Confirm the built JAR contains `data/<namespace>/recipe/*.json` (singular for 1.21+).
- Smoke-test crafting in the **oldest and newest** shipped minors.
- When using version-gated items, verify the recipe loads (no parse errors in log) on every targeted version.
- After renames or moves, grep for stale plural `recipes/` references in resource paths.
