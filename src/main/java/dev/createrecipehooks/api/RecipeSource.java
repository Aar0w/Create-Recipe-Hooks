package dev.createrecipehooks.api;

/**
 * Identifies which Create machine or mechanic produced a completed recipe.
 *
 * <p>Sources prefixed with a mod name (e.g. {@code CEI_}, {@code POWERGRID_}) are
 * only fired when that addon is present. Core Create sources are always available.
 *
 * <p>This enum is part of the stable public API and will not have entries removed
 * between minor versions. New entries may be added in minor versions.
 *
 * <h3>Source classification</h3>
 * <p>Each entry in this enum falls into one of four categories:
 * <ul>
 *   <li><strong>FACTUAL ORIGIN</strong> — unambiguously identifies a single physical machine
 *       or player action. Example: {@link #MILLSTONE} always means the Millstone block.</li>
 *   <li><strong>TAXONOMY LABEL</strong> — categorizes by recipe type or shared code path,
 *       but may cover multiple machines. Example: {@link #BASIN} fires for the Mixer,
 *       Compactor, Basin-Pressing, and any addon machine using {@code BasinRecipe.apply()}.
 *       Example: {@link #FAN_BLASTING} covers both smelting and blasting recipe types.</li>
 *   <li><strong>RESERVED</strong> — entry exists in the API but no mixin fires it yet.
 *       Filtering on a RESERVED source will never match any event in v1.</li>
 *   <li><strong>UNREACHABLE (v1)</strong> — entry exists in the API but is structurally
 *       unreachable: no code path in v1 produces this value. These entries are placeholders
 *       for distinctions that were intended but not yet implemented. Will be unreachable
 *       until a dedicated mixin or hook is added.</li>
 * </ul>
 * <p>Classification is documented per-entry in the javadoc tag line.
 */
public enum RecipeSource {

    // ------------------------------------------------------------------ //
    //  Create — Basin stack                                                //
    //  BasinRecipe.apply() → covers Mixer, Compactor, Basin-Pressing,     //
    //  and any addon machine that calls BasinRecipe.apply().              //
    // ------------------------------------------------------------------ //

    /**
     * Mixing, compacting, and basin-pressing recipes — <strong>TAXONOMY LABEL</strong>.
     *
     * <p>Fired by {@code BasinRecipe.apply()}, which is shared by:
     * <ul>
     *   <li>Mechanical Mixer (mixing recipes, potion mixing)</li>
     *   <li>Mechanical Compactor (compacting recipes)</li>
     *   <li>Basin with Mechanical Press (pressing recipes applied to basin contents)</li>
     *   <li>Any addon machine that delegates to {@code BasinRecipe.apply()},
     *       e.g. CEI Infuser, PowerGrid machines (events arrive here, not as their own source)</li>
     * </ul>
     * <p>Use {@link RecipeFinishedContext#getRecipe()} to distinguish sub-types by recipe class.
     */
    BASIN,

    // ------------------------------------------------------------------ //
    //  Create — Individual machines                                        //
    // ------------------------------------------------------------------ //

    /** Mechanical Press in belt/world mode (non-basin pressing) — <strong>FACTUAL ORIGIN</strong>. */
    MECHANICAL_PRESS,

    /** Millstone (milling recipes) — <strong>FACTUAL ORIGIN</strong>. */
    MILLSTONE,

    /** Crushing Wheel (crushing and milling recipes) — <strong>FACTUAL ORIGIN</strong>. */
    CRUSHING_WHEEL,

    /** Mechanical Saw (cutting, stonecutting, sequenced cutting) — <strong>FACTUAL ORIGIN</strong>. */
    MECHANICAL_SAW,

    /** Mechanical Crafter (Create crafting + vanilla crafting if enabled) — <strong>FACTUAL ORIGIN</strong>. */
    MECHANICAL_CRAFTER,

    // ------------------------------------------------------------------ //
    //  Create — Deployer family                                            //
    // ------------------------------------------------------------------ //

    /**
     * Deployer applying a recipe to a belt item — <strong>TAXONOMY LABEL</strong>.
     *
     * <p>Covers {@code DeployingRecipe} and {@code ItemApplicationRecipe} types processed
     * via {@code RecipeApplier}. Both automated Deployer use and manual player
     * item-application (when using the same recipe types) map to this source.
     *
     * <p><strong>player is {@code null} in v1.</strong> The Deployer's
     * {@code DeployerFakePlayer} is not captured by the current mixin hook.
     * Always check {@link RecipeFinishedContext#getPlayer()} for {@code null}.
     */
    DEPLOYER_BELT,

    /**
     * Deployer in direct/world mode — <strong>UNREACHABLE in v1</strong>.
     *
     * <p>This entry is a placeholder for a planned distinction between belt-mode and
     * direct-world-mode Deployer use. The current {@code resolveSource()} implementation
     * in {@code MixinRecipeApplier} maps all Deployer processing to {@link #DEPLOYER_BELT}.
     * No event will ever carry this source value until a dedicated code path is added.
     *
     * @deprecated Not fired in v1. Do not filter on this value.
     */
    DEPLOYER_DIRECT,

    /**
     * Player manual item application — <strong>UNREACHABLE in v1</strong>.
     *
     * <p>This entry was intended for player-initiated {@code ItemApplicationRecipe} use
     * (right-clicking with a matching item). In the current implementation,
     * {@code ItemApplicationRecipe} type is mapped to {@link #DEPLOYER_BELT} by
     * {@code resolveSource()} in {@code MixinRecipeApplier}, regardless of whether
     * a player or a Deployer triggered it.
     * No event will ever carry this source value until a dedicated hook is added.
     *
     * @deprecated Not fired in v1. Do not filter on this value.
     */
    MANUAL_APPLICATION,

    /**
     * Sand Paper polishing — <strong>FACTUAL ORIGIN</strong>.
     *
     * <p>Fired from two code paths:
     * <ul>
     *   <li>Direct player use ({@code SandPaperItem.finishUsingItem}) —
     *       {@link RecipeFinishedContext#getPlayer()} returns the real {@code ServerPlayer}.</li>
     *   <li>Deployer belt use (via {@code RecipeApplier}, same as {@link #DEPLOYER_BELT}) —
     *       {@link RecipeFinishedContext#getPlayer()} is {@code null}.</li>
     * </ul>
     * <p>Always check {@link RecipeFinishedContext#getPlayer()} for {@code null} before use.
     */
    SAND_PAPER,

    // ------------------------------------------------------------------ //
    //  Create — Sequenced Assembly                                         //
    // ------------------------------------------------------------------ //

    /**
     * Sequenced Assembly final step — <strong>FACTUAL ORIGIN</strong>.
     *
     * <p>Fired once when the <em>final</em> step of a Sequenced Assembly completes and
     * the assembled item is produced. Intermediate steps (transitional items) do not fire
     * this event.
     */
    SEQUENCED_ASSEMBLY,

    // ------------------------------------------------------------------ //
    //  Create — Fan processing                                             //
    // ------------------------------------------------------------------ //

    /** Fan Haunting (haunting recipes) — <strong>TAXONOMY LABEL</strong> (recipe type). */
    FAN_HAUNTING,

    /** Fan Splashing (splashing recipes) — <strong>TAXONOMY LABEL</strong> (recipe type). */
    FAN_SPLASHING,

    /**
     * Fan Blasting — <strong>TAXONOMY LABEL</strong> (recipe type).
     *
     * <p>Covers both vanilla {@code SmeltingRecipe} and {@code BlastingRecipe} processed
     * by any Fan with a heat source (lava, blaze burner). Use
     * {@link RecipeFinishedContext#getRecipe()} to distinguish the two recipe types.
     */
    FAN_BLASTING,

    /**
     * Fan Smoking — <strong>TAXONOMY LABEL</strong> (recipe type).
     *
     * <p>The underlying recipe is always vanilla {@code SmokingRecipe}.
     */
    FAN_SMOKING,

    // ------------------------------------------------------------------ //
    //  Create — Fluid machines                                             //
    // ------------------------------------------------------------------ //

    /** Spout filling an item (FillingBySpout / FillingRecipe) — <strong>FACTUAL ORIGIN</strong>. */
    SPOUT_FILLING,

    /** Item Drain emptying a fluid container (EmptyingRecipe or fluid capability) — <strong>FACTUAL ORIGIN</strong>. */
    ITEM_DRAIN_EMPTYING,

    // ------------------------------------------------------------------ //
    //  Create: Enchantment Industry (CEI)                                  //
    //  CEI_PRINTER: active (fires when CEI is installed).                  //
    //  All other CEI entries are Reserved — no events in v1.              //
    // ------------------------------------------------------------------ //

    /**
     * CEI Grindstone drain (grinding + sandpaper polishing).
     *
     * <p><strong>Reserved — no events fired in v1.</strong>
     * No mixin hook exists yet for CEI Grindstone. Filtering on this source
     * will never match any event until explicit support is added.
     */
    CEI_GRINDSTONE,

    /**
     * CEI Printer (printing / enchantment-copy recipes) — <strong>FACTUAL ORIGIN</strong>.
     *
     * <p>Fired when the Printer completes a copy cycle: the countdown expires, all
     * preconditions pass (valid entry, correct ink, sufficient quantity), and
     * {@code Printing.print()} produces a non-empty output stack.
     *
     * <p>Only fired when Create Enchantment Industry is installed. Silently inactive
     * without CEI ({@code @Mixin require = 0}).
     *
     * <p>{@link RecipeFinishedContext#getItemOutputs()} — the printed output item (1 entry).<br>
     * {@link RecipeFinishedContext#getItemInputs()} — the consumed belt item (1 entry).<br>
     * {@link RecipeFinishedContext#getBlockPos()} — the Printer block position.<br>
     * {@link RecipeFinishedContext#getRecipe()} / {@link RecipeFinishedContext#getRecipeId()} — {@code null};
     * CEI Printer does not use a vanilla {@link net.minecraft.world.item.crafting.Recipe} object.
     */
    CEI_PRINTER,

    /**
     * CEI Infuser — inherits {@link #BASIN} because the Infuser uses BasinRecipe.apply().
     *
     * <p>Events from the CEI Infuser arrive with source {@link #BASIN}, not this entry.
     * This entry is reserved for a future finer-grained distinction if CEI Infuser
     * receives its own dedicated hook.
     *
     * <p><strong>Reserved — no events fired in v1 with this source value.</strong>
     */
    CEI_INFUSER,

    /**
     * CEI Salvaging fan processing type.
     *
     * <p>Salvaging events currently arrive via {@link #FAN_BLASTING} or
     * {@link #FAN_SPLASHING} depending on the catalyst used.
     * This entry is reserved for explicit future support.
     *
     * <p><strong>Reserved — no events fired in v1 with this source value.</strong>
     */
    CEI_SALVAGING,

    // ------------------------------------------------------------------ //
    //  PowerGrid                                                           //
    // ------------------------------------------------------------------ //

    /**
     * PowerGrid electromagnet magnetizing recipes.
     *
     * <p><strong>Reserved — no events fired in v1.</strong>
     * No mixin hook exists yet for PowerGrid. Filtering on this source
     * will never match any event until explicit support is added.
     */
    POWERGRID_MAGNETIZING,

    // ------------------------------------------------------------------ //
    //  Fallback                                                            //
    // ------------------------------------------------------------------ //

    /**
     * Catch-all for any recipe applied through {@code RecipeApplier.applyRecipeOn()}
     * whose source could not be determined (e.g. unknown addons).
     */
    UNKNOWN
}
