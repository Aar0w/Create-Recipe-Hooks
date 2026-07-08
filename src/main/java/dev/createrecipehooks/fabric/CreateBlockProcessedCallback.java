package dev.createrecipehooks.fabric;

import dev.createrecipehooks.api.BlockProcessedContext;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

// Fabric callback fired when a Drill breaks a block, a Harvester cuts a plant, or a Saw
// cuts a lone block that is not part of a tree. Tree felling fires CreateTreeCutCallback
// instead, never both. Server side only, not cancellable.
@FunctionalInterface
public interface CreateBlockProcessedCallback {

    Event<CreateBlockProcessedCallback> EVENT = EventFactory.createArrayBacked(
        CreateBlockProcessedCallback.class,
        listeners -> context -> {
            for (CreateBlockProcessedCallback listener : listeners) {
                listener.onBlockProcessed(context);
            }
        }
    );

    void onBlockProcessed(BlockProcessedContext context);
}
