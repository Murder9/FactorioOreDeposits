# Advanced Ore Deposits

Advanced Ore Deposits is a NeoForge 1.21.1 mod that adds rare, Factorio-style surface ore deposit patches for automation-heavy modpacks.

Deposits are one block deep, store a finite remaining reserve in block entity NBT, and regenerate themselves when mined until that reserve is exhausted. The mod is balanced around Create automation, where deposits are useful but intentionally slower and more processing-heavy than normal ore mining.

## Features

- Rare surface deposit patches in plains, sunflower plains, desert, jungle, sparse jungle, and savanna.
- Supported deposits: iron, copper, gold, diamond, coal, andesite, redstone, lapis lazuli, nether quartz, and Create zinc when Create is installed.
- Deposits track `OreType`, `RemainingBlocks`, `MaxBlocks`, `PatchDiameter`, and `PatchId` in synced block entity NBT.
- Mining drops ore chunks instead of full resources.
- Deposits regenerate while they still have remaining blocks, then disappear when exhausted.
- Automation drops try nearby inventories above the deposit first, then below it, before dropping the item in-world.
- Optional Jade display for deposit type and remaining blocks.
- Optional Create goggles display for deposit type and remaining blocks.
- Sneak right-click a deposit with a Create wrench to remove it and drop the base ore/resource block.
- NeoForge common config at `config/advanced_ore_deposits-common.toml`.

## Requirements

- Minecraft `1.21.1`
- NeoForge `21.1.228` or newer in the same 1.21.1 line
- Java 21

Optional integrations:

- Create `6.0.10+`
- Jade `15.x`

## Processing

Deposit blocks drop raw chunks. Most direct resources craft from 8 raw chunks:

- 8 raw copper chunks -> raw copper
- 8 raw diamond chunks -> diamond
- 8 raw coal chunks -> coal
- 8 raw andesite chunks -> andesite
- 8 raw redstone chunks -> redstone dust
- 8 raw lapis chunks -> lapis lazuli
- 8 raw nether quartz chunks -> nether quartz

Smelted-chain ores use smelting first:

- Raw iron chunk -> smelted iron chunk -> raw iron
- Raw gold chunk -> smelted gold chunk -> raw gold
- Raw zinc chunk -> smelted zinc chunk -> raw zinc, when Create is installed

## Building

```powershell
.\gradlew.bat clean build
```

The release jar is written to:

```text
build/libs/advanced_ore_deposits-1.0.1.jar
```

## Development Notes

The deposit feature is injected into allowed vanilla biomes during `top_layer_modification`, so it runs after most surface decoration. If a deposit intersects plants or a tree, the mod clears plants and removes connected logs/leaves before placing the deposit.

Existing worlds need newly generated chunks for new deposits to appear.
