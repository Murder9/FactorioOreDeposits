# FactorioOreDeposits

Fork of Advanced Ore Deposits for NeoForge 1.21.1. Adds rare, Factorio-style surface resource deposits for automation-heavy modpacks.

## Features

- Rare surface deposit patches in plains, sunflower plains, desert, jungle, sparse jungle, and savanna.
- Vanilla deposits: iron, copper, gold, diamond, coal, andesite, redstone, lapis lazuli, nether quartz, and **calcite**.
- Create zinc deposits when Create is installed.
- **Automatic modded ore deposits:** modded blocks in the `c:ores` tag, plus conventionally named `*_ore` blocks, are discovered at runtime. Stone/deepslate/netherrack variants of the same material are collapsed to one deposit entry.
- Modded deposits remember the exact source ore block in block-entity NBT, so the same world can contain deposits for many different mods without hard-coded integrations.
- Deposits track a finite reserve and regenerate themselves while remaining blocks are available.
- Mining now gives the normal resource directly: raw ores for metal deposits and whole resource items for direct resources such as coal, diamond, redstone, lapis, and calcite.
- Modded metal ores prefer a same-mod `raw_<material>` item when present, then `<material>`, then `<material>_ingot`, and finally the ore's own item.
- Automation can insert resources into nearby inventories above/below the deposit.
- Optional Jade and Create goggles display deposit information.
- Sneak right-click with a wrench removes a deposit and drops its source ore block.
- Existing worlds need newly generated chunks for new deposits to appear.

## Requirements

- Minecraft `1.21.1`
- NeoForge `21.1.228` or newer in the same 1.21.1 line
- Java 21

Optional integrations:

- Create `6.0.10+`
- Jade `15.x`

## Building

```powershell
.\gradlew.bat clean build
```

The release jar is written to:

```text
build/libs/advanced_ore_deposits-<version>.jar
```
