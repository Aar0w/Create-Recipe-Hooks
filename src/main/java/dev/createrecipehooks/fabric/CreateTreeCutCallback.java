package dev.createrecipehooks.fabric;

import dev.createrecipehooks.api.BlockProcessedContext;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fabric-side public event — the counterpart of the Forge {@code CreateTreeCutEvent}.
 * Fires once per tree felled by a Mechanical Saw (stationary or contraption actor);
 * the context carries {@link BlockProcessedContext#getLogCount()} and
 * {@link BlockProcessedContext#getLeafCount()}.
 *
 * <p>Mutually exclusive with {@link CreateBlockProcessedCallback}: a saw cut fires
 * exactly one of the two.
 *
 * <p>Fired on the server tick thread only. Not cancellable.
 */
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
