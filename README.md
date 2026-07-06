# Create Recipe Hooks (Fabric, Minecraft 1.20.1)

A server-side API mod for **Minecraft 1.20.1 / Fabric** that fires a unified event whenever a recipe completes in any **Create Fabric 6.0.8.1+** machine, and whenever a Create machine processes a world block: Drill mining, Harvester reaping, Saw felling trees. Lets quests, scripts, and other mods react to "player smelted / crushed / mined X", with player attribution via UUID.

## Requirements

| Component | Version | Required |
|---|---|---|
| Minecraft | 1.20.1 | yes |
| Fabric Loader | 0.15+ | yes |
| Fabric API | 0.92.2+ | yes |
| Create Fabric | 6.0.8.1+ | yes |
| KubeJS (Fabric) | 2001.6.5+ | only for JS scripts |

The mod is **server-side**: clients without it can join a server that has it. For singleplayer, install it in your instance.

## The Event

For Java mods the events are standard Fabric callbacks:

```
dev.createrecipehooks.fabric.CreateRecipeFinishedCallback.EVENT   (recipe completions)
dev.createrecipehooks.fabric.CreateBlockProcessedCallback.EVENT   (Drill, Harvester, lone Saw cuts)
dev.createrecipehooks.fabric.CreateTreeCutCallback.EVENT          (Saw felling a tree)
```

This section describes the recipe callback; the block events are documented in the
"Block processing events" section below.

One event = one recipe application. If a machine processes a whole stack at once (e.g. a Fan smelting an ItemEntity with a stack of 4, or a Crushing Wheel grinding a stack of ore), there will be **one** event, with the quantity reflected in the `getCount()` of the output stacks.

The event fires **server-side only** and is not cancellable.

### Context methods (`RecipeFinishedContext`)

| Method | Type | Description |
|---|---|---|
| `getSource()` | `RecipeSource` | Which machine completed the recipe (see table below). Never `null` |
| `getLevel()` | `Level` | The server level. Never `null` |
| `getRecipeId()` | `ResourceLocation` | Recipe ID, e.g. `create:crushing/raw_iron`. May be `null` (Spout/Item Drain via fluid capability, potion emptying) |
| `getRecipe()` | `Recipe<?>` | The recipe object. May be `null` even when `getRecipeId()` is present |
| `getItemOutputs()` | `List<ItemStack>` | Output items. Do not mutate. Empty list when there are no item outputs |
| `getItemInputs()` | `List<ItemStack>` | Input items (available for Fan, Sequenced Assembly, Spout, Item Drain) |
| `getFluidOutputs()` | `List<FluidAmount>` | Fluid outputs in **millibuckets** (Basin, Item Drain). Create Fabric's internal droplet amounts are converted for you (81 droplets = 1 mB) |
| `getBlockPos()` | `BlockPos` | Position of the machine. `null` for Fan (item in world), belt Deployer, Sand Paper |
| `getPlayer()` | `ServerPlayer` | Only for `SAND_PAPER` (manual use). `null` for everything else; use metadata instead |
| `getMetadata()` | `Map<String, Object>` | Metadata keys (see below) |
| `getTimestamp()` | `long` | `System.nanoTime()` at the moment of the event |

## Sources (`RecipeSource`)

### Active sources (events are fired)

| Source | Machine | `owner_uuid` in metadata | Who it refers to |
|---|---|---|---|
| `BASIN` | Mixer / Compactor / pressing in a Basin | ✅ | who placed the Basin |
| `MECHANICAL_PRESS` | Press (belt and world modes) | ✅ | who placed the Press |
| `MILLSTONE` | Millstone | ✅ | who placed the Millstone |
| `CRUSHING_WHEEL` | Crushing Wheels | ✅ | **who threw the item** into the wheels; for automated input (belts, hoppers) falls back to **whoever placed the wheels**. When two wheels have different owners, one of them is picked deterministically |
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
| `ITEM_DRAIN_EMPTYING` | Item Drain | ✅ | who placed the Drain. `getRecipeId()` is `null` for capability emptying (buckets) and potion emptying |
| `UNKNOWN` | Unrecognized addon going through `RecipeApplier` | ✅ when context is known | n/a |

Fan events cover **both modes**: items on a belt and items lying in the air current in the world.
Item Drain covers **three paths**: emptying recipes, fluid-capability containers (buckets), and potions.

### Not available on Fabric

`CEI_PRINTER`: there is no Create Enchantment Industry port for Create Fabric 6.x, so this source never fires on Fabric.

### Reserved sources (NO events are fired in v1)

`DEPLOYER_DIRECT`, `MANUAL_APPLICATION`, `CEI_GRINDSTONE`, `CEI_INFUSER` (events arrive as `BASIN`), `CEI_SALVAGING` (arrive as `FAN_*`), `POWERGRID_MAGNETIZING`. Filtering on these values will never match.

## Block processing events

Some Create machines process world blocks instead of recipes. CRH tracks them with two
additional events alongside `recipeFinished`:

| Event | Fires | Sources |
|---|---|---|
| `CRHEvents.blockProcessed` | once per processed block | `MECHANICAL_DRILL`, `MECHANICAL_HARVESTER`, `MECHANICAL_SAW` (a lone sawed block that is not part of a tree) |
| `CRHEvents.treeCut` | once per felled tree | `MECHANICAL_SAW` only |

The two are mutually exclusive: a saw cut fires exactly one of them, never both. A "tree"
also includes bamboo, cactus, sugar cane, kelp and chorus columns; a single log with
nothing connected above counts as a lone block and fires `blockProcessed`.

### Sources and attribution

| Source | Machine | Modes | `owner_uuid` refers to |
|---|---|---|---|
| `MECHANICAL_DRILL` | Mechanical Drill | stationary and contraption | who placed the Drill |
| `MECHANICAL_HARVESTER` | Mechanical Harvester | contraption only | who placed the Harvester |
| `MECHANICAL_SAW` | horizontal Mechanical Saw | stationary and contraption | who placed the Saw |

`event.isContraption()` tells the two modes apart. For contraption actors the owner UUID
travels inside the block entity NBT that Create serializes at assembly, so attribution
keeps working while the machine is moving and survives restarts. Machines placed before
CRH was installed have no stored owner: break and place them again once.

The upward-facing Saw still reports its cutting and stonecutting recipes through
`recipeFinished`; the block events cover only the horizontal (world-cutting) mode.

### Event methods

Both events share the `recipeFinished` basics (`getSource()`, `getOwner()`,
`getOwnerUuid()`, `getMetadata()`, `getLevel()`, `getTimestamp()`) plus:

| Method | Type | Description |
|---|---|---|
| `getBlockId()` | `String` | Processed block id, e.g. `minecraft:stone`. Never `null` |
| `getBlockState()` | `BlockState` | Full state of the processed block (crop age etc.). For `treeCut`: the starting log the saw touched |
| `getBlockPos()` | `BlockPos` | Position of the processed block |
| `isContraption()` | `boolean` | `true` when the machine was moving as a contraption actor |
| `getLogCount()` | `int` | `treeCut` only: number of logs in the felled tree (column height for bamboo and similar). `-1` when the tree was felled through the Dynamic Trees mod (size unknown) |
| `getLeafCount()` | `int` | `treeCut` only: number of leaf blocks, same `-1` rule |

One event = one block (`blockProcessed`) or one whole tree (`treeCut`): the Saw fells the
entire tree in a single operation, individual logs are not reported separately. Use
`getLogCount()` to sum logs for quests.

### Example: mine 64 stone with a Drill

```javascript
CRHEvents.blockProcessed('MECHANICAL_DRILL', event => {
    if (event.getBlockId() !== 'minecraft:stone') return;
    const player = event.getOwner();
    if (!player) return;

    const nbt = player.getPersistentData();
    const count = (nbt.contains('crh_drill_stone') ? nbt.getInt('crh_drill_stone') : 0) + 1;

    if (count < 64) {
        nbt.putInt('crh_drill_stone', count);
        return;
    }

    nbt.putInt('crh_drill_stone', 0);
    player.server.runCommandSilent('ftbquests change_progress ' + player.name.string + ' complete <QUEST_ID>');
});
```

### Example: cut 500 logs with a Saw

```javascript
CRHEvents.treeCut('MECHANICAL_SAW', event => {
    const player = event.getOwner();
    if (!player) return;

    const logs = event.getLogCount();
    if (logs <= 0) return; // -1 when felled through Dynamic Trees, size unknown

    const nbt = player.getPersistentData();
    const total = (nbt.contains('crh_saw_logs') ? nbt.getInt('crh_saw_logs') : 0) + logs;

    if (total < 500) {
        nbt.putInt('crh_saw_logs', total);
        return;
    }

    nbt.putInt('crh_saw_logs', 0);
    player.server.runCommandSilent('ftbquests change_progress ' + player.name.string + ' complete <QUEST_ID>');
});
```

## Metadata keys

| Key | Type | Description |
|---|---|---|
| `createrecipehooks:owner_uuid` | `String` | Player UUID (see the sources table). Absent when the machine was placed by a non-player (piston, command) or the item came from automation |

The owner UUID is written into the block entity's NBT when a player places the block, and survives world reloads. For the Crushing Wheel the UUID is taken from the `ItemEntity` (whoever dropped the item).

## Script examples (KubeJS)

CRH registers its own KubeJS event group, `CRHEvents.recipeFinished`, **identical to the Forge
version**: the same scripts work on both loaders without changes. The first argument is an
optional source filter; the event object resolves the attributed player for you via
`event.getOwner()`. `getSource()` and `getRecipeId()` return plain strings.
Place scripts in `kubejs/server_scripts/`. Ready-made test scripts for every machine live
in the repository's `script_test/` folder.

### 1. Basic: an emerald for any recipe of a machine

```javascript
CRHEvents.recipeFinished('MECHANICAL_PRESS', event => {
    const player = event.getOwner(); // null when not attributed or the player is offline
    if (!player) return;

    player.server.runCommandSilent('give ' + player.name.string + ' minecraft:emerald 1');
});
```

The source filter matches exactly one source name. To catch all 4 Fan types, subscribe without
the filter and check the prefix:

```javascript
CRHEvents.recipeFinished(event => {
    if (!event.getSource().startsWith('FAN_')) return;
    // ...
});
```

### 2. Filtering by a specific recipe

```javascript
CRHEvents.recipeFinished('SPOUT_FILLING', event => {
    if (event.getRecipeId() !== 'create:filling/honey_bottle') return;

    // ... award the reward as in example 1
});
```

### 3. Counting batch quantity (Crushing Wheel)

A single batch may contain multiple items, so sum `getCount()`:

```javascript
CRHEvents.recipeFinished('CRUSHING_WHEEL', event => {
    const BuiltInRegistries = Java.loadClass('net.minecraft.core.registries.BuiltInRegistries');
    const outputs = event.getItemOutputs();

    let total = 0;
    for (let i = 0; i < outputs.size(); i++) {
        const stack = outputs.get(i);
        const key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key && key.toString() === 'create:crushed_raw_iron') total += stack.getCount();
    }
    if (total < 8) return;

    const player = event.getOwner();
    if (!player) return;
    player.server.runCommandSilent('ftbquests change_progress ' + player.name.string + ' complete <QUEST_ID>');
});
```

### 4. Accumulating a counter across events (via player NBT)

The Press processes 1 item per event. The counter is stored in `player.getPersistentData()`, which survives server restarts:

```javascript
CRHEvents.recipeFinished('MECHANICAL_PRESS', event => {
    if (event.getRecipeId() !== 'create:pressing/iron_ingot') return;

    const player = event.getOwner();
    if (!player) return;

    const nbt = player.getPersistentData();
    const count = (nbt.contains('crh_press_iron') ? nbt.getInt('crh_press_iron') : 0) + 1;

    if (count < 4) {
        nbt.putInt('crh_press_iron', count);
        return;
    }

    nbt.putInt('crh_press_iron', 0);
    player.server.runCommandSilent('ftbquests change_progress ' + player.name.string + ' complete <QUEST_ID>');
});
```

## Scripting pitfalls

- **Do not declare `const`/`let` at the top level of a file.** KubeJS loads all server_scripts into one shared scope, so identical names in two files cause `TypeError: redeclaration of const`. Keep all declarations inside the callback.
- **`getItemOutputs()` is a `java.util.List`**, not a JS array. Iterate via `.size()` / `.get(i)`.
- **JS variables do not persist across events.** A `new Map()` at file level will not keep your counter; use `player.getPersistentData()` (example 4).
- **`getOwner()` may be `null`.** The machine may have been placed by a non-player, an item in the Crushing Wheel may have come from a hopper, or the player may be offline. Always check. Use `getOwnerUuid()` when you need the attribution string even for offline players.
- **`getRecipeId()` may be `null`** for Spout capability fills, Item Drain capability/potion emptying, so null-check before use.
- FTB Quests command: `ftbquests change_progress <player> complete <quest_id>`.

## Usage from Java

```java
CreateRecipeFinishedCallback.EVENT.register(ctx -> {
    if (ctx.getSource() == RecipeSource.MILLSTONE) {
        // ctx.getRecipeId(), ctx.getItemOutputs(), ctx.getBlockPos(), ...
    }
});

CreateBlockProcessedCallback.EVENT.register(ctx -> {
    // ctx.getBlockId(), ctx.isContraption(), ctx.getMetadata()
});

CreateTreeCutCallback.EVENT.register(ctx -> {
    // ctx.getLogCount(), ctx.getLeafCount()
});
```

Public API: `dev.createrecipehooks.api` (`RecipeFinishedContext`, `BlockProcessedContext`, `RecipeSource`, `FluidAmount`, `CreateRecipeHooks.register(...)`, `CreateRecipeHooks.registerBlockProcessed(...)`, `CreateRecipeHooks.registerTreeCut(...)`) plus the callbacks in `dev.createrecipehooks.fabric`. The `RecipeSource` enum is stable: values are never removed between minor versions; new ones may be added.

### Notes for mods depending on CRH

- **Events fire on the logical server only.** In singleplayer that is the integrated server, so everything works there identically; nothing special is needed. If your mod wants to show something on the client (HUD, toasts, particles) in response to an event, send your own packet: CRH provides no networking.
- **Listeners are synchronous** and run on the server tick thread. Return promptly; for heavy work (databases, HTTP) hand the data off to your own worker thread. See the threading notes in the `RecipeFinishedContext` javadoc.
- **Declaring the dependency**: add CRH to the `depends` block of your `fabric.mod.json` as usual. The jar loads harmlessly on clients, so a regular dependency is fine on both sides.
- **License**: linking against the API imposes no obligations, your mod can use any license (LGPL linking exception). Only forks and derivatives of CRH itself must remain LGPL.

## License

Copyright (c) 2026 Aarow

LGPL-3.0-or-later. You may use this mod in modpacks and link against its API freely.
Forks and derivative mods must remain open source under the same license with attribution.
See [LICENSE](../LICENSE) (LGPL-3.0) and [COPYING](../COPYING) (GPL-3.0).
