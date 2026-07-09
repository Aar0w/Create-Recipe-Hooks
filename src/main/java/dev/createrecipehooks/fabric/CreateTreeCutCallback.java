package dev.createrecipehooks.fabric;

import dev.createrecipehooks.api.BlockProcessedContext;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

// Fabric callback fired when a Mechanical Saw fells a whole tree: one event per tree, with log and leaf counts.
// Lone blocks fire CreateBlockProcessedCallback instead, never both. Server side only, not cancellable.
@FunctionalInterface
public interface CreateTreeCutCallback {

    Event<CreateTreeCutCallback> EVENT = EventFactory.createArrayBacked(
        CreateTreeCutCallback.class,
        listeners -> context -> {
            for (CreateTreeCutCallback listener : listeners) {
                listener.onTreeCut(context);
            }
        }
    );

    void onTreeCut(BlockProcessedContext context);
}
