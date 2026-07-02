# Create Recipe Hooks (CRH)

A server-side API mod for **Minecraft 1.20.1 / Forge 47+** that fires a unified Forge event whenever a recipe completes in any **Create 6.0.8+** machine (and some addon machines). Lets quests, scripts, and other mods react to "player smelted / crushed / mixed X", with player attribution via UUID.

## Requirements

| Component | Version | Required |
|---|---|---|
| Minecraft | 1.20.1 | yes |
| Forge | 47+ | yes |
| Create | 6.0.8+ | yes |
| KubeJS + **EventJS** | 1.20.1 | only for JS scripts |

> **Important:** the mod itself does not depend on KubeJS. However, to subscribe to the event from a JS script you need **EventJS** (a KubeJS addon that adds `NativeEvents.onEvent` for native Forge events). Without EventJS the event is only accessible to Java mods via `MinecraftForge.EVENT_BUS`.

## The Event

The mod posts a single event on the Forge event bus:

```
dev.createrecipehooks.neoforge.CreateRecipeFinishedEvent
```

One event = one recipe application. If a machine processes a whole stack at once (e.g. a Fan smelting an ItemEntity with a stack of 4, or a Crushing Wheel grinding a stack of ore), there will be **one** event, with the quantity reflected in the `getCount()` of the output stacks.

The event fires **server-side only**.

### Event methods

| Method | Type | Description |
|---|---|---|
| `getSource()` | `RecipeSource` | Which machine completed the recipe (see table below). Never `null` |
| `getLevel()` | `Level` | The server level. Never `null` |
| `getRecipeId()` | `ResourceLocation` | Recipe ID, e.g. `create:crushing/raw_iron`. May be `null` (Spout/Item Drain via fluid capability, CEI Printer) |
| `getRecipe()` | `Recipe<?>` | The recipe object. May be `null` even when `getRecipeId()` is present |
| `getItemOutputs()` | `List<ItemStack>` | Output items. Do not mutate. Empty list when there are no item outputs |
| `getItemInputs()` | `List<ItemStack>` | Input items (available for Fan, Sequenced Assembly, CEI Printer) |
| `getFluidOutputs()` | `List<FluidAmount>` | Fluid outputs (Basin, Item Drain) |
| `getBlockPos()` | `BlockPos` | Position of the machine. `null` for Fan (item in world), belt Deployer, Sand Paper |
| `getPlayer()` | `ServerPlayer` | Only for `SAND_PAPER` (manual use). `null` for everything else; use metadata instead |
| `getMetadata()` | `Map<String, Object>` | Metadata keys (see below) |
| `getTimestamp()` | `long` | `System.nanoTime()` at the moment of the event |

## Sources (`RecipeSource`)

### Active sources (events are fired)

| Source | Machine | `owner_uuid` in metadata | Who it refers to |
|---|---|---|---|
| `BASIN` | Mixer / Compactor / pressing in a Basin / CEI Infuser | ✅ | who placed the Basin |
| `MECHANICAL_PRESS` | Press (belt and world modes) | ✅ | who placed the Press |
| `MILLSTONE` | Millstone | ✅ | who placed the Millstone |
| `CRUSHING_WHEEL` | Crushing Wheels | ✅ | **who threw the item** into the wheels (not an owner, since the controller is never player-placed). Items from hoppers/machines carry no UUID |
| `MECHANICAL_SAW` | Saw | ✅ | who placed the Saw |
| `MECHANICAL_CRAFTER` | Mechanical Crafter | ✅ | who placed the output crafter (the last one in the chain) |
| `DEPLOYER_BELT` | Deployer over a belt | ✅ | the Deployer's owner (Create's built-in field) |
| `SAND_PAPER` | Sand Paper | no | `getPlayer()` is non-null on manual use |
| `SEQUENCED_ASSEMBLY` | Final step of a Sequenced Assembly | no | intermediate steps do not fire events |
| `FAN_BLASTING` | Fan + lava/burner (both smelting **and** blasting recipes) | ✅ | who placed the Fan |
| `FAN_SMOKING` | Fan + fire | ✅ | who placed the Fan |
| `FAN_SPLASHING` | Fan + water | ✅ | who placed the Fan |
| `FAN_HAUNTING` | Fan + soul fire | ✅ | who placed the Fan |
| `SPOUT_FILLING` | Spout | ✅ | who placed the Spout. `getRecipeId()` is `null` when filling via fluid capability (buckets and other containers without a `FillingRecipe`) |
| `ITEM_DRAIN_EMPTYING` | Item Drain | ✅ | who placed the Drain |
| `CEI_PRINTER` | Printer from Create Enchantment Industry | ✅ | who placed the Printer (only when CEI is installed) |
| `UNKNOWN` | Unrecognized addon going through `RecipeApplier` | ✅ when context is known | n/a |

Fan events cover **both modes**: items on a belt and items lying in the air current in the world.

### Reserved sources (NO events are fired in v1)

`DEPLOYER_DIRECT`, `MANUAL_APPLICATION`, `CEI_GRINDSTONE`, `CEI_INFUSER` (events arrive as `BASIN`), `CEI_SALVAGING` (arrive as `FAN_*`), `POWERGRID_MAGNETIZING`. Filtering on these values will never match.

## Metadata keys

| Key | Type | Description |
|---|---|---|
| `createrecipehooks:owner_uuid` | `String` | Player UUID (see the sources table). Absent when the machine was placed by a non-player (piston, command) or the item came from automation |

The owner UUID is written into the block entity's NBT when a player places the block, and survives world reloads. For the Crushing Wheel the UUID is taken from the `ItemEntity` (whoever dropped the item).

## Script examples (KubeJS + EventJS)

All examples are verified in-game. Place them in `kubejs/server_scripts/`.

### 1. Basic: an emerald for any recipe of a machine

```javascript
NativeEvents.onEvent(
    Java.loadClass('dev.createrecipehooks.neoforge.CreateRecipeFinishedEvent'),
    event => {
        if (event.getSource().name() !== 'MECHANICAL_PRESS') return;

        const ownerUuidStr = event.getMetadata().get('createrecipehooks:owner_uuid');
        if (!ownerUuidStr) return;

        const player = event.getLevel().server.playerList.getPlayer(
            Java.loadClass('java.util.UUID').fromString(ownerUuidStr)
        );
        if (!player) return; // owner is offline

        player.server.runCommandSilent('give ' + player.name.string + ' minecraft:emerald 1');
    }
);
```

For the Fan use `event.getSource().name().startsWith('FAN_')` to catch all 4 processing types.

### 2. Filtering by a specific recipe

```javascript
NativeEvents.onEvent(
    Java.loadClass('dev.createrecipehooks.neoforge.CreateRecipeFinishedEvent'),
    event => {
        if (event.getSource().name() !== 'BASIN') return;

        const recipeId = event.getRecipeId();
        if (!recipeId) return;
        if (recipeId.toString() !== 'create_enchantment_industry:mixing/hyper_experience') return;

        // ... award the reward as in example 1
    }
);
```

### 3. Counting batch quantity (Crushing Wheel)

A single batch may contain multiple items, so sum `getCount()`:

```javascript
NativeEvents.onEvent(
    Java.loadClass('dev.createrecipehooks.neoforge.CreateRecipeFinishedEvent'),
    event => {
        if (event.getSource().name() !== 'CRUSHING_WHEEL') return;

        const BuiltInRegistries = Java.loadClass('net.minecraft.core.registries.BuiltInRegistries');
        const outputs = event.getItemOutputs();

        let total = 0;
        for (let i = 0; i < outputs.size(); i++) {
            const stack = outputs.get(i);
            const key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (key && key.toString() === 'create:crushed_raw_iron') total += stack.getCount();
        }
        if (total < 8) return;

        // ... find the player by owner_uuid and complete the quest:
        // player.server.runCommandSilent('ftbquests change_progress ' + player.name.string + ' complete <QUEST_ID>');
    }
);
```

### 4. Accumulating a counter across events (via player NBT)

The Press processes 1 item per event. The counter is stored in `player.getPersistentData()`, which survives server restarts:

```javascript
NativeEvents.onEvent(
    Java.loadClass('dev.createrecipehooks.neoforge.CreateRecipeFinishedEvent'),
    event => {
        if (event.getSource().name() !== 'MECHANICAL_PRESS') return;

        const recipeId = event.getRecipeId();
        if (!recipeId || recipeId.toString() !== 'create:pressing/iron_ingot') return;

        const ownerUuidStr = event.getMetadata().get('createrecipehooks:owner_uuid');
        if (!ownerUuidStr) return;

        const player = event.getLevel().server.playerList.getPlayer(
            Java.loadClass('java.util.UUID').fromString(ownerUuidStr)
        );
        if (!player) return;

        const nbt = player.getPersistentData();
        const count = (nbt.contains('crh_press_iron') ? nbt.getInt('crh_press_iron') : 0) + 1;

        if (count < 4) {
            nbt.putInt('crh_press_iron', count);
            return;
        }

        nbt.putInt('crh_press_iron', 0);
        player.server.runCommandSilent('ftbquests change_progress ' + player.name.string + ' complete <QUEST_ID>');
    }
);
```

### 5. FTB Quests: complete a quest on any recipe of a machine

Verified on a dedicated server. Works the same for `MILLSTONE`, `SPOUT_FILLING`, `DEPLOYER_BELT`: only the source name and quest ID change.

```javascript
NativeEvents.onEvent(
    Java.loadClass('dev.createrecipehooks.neoforge.CreateRecipeFinishedEvent'),
    event => {
        if (event.getSource().name() !== 'MILLSTONE') return;

        const recipeId = event.getRecipeId();
        console.info('[Quest/Millstone] Recipe: ' + (recipeId ? recipeId.toString() : 'null'));

        const ownerUuidStr = event.getMetadata().get('createrecipehooks:owner_uuid');
        if (!ownerUuidStr) { console.warn('[Quest/Millstone] No owner UUID'); return; }

        const player = event.getLevel().server.playerList.getPlayer(
            Java.loadClass('java.util.UUID').fromString(ownerUuidStr)
        );
        if (!player) { console.warn('[Quest/Millstone] Owner is offline'); return; }

        player.server.runCommandSilent('ftbquests change_progress ' + player.name.string + ' complete <QUEST_ID>');
        console.info('[Quest/Millstone] Quest complete for ' + player.name.string);
    }
);
```

### 6. Spout: both filling paths

The Spout fills items in two different ways, and both fire `SPOUT_FILLING`:

- **Recipe path:** items with a `FillingRecipe` (honey bottle, blaze cake, ...). `getRecipeId()` returns the recipe ID, e.g. `create:filling/honey_bottle`.
- **Capability path:** buckets and any fluid-container item without a recipe. `getRecipeId()` returns **`null`** (filling a bucket with water is not a recipe).

Do not filter Spout events by recipe ID unless you specifically want recipe-based fills only:

```javascript
NativeEvents.onEvent(
    Java.loadClass('dev.createrecipehooks.neoforge.CreateRecipeFinishedEvent'),
    event => {
        if (event.getSource().name() !== 'SPOUT_FILLING') return;

        // recipeId is null for capability fills (buckets); this is normal
        const recipeId = event.getRecipeId();
        console.info('[Quest/Spout] Recipe: ' + (recipeId ? recipeId.toString() : 'null'));

        const ownerUuidStr = event.getMetadata().get('createrecipehooks:owner_uuid');
        if (!ownerUuidStr) { console.warn('[Quest/Spout] No owner UUID'); return; }

        const player = event.getLevel().server.playerList.getPlayer(
            Java.loadClass('java.util.UUID').fromString(ownerUuidStr)
        );
        if (!player) { console.warn('[Quest/Spout] Owner is offline'); return; }

        player.server.runCommandSilent('ftbquests change_progress ' + player.name.string + ' complete <QUEST_ID>');
        console.info('[Quest/Spout] Quest complete for ' + player.name.string);
    }
);
```

## Scripting pitfalls

- **Do not declare `const`/`let` at the top level of a file.** KubeJS loads all server_scripts into one shared scope, so identical names in two files cause `TypeError: redeclaration of const`. Keep all declarations inside the callback.
- **`getItemOutputs()` is a `java.util.List`**, not a JS array. Iterate via `.size()` / `.get(i)`.
- **JS variables do not persist across events.** A `new Map()` at file level will not keep your counter; use `player.getPersistentData()` (example 4).
- **Always null-check `owner_uuid`.** The machine may have been placed by a non-player, and an item in the Crushing Wheel may have come from a hopper.
- **The player may be offline** when the recipe completes, in which case `playerList.getPlayer()` returns `null`.
- **`getRecipeId()` may be `null`** for Spout capability fills and Item Drain capability emptying, so null-check before calling `.toString()`.
- FTB Quests command: `ftbquests change_progress <player> complete <quest_id>`.

## Usage from Java

```java
MinecraftForge.EVENT_BUS.addListener((CreateRecipeFinishedEvent event) -> {
    RecipeFinishedContext ctx = event.getContext();
    // ...
});
```

Public API: `dev.createrecipehooks.api` (`RecipeFinishedContext`, `RecipeSource`, `FluidAmount`, `CreateRecipeHooks.register(...)`). The `RecipeSource` enum is stable: values are never removed between minor versions; new ones may be added.

## License

Copyright (c) 2026 Aarow

LGPL-3.0-or-later. You may use this mod in modpacks and link against its API freely.
Forks and derivative mods must remain open source under the same license with attribution.
See [LICENSE](LICENSE) (LGPL-3.0) and [COPYING](COPYING) (GPL-3.0).
