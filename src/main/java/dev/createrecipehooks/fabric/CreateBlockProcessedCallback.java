package dev.createrecipehooks.fabric;

import dev.createrecipehooks.api.BlockProcessedContext;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fabric-side public event — the counterpart of the Forge {@code CreateBlockProcessedEvent}.
 * Fires once per block broken by a Mechanical Drill, per crop cut by a Mechanical
 * Harvester, and per lone (non-tree) block cut by a Mechanical Saw.
 *
 * <p>Usage from another mod:
 * <pre>{@code
 * CreateBlockProcessedCallback.EVENT.register(ctx -> {
 *     if (ctx.getSource() == RecipeSource.MECHANICAL_DRILL) { ... }
 * });
 * }</pre>
 *
 * <p>Fired on the server tick thread only, after the block is gone. Not cancellable.
 * Tree felling is a separate event: {@link CreateTreeCutCallback}.
 */
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
