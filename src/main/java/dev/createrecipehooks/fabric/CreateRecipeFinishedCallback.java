package dev.createrecipehooks.fabric;

import dev.createrecipehooks.api.RecipeFinishedContext;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fabric-side public event — the counterpart of the Forge {@code CreateRecipeFinishedEvent}.
 *
 * <p>Usage from another mod:
 * <pre>{@code
 * CreateRecipeFinishedCallback.EVENT.register(ctx -> {
 *     if (ctx.getSource() == RecipeSource.MILLSTONE) { ... }
 * });
 * }</pre>
 *
 * <p>Fired on the server tick thread only, after the recipe has been applied.
 * Not cancellable.
 */
@FunctionalInterface
public interface CreateRecipeFinishedCallback {

    Event<CreateRecipeFinishedCallback> EVENT = EventFactory.createArrayBacked(
        CreateRecipeFinishedCallback.class,
        listeners -> context -> {
            for (CreateRecipeFinishedCallback listener : listeners) {
                listener.onRecipeFinished(context);
            }
        }
    );

    void onRecipeFinished(RecipeFinishedContext context);
}
