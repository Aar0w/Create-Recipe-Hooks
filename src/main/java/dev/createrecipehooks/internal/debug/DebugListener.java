package dev.createrecipehooks.internal.debug;

import dev.createrecipehooks.CreateRecipeHooksMod;
import dev.createrecipehooks.api.IRecipeFinishedListener;
import dev.createrecipehooks.api.RecipeFinishedContext;

/**
 * Temporary debug listener that logs every recipe completion.
 *
 * <p>Enable with JVM argument: {@code -Dcrh.debug=true}
 *
 * <h2>Manual Test Checklist</h2>
 * Enable debug logging and verify each source appears in the log with correct data.
 *
 * <h3>Test 1 — BASIN (Mixing)</h3>
 * <pre>
 * Setup: Mechanical Mixer above a Basin with 2x Iron Ingot
 * Recipe: create:mixing/iron_nugget (or any mixing recipe)
 * Expected log:
 *   [CRH DEBUG] source=BASIN recipeId=create:mixing/... blockPos=x,y,z
 *   itemOutputs: [iron_nugget x9]
 *   fluidOutputs: []
 * </pre>
 *
 * <h3>Test 2 — CEI Infuser (fires as BASIN)</h3>
 * <pre>
 * Setup: CEI Infuser above Basin with applicable recipe
 * Expected log: source=BASIN (Infuser uses BasinRecipe.apply)
 * </pre>
 *
 * <h3>Test 3 — CRUSHING_WHEEL</h3>
 * <pre>
 * Setup: Two Crushing Wheels + iron ore dropped between them
 * Expected log:
 *   source=CRUSHING_WHEEL recipeId=create:crushing/iron_ore
 *   itemOutputs: [crushed_iron_ore x1, gravel x1 (chance)]
 * </pre>
 *
 * <h3>Test 4 — FAN_BLASTING / FAN_HAUNTING / FAN_SPLASHING / FAN_SMOKING</h3>
 * <pre>
 * Setup: Encased Fan blowing over lava (blasting) / soul fire (haunting) /
 *        water (splashing) / campfire (smoking)
 * Drop appropriate items into the fan air stream.
 * Expected log: source=FAN_BLASTING (or relevant fan type)
 *   itemInputs: [iron_ore x1], itemOutputs: [iron_ingot x1]
 * </pre>
 *
 * <h3>Test 5 — MECHANICAL_PRESS (belt/world)</h3>
 * <pre>
 * Setup: Mechanical Press above a belt; place iron ingot on belt
 * Recipe: create:pressing/iron_sheet
 * Expected log:
 *   source=MECHANICAL_PRESS recipeId=create:pressing/iron_sheet
 *   itemOutputs: [iron_sheet x1]
 * IMPORTANT: This test verifies fix #1 — previously returned Source=UNKNOWN.
 * </pre>
 *
 * <h3>Test 6 — SEQUENCED_ASSEMBLY</h3>
 * <pre>
 * Setup: Standard SA line (Press + Deployer + Saw)
 * Expected log: source=SEQUENCED_ASSEMBLY fires exactly ONCE at final step.
 *   Intermediate steps (transitional items) must NOT appear in log.
 * Verify: place a stack in the SA line; count log entries = 1 per completed item.
 * </pre>
 *
 * <h3>Test 7 — SAND_PAPER (direct use)</h3>
 * <pre>
 * Setup: Hold Sand Paper; right-click on a polishable item in inventory
 * Expected log:
 *   source=SAND_PAPER player=PlayerName (real player, not FakePlayer)
 * IMPORTANT: This test verifies fix #7 — previously missing for hand use.
 * </pre>
 *
 * <h3>Test 8 — SPOUT_FILLING</h3>
 * <pre>
 * Setup: Spout above belt; Spout tank contains Water; Dirt on belt
 * Recipe: create:filling/... or any mod filling recipe
 * Expected log:
 *   source=SPOUT_FILLING recipeId=...
 *   itemOutputs: [filled item], itemInputs: [dirt]
 * IMPORTANT: This test verifies fix #2 — tryFill() was wrong target.
 * </pre>
 *
 * <h3>Test 9 — ITEM_DRAIN_EMPTYING</h3>
 * <pre>
 * Setup: Item Drain below belt; place Water Bucket on belt
 * Expected log:
 *   source=ITEM_DRAIN_EMPTYING
 *   fluidOutputs: [water 1000mb]
 *   itemOutputs: [bucket]
 * </pre>
 *
 * <h3>Negative tests (must NOT appear)</h3>
 * <ul>
 *   <li>Intermediate SA steps: insert transitional item into Press — no SEQUENCED_ASSEMBLY log.</li>
 *   <li>Client-side: log must never appear when playing in single-player from the client thread.</li>
 *   <li>simulate=true calls: no event during Basin match-checking phase.</li>
 * </ul>
 *
 * <p><strong>Internal — not part of the public API. May change without notice.</strong>
 */
public final class DebugListener implements IRecipeFinishedListener {

    @Override
    public void onRecipeFinished(RecipeFinishedContext ctx) {
        CreateRecipeHooksMod.LOGGER.info(
            "[CRH DEBUG] source={} recipeId={} blockPos={} player={} outputs={} inputs={}",
            ctx.getSource(),
            ctx.getRecipeId(),
            ctx.getBlockPos(),
            ctx.getPlayer() != null ? ctx.getPlayer().getScoreboardName() : "none",
            ctx.getItemOutputs().size(),
            ctx.getItemInputs().size()
        );
        CrhDebugLogger.log(ctx);
    }
}
