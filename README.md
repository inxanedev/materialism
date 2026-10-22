# Materialistic

A dark-fantasy Fabric economy and shop mod for Minecraft **26.2**.

Materialistic adds a per-world currency, a sell screen, and a category-based shop.

## Features

- Sell items through `/sell`, a keybind, or the **Sell Items** button in the shop.
- Buy items through `/shop` or `/buy`.
- Check your balance with `/balance` or `/bal`.
- Per-world JSON economy stored by player UUID.
- Sell prices account for item durability.
- Shop supports categories, search, paging, stack/custom quantities, and shulker-box packaging.
- Optional ModMenu configuration screen.
- Controller-friendly shop navigation: every item slot and control is focusable, compatible with Controlify.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.20.1 or newer
- Fabric API 0.161.0+26.2
- Java 25
- ModMenu 20.0.3 or newer (optional)
- Controlify 3.5.3 or newer (optional, for controller navigation)

## Build

```sh
./gradlew build
```

The mod jar is written to `build/libs/materialistic-0.2.1-SNAPSHOT.jar`.

## Commands

| Command | Description |
| --- | --- |
| `/sell` | Open the selling screen |
| `/shop`, `/buy` | Open the shop |
| `/balance`, `/bal` | Show your balance |

The sell and shop keybinds are unassigned by default.

## Configuration

Shop pricing is controlled server-side by `config/materialistic-shop.json`:

```json
{
  "buyPriceMultiplier": 3.0,
  "shulkerBoxPriceOverride": null,
  "maxShulkerBoxesPerPurchase": 5,
  "itemPriceOverrides": {}
}
```

The client settings file is `config/materialistic-client.json`.

## Version

Development version: **v0.2.1**
