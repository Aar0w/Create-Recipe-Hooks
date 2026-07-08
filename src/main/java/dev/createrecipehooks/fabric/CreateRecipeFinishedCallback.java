package dev.createrecipehooks.fabric;

import dev.createrecipehooks.api.RecipeFinishedContext;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

// Fabric callback fired whenever a Create (or addon) recipe completes on the server.
// Server side only, not cancellable.
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
