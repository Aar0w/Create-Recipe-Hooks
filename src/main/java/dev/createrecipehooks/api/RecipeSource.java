package dev.createrecipehooks.api;

// Which machine produced the event. Entries are never removed, new ones may be added;
// reserved entries fire nothing yet.
public enum RecipeSource {

    // Basin recipes: Mixer, Compactor, pressing on a Basin, and any addon machine that goes through BasinRecipe.apply() (for example the CEI Infuser).
    BASIN,

    // Mechanical Press in belt and world modes (non-basin pressing).
    MECHANICAL_PRESS,

    // Millstone milling recipes.
    MILLSTONE,

    // Crushing Wheels crushing recipes.
    CRUSHING_WHEEL,

    // Mechanical Saw. Used by two event families: recipeFinished for the upward-facing saw's cutting and stonecutting recipes, blockProcessed and treeCut for the horizontal world-cutting saw.
    MECHANICAL_SAW,

    // Mechanical Crafter results, both Create's mechanical crafting and vanilla crafting.
    MECHANICAL_CRAFTER,

    // Deployer applying a recipe (deploying and item-application recipe types).
    // getPlayer() is null; attribution comes from the Deployer's owner metadata.
    DEPLOYER_BELT,

    // RESERVED, no events fired: all Deployer processing currently arrives as DEPLOYER_BELT.
    DEPLOYER_DIRECT,

    // RESERVED, no events fired: manual item application currently arrives as DEPLOYER_BELT.
    MANUAL_APPLICATION,

    // Sand Paper polishing. On manual use getPlayer() returns the real player;
    // on belt use (via Deployer) it is null.
    SAND_PAPER,

    // Final step of a Sequenced Assembly producing the finished item.
    // Intermediate steps do not fire events.
    SEQUENCED_ASSEMBLY,

    // Fan with soul fire (haunting recipes).
    FAN_HAUNTING,

    // Fan with water (splashing recipes).
    FAN_SPLASHING,

    // Fan with lava or blaze burner. Covers both vanilla smelting and blasting recipe types;
    // use RecipeFinishedContext#getRecipe() to distinguish them.
    FAN_BLASTING,

    // Fan with fire (vanilla smoking recipes).
    FAN_SMOKING,

    // Spout filling an item. getRecipeId() is null when filling goes through the fluid capability instead of a FillingRecipe (buckets and similar containers).
    SPOUT_FILLING,

    // Item Drain emptying a container. getRecipeId() is null for capability emptying (buckets) and potions.
    ITEM_DRAIN_EMPTYING,

    // RESERVED, no events fired: no hook exists for the CEI Grindstone yet.
    CEI_GRINDSTONE,

    // Printer from Create Enchantment Industry, fired only when CEI is installed.
    // getRecipeId() is null: the Printer does not use a vanilla recipe object.
    CEI_PRINTER,

    // RESERVED, no events fired: CEI Infuser events arrive as BASIN.
    CEI_INFUSER,

    // RESERVED, no events fired: CEI Salvaging events arrive as FAN_*.
    CEI_SALVAGING,

    // RESERVED, no events fired: no hook exists for PowerGrid yet.
    POWERGRID_MAGNETIZING,

    // Mechanical Drill, blockProcessed events only: one event per broken block, both stationary and on contraptions (BlockProcessedContext#isContraption()).
    MECHANICAL_DRILL,

    // Mechanical Harvester, blockProcessed events only: one event per harvested plant.
    // The Harvester only operates on contraptions.
    MECHANICAL_HARVESTER,

    // Catch-all for unrecognized addon recipes going through RecipeApplier.
    UNKNOWN
}
