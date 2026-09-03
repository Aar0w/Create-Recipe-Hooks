# Create Recipe Hooks (CRH)

A server-side API mod for **Minecraft 1.20.1** that fires a unified event whenever a recipe completes in any **Create** machine, and whenever a Create machine processes a world block (Drill mining, Harvester reaping, Saw felling trees). Lets quests, KubeJS scripts, and other mods react to "player smelted / crushed / mined X", with player attribution via UUID.

Available for both loaders as separate builds with identical behavior and an identical scripting API:

| Loader | Project folder | Create version | Documentation |
|---|---|---|---|
| **Forge** 47+ (also runs on NeoForge 1.20.1) | [1.20.1/](1.20.1/) | Create 6.0.8+ | [Forge README](1.20.1/README.md) |
| **Fabric** 0.15+ | [1.20.1-fabric/](1.20.1-fabric/) | Create Fabric 6.0.8.1+ | [Fabric README](1.20.1-fabric/README.md) |

## Highlights

- One event per recipe completion, 16+ tracked sources: Basin, Press, Millstone, Crushing Wheels, Saw, Crafter, Deployer, all 4 Fan types, Spout, Item Drain, Sand Paper, Sequenced Assembly, and more
- **Block processing events**: `blockProcessed` (Mechanical Drill, Mechanical Harvester, lone Saw cuts) and `treeCut` (Saw felling a whole tree, with exact log and leaf counts), both stationary and on contraptions
- **Player attribution**: events carry the UUID of whoever placed the machine (or threw the item, for Crushing Wheels), persisted across restarts and carried along inside contraptions
- **Native KubeJS integration**: `CRHEvents.recipeFinished('MILLSTONE', event => ...)` with built-in owner resolution; the same scripts run on Forge and Fabric
- Server-side only: not required on clients
- Ready-made test scripts for every machine in [script_test/](script_test/)

## Quick taste (KubeJS)

```javascript
CRHEvents.recipeFinished('MILLSTONE', event => {
    const player = event.getOwner();
    if (!player) return;
    player.server.runCommandSilent('give ' + player.name.string + ' minecraft:emerald 1');
});
```

Full event reference, source tables, metadata keys, Java API and scripting pitfalls are in the per-loader READMEs linked above.

## License

Copyright (c) 2026 Aarow

LGPL-3.0-or-later. Modpacks and API consumers are unrestricted; forks and derivative mods must
remain open source under the same license with attribution.
See [LICENSE](LICENSE) (LGPL-3.0) and [COPYING](COPYING) (GPL-3.0).
