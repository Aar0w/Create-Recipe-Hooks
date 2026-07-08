package dev.createrecipehooks.internal.debug;

import dev.createrecipehooks.CreateRecipeHooksMod;
import dev.createrecipehooks.api.IRecipeFinishedListener;
import dev.createrecipehooks.api.RecipeFinishedContext;

/**
 * Debug listener that logs every recipe completion when the mod is launched with
 * -Dcrh.debug=true. Internal, not part of the public API.
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
