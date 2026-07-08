package dev.createrecipehooks.fabric;

import dev.createrecipehooks.api.IRecipeFinishedListener;
import dev.createrecipehooks.api.RecipeFinishedContext;
import dev.createrecipehooks.core.RecipeEventDispatcher;

// Fabric adapter: bridges the dispatcher to the public callbacks.
public final class FabricAdapter implements IRecipeFinishedListener {

    public static final FabricAdapter INSTANCE = new FabricAdapter();

    private FabricAdapter() {}

    @Override
    public void onRecipeFinished(RecipeFinishedContext ctx) {
        CreateRecipeFinishedCallback.EVENT.invoker().onRecipeFinished(ctx);
    }

    // Wires this adapter into the dispatch chain. Called once from the mod entrypoint.
    public static void register() {
        RecipeEventDispatcher.registerListener(INSTANCE);
        RecipeEventDispatcher.registerBlockProcessedListener(
            ctx -> CreateBlockProcessedCallback.EVENT.invoker().onBlockProcessed(ctx));
        RecipeEventDispatcher.registerTreeCutListener(
            ctx -> CreateTreeCutCallback.EVENT.invoker().onTreeCut(ctx));
    }
}
